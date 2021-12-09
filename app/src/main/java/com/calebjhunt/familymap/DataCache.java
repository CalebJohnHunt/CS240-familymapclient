package com.calebjhunt.familymap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import model.Event;
import model.Person;

public class DataCache {

    public static class Settings {
        private boolean showLifeStoryLines     = true;
        private boolean showFamilyTreeLines    = true;
        private boolean showSpouseLines        = true;
        private boolean filterFatherSideEvents = true;
        private boolean filterMotherSideEvents = true;
        private boolean filterMaleEvents       = true;
        private boolean filterFemaleEvents     = true;

        public boolean isShowLifeStoryLines() {
            return showLifeStoryLines;
        }

        public void setShowLifeStoryLines(boolean showLifeStoryLines) {
            this.showLifeStoryLines = showLifeStoryLines;
        }

        public boolean isShowFamilyTreeLines() {
            return showFamilyTreeLines;
        }

        public void setShowFamilyTreeLines(boolean showFamilyTreeLines) {
            this.showFamilyTreeLines = showFamilyTreeLines;
        }

        public boolean isShowSpouseLines() {
            return showSpouseLines;
        }

        public void setShowSpouseLines(boolean showSpouseLines) {
            this.showSpouseLines = showSpouseLines;
        }

        public boolean isFilterFatherSideEvents() {
            return filterFatherSideEvents;
        }

        public void setFilterFatherSideEvents(boolean filterFatherSideEvents) {
            this.filterFatherSideEvents = filterFatherSideEvents;
        }

        public boolean isFilterMotherSideEvents() {
            return filterMotherSideEvents;
        }

        public void setFilterMotherSideEvents(boolean filterMotherSideEvents) {
            this.filterMotherSideEvents = filterMotherSideEvents;
        }

        public boolean isFilterMaleEvents() {
            return filterMaleEvents;
        }

        public void setFilterMaleEvents(boolean filterMaleEvents) {
            this.filterMaleEvents = filterMaleEvents;
        }

        public boolean isFilterFemaleEvents() {
            return filterFemaleEvents;
        }

        public void setFilterFemaleEvents(boolean filterFemaleEvents) {
            this.filterFemaleEvents = filterFemaleEvents;
        }
    }

    private static final DataCache instance  = new DataCache();

    private final Set<String> paternalAncestors = new HashSet<>();
    private final Set<String> maternalAncestors = new HashSet<>();
    private final Set<Person> people = new HashSet<>();
    private final Set<Event>  events = new HashSet<>();
    private final Set<Event>  filteredEvents = new HashSet<>();
    private final Map<String, Person> personIDToPerson = new HashMap<>();
    private final Map<String, Event> eventIDToEvent = new HashMap<>();
    private final Map<String, List<Event>> personEvents = new HashMap<>();

    public Settings settings = new Settings();

    private DataCache() {}

    public static DataCache getInstance() {
        return instance;
    }

    public void addPeople(Person[] newPeople) {
        for (Person p : newPeople) {
            people.add(p);
            personIDToPerson.put(p.getPersonID(), p);
        }
    }

    public void organizeAncestors(String personID) {
        Person person = getPersonByID(personID);
        String parentID;
        if ((parentID = person.getFatherID()) != null) {
            paternalAncestors.add(parentID);
            organizeAncestorsHelper(parentID, true);
        }

        if ((parentID = person.getMotherID()) != null) {
            maternalAncestors.add(parentID);
            organizeAncestorsHelper(parentID, false);
        }
    }

    private void organizeAncestorsHelper(String personID, boolean isPaternal) {
        Person person = getPersonByID(personID);
        String ancestorID;
        if ((ancestorID = person.getFatherID()) != null) {
            if (isPaternal) {
                paternalAncestors.add(ancestorID);
            } else {
                maternalAncestors.add(ancestorID);
            }
            organizeAncestorsHelper(ancestorID, isPaternal);
        }

        if ((ancestorID = person.getMotherID()) != null) {
            if (isPaternal) {
                paternalAncestors.add(ancestorID);
            } else {
                maternalAncestors.add(ancestorID);
            }
            organizeAncestorsHelper(ancestorID, isPaternal);
        }
    }

    public void addEvents(Event[] newEvents) {
        List<Event> eventList;
        for (Event e : newEvents) {
            eventIDToEvent.put(e.getEventID(), e);
            events.add(e);
            filteredEvents.add(e);
            if ((eventList = personEvents.get(e.getPersonID())) != null) {
                insertEventInOrder(e, eventList);
            } else {
                eventList = new ArrayList<>();
                eventList.add(e);
                personEvents.put(e.getPersonID(), eventList);
            }
        }
    }

