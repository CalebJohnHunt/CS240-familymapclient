package com.calebjhunt.familymap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import model.Event;
import model.Person;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

public class MapsFragment extends Fragment {

    public static final String ARG_EVENT_ID = "argPersonID";

    private ImageView icon;
    private TextView  nameDetails;
    private TextView  eventDetails;


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

        Person person = cache.getPersonByID(event.getPersonID());

        updateGenderIcon(person.getGender());
        updateName(person.getFirstName(), person.getLastName());
        updateEventDetails(event);

        // false = move to the marker and show info window
        return false;
    };

    private final OnMapReadyCallback callback = googleMap -> {
        googleMap.setOnMapLoadedCallback(loadedCallback);

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


        for (Event e : cache.getEvents()) {
            LatLng location = new LatLng(e.getLatitude(), e.getLongitude());

            Float color = findColor(e.getEventType());

            MarkerOptions options = new MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.defaultMarker(color));

            Marker marker = googleMap.addMarker(options);
            if (marker != null)
                marker.setTag(e);

            // Move to the selected event
            if (e.getEventID().equals(selectedEventID)) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(location));
            }

        }
    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        setUpFields(view);

        Bundle args = getArguments();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;

        mapFragment.getMapAsync(callback);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
    }

    private void updateGenderIcon(String gender) {
        FontAwesomeIcons iconType;
        int genderColor;
        if (gender.equals("m")) {
            iconType = FontAwesomeIcons.fa_male;
            genderColor = R.color.male_icon;
        } else {
            iconType = FontAwesomeIcons.fa_female;
            genderColor = R.color.female_icon;
        }

        Drawable genderIcon = new IconDrawable(getActivity(), iconType).colorRes(genderColor).sizeDp(40);
        icon.setImageDrawable(genderIcon);
    }

    private void updateName(String firstName, String lastName) {
        nameDetails.setText(getString(R.string.fullName, firstName, lastName));
    }

    private void updateEventDetails(Event event) {
        eventDetails.setText(getString(R.string.mapEventDetailsFormatted, event.getEventType().toUpperCase(Locale.ROOT), event.getCity(), event.getCountry(), event.getYear()));
    }
}