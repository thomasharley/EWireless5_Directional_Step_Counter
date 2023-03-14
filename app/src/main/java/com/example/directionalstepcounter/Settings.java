package com.example.directionalstepcounter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

/**
 * This Settings class handles the operation of the settings screen.
 *
 * @author Thomas Harley (s1810956@ed.ac.uk)
 */

public class Settings extends PreferenceFragmentCompat {

    Settings.SettingsListener activitycommander;
    androidx.preference.EditTextPreference editTextPreference;

    public interface SettingsListener {

        // Interface to communicate data from fragment -> main activity
        public void SettingsData(float height, float weight, float stepgoal);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activitycommander = (Settings.SettingsListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        // Create "save settings" button
        Preference button = findPreference("button");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                updateSettings(); // calls update settings function on click
                Toast.makeText(getContext(), "Settings Saved", Toast.LENGTH_SHORT).show(); // indicates settings updated
                return true;
            }
        });

        // Change input type of editTextPreference to just numbers. This is to prevent accidental
        // entering of invalid characters that cannot be cast to float, causing application to crash
        int i;
        String keys[] = {"stepgoal", "weight", "height"};
        for(i=0; i<3; i++) {
            editTextPreference = (EditTextPreference) findPreference(keys[i]);
            editTextPreference.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);

                }
            });
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        updateSettings(); // Update settings and send data to main activity
    }

    public void updateSettings() {
        // Purpose of this function is to send updated settings to main activity via interface settings listener
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext()); // Fetch settings values from shared preferences

        // access shared preferences
        SharedPreferences.Editor editor = prefs.edit();
        String stepgoal = prefs.getString("stepgoal", "0"); // step goal
        String weight = prefs.getString("weight", "0"); // weight
        String height = prefs.getString("height", "0"); // height

        // Cast these string values to floats.
        float stepgoalflt = Float.parseFloat(stepgoal);
        float weightflt = Float.parseFloat(weight);
        float heightflt = Float.parseFloat(height);
        activitycommander.SettingsData(heightflt, weightflt, stepgoalflt); // send data via interface
    }
}
