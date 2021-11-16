package com.calebjhunt.familymap;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import model.User;
import request.GetFamilyEventsRequest;
import request.GetFamilyRequest;
import request.LoginRequest;
import request.RegisterRequest;
import result.GetFamilyEventsResult;
import result.GetFamilyResult;
import result.LoginResult;
import result.RegisterResult;

public class ServerProxyTest {
    private ServerProxy proxy;

    @Before
    public void setup() {
        proxy = new ServerProxy();
        proxy.setServerHost("localhost");
        proxy.clear();
    }

    @Test
    public void loginTest() {
        RegisterRequest r = new RegisterRequest("user1", "pass", "e@mail", "first", "last", "m");
        RegisterResult re = proxy.register(r);

        LoginRequest request = new LoginRequest("user1", "pass");
        LoginResult  result  = proxy.login(request);

        assertTrue(result.isSuccess());
        assertEquals(request.getUsername(), result.getUsername());
    }

    @Test
    public void getFamilyTest() {
        User user = new User("GF_UN", "pass", "@gov", "first", "last", "f", "uPID");
        RegisterResult regRes = proxy.register(new RegisterRequest(user.getUsername(), user.getPassword(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getGender()));
        GetFamilyRequest request = new GetFamilyRequest(regRes.getAuthtoken());
        GetFamilyResult  result  = proxy.getFamily(request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(31, result.getData().length);
    }

    @Test
    public void getFamilyEventsTest() {
        User user = new User("GF_UN", "pass", "@gov", "first", "last", "f", "uPID");
        RegisterResult regRes = proxy.register(new RegisterRequest(user.getUsername(), user.getPassword(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getGender()));
        GetFamilyEventsRequest request = new GetFamilyEventsRequest(regRes.getAuthtoken());
        GetFamilyEventsResult result  = proxy.getFamilyEvents(request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(91, result.getData().length);
    }
}
