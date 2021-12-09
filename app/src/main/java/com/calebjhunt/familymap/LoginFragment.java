package com.calebjhunt.familymap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Util.JSONHandler;
import model.Event;
import model.Person;
import request.GetFamilyEventsRequest;
import request.GetFamilyRequest;
import request.LoginRequest;
import request.RegisterRequest;
import result.GetFamilyEventsResult;
import result.GetFamilyResult;
import result.LoginResult;
import result.RegisterResult;

public class LoginFragment extends Fragment {

    public interface Listener {
        void notifyDone();
    }

    private Listener listener;

    public void registerListener(Listener listener) {
        this.listener = listener;
    }

    private static final String AUTH_TOKEN_KEY         = "AuthTokenKey";
    private static final String PERSON_ID_KEY          = "PersonIDKey";
    private static final String SUCCESSFUL_SIGN_IN_KEY = "SuccessfulSignInKey";
    private static final String ERROR_MESSAGE_KEY      = "ErrorMessageKey";
    private static final String FAMILY_ARRAY_KEY       = "FamilyArrayKey";
    private static final String EVENTS_ARRAY_KEY       = "EventsArrayKey";

    private EditText serverHost;
    private EditText serverPort;
    private EditText username;
    private EditText password;
    private EditText firstName;
    private EditText lastName;
    private EditText email;
    private RadioGroup gender;
    private Button signIn;
    private Button register;
    private final DataCache cache = DataCache.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);
        setUpFields(view);

        signIn.setOnClickListener(l -> attemptSignIn(true));

        register.setOnClickListener(l -> attemptSignIn(false));

        return view;
    }

    private void attemptSignIn(boolean isSignIn) {
        Handler uiThreadMessageHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                Bundle bundle = message.getData();
                boolean success = bundle.getBoolean(SUCCESSFUL_SIGN_IN_KEY);
                if (success) {
                    String authTokenID = bundle.getString(AUTH_TOKEN_KEY, "");
                    String personID    = bundle.getString(PERSON_ID_KEY, "");

                    Handler uiThreadMessageHandler = new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message message) {
                            Bundle bundle = message.getData();

                            String familyJSON = bundle.getString(FAMILY_ARRAY_KEY, "");
                            Person[] family   = (Person[]) JSONHandler.jsonToObject(familyJSON, Person[].class);
                            cache.addPeople(family);
                            cache.organizeAncestors(personID);
                            Log.d("LoginFragment: getData", "Added family to the cache");

                            String eventsJSON = bundle.getString(EVENTS_ARRAY_KEY, "");
                            Event[]  events   = (Event[]) JSONHandler.jsonToObject(eventsJSON, Event[].class);
                            cache.addEvents(events);
                            Log.d("LoginFragment: getData", "Added events to the cache");


                            // For Login assignment, not necessary for the client
                            ////  Person user = cache.getPersonByID(personID);
                            ////  Toast.makeText(getContext(), user.getFirstName() + " " + user.getLastName(), Toast.LENGTH_SHORT).show();

                            if (listener != null)
                                listener.notifyDone();
                        }
                    };

                    GetDataTask task = new GetDataTask(uiThreadMessageHandler, authTokenID, serverHost, serverPort);
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(task);

                } else {
                    String errorMessage = bundle.getString(ERROR_MESSAGE_KEY);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        };

        SignInTask task = new SignInTask(uiThreadMessageHandler, serverHost, serverPort,
                username, password, firstName, lastName, email,
                gender.getCheckedRadioButtonId()==R.id.loginRadioMale, isSignIn);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(task);
    }

    private static class GetDataTask implements Runnable {

        private final Handler messageHandler;
        private final String authTokenID;
        private final EditText serverHost;
        private final EditText serverPort;

        public GetDataTask(Handler messageHandler, String authTokenID, EditText serverHost, EditText serverPort) {
            this.messageHandler = messageHandler;
            this.authTokenID = authTokenID;
            this.serverHost = serverHost;
            this.serverPort = serverPort;
        }

        @Override
        public void run() {
            Log.d("GetDataTask", "Run started");
            ServerProxy proxy = new ServerProxy();
            proxy.setServerHost(serverHost.getText().toString());
            proxy.setServerPort(serverPort.getText().toString());
            Log.d("GetDataTask", "Set up proxy");

            GetFamilyRequest gfRequest = new GetFamilyRequest(authTokenID);
            GetFamilyResult  gfResult  = proxy.getFamily(gfRequest);
            Log.d("GetDataTask", "Made GetFamily call");
            String gfResultData = JSONHandler.objectToJson(gfResult.getData());
            Log.d("GetDataTask", "GetFamily to JSON");

            GetFamilyEventsRequest gfeRequest = new GetFamilyEventsRequest(authTokenID);
            GetFamilyEventsResult  gfeResult  = proxy.getFamilyEvents(gfeRequest);
            Log.d("GetDataTask", "Made GetFamilyEvents call");
            String gfeResultData = JSONHandler.objectToJson(gfeResult.getData());
            Log.d("GetDataTask", "GetFamilyEvents to JSON");

            Log.d("GetDataTask", "About to sendMessage");
            sendMessage(gfResultData, gfeResultData);
        }

        private void sendMessage(String family, String events) {
            Log.d("GetDataTask", "sendMessage started");
            Message message = Message.obtain();

            Bundle messageBundle = new Bundle();
            messageBundle.putString(FAMILY_ARRAY_KEY, family);
            messageBundle.putString(EVENTS_ARRAY_KEY, events);
            Log.d("GetDataTask", "strings put in bundle");

            message.setData(messageBundle);
            Log.d("GetDataTask", "message data set");

            messageHandler.sendMessage(message);
            Log.d("GetDataTask", "message sent");
        }
    }

    private static class SignInTask implements Runnable {

        private final Handler messageHandler;
        private final EditText serverHost;
        private final EditText serverPort;
        private final EditText username;
        private final EditText password;
        private final EditText firstName;
        private final EditText lastName;
        private final EditText email;
        private final boolean isMale;
        private final boolean isSignIn;

        public SignInTask(Handler messageHandler, EditText serverHost, EditText serverPort,
                          EditText username, EditText password, EditText firstName, EditText lastName,
                          EditText email, boolean isMale, boolean isSignIn) {
            this.messageHandler = messageHandler;
            this.serverHost = serverHost;
            this.serverPort = serverPort;
            this.username = username;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.isMale = isMale;
            this.isSignIn = isSignIn;
        }

        @Override
        public void run() {
            ServerProxy proxy = new ServerProxy();
            proxy.setServerHost(serverHost.getText().toString());
            proxy.setServerPort(serverPort.getText().toString());
            if (isSignIn) {
                LoginRequest request = new LoginRequest(username.getText().toString(), password.getText().toString());
                LoginResult  result  = proxy.login(request);
                sendMessage(result.getAuthtoken(), result.getPersonID(), result.isSuccess(), result.getMessage());
            } else {
                String gender = isMale ? "m" : "f";
                RegisterRequest request = new RegisterRequest(username.getText().toString(),
                        password.getText().toString(), email.getText().toString(),
                        firstName.getText().toString(), lastName.getText().toString(), gender);
                RegisterResult result = proxy.register(request);
                sendMessage(result.getAuthtoken(), result.getPersonID(), result.isSuccess(), result.getMessage());
            }
        }

        private void sendMessage(String authTokenID, String personID, boolean success, String errorMessage) {
            Message message = Message.obtain();

            Bundle messageBundle = new Bundle();
            messageBundle.putString(AUTH_TOKEN_KEY, authTokenID);
            messageBundle.putString(PERSON_ID_KEY, personID);
            messageBundle.putBoolean(SUCCESSFUL_SIGN_IN_KEY, success);
            messageBundle.putString(ERROR_MESSAGE_KEY, errorMessage);

            message.setData(messageBundle);

            messageHandler.sendMessage(message);
        }
    }

    private void setUpFields(View view) {
        serverHost = view.findViewById(R.id.loginServerHost);
        serverPort = view.findViewById(R.id.loginServerPort);
        username   = view.findViewById(R.id.loginUsername);
        password   = view.findViewById(R.id.loginPassword);
        firstName  = view.findViewById(R.id.loginFirstName);
        lastName   = view.findViewById(R.id.loginLastName);
        email      = view.findViewById(R.id.loginEmail);
        gender     = view.findViewById(R.id.loginGenderRadio);
        signIn     = view.findViewById(R.id.loginSignInButton);
        register   = view.findViewById(R.id.loginRegisterButton);

        TextWatcher updateButtons = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!(serverHost.getText().toString().equals("")   ||
                        serverPort.getText().toString().equals("") ||
                        username.getText().toString().equals("")   ||
                        password.getText().toString().equals(""))) {

                    signIn.setEnabled(true);

                    // Are these fields non-empty?
                    register.setEnabled(!(firstName.getText().toString().equals("") ||
                            lastName.getText().toString().equals("") ||
                            email.getText().toString().equals("") ||
                            gender.getCheckedRadioButtonId() == -1));

                } else {
                    signIn.setEnabled(false);
                    register.setEnabled(false);
                }
            }
        };

        serverHost.addTextChangedListener(updateButtons);
        serverPort.addTextChangedListener(updateButtons);
        username.addTextChangedListener(updateButtons);
        password.addTextChangedListener(updateButtons);
        firstName.addTextChangedListener(updateButtons);
        lastName.addTextChangedListener(updateButtons);
        email.addTextChangedListener(updateButtons);
        gender.setOnCheckedChangeListener((group, checkedId) -> {
            // We don't use any of the parameters, so we can reuse the listener here.
            updateButtons.onTextChanged(null, 0, 0, 0);
        });
    }
}