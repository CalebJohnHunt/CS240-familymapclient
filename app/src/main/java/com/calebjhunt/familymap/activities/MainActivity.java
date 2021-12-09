package com.calebjhunt.familymap.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.calebjhunt.familymap.fragments.LoginFragment;
import com.calebjhunt.familymap.fragments.MapsFragment;
import com.calebjhunt.familymap.R;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

public class MainActivity extends AppCompatActivity implements LoginFragment.Listener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Iconify.with(new FontAwesomeModule());

        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.mainFragmentFrameLayout);

        if (fragment == null) {
            fragment = createLoginFragment();

            fm.beginTransaction()
                    .add(R.id.mainFragmentFrameLayout, fragment)
                    .commit();
        } else {
            if (fragment instanceof LoginFragment) {
                ((LoginFragment) fragment).registerListener(this);
            }
        }

    }

    private Fragment createLoginFragment() {
        LoginFragment fragment = new LoginFragment();
        fragment.registerListener(this);
        return fragment;
    }


    @Override
    public void notifyDone() {
        FragmentManager fm = this.getSupportFragmentManager();
        Fragment fragment = new MapsFragment();

        fm.beginTransaction()
                .replace(R.id.mainFragmentFrameLayout, fragment)
                .commit();
    }
}