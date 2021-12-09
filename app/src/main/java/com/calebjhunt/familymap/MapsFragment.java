package com.calebjhunt.familymap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.app.Activity;
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

import model.Event;
import model.Person;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

public class MapsFragment extends Fragment {

    private GoogleMap gMap;
    private final List<Polyline> curLines = new ArrayList<>();

    public  static final String ARG_EVENT_ID = "argEventID";
    private static final int   RETURNING_FROM_SETTINGS = 0;

    private ImageView icon;
    private TextView  nameDetails;
    private TextView  eventDetails;
    private LinearLayout mapText;

    private Person selectedEventPerson = null;


    private final DataCache cache = DataCache.getInstance();
    private static final Map<String, Float> eventTypeToColor = new HashMap<>();
    private static final float MARKER_HUE_START = 150f;
    private static final float MARKER_HUE_STEP = 30f;
    private static float currentMarkerHue = MARKER_HUE_START;
    private static final int ANCESTOR_LINE_WEIGHT_MIN = 1;
    private static final int STARTING_ANCESTOR_LINE_WEIGHT = 15;
    private static final int ANCESTOR_LINE_WEIGHT_STEP = 3;
    private static final int GENERIC_LINE_WEIGHT = 10;

    private final GoogleMap.OnMarkerClickListener markerClickListener = marker -> {

        Event event = (Event) marker.getTag();
        if (event != null)
            selectEvent(event);

        return false;
    };

    private final OnMapReadyCallback callback = googleMap -> {
        this.gMap = googleMap;

        googleMap.setOnMarkerClickListener(markerClickListener);

        String selectedEventID;
        Bundle args = getArguments();
        if (args == null) {
            selectedEventID = null;
        } else {
            // This could still be null if we are given a meaningless argument
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
        if (mapFragment != null)
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
                    .actionBarSize());

            settings.setIcon(new IconDrawable(getActivity(), FontAwesomeIcons.fa_cog)
                    .colorRes(R.color.white)
                    .actionBarSize());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.search) {
            Intent intent = new Intent(getContext(), SearchActivity.class);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivityForResult(intent, RETURNING_FROM_SETTINGS);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RETURNING_FROM_SETTINGS) {
                resetFragment();
                if (gMap != null)
                    gMap.clear();

                addMarkers(null);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void resetFragment() {
        this.selectedEventPerson = null;
        this.icon.setImageDrawable(null);
        this.nameDetails.setText("");
        this.eventDetails.setText("");
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
        if (cache.settings.isShowSpouseLines())
            drawSpouseLine(event);

        if (cache.settings.isShowFamilyTreeLines())
        drawFamilyTreeLines(event, person, STARTING_ANCESTOR_LINE_WEIGHT);

        if (cache.settings.isShowLifeStoryLines())
            drawLifeStoryLines();
    }

    private void drawLifeStoryLines() {
        // eventList should already be in order! It's DataCache's job to order them as they come in.
        List<Event> eventList = cache.getPersonEvents(selectedEventPerson.getPersonID());
        for (int i = 0; i < eventList.size()-1; ++i) {
            Event from = eventList.get(i);
            Event to   = eventList.get(i+1);

            // If either event is not within filter, skip it
            if (!(cache.isEventInFilter(from) &&cache.isEventInFilter(to)))
                continue;

            LatLng start = new LatLng(from.getLatitude(), from.getLongitude());
            LatLng end   = new LatLng(to.getLatitude(), to.getLongitude());
            PolylineOptions options = new PolylineOptions()
                    .add(start)
                    .add(end)
                    .color(getResources().getColor(R.color.life_story_line))
                    .width(Math.max(1, GENERIC_LINE_WEIGHT-i)); // Slight thinning as time goes on
            Polyline line = gMap.addPolyline(options);
            curLines.add(line);
        }
    }

    private void drawFamilyTreeLines(Event event, Person person, int weight) {
        if (person.getFatherID() != null) {
            Event fatherEvent = cache.findEarliestFilteredEvent(person.getFatherID());
            if (fatherEvent != null) {
                drawAncestorLine(event, fatherEvent, weight);
                drawFamilyTreeLines(fatherEvent, cache.getPersonByID(person.getFatherID()), Math.max(ANCESTOR_LINE_WEIGHT_MIN, weight - ANCESTOR_LINE_WEIGHT_STEP));
            }
        }
        if (person.getMotherID() != null) {
            Event motherEvent = cache.findEarliestFilteredEvent(person.getMotherID());
            if (motherEvent != null) {
                drawAncestorLine(event, motherEvent, weight);
                drawFamilyTreeLines(motherEvent, cache.getPersonByID(person.getMotherID()), Math.max(ANCESTOR_LINE_WEIGHT_MIN, weight - ANCESTOR_LINE_WEIGHT_STEP));
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

        Event earliestSpouseEvent = cache.findEarliestFilteredEvent(selectedEventPerson.getSpouseID());

        if (earliestSpouseEvent == null)
            return;

        LatLng start = new LatLng(event.getLatitude(), event.getLongitude());
        LatLng end   = new LatLng(earliestSpouseEvent.getLatitude(), earliestSpouseEvent.getLongitude());

        PolylineOptions options = new PolylineOptions()
                .add(start)
                .add(end)
                .color(getResources().getColor(R.color.spouse_line))
                .width(GENERIC_LINE_WEIGHT);

        Polyline line = gMap.addPolyline(options);

        this.curLines.add(line);
    }

    public static void clearColors() {
        eventTypeToColor.clear();
        currentMarkerHue = MARKER_HUE_START;
    }

    private Float findColor(String eventType) {
        Float color;
        if ((color = eventTypeToColor.get(eventType)) == null) {
            nextMarkerHue();

            color = currentMarkerHue;
            eventTypeToColor.put(eventType, color);
        }

        return color;
    }

    private static void nextMarkerHue() {
        currentMarkerHue += MARKER_HUE_STEP;
        if (currentMarkerHue > 359) {
            currentMarkerHue = 0f;
        }
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
        for (Event e : cache.getFilteredEvents()) {
            LatLng location = new LatLng(e.getLatitude(), e.getLongitude());

            Float color = findColor(e.getEventType());

            MarkerOptions options = new MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.defaultMarker(color));

            Marker marker = gMap.addMarker(options);
            if (marker != null)
                marker.setTag(e);

            // Move to the selected event
            if (selectedEventID != null &&
                    e.getEventID().equals(selectedEventID)) {
                selectEvent(e);
            }

        }

    }
}