package com.martinojones.whattoeat;

import android.Manifest;
import android.app.Activity;
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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    //UI Elements
    private Button goButton;
    private Button directions;
    private TextView resturantName;
    private TextView restAddress;
    private SeekBar distanceBar;
    private TextView distanceValue;
    private FloatingActionButton shareButton;

    private GetResturants downloadData;
    private List<Resturant> resturants;
    private Handler handler;

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
    String distance;


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

        //This is for the GPS error message
        checkGPS();

        //Assign UI elements
        goButton = (Button) findViewById(R.id.goButton);
        resturantName = (TextView) findViewById(R.id.resturantName);
        directions = (Button) findViewById(R.id.directions);
        restAddress = (TextView) findViewById(R.id.resturantAddress);
        distanceBar = (SeekBar) findViewById(R.id.seekBar);
        distanceValue = (TextView) findViewById(R.id.distanceValue);
        shareButton = (FloatingActionButton) findViewById(R.id.shareButton);


        //Setup UI
        resturantName.setText("PRESS SEARCH");
        goButtonEnabled = true;
        goButton.setFocusableInTouchMode(true);
        goButton.setFocusable(true);
        distanceBar.setProgress(settings.getInt("DISTANCE", 10));
        updateDistance(settings.getInt("DISTANCE", 10));
        restAddress.setText("");

        //setTitle("What To Eat");



        //Assign action listners
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkGPSPermission();

                if(goButtonEnabled == false)
                {
                    Toast.makeText(getApplicationContext(), "Please don't spam search.", Toast.LENGTH_SHORT).show();
                    return;
                }

                goButtonEnabled = false;

                //Check GPS Cord
                if(Double.toString(longitude).equals("0.0") && Double.toString(latitude).equals("0.0") )
                {
                    Toast.makeText(getApplicationContext(), "No GPS, try again.", Toast.LENGTH_SHORT).show();
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


                new run(LONGITUDE, LATITUDE, distance).execute();

            }
        });


        directions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Make sure a resturant is set or not empty
                if(currectResturant == null || currectResturant.getAddress().isEmpty())
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

        distanceBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){

                //1 is the lowest distance
                if(progress < 1)
                {
                    progress = 1;
                }

                progressChanged = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                editor.putInt("DISTANCE", progressChanged);
                editor.commit();
                updateDistance(progressChanged);
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currectResturant != null)
                {
                    String sendMessage = currectResturant.getName() + "\n" + currectResturant.getAddress();
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, sendMessage);
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.app_name)));
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Search for a restaurant first", Toast.LENGTH_SHORT).show();
                    return;
                }
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

    //RUn this method to update the distance value and seekbar text
    private void updateDistance(int distanceChange)
    {
        distance = Integer.toString((int) (distanceChange * 1609.344));


        distanceValue.setText(distanceChange + " Miles");



        Log.d("DISTANCE", "Meters: " + distance);
        Log.d("DISTANCE", "Miles: " + distanceChange);
    }

    //This will update the UI if there's no GPS signal
    private void checkGPS()
    {
        Runnable check = new Runnable() {
            @Override
            public void run() {
                while(true)
                {
                    //Run the runnable using handler
                    updateUI();

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

            }
        };

        new Thread(check).start();
    }



    //Used to get GPS location
    private class myLocationlistener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if(location != null){
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
            else
            {
                latitude = 0.0;
                longitude = 0.0;
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
        private String distance;
        private String ZIPCODE;

        run(String longitude, String latitude, String dist)
        {
            this.LONG = longitude;
            this.LATT = latitude;
            this.distance = dist;
        }

        run(String zipcode, String dist)
        {
            this.ZIPCODE = zipcode;
            this.distance = dist;
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
                downloadData = new GetResturants(LONG, LATT, distance);
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



                    mainupdateUI();

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {

                    }


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
                goButton.animate().rotation(0).setInterpolator(new AccelerateDecelerateInterpolator());
                goButtonEnabled = true;

            }
        });

    }

    private void updateUI()
    {
        try
        {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(latitude == 0.0 || longitude == 0.0)
                    {
                        goButton.setText("NO GPS");
                        Log.d("LOCATION", "NO GPS");
                    }
                    else
                    {
                        goButton.setText("");
                    }

                }
            });
        }
        catch(NullPointerException ex)
        {

        }

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
