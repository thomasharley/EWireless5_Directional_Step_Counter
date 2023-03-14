package com.example.directionalstepcounter;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * This Compass class handles the operations of the Compass screen.
 *
 * @author Thomas Harley (s1810956@ed.ac.uk)
 */

public class Compass extends Fragment implements SensorEventListener {
    // Declare managers
    LocationManager locationManager;
    LocationListener locationListener;

    // Declare views
    TextView textDegrees, textCoordinates, textLocation, textElevation, textAccuracy, textDebug;
    ImageView imageViewCompass, imageViewAccuracy;
    GradientDrawable shape; // shape to display accuracy colour

    // Declarations for location data (geocoder)
    Geocoder geocoder;
    List<Address> addresses;

    // Declare sensors
    private SensorManager sm;
    private Sensor aSensor;
    private Sensor mSensor;
    private Sensor gSensor;
    private static final int REQUEST_ID_LOCATION_PERMISSION = 99;

    // Declare all variables
    String direction2, coord, coord2;
    private float gravity [] = new float[3]; // for isolated gravity force calculated from accelerometer
    float[] magneticFieldValues = new float[3];
    float[] gyroscopeValues = new float[3];
    double azimuth, previousazimuth, previoustime;
    double MA_gyroscope, MA_azimuth, MA_degree;

    CompassListener activitycommander; // Declare interface listener

    public interface CompassListener {

        // Interface to communicate data from fragment -> main activity
        void DegreeData(float degree);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activitycommander = (CompassListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // This executes on creation of the app
        super.onCreate(savedInstanceState);
        geocoder = new Geocoder(getContext(), Locale.getDefault()); // get geocoder
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_compass, container, false);

        // Initialisation for text and image views
        textDegrees = view.findViewById(R.id.textDegrees);
        textCoordinates = view.findViewById(R.id.textCoordinates);
        textLocation = view.findViewById(R.id.textLocation);
        textElevation = view.findViewById(R.id.textelevation);
        textAccuracy = view.findViewById(R.id.textAccuracy);
        textDebug = view.findViewById(R.id.textDebug);
        imageViewCompass = view.findViewById(R.id.imageViewCompass);
        imageViewAccuracy = view.findViewById(R.id.imageViewAccuracy);
        shape = (GradientDrawable) imageViewAccuracy.getDrawable();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        askPermissions(); // ask and fetch appropriate permissions
        initialisation(); // initialisation function
        calculateOrientation(); // calculate orientation of compass (when app first started)
    }

