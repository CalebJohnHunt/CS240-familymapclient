package com.calebjhunt.familymap.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.calebjhunt.familymap.DataCache;
import com.calebjhunt.familymap.R;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import model.Event;
import model.Person;

public class SearchActivity extends AppCompatActivity {

    private static final int PERSON_ITEM_VIEW_TYPE = 0;
    private static final int EVENT_ITEM_VIEW_TYPE  = 1;

    private final DataCache cache = DataCache.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        setup();
    }

    private void setup() {
        RecyclerView recyclerView = findViewById(R.id.searchRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(SearchActivity.this));

        SearchView search = findViewById(R.id.searchSearch);
        search.setOnClickListener(v -> {
            search.setIconified(false);
        });
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                List<Person> searchedPeople;
                List<Event>  searchedFilteredEvents;
                if (newText.equals("")) {
                    searchedPeople = new ArrayList<>();
                    searchedFilteredEvents = new ArrayList<>();
                } else {
                    searchedPeople = cache.searchPeople(newText);
                    searchedFilteredEvents = cache.searchFilteredEvents(newText);
                }
                recyclerView.setAdapter(new SearchResultsAdapter(searchedPeople, searchedFilteredEvents));
                return true;
            }
        });
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

    private class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultViewHolder> {
        private final List<Person> people;
        private final List<Event>  events;

        SearchResultsAdapter(List<Person> people, List<Event> events) {
            this.people = people;
            this.events = events;
        }

        @Override
        public int getItemViewType(int position) {
            if (position < people.size())
                return PERSON_ITEM_VIEW_TYPE;
            else if (position < people.size() + events.size())
                return EVENT_ITEM_VIEW_TYPE;
            else
                throw new IllegalArgumentException("Bad position: " + position);
        }

        @NonNull
        @Override
        public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item, parent, false);

            return new SearchResultViewHolder(view, viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
            if (position < people.size()) {
                holder.bind(people.get(position));
            } else if (position < people.size() + events.size()) {
                holder.bind(events.get(position - people.size()));
            } else {
                throw new IllegalArgumentException("Bad position: " + position);
            }
        }

        @Override
        public int getItemCount() {
            return people.size() + events.size();
        }
    }

    private class SearchResultViewHolder extends RecyclerView.ViewHolder {
        private final TextView topText;
        private final TextView bottomText;
        private final ImageView icon;

        private final int viewType;
        private Person person;
        private Event event;

        SearchResultViewHolder(View view, int viewType) {
            super(view);
            this.viewType = viewType;

            View.OnClickListener onClickListener = v -> {
                Intent intent;
                switch (this.viewType) {
                    case PERSON_ITEM_VIEW_TYPE:
                        intent = new Intent(itemView.getContext(), PersonActivity.class);
                        intent.putExtra(PersonActivity.PERSON_KEY, this.person.getPersonID());
                        startActivity(intent);

                        break;
                    case EVENT_ITEM_VIEW_TYPE:
                        intent = new Intent(itemView.getContext(), EventActivity.class);
                        intent.putExtra(EventActivity.EVENT_KEY, this.event.getEventID());
                        startActivity(intent);

                        break;
                    default:
                        throw new IllegalArgumentException("Back viewType: " + viewType);
                }
            };

            itemView.setOnClickListener(onClickListener);

            topText = itemView.findViewById(R.id.listItemTopText);
            bottomText = itemView.findViewById(R.id.listItemBottomText);
            icon = itemView.findViewById(R.id.listItemIcon);
        }

        private void bind(Event event) {
            this.event = event;

            topText.setText(getString(R.string.eventFormatted, event.getEventType().toUpperCase(Locale.ROOT),
                    event.getCity(), event.getCountry(), event.getYear()));
            Person person = cache.getPersonByID(event.getPersonID());
            bottomText.setText(getString(R.string.fullName, person.getFirstName(), person.getLastName()));
            icon.setImageDrawable(new IconDrawable(itemView.getContext(), FontAwesomeIcons.fa_map_marker)
                                    .colorRes(R.color.red)
                                    .sizeDp(40));
        }

        private void bind(Person person) {
            this.person = person;

            topText.setText(getString(R.string.fullName, person.getFirstName(), person.getLastName()));
            if (person.getGender().equals("m")) {
                bottomText.setText(getString(R.string.male));
                icon.setImageDrawable(new IconDrawable(itemView.getContext(), FontAwesomeIcons.fa_male)
                                    .colorRes(R.color.male_icon)
                                    .sizeDp(40));
            } else {
                bottomText.setText(getString(R.string.female));
                icon.setImageDrawable(new IconDrawable(itemView.getContext(), FontAwesomeIcons.fa_female)
                        .colorRes(R.color.female_icon)
                        .sizeDp(40));
            }
        }

    }
}