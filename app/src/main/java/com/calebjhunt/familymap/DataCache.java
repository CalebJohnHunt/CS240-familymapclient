package com.calebjhunt.familymap;

import java.util.HashMap;
import java.util.Map;

import model.Event;
import model.Person;

public class DataCache {

    private static final DataCache instance  = new DataCache();

    private final Map<String, Person> people = new HashMap<>();
    private final Map<String, Event> events  = new HashMap<>();

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

    public void addEvents(Event[] newEvents) {
        for (Event e : newEvents) {
            events.put(e.getEventID(), e);
        }
    }

    public Person getPersonByID(String id) {
        return people.get(id);
    }

    public Event  getEventByID(String id) {
        return events.get(id);
    }

//    Map<String, List<Event>> personEvents;
//    Set<String> paternalAncestors;
//    Set<String> maternalAncestors;

//    Settings settings;
//    List<Event> getPersonEvents(String id) { return null; }

}