    private void insertEventInOrder(Event e, List<Event> eventList) {
        for (int i = 0; i < eventList.size(); ++i) {
            if (e.getYear() < eventList.get(i).getYear()) {
                eventList.add(i, e);
                return;
            } else if (e.getYear().equals(eventList.get(i).getYear())) {
                // Alphabetically by type!
                if (e.getEventType().compareToIgnoreCase(eventList.get(i).getEventType()) < 0) {
                    eventList.add(i, e);
                    return;
                }
            }
        }

        eventList.add(e);
    }

    public Event findEarliestFilteredEvent(String personID) {
        Event earliestEvent = null;
        List<Event> events = this.getPersonEvents(personID);
        if (events == null)
            return null;

        for (Event e : events) {
            if (!isEventInFilter(e))
                continue;
            if (earliestEvent == null)
                earliestEvent = e;
            if (e.getYear() < earliestEvent.getYear()) {
                earliestEvent = e;
            }
        }

        return earliestEvent;
    }

    public List<Event> getPersonEvents(String personID) {
        return personEvents.get(personID);
    }

    public List<Event> getFilteredPersonEvents(String personID) {
        List<Event> events = new ArrayList<>();
        for (Event e : getPersonEvents(personID)) {
            if (isEventInFilter(e)) {
                events.add(e);
            }
        }

        return events;
    }

    public List<Person> getFamily(String personID) {
        Person person = getPersonByID(personID);
        Person temp;
        String id;

        // Parents, spouse, children
        List<Person> family = new ArrayList<>();

        // Parents
        if ((id = person.getFatherID()) != null) {
            if ((temp = getPersonByID(id)) != null) {
                family.add(temp);
            }
        }
        if ((id = person.getMotherID()) != null) {
            if ((temp = getPersonByID(id)) != null) {
                family.add(temp);
            }
        }

        // Spouse
        if ((id = person.getSpouseID()) != null) {
            if ((temp = getPersonByID(id)) != null) {
                family.add(temp);
            }
        }

        // Children
        for (Person potentialChild : people) {
            if (potentialChild.getFatherID() != null && potentialChild.getFatherID().equals(personID) ||
                    potentialChild.getMotherID() != null && potentialChild.getMotherID().equals(personID)) {
                family.add(potentialChild);
            }
        }

        return family;
    }
    
    public void filterEvents() {
        filteredEvents.clear();

        for (Event e : this.events) {
            Person owner = getPersonByID(e.getPersonID());
            if (!settings.isFilterMaleEvents() && owner.getGender().equals("m"))
                continue;
            if (!settings.isFilterFemaleEvents() && owner.getGender().equals("f"))
                continue;
            if (!settings.isFilterFatherSideEvents() && paternalAncestors.contains(e.getPersonID()))
                continue;
            if (!settings.isFilterMotherSideEvents() && maternalAncestors.contains(e.getPersonID()))
                continue;

            filteredEvents.add(e);
        }
    }

    public List<Person> searchPeople(String query) {
        List<Person> searchedPeople = new ArrayList<>();
        for (Person p : people) {
            if (containedInAnyInsensitive(query, p.getFirstName(), p.getLastName()))
                searchedPeople.add(p);
        }

        return searchedPeople;
    }

    public List<Event> searchFilteredEvents(String query) {
        List<Event> searchedEvents = new ArrayList<>();
        for (Event e : filteredEvents) {
            if (containedInAnyInsensitive(query, e.getCountry(), e.getCity(), e.getEventType(), Integer.toString(e.getYear())))
                searchedEvents.add(e);
        }

        return searchedEvents;
    }

    private boolean containedInAnyInsensitive(String needle, String... haystacks) {
        for (String s : haystacks) {
            if (s.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT)))
                return true;
        }

        return false;
    }

    public void clear() {
        paternalAncestors.clear();
        maternalAncestors.clear();
        people.clear();
        events.clear();
        filteredEvents.clear();
        personIDToPerson.clear();
        eventIDToEvent.clear();
        personEvents.clear();
        settings = new Settings();
    }

    public boolean isEventInFilter(Event event) {
        return filteredEvents.contains(event);
    }

    public Person getPersonByID(String id) {
        return personIDToPerson.get(id);
    }

    public Event  getEventByID(String id) {
        return eventIDToEvent.get(id);
    }

    public Set<Event> getEvents() {
        return events;
    }

    public Set<Event> getFilteredEvents() {
        return filteredEvents;
    }

    public Set<Person> getPeople() {
        return people;
    }
}