    private void askPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            // Check if we have location permission
            int CoarseLocationPermission = ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            int FineLocationPermission = ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION);
            int internetPermission = ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.INTERNET);

            if (CoarseLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    internetPermission != PackageManager.PERMISSION_GRANTED ||
                    FineLocationPermission != PackageManager.PERMISSION_GRANTED ) {
                // If don't have permission prompt user
                this.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.INTERNET
                        },
                        REQUEST_ID_LOCATION_PERMISSION
                );
                return;
            }
        }
        // Check Location Permissions
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
    }

    // When you have the request results
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_ID_LOCATION_PERMISSION: {
                // Note: If request is cancelled, the result arrays are empty.
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    // Message to indicate permission granted
                    Toast.makeText(getContext(), "Permission Granted", Toast.LENGTH_LONG).show();

                }
                // Cancelled or deleted
                else {
                    // Message to indicate permission denied
                    Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @SuppressLint("MissingPermission") // suppression of permissions warning
    private void initialisation() {

        // Call each required sensor when the activity starts
        sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        if(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) !=null ) {
            aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sm.registerListener(this,aSensor,SensorManager.SENSOR_DELAY_UI);
        }
        if(sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) !=null) {
            gSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sm.registerListener(this,gSensor,SensorManager.SENSOR_DELAY_UI);
        }
        if(sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) !=null ) {
            mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            sm.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_UI);
        }

        // Initialisation of location manager/ listener
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            public void onLocationChanged(@NonNull Location location) {
                if(location != null) {

                    double tlat = location.getLatitude(); // latitude
                    double tlong = location.getLongitude(); // longitude
                    try {
                        addresses = geocoder.getFromLocation(tlat, tlong, 1);
                    } catch (IOException e) { e.printStackTrace(); }

                    // Manipulation of variables, round to 5 decimal places
                    double alt = location.getAltitude();
                    tlat = (double)Math.round(tlat * 100000d) / 100000d;
                    tlong = (double)Math.round(tlong * 100000d) / 100000d;

                    // Determine whether latitude/longitude is N,S,E,W depending on positive or negative sign
                    if (tlat >= 0) { coord = "N"; }
                    else { coord = "S"; tlat = -tlat; }
                    if (tlong >= 0) { coord2 = "E"; }
                    else { coord2 = "W"; tlong = -tlong; }

                    // Determine city and country from geocoder and set appropriate text views
                    if (addresses != null && addresses.size() > 0) {
                        String city = addresses.get(0).getSubAdminArea();
                        String country = addresses.get(0).getCountryCode();
                        textLocation.setText(city + ", " + country); // Setting textview
                    }

                    textCoordinates.setText(tlat + "°" + coord + ", " + tlong + "°" + coord2); // Set coordinates textview
                    int altitude = (int) alt; // cast alt to integer
                    textElevation.setText(altitude + " m Elevation"); // Set elevation text view
                }
            }
        };
        // Fetch location updates from GPS_PROVIDER
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        // Triggered every time sensors update
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) { // update gyroscope values
            gyroscopeValues[0] = event.values[0];
            gyroscopeValues[1] = event.values[1];
            gyroscopeValues[2] = event.values[2];
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) { // update magnetometer values
            magneticFieldValues[0] = event.values[0];
            magneticFieldValues[1] = event.values[1];
            magneticFieldValues[2] = event.values[2];
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) { // update accelerometer values
            // Here, alpha is calculated as t / (t + dT), where t is the low-pass filter's
            // time-constant and dT is the event delivery rate.
            final float alpha = (float) 0.8;

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
        }
        calculateOrientation(); // calculate compass orientation based on sensors
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void calculateOrientation() {

        // Declaration of floats for orientation data
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, gravity, magneticFieldValues);
        SensorManager.getOrientation(R, values);

         // this isolates the z axis of getOrientation().
        azimuth = -atan2(R[3], R[0]);

        calculateAccuracy(); // This calculates the overall accuracy of compass by comparing
        // getOrientation() value (magnetometer) to movement of the gyroscope.

        float degree = (float) Math.toDegrees(azimuth); // more accurate degree calculation that corrects for device tilt (vertical).
        // values[0] = (float) Math.toDegrees(values[0]);
        // values[1] = (float) Math.toDegrees(values[1]);
        // values[2] = (float) Math.toDegrees(values[2]);

        // Set the values for compass (text based) based on angles in degrees.
        if (degree >= -22.5 && degree < 22.5) { direction2 = "N"; }
        else if (degree >= 22.5 && degree < 67.5) { direction2 = "NE"; }
        else if (degree >= 67.5 && degree < 112.5) { direction2 = "E"; }
        else if (degree >= 112.5 && degree < 157.5) { direction2 = "SE"; }
        else if (degree >= -67.5 && degree < -22.5) { direction2 = "NW"; }
        else if (degree >= -112.5 && degree < -67.5) { direction2 = "W"; }
        else if (degree >= -157.5 && degree < -112.5) { direction2 = "SW"; }
        else { direction2 = "S"; }

        // Convert from -180 to 180 range to 0 to 360 range
        if(degree < 0) {
            degree = (360 + degree);
        }
        // Moving average - attempted implementation
        MA_degree = MovingAverage(MA_degree, degree, 1);

        // Print some debug text at the bottom of fragment display
        textDebug.setText(degree + "°, " + abs(MA_azimuth - MA_gyroscope));

        if(imageViewCompass != null) {
            imageViewCompass.setRotation(-(float) MA_degree); // Rotate image view of compass
        }
        activitycommander.DegreeData((float)MA_degree); // Send degree data to main activity using compass listener interface

        degree = Math.round(MA_degree); // Round degree value
        int degreeint = (int) degree; // Cast to integer
        if(textDegrees != null) {
            textDegrees.setText(degreeint + "° " + direction2); // Display degree value in text view
            // along with direction
        }
    }

    public void calculateAccuracy() {

        // Declare latest time and latest azimuth value sampled
        double newtime = System.nanoTime();
        double newazimuth = azimuth;
        // Perform conversion from rads to rads/s
        double convertedazimuth = (newazimuth - previousazimuth)/(newtime - previoustime) * 1000000000; // since this gives answer in rads/ns.
        // Update previous variables to latest values
        previoustime = newtime;
        previousazimuth = newazimuth;

        MA_azimuth = MovingAverage(MA_azimuth, abs(convertedazimuth), 6); // calculate moving average of magnetometer rotational velocity (rad/s)
        MA_gyroscope = MovingAverage(MA_gyroscope, abs(gyroscopeValues[2]), 6); // calculate moving average of gyroscope rotational velocity (rad/s)

        // These values can be adjusted as necessary
        if(abs(MA_azimuth - MA_gyroscope) > 6) {
            shape.setColor(Color.parseColor("#A02422")); // red - bad accuracy
        }
        else if (abs(MA_azimuth - MA_gyroscope) > 3) {
            shape.setColor(Color.parseColor("#DD7500")); // orange - okay accuracy
        }
        else {
            shape.setColor(Color.parseColor("#63AB62")); // green - good accuracy
        }
    }

    public double MovingAverage(double MA_curr, double new_sample, int no_of_samples) {
        // Calculate generalised moving average
        MA_curr = (MA_curr * (no_of_samples - 1) + new_sample);
        double MA_new = MA_curr / no_of_samples;
        return MA_new;
    }

    @Override
    public void onPause() {
        super.onPause();
        //sm.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register sensors at rate of approximately 60,000 us (SENSOR_DELAY_UI)
        sm.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_UI);
    }
}