package com.calebjhunt.familymap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import model.Event;
import model.Person;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

public class MapsFragment extends Fragment {

    private GoogleMap gMap;
    private final List<Polyline> curLines = new ArrayList<>();

    public static final String ARG_EVENT_ID = "argEventID";

    private ImageView icon;
    private TextView  nameDetails;
    private TextView  eventDetails;
    private LinearLayout mapText;

    private Person selectedEventPerson = null;


    private final DataCache cache = DataCache.getInstance();
    private final Map<String, Float> eventTypeToColor = new HashMap<String, Float>(){
        {
            put("death", BitmapDescriptorFactory.HUE_RED);
            put("birth", BitmapDescriptorFactory.HUE_GREEN);
            put("marriage", BitmapDescriptorFactory.HUE_BLUE);
        }
    };

    private final GoogleMap.OnMapLoadedCallback loadedCallback = () -> {
        // You probably don't need this callback. It occurs after onMapReady and I have seen
        // cases where you get an error when adding markers or otherwise interacting with the map in
        // onMapReady(...) because the map isn't really all the way ready. If you see that, just
        // move all code where you interact with the map (everything after
        // map.setOnMapLoadedCallback(...) above) to here.
    };

    private final GoogleMap.OnMarkerClickListener markerClickListener = marker -> {

        Event event = (Event) marker.getTag();
        assert event != null;

        selectEvent(event);
        // false = move to the marker and show info window (not sure what info window is)
        return false;
    };

