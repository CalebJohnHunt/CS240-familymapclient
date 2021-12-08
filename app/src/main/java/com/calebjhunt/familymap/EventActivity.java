package com.calebjhunt.familymap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

public class EventActivity extends AppCompatActivity {

    public static final String EVENT_KEY = "EVENT_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        String eventID = null;

        Intent intent = getIntent();
        if (intent != null) {
            eventID = intent.getStringExtra(EVENT_KEY);
        }

        Fragment fragment = new MapsFragment();
        Bundle args = new Bundle();
        args.putString(MapsFragment.ARG_EVENT_ID, eventID);
        fragment.setArguments(args);

        FragmentManager fm = getSupportFragmentManager();

        fm.beginTransaction()
                .add(R.id.eventFragmentFrameLayout, fragment)
                .commit();

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        return true;
    }
}