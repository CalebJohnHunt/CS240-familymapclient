package com.calebjhunt.familymap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import java.util.List;
import java.util.Locale;

import model.Event;
import model.Person;

public class PersonActivity extends AppCompatActivity {

    private final DataCache cache = DataCache.getInstance();

    protected Person person;

    public static final String PERSON_KEY = "PERSON_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);

        Intent intent = getIntent();
        String personID = intent.getStringExtra(PERSON_KEY);

        initializePersonDetails(personID);

        ExpandableListView expandableListView = findViewById(R.id.personExpandableList);

        expandableListView.setAdapter(new ExpandableListAdapter(cache.getFamily(personID), cache.getFilteredPersonEvents(personID)));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|
                    Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        return true;
    }

    private void initializePersonDetails(String personID) {

        this.person = cache.getPersonByID(personID);

        TextView firstNameView = findViewById(R.id.personFirstName);
        TextView lastNameView  = findViewById(R.id.personLastName);
        TextView genderView    = findViewById(R.id.personGender);
        firstNameView.setText(person.getFirstName());
        lastNameView.setText(person.getLastName());
        if (person.getGender().equals("m")) {
            genderView.setText(getString(R.string.male));
        } else {
            genderView.setText(getString(R.string.female));
        }
    }

    private class ExpandableListAdapter extends BaseExpandableListAdapter {

        private static final int PEOPLE_GROUP_POSITION = 0;
        private static final int EVENTS_GROUP_POSITION = 1;

        private final List<Person> people;
        private final List<Event>  events;

        ExpandableListAdapter(List<Person> people, List<Event> events) {
            this.people = people;
            this.events = events;
        }

        @Override
        public int getGroupCount() {
            return 2;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            switch (groupPosition) {
                case PEOPLE_GROUP_POSITION:
                    return this.people.size();
                case EVENTS_GROUP_POSITION:
                    return this.events.size();
                default:
                    throw new IllegalArgumentException("Unrecognized group position: " + groupPosition);

            }
        }

        @Override
        public Object getGroup(int groupPosition) {
            switch (groupPosition) {
                case PEOPLE_GROUP_POSITION:
                    return getString(R.string.familyTitle);
                case EVENTS_GROUP_POSITION:
                    return getString(R.string.lifeEventsTitle);
                default:
                    throw new IllegalArgumentException("Unrecognized group position: " + groupPosition);

            }
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            switch (groupPosition) {
                case PEOPLE_GROUP_POSITION:
                    return this.people.get(childPosition);
                case EVENTS_GROUP_POSITION:
                    return this.events.get(childPosition);
                default:
                    throw new IllegalArgumentException("Unrecognized group position: " + groupPosition);

            }
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.person_list_group, parent, false);
            }

            TextView title = convertView.findViewById(R.id.personListTitle);

            switch (groupPosition) {
                case PEOPLE_GROUP_POSITION:
                    title.setText(R.string.familyTitle);
                    break;
                case EVENTS_GROUP_POSITION:
                    title.setText(R.string.lifeEventsTitle);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized group position: " + groupPosition);
            }

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View itemView = getLayoutInflater().inflate(R.layout.list_item, parent, false);

            ImageView icon = itemView.findViewById(R.id.listItemIcon);
            TextView  topText = itemView.findViewById(R.id.listItemTopText);
            TextView  bottomText = itemView.findViewById(R.id.listItemBottomText);


            switch (groupPosition) {
                case PEOPLE_GROUP_POSITION:
                    initializeWithPerson(itemView, icon, topText, bottomText, parent, childPosition);

                    break;
                case EVENTS_GROUP_POSITION:
                    initializeWithEvent(itemView, icon, topText, bottomText, parent, childPosition);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized group position: " + groupPosition);
            }

            return itemView;
        }

        private void initializeWithPerson(View itemView, ImageView icon, TextView topText, TextView bottomText, ViewGroup parent, int childPosition) {
            FontAwesomeIcons genderType;
            int genderColor;
            Integer relationship = null;
            Person listedPerson = this.people.get(childPosition);
            if (listedPerson.getGender().equals("m")) {
                genderType = FontAwesomeIcons.fa_male;
                genderColor = R.color.male_icon;
            } else {
                genderType = FontAwesomeIcons.fa_female;
                genderColor = R.color.female_icon;
            }

            if (listedPerson.getSpouseID() != null && listedPerson.getSpouseID().equals(person.getPersonID())) {
                relationship = R.string.spouse;
            } else if (listedPerson.getFatherID() != null && listedPerson.getFatherID().equals(person.getPersonID()) ||
                    listedPerson.getMotherID() != null && listedPerson.getMotherID().equals(person.getPersonID())) {
                relationship = R.string.child;
            } else if (person.getFatherID() != null && person.getFatherID().equals(listedPerson.getPersonID())) {
                relationship = R.string.father;
            } else if (person.getMotherID() != null && person.getMotherID().equals(listedPerson.getPersonID())) {
                relationship = R.string.mother;
            }

            if (relationship != null)
                bottomText.setText(getString(relationship));
            topText.setText(getString(R.string.fullName, listedPerson.getFirstName(), listedPerson.getLastName()));

            Drawable d = new IconDrawable(parent.getContext(), genderType).colorRes(genderColor).sizeDp(40);
            icon.setImageDrawable(d);


            itemView.setOnClickListener(v -> {
                Log.v("PersonAct:Person_Item", "Clicked a person item!");
                Intent intent = new Intent(parent.getContext(), PersonActivity.class);

                intent.putExtra(PERSON_KEY, listedPerson.getPersonID());
                startActivity(intent);
            });
        }

        private void initializeWithEvent(View itemView, ImageView icon, TextView topText, TextView bottomText, ViewGroup parent, int childPosition) {
            Event event = this.events.get(childPosition);

            topText.setText(getString(R.string.eventFormatted, event.getEventType().toUpperCase(Locale.ROOT),
                    event.getCity(), event.getCountry(), event.getYear()));
            Person person = cache.getPersonByID(event.getPersonID());
            bottomText.setText(getString(R.string.fullName, person.getFirstName(), person.getLastName()));

            FontAwesomeIcons pin = FontAwesomeIcons.fa_map_marker;
            Drawable d = new IconDrawable(parent.getContext(), pin).colorRes(R.color.red).sizeDp(40);
            icon.setImageDrawable(d);

            // TODO: Add onClickListener
            itemView.setOnClickListener(v -> {
                Log.v("PersonAct:Event_Item", "Clicked an event item!");
                Intent intent = new Intent(parent.getContext(), EventActivity.class);
                intent.putExtra(EventActivity.EVENT_KEY, event.getEventID());
                startActivity(intent);
            });
        }

            @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}