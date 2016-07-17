package com.martinojones.whattoeat;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Button goButton;
    private Button directions;
    private GetResturants downloadData;
    private List<Resturant> resturants;
    private Handler handler;
    private TextView resturantName;
    private TextView restAddress;
    private run DOWNLOAD;
    private boolean goButtonEnabled;
    private Resturant currectResturant;
    public static final String PREFS_NAME = "MyPrefsFile";
    String FILENAME = "WhatToEat";
    public static String LONGITUDE = "";
    public static String LATITUDE = "";

    //GPS
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
    private LocationManager locationManager;
    private LocationListener locationListener;
    Location location; // location
    double latitude; // latitude
    double longitude; // longitude


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setup GPS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationListener = new myLocationlistener();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        final SharedPreferences.Editor editor = settings.edit();




        //Setup handler
        handler = new Handler();

        //Assign UI elements
        goButton = (Button) findViewById(R.id.goButton);
        resturantName = (TextView) findViewById(R.id.resturantName);
        directions = (Button) findViewById(R.id.directions);
        restAddress = (TextView) findViewById(R.id.resturantAddress);


        //Setup UI
        resturantName.setText("PRESS SEARCH");
        goButtonEnabled = true;
        goButton.setFocusableInTouchMode(true);
        goButton.setFocusable(true);
        restAddress.setText("");
        setTitle("What To Eat");



        //Assign action listners
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkGPSPermission();

                if(goButtonEnabled == false)
                {
                    Toast.makeText(getApplicationContext(), "Please don't spam search.", Toast.LENGTH_SHORT).show();
                }

                //Check GPS Cord
                if(Double.toString(longitude).equals("0.0") && Double.toString(latitude).equals("0.0") )
                {
                    Toast.makeText(getApplicationContext(), "No GPS location yet, try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                //Assign GPS
                LONGITUDE = Double.toString(longitude);
                LATITUDE = Double.toString(latitude);

                //Reset focus
                goButton.requestFocus();

                //Set loading message
                resturantName.setText("Looking...");
                restAddress.setText("");

                //Make sure within timeout to prevent spamming
                new run(LONGITUDE, LATITUDE).execute();

            }
        });


        directions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Make sure a resturant is set or not empty
                if(currectResturant == null && currectResturant.getAddress().isEmpty())
                {
                    Toast.makeText(getApplicationContext(), "Search for a restaurant first", Toast.LENGTH_SHORT).show();
                    return;
                }

                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + currectResturant.getAddress() + " " + currectResturant.getPostalcode());

                Log.d("SEARCHADDRESS", "geo:0,0?q=" + currectResturant.getAddress() + currectResturant.getPostalcode());

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

                mapIntent.setPackage("com.google.android.apps.maps");

                // Attempt to start an activity that can handle the Intent
                startActivity(mapIntent);

            }
        });


        //Setup GPS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PackageManager.PERMISSION_GRANTED);

            return;
        }



        //GPS
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new myLocationlistener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

    }


    private class myLocationlistener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if(location != null){
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            //Log.d("LOCATION", "Locations is: LAT: " + latitude + " LONG: " + longitude);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    }


    //RUN IN BACKGROUND
    private class run extends AsyncTask<String, Void, String>
    {
        private String LONG;
        private String LATT;

        run(String longitude, String latitude)
        {
            this.LONG = longitude;
            this.LATT = latitude;
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params)
        {

            while((LONG == null || LONG.isEmpty()) && (LATT == null || LATT.isEmpty()))
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            }

            //Try to download the XML data
            try
            {
                downloadData = null;
                downloadData = new GetResturants(LONG, LATT);
                downloadData.execute();
            }
            catch(Exception ex)
            {

            }

            resturants = downloadData.getResturants();


            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Runnable updateUI = new Runnable() {
                @Override
                public void run() {
                    while(downloadData.getmDownloadStatus() != DownloadStatus.OK)
                    {
                        try
                        {
                            float deg = goButton.getRotation() + 180F;
                            goButton.animate().rotation(deg).setInterpolator(new AccelerateDecelerateInterpolator());
                        }
                        catch(Exception ex)
                        {

                        }

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {

                        }

                    }

                    goButton.animate().rotation(0).setInterpolator(new AccelerateDecelerateInterpolator());

                    mainupdateUI();

                    goButtonEnabled = false;

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {

                    }

                    goButtonEnabled = true;


                    //lockButton();
                }
            };

            new Thread(updateUI).start();

        }

    }

    private void mainupdateUI()
    {

        handler.post(new Runnable() {

            @Override
            public void run() {

                int count = resturants.size();

                //Check resturant size
                if(count == 0)
                {
                    Toast.makeText(getApplicationContext(), "None Found!", Toast.LENGTH_SHORT).show();
                    resturantName.setText("PRESS SEARCH");
                    return;
                }

                Random rand = new Random(System.currentTimeMillis());
                int randomNum = rand.nextInt(count) + 0;


                Log.d("RANDOMNUMBER", Integer.toString(randomNum));

                //Set the current resturant
                currectResturant = resturants.get(randomNum);

                //Update UI elements
                resturantName.setText(currectResturant.getName());
                restAddress.setText(currectResturant.getAddress());

            }
        });

    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }



    private void checkGPSPermission()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PackageManager.PERMISSION_GRANTED);

            return;
        }
        else
        {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationListener = new myLocationlistener();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }
    }


}