    private final OnMapReadyCallback callback = googleMap -> {
        googleMap.setOnMapLoadedCallback(loadedCallback);

        this.gMap = googleMap;

        googleMap.setOnMarkerClickListener(markerClickListener);

        // TODO: The selected event should show up on the details TextView too!
        String selectedEventID;
        Bundle args = getArguments();
        if (args == null) {
            selectedEventID = null;
        } else {
            // This could still be null if we give an argument for a different parameter
            selectedEventID = args.getString(ARG_EVENT_ID);
        }

        addMarkers(selectedEventID);
    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        setHasOptionsMenu(true);

        setUpFields(view);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;

        mapFragment.getMapAsync(callback);

        return view;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // The only time we want the search and options menu is on the main activity maps
        if (getActivity() instanceof MainActivity) {
            inflater.inflate(R.menu.main_menu, menu);
            MenuItem search   = menu.findItem(R.id.search);
            MenuItem settings = menu.findItem(R.id.settings);

            search.setIcon(new IconDrawable(getActivity(), FontAwesomeIcons.fa_search)
                    .colorRes(R.color.white)
                    .actionBarSize())

            settings.setIcon(new IconDrawable(getActivity(), FontAwesomeIcons.fa_cog)
                    .colorRes(R.color.white)
                    .actionBarSize());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    private void selectEvent(Event event) {
        clearLines();

        this.selectedEventPerson = cache.getPersonByID(event.getPersonID());

        LatLng location = new LatLng(event.getLatitude(), event.getLongitude());
        gMap.animateCamera(CameraUpdateFactory.newLatLng(location));
        updateEventDetails(event);
        drawLines(event);
    }

    private void updateEventDetails(Event event) {
        updateGenderIcon();
        updateName();
        updateEventTypeAndLocation(event);
    }

    private void clearLines() {
        for (Polyline l : curLines) {
            l.remove();
        }

        curLines.clear();
    }

    private void drawLines(Event event) {
        Person person = cache.getPersonByID(event.getPersonID());
        drawSpouseLine(event);
        drawAncestorLines(event, person, 10);
        drawLifeStoryLines();
    }

    private void drawLifeStoryLines() {
        // eventList should already be in order! It's DataCache's job to order them as they come in.
        List<Event> eventList = cache.getPersonEvents(selectedEventPerson.getPersonID());
        for (int i = 0; i < eventList.size()-1; ++i) {
            Event from = eventList.get(i);
            Event to   = eventList.get(i+1);
            LatLng start = new LatLng(from.getLatitude(), from.getLongitude());
            LatLng end   = new LatLng(to.getLatitude(), to.getLongitude());
            PolylineOptions options = new PolylineOptions()
                    .add(start)
                    .add(end)
                    .color(getResources().getColor(R.color.life_story_line))
                    .width(Math.max(1, 10-i)); // Slight thinning as time goes on
            Polyline line = gMap.addPolyline(options);
            curLines.add(line);
        }
    }

    private void drawAncestorLines(Event event, Person person, int weight) {
        if (person.getFatherID() != null) {
            Event fatherEvent = cache.findEarliestEvent(person.getFatherID());
            if (fatherEvent != null) {
                drawAncestorLine(event, fatherEvent, weight);
                drawAncestorLines(fatherEvent, cache.getPersonByID(person.getFatherID()), Math.max(1, weight-2));
            }
        }
        if (person.getMotherID() != null) {
            Event motherEvent = cache.findEarliestEvent(person.getMotherID());
            if (motherEvent != null) {
                drawAncestorLine(event, motherEvent, weight);
                drawAncestorLines(motherEvent, cache.getPersonByID(person.getMotherID()), Math.max(1, weight-2));
            }
        }
    }

    private void drawAncestorLine(Event from, Event to, int weight) {
        LatLng start = new LatLng(from.getLatitude(), from.getLongitude());
        LatLng end   = new LatLng(to.getLatitude(),   to.getLongitude());

        PolylineOptions options = new PolylineOptions()
                .add(start)
                .add(end)
                .color(getResources().getColor(R.color.ancestor_line))
                .width(weight);
        Polyline line = gMap.addPolyline(options);
        curLines.add(line);
    }

    private void drawSpouseLine(Event event) {
        if (selectedEventPerson.getSpouseID() == null)
            return;

        Event earliestSpouseEvent = cache.findEarliestEvent(selectedEventPerson.getSpouseID());

        if (earliestSpouseEvent == null)
            return;

        LatLng start = new LatLng(event.getLatitude(), event.getLongitude());
        LatLng end   = new LatLng(earliestSpouseEvent.getLatitude(), earliestSpouseEvent.getLongitude());

        PolylineOptions options = new PolylineOptions()
                .add(start)
                .add(end)
                .color(getResources().getColor(R.color.spouse_line))
                .width(10);

        Polyline line = gMap.addPolyline(options);

        this.curLines.add(line);
    }

    private Float findColor(String eventType) {
        Float color;
        if ((color = eventTypeToColor.get(eventType)) == null) {
            color = new Random().nextFloat()*360;
            eventTypeToColor.put(eventType, color);
        }

        return color;
    }

    private void setUpFields(View view) {
        icon         = view.findViewById(R.id.mapTextIcon);
        nameDetails  = view.findViewById(R.id.mapEventPersonName);
        eventDetails = view.findViewById(R.id.mapEventDetails);
        mapText      = view.findViewById(R.id.mapText);
        mapText.setOnClickListener( v -> {
            if (selectedEventPerson == null)
                return;

            Intent intent = new Intent(this.getContext(), PersonActivity.class);
            intent.putExtra(PersonActivity.PERSON_KEY, selectedEventPerson.getPersonID());
            startActivity(intent);
        });
    }

    private void updateGenderIcon() {
        FontAwesomeIcons iconType;
        int genderColor;
        if (selectedEventPerson.getGender().equals("m")) {
            iconType = FontAwesomeIcons.fa_male;
            genderColor = R.color.male_icon;
        } else {
            iconType = FontAwesomeIcons.fa_female;
            genderColor = R.color.female_icon;
        }

        Drawable genderIcon = new IconDrawable(getActivity(), iconType).colorRes(genderColor).sizeDp(40);
        icon.setImageDrawable(genderIcon);
    }

    private void updateName() {
        nameDetails.setText(getString(R.string.fullName, selectedEventPerson.getFirstName(), selectedEventPerson.getLastName()));
    }

    private void updateEventTypeAndLocation(Event event) {
        eventDetails.setText(getString(R.string.eventFormatted,
                event.getEventType().toUpperCase(Locale.ROOT),
                event.getCity(), event.getCountry(), event.getYear()));
    }

    private void addMarkers(String selectedEventID) {
        for (Event e : cache.getEvents()) {
            LatLng location = new LatLng(e.getLatitude(), e.getLongitude());

            Float color = findColor(e.getEventType());

            MarkerOptions options = new MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.defaultMarker(color));

            Marker marker = gMap.addMarker(options);
            if (marker != null)
                marker.setTag(e);

            // Move to the selected event
            if (e.getEventID().equals(selectedEventID)) {
                selectEvent(e);
            }

        }

    }
}