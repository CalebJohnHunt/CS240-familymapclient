package com.calebjhunt.familymap;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import model.Event;
import model.Person;
import model.User;
import request.GetFamilyEventsRequest;
import request.GetFamilyRequest;
import request.RegisterRequest;
import result.RegisterResult;

public class DataCacheTest {
    DataCache cache;
    ServerProxy proxy;
    private final User user = new User("ex_un", "ex_pass", "ex@gov", "first_ex",
            "last_ex", "f", "ex_uPID");
    String authTokenID;
    String personID;
    Person person;

    @Before
    public void setup() {
        cache = DataCache.getInstance();
        cache.clear();
        proxy = new ServerProxy();
        proxy.setServerHost("localhost");
        proxy.setServerPort("8080");
        proxy.clear();
    }

    private void genericRegister() {
        RegisterResult re = proxy.register(new RegisterRequest(user.getUsername(), user.getPassword(),
                user.getEmail(), user.getFirstName(), user.getLastName(), user.getGender()));
        authTokenID = re.getAuthtoken();
        personID    = re.getPersonID();
        Person[] people = proxy.getFamily(new GetFamilyRequest(authTokenID)).getData();
        Event[]  events = proxy.getFamilyEvents(new GetFamilyEventsRequest(authTokenID)).getData();
        cache.addPeople(people);
        cache.addEvents(events);
        person = cache.getPersonByID(personID);
    }

    @Test
    public void getFamilyTest() {
        genericRegister();
        List<Person> family = cache.getFamily(personID);
        assertNotNull(family);
        assertEquals(2, family.size());
        assertEquals(person.getFatherID(), family.get(0).getPersonID());
        assertEquals(cache.getPersonByID(person.getFatherID()), family.get(0));
        assertEquals(person.getMotherID(), family.get(1).getPersonID());
    }

    @Test
    public void getFamilyMultipleChildrenTest() {
        Person[] people = {
            new Person("child1_id", "child1_un", "child1_first", "child1_last", "m", "main_id", null, null),
            new Person("main_id", "main_un", "main_first", "main_last", "m", null, null, null),
            new Person("child2_id", "child2_un", "child2_first", "child2_last", "f", "main_id", null, null),
            new Person("child3_id", "child3_un", "child3_first", "child3_last", "f", "main_id", null, null),
        };

        cache.addPeople(people);
        List<Person> family = cache.getFamily("main_id");
        assertNotNull(family);
        assertEquals(3, family.size());
    }

    @Test
    public void filterEventsNoFiltersTest() {
        genericRegister();
        cache.filterEvents();
        Set<Event> events = cache.getEvents();
        Set<Event> filteredEvents = cache.getFilteredEvents();
        assertNotNull(events);
        assertNotNull(filteredEvents);
        assertEquals(events, filteredEvents);
    }

    @Test
    public void filterEventsMaleFilterTest() {
        genericRegister();

        cache.settings.setFilterMaleEvents(false);

        cache.filterEvents();
        Set<Event> filteredEvents = cache.getFilteredEvents();
        assertNotNull(filteredEvents);
        for (Event e : filteredEvents) {
            assertEquals("f", cache.getPersonByID(e.getPersonID()).getGender());
        }
    }

    @Test
    public void filterEventsMaleAndFemaleFilterTest() {
        genericRegister();

        cache.settings.setFilterMaleEvents(false);
        cache.settings.setFilterFemaleEvents(false);

        cache.filterEvents();
        Set<Event> filteredEvents = cache.getFilteredEvents();
        assertNotNull(filteredEvents);
        assertEquals(0, filteredEvents.size());
    }

    @Test
    public void chronologicalEventsTest() {
        genericRegister();
        for (Person p : cache.getPeople()) {
            int lowYear = 0;
            for (Event e : cache.getPersonEvents(p.getPersonID())) {
                assertTrue(lowYear <= e.getYear());
                lowYear = e.getYear();
            }
        }
    }

    @Test
    public void chronologicalEventsCustomTest() {
        Person p = new Person("cool beans", "", "", "", "", "", "", "");
        cache.addPeople(new Person[] {p});
        Event[] events = new Event[] {
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", 0),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", -10000),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", 0),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", 10),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", -1),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", 894),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", 0),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", -10000),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", 0),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", 10),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", -1),
                new Event("", "", p.getPersonID(), 0f, 0f, "", "", "", 894),
        };
        cache.addEvents(events);

        int lowYear = Integer.MIN_VALUE;

        assertEquals(events.length, cache.getPersonEvents(p.getPersonID()).size());
        for (Event e : cache.getPersonEvents(p.getPersonID())) {
            assertTrue(lowYear <= e.getYear());
            lowYear = e.getYear();
        }
    }

    @Test
    public void searchTest() {
        Event[] events = {
                new Event("", "", "", 0f, 0f, "abcdef", "", "", 123),
                new Event("", "", "", 0f, 0f, "", "abcdef", "", 1),
                new Event("", "", "", 0f, 0f, "", "", "abcdef", 23),
        };
        Person[] people = {
                new Person("","","abcdef","","","","",""),
                new Person("","","","abcdef","","","",""),
        };

        cache.addEvents(events);
        cache.addPeople(people);

        List<Event> aEvents = cache.searchFilteredEvents("a");
        List<Event> dEvents = cache.searchFilteredEvents("d");
        List<Event> DEFEvents = cache.searchFilteredEvents("DEF");
        List<Event> ABCDEFEvents = cache.searchFilteredEvents("ABCDEF");

        assertEquals(aEvents, dEvents);
        assertEquals(DEFEvents, dEvents);
        assertEquals(DEFEvents, ABCDEFEvents);

        List<Event> oneEvents = cache.searchFilteredEvents("1");
        assertEquals(2, oneEvents.size());

        List<Person> aPeople = cache.searchPeople("a");
        List<Person> dPeople = cache.searchPeople("d");
        List<Person> DEFPeople = cache.searchPeople("DEF");
        List<Person> ABCDEFPeople = cache.searchPeople("ABCDEF");

        assertEquals(aPeople, dPeople);
        assertEquals(DEFPeople, dPeople);
        assertEquals(DEFPeople, ABCDEFPeople);
    }

    @Test
    public void searchBadQueriesTest() {
        genericRegister();
        List<Event> events = cache.searchFilteredEvents("This is an entirely ridiculous query");
        List<Person> people = cache.searchPeople("A similarly crazy query");

        assertNotNull(events);
        assertNotNull(people);
        assertEquals(0, events.size());
        assertEquals(0, people.size());
    }

}
