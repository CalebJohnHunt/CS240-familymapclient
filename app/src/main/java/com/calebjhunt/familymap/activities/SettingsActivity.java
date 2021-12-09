package com.calebjhunt.familymap.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import com.calebjhunt.familymap.DataCache;
import com.calebjhunt.familymap.fragments.MapsFragment;
import com.calebjhunt.familymap.R;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat lifeStorySwitch;
    private SwitchCompat familyTreeSwitch;
    private SwitchCompat spouseSwitch;
    private SwitchCompat fatherSwitch;
    private SwitchCompat motherSwitch;
    private SwitchCompat maleEventsSwitch;
    private SwitchCompat femaleEventsSwitch;
    private RelativeLayout logoutView;

    private DataCache cache = DataCache.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setup();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            endActivity();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        endActivity();
    }

    private void endActivity() {
        cache.filterEvents();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void setup() {
        lifeStorySwitch    = findViewById(R.id.settingsLifeStorySwitch);
        lifeStorySwitch.setChecked(cache.settings.isShowLifeStoryLines());
        lifeStorySwitch.setOnClickListener(v -> {
            cache.settings.setShowLifeStoryLines(lifeStorySwitch.isChecked());
        });

        familyTreeSwitch   = findViewById(R.id.settingsFamilyTreeSwitch);
        familyTreeSwitch.setChecked(cache.settings.isShowFamilyTreeLines());
        familyTreeSwitch.setOnClickListener(v -> {
            cache.settings.setShowFamilyTreeLines(familyTreeSwitch.isChecked());
        });

        spouseSwitch       = findViewById(R.id.settingsSpouseSwitch);
        spouseSwitch.setChecked(cache.settings.isShowSpouseLines());
        spouseSwitch.setOnClickListener(v -> {
            cache.settings.setShowSpouseLines(spouseSwitch.isChecked());
        });

        fatherSwitch       = findViewById(R.id.settingsFatherSwitch);
        fatherSwitch.setChecked(cache.settings.isFilterFatherSideEvents());
        fatherSwitch.setOnClickListener(v -> {
            cache.settings.setFilterFatherSideEvents(fatherSwitch.isChecked());
        });

        motherSwitch       = findViewById(R.id.settingsMotherSwitch);
        motherSwitch.setChecked(cache.settings.isFilterMotherSideEvents());
        motherSwitch.setOnClickListener(v -> {
            cache.settings.setFilterMotherSideEvents(motherSwitch.isChecked());
        });

        maleEventsSwitch   = findViewById(R.id.settingsMaleSwitch);
        maleEventsSwitch.setChecked(cache.settings.isFilterMaleEvents());
        maleEventsSwitch.setOnClickListener(v -> {
            cache.settings.setFilterMaleEvents(maleEventsSwitch.isChecked());
        });

        femaleEventsSwitch = findViewById(R.id.settingsFemaleSwitch);
        femaleEventsSwitch.setChecked(cache.settings.isFilterFemaleEvents());
        femaleEventsSwitch.setOnClickListener(v -> {
            cache.settings.setFilterFemaleEvents(femaleEventsSwitch.isChecked());
        });

        logoutView         = findViewById(R.id.settingsLogout);
        logoutView.setOnClickListener(v -> {
            cache.clear();
            MapsFragment.clearColors();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

    }
}