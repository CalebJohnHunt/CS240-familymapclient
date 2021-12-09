package com.calebjhunt.familymap;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import Util.JSONHandler;
import Util.ReadString;
import request.GetFamilyEventsRequest;
import request.GetFamilyRequest;
import request.LoginRequest;
import request.RegisterRequest;
import result.ClearResult;
import result.GetFamilyEventsResult;
import result.GetFamilyResult;
import result.LoginResult;
import result.RegisterResult;

// Support all the same APIs as the server
public class ServerProxy {

    private String serverHost = "192.168.0.134";
    private String serverPort = "8080";
    private DataCache cache   = DataCache.getInstance();

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    public LoginResult login(LoginRequest request) {

        try {
            URL url = new URL("http://" + serverHost + ":" + serverPort + "/user/login");

            HttpURLConnection http = setUpHttpURLConnection(url, "POST", true);
            http.connect();

            OutputStreamWriter writer = new OutputStreamWriter(http.getOutputStream());
            JSONHandler.objectToJsonWriter(request, writer);
            writer.close();

            String resData = readResponse(http);

            return (LoginResult) JSONHandler.jsonToObject(resData, LoginResult.class);
        } catch (MalformedURLException e) {
            return new LoginResult("Error: Bad URL. Is the port correct?");
        } catch (SocketTimeoutException e) {
            return new LoginResult("Error: Bad URL. Host and/or port are wrong.");
        } catch (UnknownHostException e) {
            return new LoginResult("Error: Bad URL. Is the host correct?");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new LoginResult("Error: Server error; check logs for more information.");
    }

    public RegisterResult register(RegisterRequest request) {

        try {
            URL url = new URL("http://" + serverHost + ":" + serverPort + "/user/register");

            HttpURLConnection http = setUpHttpURLConnection(url, "POST", true);
            http.connect();

            OutputStreamWriter writer = new OutputStreamWriter(http.getOutputStream());
            JSONHandler.objectToJsonWriter(request, writer);
            writer.close();

            String resData = readResponse(http);

            return (RegisterResult) JSONHandler.jsonToObject(resData, RegisterResult.class);

        } catch (MalformedURLException e) {
            return new RegisterResult("Error: Bad URL. Is the port correct?");
        } catch (SocketTimeoutException e) {
            return new RegisterResult("Error: Bad URL. Host and/or port are wrong.");
        } catch (UnknownHostException e) {
            return new RegisterResult("Error: Bad URL. Is the host correct?");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new RegisterResult("Error: Server error; check logs for more information.");
    }

    public GetFamilyResult getFamily(GetFamilyRequest request) {
        try {
            URL url = new URL("http://" + serverHost + ":" + serverPort + "/person");

            HttpURLConnection http = setUpHttpURLConnection(url, "GET", false);

            http.addRequestProperty("Authorization", request.getAuthTokenID());
            http.connect();

            String resData = readResponse(http);

            return (GetFamilyResult) JSONHandler.jsonToObject(resData, GetFamilyResult.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new GetFamilyResult("Error: Server error; check logs for more information.");
    }

    public GetFamilyEventsResult getFamilyEvents(GetFamilyEventsRequest request) {
        try {
            URL url = new URL("http://" + serverHost + ":" + serverPort + "/event");

            HttpURLConnection http = setUpHttpURLConnection(url, "GET", false);

            http.addRequestProperty("Authorization", request.getAuthTokenID());
            http.connect();

            String resData = readResponse(http);

            return (GetFamilyEventsResult) JSONHandler.jsonToObject(resData, GetFamilyEventsResult.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new GetFamilyEventsResult("Error: Server error; check logs for more information.");
    }

    public ClearResult clear() {
        try {
            URL url = new URL("http://" + serverHost + ":" + serverPort + "/clear");

            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            http.setRequestMethod("POST");
            http.setDoOutput(false);
            http.addRequestProperty("Accept", "application/json");
            http.connect();

            String resData = readResponse(http);

            return (ClearResult) JSONHandler.jsonToObject(resData, ClearResult.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ClearResult(false, "Error: Server error; check logs for more information.");
    }

    /*
     * clear
     * load
     * fill
     * I'm sure there are more
     */

    private static String readResponse(HttpURLConnection http) throws IOException {
        String resData;

        if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream resBody = http.getInputStream();
            resData = ReadString.read(resBody);
            resBody.close();
        } else {
            InputStream errBody = http.getErrorStream();
            resData = ReadString.read(errBody);
            errBody.close();
        }

        return resData;
    }

    private HttpURLConnection setUpHttpURLConnection(URL url, String method, boolean doOutput) throws IOException {
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod(method);
        http.setDoOutput(doOutput);
        http.addRequestProperty("Accept", "application/json");

        // 5 second timeout. Honestly way too long, but it's better than infinity!
        http.setConnectTimeout(5000);

        return http;
    }

}
