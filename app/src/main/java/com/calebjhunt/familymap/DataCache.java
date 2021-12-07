package com.calebjhunt.familymap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Event;
import model.Person;

public class DataCache {

    private static final DataCache instance  = new DataCache();

    private final Set<String> paternalAncestors = new HashSet<>();
    private final Set<String> maternalAncestors = new HashSet<>();
    private final Set<Person> people = new HashSet<>();
    private final Set<Event>  events = new HashSet<>();
    private final Map<String, Person> personIDToPerson = new HashMap<>();
    private final Map<String, Event> eventIDToEvent = new HashMap<>();
    private final Map<String, List<Event>> personEvents = new HashMap<>();

    private DataCache() {}

    public static DataCache getInstance() {
        return instance;
    }

    private String authTokenID;

    public String getAuthTokenID() {
        return authTokenID;
    }

    public void setAuthTokenID(String authTokenID) {
        this.authTokenID = authTokenID;
    }

    public void addPeople(Person[] newPeople) {
        for (Person p : newPeople) {
            people.add(p);
            personIDToPerson.put(p.getPersonID(), p);
            if (p.getGender().equals("m'")) {
                paternalAncestors.add(p.getPersonID());
            } else {
                maternalAncestors.add(p.getPersonID());
            }
        }
    }

    public void addEvents(String personID, Event[] newEvents) {
        List<Event> eventList;
        for (Event e : newEvents) {
            eventIDToEvent.put(e.getEventID(), e);
            events.add(e);
            if ((eventList = personEvents.get(e.getPersonID())) != null) {
                insertEventInOrder(e, eventList);
            } else {
                eventList = new ArrayList<Event>();
                eventList.add(e);
                personEvents.put(e.getPersonID(), eventList);
            }
        }
    }

    private void insertEventInOrder(Event e, List<Event> eventList) {
        if (e.getEventType().equals("birth")) {
            eventList.add(0, e);
            return;
        }

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

    public Event findEarliestEvent(String personID) {
        Event earliestEvent = null;
        List<Event> events = this.getPersonEvents(personID);
        if (events == null)
            return null;

        for (Event e : events) {
            if (earliestEvent == null)
                earliestEvent = e;
            if (e.getYear() < earliestEvent.getYear()) {
                earliestEvent = e;
            }
        }

        return earliestEvent;
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



//    Settings settings;
    public List<Event> getPersonEvents(String personID) {
        return personEvents.get(personID);
    }

    public List<Person> getFamily(String personID) {
        Person person = getPersonByID(personID);
        Person temp;
        String id = null;

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

}
