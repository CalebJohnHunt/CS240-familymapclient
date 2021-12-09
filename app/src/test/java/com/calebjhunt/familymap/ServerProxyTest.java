package com.calebjhunt.familymap;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import model.Event;
import model.Person;
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
    private final User user = new User("ex_un", "ex_pass", "ex@gov", "first_ex",
            "last_ex", "f", "ex_uPID");
    private final RegisterRequest registerRequest = new RegisterRequest(user.getUsername(), user.getPassword(),
            user.getEmail(), user.getFirstName(), user.getLastName(), user.getGender());

    @Before
    public void setup() {
        proxy = new ServerProxy();
        proxy.setServerHost("localhost");
        proxy.setServerPort("8080");
        proxy.clear();
    }

    private RegisterResult registerExample() {
        RegisterResult re = proxy.register(this.registerRequest);
        assertTrue(re.isSuccess());

        return re;
    }

    @Test
    public void registerSuccessTest() {
        RegisterResult re = registerExample();

        assertTrue(re.isSuccess());
        assertEquals(registerRequest.getUsername(), re.getUsername());
        assertNotNull(re.getAuthtoken());
    }

    @Test
    public void registerDuplicateUsernameFailTest() {
        RegisterRequest r = new RegisterRequest("duplicate", "pass", "e@mail", "first", "last", "m");
        proxy.register(r);

        r = new RegisterRequest("duplicate", "pass", "e@mail", "first", "last", "m");
        RegisterResult re = proxy.register(r);
        assertFalse(re.isSuccess());
        assertEquals("Error: Could not register user.", re.getMessage());
    }

    @Test
    public void loginTest() {
        registerExample();

        LoginRequest request = new LoginRequest(registerRequest.getUsername(), registerRequest.getPassword());
        LoginResult  result  = proxy.login(request);

        assertTrue(result.isSuccess());
        assertEquals(request.getUsername(), result.getUsername());
        assertEquals(registerRequest.getUsername(), result.getUsername());
        assertNotNull(result.getAuthtoken());
    }

    @Test
    public void loginBadUsernameFailTest() {
        registerExample();

        LoginRequest request = new LoginRequest("Wrong Username", registerRequest.getPassword());
        LoginResult  result  = proxy.login(request);

        assertFalse(result.isSuccess());
        assertEquals("Error: Username or password could not be found.", result.getMessage());
    }

    @Test
    public void loginBadPasswordFailTest() {
        registerExample();

        LoginRequest request = new LoginRequest(registerRequest.getUsername(), "Wrong Password");
        LoginResult  result  = proxy.login(request);

        assertFalse(result.isSuccess());
        assertEquals("Error: Username or password could not be found.", result.getMessage());
    }

    @Test
    public void getFamilyTest() {
        RegisterResult regRes = registerExample();
        GetFamilyRequest request = new GetFamilyRequest(regRes.getAuthtoken());
        GetFamilyResult  result  = proxy.getFamily(request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(31, result.getData().length);
        for (Person p : result.getData()) {
            assertEquals(user.getUsername(), p.getAssociatedUsername());
            assertNotEquals(user.getPersonID(), p.getPersonID());
        }
    }

    @Test
    public void getFamilyWrongAuthTokenFailTest() {
        registerExample();
        GetFamilyRequest request = new GetFamilyRequest("Wrong AuthToken");
        GetFamilyResult  result  = proxy.getFamily(request);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Error: Bad AuthToken.", result.getMessage());
    }

    @Test
    public void getFamilyEventsTest() {
        RegisterResult regRes = registerExample();
        GetFamilyEventsRequest request = new GetFamilyEventsRequest(regRes.getAuthtoken());
        GetFamilyEventsResult result  = proxy.getFamilyEvents(request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(91, result.getData().length);
        for (Event e : result.getData()) {
            assertEquals(user.getUsername(), e.getAssociatedUsername());
        }
    }

    @Test
    public void getFamilyEventsWrongAuthTokenTest() {
        registerExample();
        GetFamilyEventsRequest request = new GetFamilyEventsRequest("Wrong AuthToken");
        GetFamilyEventsResult result  = proxy.getFamilyEvents(request);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Error: Bad AuthToken.", result.getMessage());
    }
}
