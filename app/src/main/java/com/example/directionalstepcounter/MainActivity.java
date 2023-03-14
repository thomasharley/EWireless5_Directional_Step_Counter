package com.example.directionalstepcounter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.os.Bundle;
import android.view.MenuItem;
import com.google.android.material.navigation.NavigationView;

/**
 * This Main Activity class handles switching between fragments and sending of data between fragments
 * as well as initialisation of main content view.
 *
 * @author Thomas Harley (s1810956@ed.ac.uk)
 */

public class MainActivity extends AppCompatActivity implements Compass.CompassListener, Settings.SettingsListener {

    FragmentManager fragmentManager; // Declare manager

    // Declare all fragments
    Fragment compassfragment = new Compass(); Fragment pedometerfragment = new Pedometer();
    Fragment settingsfragment = new Settings(); Fragment currentfragment;

    // For the Nav Bar
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;

    String title; // Used to change title at top of app

    @Override
    public void DegreeData(float degree) {

        // Method from compass.compasslistener, fetch degree data and send to pedometer
        Pedometer pedometer = (Pedometer) pedometerfragment;
        pedometer.setCardinalSteps(degree);
    }

    @Override
    public void SettingsData(float height, float weight, float stepgoal) {

        // Method from settings.settingslistener, fetch settings data and send to pedometer
        Pedometer pedometer = (Pedometer) pedometerfragment;
        pedometer.setSettingsValues(height, weight, stepgoal);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // This executes on creation of the app
        setTitle("Compass+"); // Title of this app is Compass+

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Set view to activity_main.xml file
        handlingNavBar(); // initialises navbar
        initialiseFragments(); // initialises all the fragments and shows default (compass)
    }

    private void handlingNavBar() {

        getSupportActionBar().setTitle("Compass App");
        // The drawer layout toggles menu icon to open drawer and back button to close drawer
        drawerLayout = findViewById(R.id.my_drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);

        // Passes the Open and Close toggle for the drawer layout listener to toggle the button state
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // makes the Navigation drawer icon always appear on the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        NavigationView navigation = findViewById(R.id.navigation);

        // Listens for clicks of nav bar buttons
        navigation.setNavigationItemSelectedListener(item -> {
            // hide current fragment
            fragmentManager.beginTransaction().hide(currentfragment).commit();
            // switch statement to select between fragments
            switch(item.getItemId()) {
                case R.id.nav_compass:
                    currentfragment = compassfragment; // switch to compass
                    title = "Compass App";
                    break;
                case R.id.nav_pedometer:
                    currentfragment = pedometerfragment; // switch to pedometer
                    title = "Pedometer App";
                    break;
                case R.id.nav_settings:
                    currentfragment = settingsfragment; // switch to settings
                    title = "Settings";
                default:
                    break;
            }
            // Show the fragment that was just selected
            fragmentManager.beginTransaction().show(currentfragment).commit();
            getSupportActionBar().setTitle(title); // sets title bar to fragment name
            drawerLayout.closeDrawer(GravityCompat.START); // close Navigation drawer

            return true;
        });
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        // When menu/back button is pressed
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initialiseFragments() {

        fragmentManager = getSupportFragmentManager();

        // Initialise fragments and add them to flcontent frame layout declared in activity_main.xml
        fragmentManager.beginTransaction().add(R.id.flContent, compassfragment).commit();
        fragmentManager.beginTransaction().add(R.id.flContent, pedometerfragment).commit();
        fragmentManager.beginTransaction().add(R.id.flContent, settingsfragment).commit();

        // Hide secondary fragments (pedometer, settings)
        fragmentManager.beginTransaction().hide(pedometerfragment).commit();
        fragmentManager.beginTransaction().hide(settingsfragment).commit();

        // Set current fragment to the default (compass)
        currentfragment = compassfragment;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}