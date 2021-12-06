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

    private final Map<String, Person> people = new HashMap<>();
    private final Map<String, Event> eventIDToEvent = new HashMap<>();
    private final Set<Event> events = new HashSet<>();
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
            people.put(p.getPersonID(), p);
        }
    }

    public void addEvents(String personID, Event[] newEvents) {
        List<Event> eventList;
        for (Event e : newEvents) {
            eventIDToEvent.put(e.getEventID(), e);
            events.add(e);
            if ((eventList = personEvents.get(personID)) != null) {
                eventList.add(e);
            } else {
                eventList = new ArrayList<Event>();
                eventList.add(e);
                personEvents.put(personID, eventList);
            }
        }
    }

    public Person getPersonByID(String id) {
        return people.get(id);
    }

    public Event  getEventByID(String id) {
        return eventIDToEvent.get(id);
    }

    public Set<Event> getEvents() {
        return events;
    }

//    Set<String> paternalAncestors;
//    Set<String> maternalAncestors;

//    Settings settings;
    public List<Event> getPersonEvents(String personID) {
        return personEvents.get(personID);
    }

}
