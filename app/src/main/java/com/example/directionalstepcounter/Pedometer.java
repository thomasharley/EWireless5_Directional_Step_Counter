package com.example.directionalstepcounter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

/**
 * This Pedometer class handles the operations of the Pedometer screen.
 *
 * @author Thomas Harley (s1810956@ed.ac.uk)
 */

public class Pedometer extends Fragment implements SensorEventListener {

    // Declare all variables
    int stepCount, stepCountNorth, stepCountEast, stepCountSouth, stepCountWest, CaloriesInt;
    float Degree, Height, Weight;
    double Distance, Calories;

    // Declare sensors
    private SensorManager  sm;
    private Sensor sSensor;

    // Declare views
    TextView textStepCount, textStepGoal, textStepGoal2, textStepsNorth, textStepsSouth, textStepsEast, textStepsWest, textDegrees, textDistance, textCalories;
    CircularProgressBar circularProgressBar;
    ImageView imageViewCompass;
    ImageButton resetbutton;

    SharedPreferences pref; // Declare shared preferences interface

    private boolean isCounterSensorPresent; // boolean to determine if step count sensor is present on phone

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check Activity Permissions
        if(ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
            // ask for permission
            requestPermissions(new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION}, Sensor.TYPE_STEP_DETECTOR);
        }
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Prevent Screen from dimming
        sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE); // Declare sensor manager

        // Initialise STEP_DETECTOR sensor
        if(sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {
            sSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            sm.registerListener(this, sSensor, SensorManager.SENSOR_DELAY_UI);
            isCounterSensorPresent = true;
        }
        else {
            textStepCount.setText("null"); // displays null if no TYPE_STEP_DETECTOR present
            isCounterSensorPresent = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pedometer, container, false);
        // Initialisation for text views, image views and circular progress bar
        textStepCount = view.findViewById(R.id.textStepsNumber);
        textStepGoal = view.findViewById(R.id.textStepGoal);
        resetbutton = view.findViewById(R.id.imageResetButton);
        textStepsNorth = view.findViewById(R.id.textStepsNorth);
        textStepsSouth = view.findViewById(R.id.textStepsSouth);
        textStepsWest = view.findViewById(R.id.textStepsWest);
        textStepsEast = view.findViewById(R.id.textStepsEast);
        textStepGoal2 = view.findViewById(R.id.textStepGoal2);
        textDistance = view.findViewById(R.id.textKilometres);
        textCalories = view.findViewById(R.id.textCalories);
        textDegrees = view.findViewById(R.id.textDegrees2);
        imageViewCompass = view.findViewById(R.id.imageViewCompassIcon);
        circularProgressBar = view.findViewById(R.id.circularProgressBar);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        // Set StepCounter to 0 at initialisation
        if(textStepCount != null) { textStepCount.setText(String.valueOf(stepCount)); }

        // Initialise reset button, if clicked message is displayed
        resetbutton.setOnClickListener (new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getContext(), "Long tap to reset the steps", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialise shared preferences
        pref = getContext().getSharedPreferences("MyPref", 0);

        // Restore or initialise pedometer data that was saved prior to app close.
        stepCount = pref.getInt("stepCount", 0); // fetching integer from memory.
        stepCountNorth = pref.getInt("stepCountNorth", 0);
        stepCountEast = pref.getInt("stepCountEast", 0);
        stepCountSouth = pref.getInt("stepCountSouth", 0);
        stepCountWest = pref.getInt("stepCountWest", 0);
        writeStepsData(); // write data to text views.

        // If reset button is long clicked then pedometer data is reset
        resetbutton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                SharedPreferences.Editor editor = pref.edit(); // access shared preferences
                editor.clear(); editor.commit(); // clear saved data

                // Set variables back to 0
                stepCount = 0; stepCountNorth = 0; stepCountEast = 0; stepCountSouth = 0;
                stepCountWest = 0; Distance = 0; Calories = 0; CaloriesInt = 0;
                writeStepsData(); // write the new erased data (0) to text views

                return true;
            }
        });
    }

    public void setCardinalSteps(float degree) {

        // Data from compass -> main activity -> pedometer
        Degree = degree;
        int degreeint = (int) degree; // cast to integer
        // Assigning degree value to mini display on pedometer page.
        if(textDegrees != null) {
            textDegrees.setText(degreeint + "° ");
            imageViewCompass.setRotation(Degree);
        }
    }

    public void setSettingsValues(float height, float weight, float stepgoal) {

        // Data from settings -> main activity -> settings
        int stepGoal = (int) stepgoal; // data cast to int
        textStepGoal.setText("/" + stepGoal);
        textStepGoal2.setText("" + stepGoal);
        circularProgressBar.setProgressMax(stepGoal); // set circular progress bar
        // Processing of height and weight data.
        Height = height;
        Weight = weight;
        // calculate distance and calories based on step count and settings data
        calculateDistanceCalories();
        // write steps data
        writeStepsData();
    }

    public void calculateDistanceCalories() {

        // Convert steps to distance based on height
        Distance = (stepCount * (0.43 * Height * 0.01)) / 1000; // given in km
        Calories = ((Distance * 1000) / 1.34) * 3.5 * 3.5 * Weight / (200 * 60); // given in kcal
        int scale = (int) Math.pow(10, 2);
        Distance = (double) Math.round(Distance * scale) / scale;
        CaloriesInt = (int) Math.round(Calories);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // Initialise shared preferences editor.
        SharedPreferences.Editor editor = pref.edit();

        if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) { // update step detector
            stepCount = pref.getInt("stepCount", 0); // fetching saved integer from memory.
            stepCount++; // increment
            calculateDistanceCalories(); // with new step count, recalculate distance and calories

            // Attribute each step to a particular cardinal direction: N, S, E, W.
            if (Degree >= -45 && Degree < 45) {
                stepCountNorth = pref.getInt("stepCountNorth", 0);
                stepCountNorth++;
            } else if (Degree >= 45 && Degree < 135) {
                stepCountEast = pref.getInt("stepCountEast", 0);
                stepCountEast++;
            } else if (Degree >= 135 && Degree < 225) {
                stepCountSouth=pref.getInt("stepCountSouth", 0);
                stepCountSouth++;
            } else {
                stepCountWest = pref.getInt("stepCountWest", 0);
                stepCountWest++;
            }
        }

        // Update step data text fields
        writeStepsData();
        // Save data to shared preferences so it isn't erased if app is closed
        editor.putInt("stepCount", stepCount);
        editor.putInt("stepCountNorth", stepCountNorth);
        editor.putInt("stepCountSouth", stepCountSouth);
        editor.putInt("stepCountEast", stepCountEast);
        editor.putInt("stepCountWest", stepCountWest);
        editor.commit(); // commit changes
    }

    public void writeStepsData() {

        // Writing data from variables to text views and progress bar
        textStepCount.setText(String.valueOf(stepCount));
        textStepsNorth.setText(String.valueOf(stepCountNorth));
        textStepsSouth.setText(String.valueOf(stepCountSouth));
        textStepsEast.setText(String.valueOf(stepCountEast));
        textStepsWest.setText(String.valueOf(stepCountWest));
        circularProgressBar.setProgressWithAnimation(stepCount);
        textDistance.setText("" + Distance);
        textCalories.setText("" + CaloriesInt);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onPause() {
        super.onPause();
        //sm.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register step sensor TYPE_STEP_DETECTOR at fastest rate
        sm.registerListener(this, sSensor, SensorManager.SENSOR_DELAY_UI);
    }
}