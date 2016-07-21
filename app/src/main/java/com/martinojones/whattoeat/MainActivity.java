package com.martinojones.whattoeat;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
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
import android.provider.Browser;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    //UI Elements
    private Button goButton;
    private Button directions;
    private TextView resturantName;
    private TextView restAddress;
    private SeekBar distanceBar;
    private TextView distanceValue;
    private FloatingActionButton shareButton;
    private Button websiteButton;

    private GetResturants downloadData;
    private List<Resturant> resturants;
    private Handler handler;

    private run DOWNLOAD;
    private boolean goButtonEnabled;
    private Resturant currectResturant;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static String LONGITUDE = "";
    private static String LATITUDE = "";

    //GPS
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double latitude; // latitude
    private double longitude; // longitude
    private String distance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Check permission for location
        checkGPSPermission();

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        final SharedPreferences.Editor editor = settings.edit();

        //Ask for feedback
        int feedback = settings.getInt("FEEDBACK", 0);

        //If feedback is 0 then prompt
        if (feedback == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("Beta Feedback");
            builder.setMessage("Can you provide feedback?");

            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {

                    //Launch the feedback form
                    String url = "https://drive.google.com/open?id=1Yp4Ai8NKOT-bFeRrzX4ClvCPCk_fKCM3L5vVwOCws2w";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);

                    editor.putInt("FEEDBACK", 1);
                    editor.commit();

                    dialog.dismiss();
                }
            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //They said no
                    dialog.dismiss();
                }
            });

            builder.show();


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
            websiteButton = (Button) findViewById(R.id.buttonWebsite);


            //Setup UI
            resturantName.setText("PRESS SEARCH");
            goButtonEnabled = true;
            goButton.setFocusableInTouchMode(true);
            goButton.setFocusable(true);
            distanceBar.setProgress(settings.getInt("DISTANCE", 10));
            updateDistance(settings.getInt("DISTANCE", 10));
            websiteButton.setVisibility(View.INVISIBLE);
            restAddress.setText("");


            //Assign action listners
            goButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    checkGPSPermission();

                    if (goButtonEnabled == false) {
                        Toast.makeText(getApplicationContext(), "Please don't spam search.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    goButtonEnabled = false;

                    //Check GPS Cord
                    if (Double.toString(longitude).equals("0.0") && Double.toString(latitude).equals("0.0")) {
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
                    if (currectResturant == null || currectResturant.getAddress().isEmpty()) {
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

                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    //1 is the lowest distance
                    if (progress < 1) {
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

            websiteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //Make sure the website isn't null
                    if(!(currectResturant.getWebsite() == null))
                    {
                        launchWebsite();

                    }
                }
            });

            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currectResturant != null) {
                        String sendMessage = currectResturant.getName() + "\n" + currectResturant.getAddress();
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, sendMessage);
                        sendIntent.setType("text/plain");
                        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.app_name)));
                    } else {
                        Toast.makeText(getApplicationContext(), "Search for a restaurant first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            });


            //checkGPSPermission();
        /*
        //Setup GPS
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_FINE_LOCATION},
                    PackageManager.PERMISSION_GRANTED);

            return;
        }


        //GPS
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new myLocationlistener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
        */

        }
    }

    //Launch the website of the rest if they have one
    private void launchWebsite() {
        String url = currectResturant.getWebsite();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browserIntent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
        startActivity(browserIntent);

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
                    notifyLocationUI();

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

            }
        };

        //Run in background as this is on UI thread
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


    //RUN IN BACKGROUND - DOES ALL THE WORK
    private class run extends AsyncTask<String, Void, String>
    {
        private String LONG;
        private String LATT;
        private String distance;

        run(String longitude, String latitude, String dist)
        {
            this.LONG = longitude;
            this.LATT = latitude;
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

                    //Wait for the download of the JSON data to finish
                    while(downloadData.getmDownloadStatus() != DownloadStatus.OK)
                    {
                        try
                        {
                            //Continue roating the go button while downaloding
                            float deg = goButton.getRotation() + 180F;
                            goButton.animate().rotation(deg).setInterpolator(new AccelerateDecelerateInterpolator());
                        }
                        catch(Exception ex)
                        {

                        }

                        //Sleep, adds slight pause during rotating
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {

                        }

                    }


                    //Set the current restaurant and update the UI
                    setCurrentRestaurant();

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {

                    }

                }
            };

            //Run the above code in a new thread as this is on the UI thread
            new Thread(updateUI).start();

        }

    }

    //This method is for getting the current restaurant and setting it in the UI
    private void setCurrentRestaurant()
    {

        handler.post(new Runnable() {

            @Override
            public void run() {

                int count = resturants.size();

                //Check restaurant size
                if(count == 0)
                {
                    Toast.makeText(getApplicationContext(), "None Found!", Toast.LENGTH_SHORT).show();
                    resturantName.setText("PRESS SEARCH");
                    goButtonEnabled = true;
                    return;
                }

                //Found restaurant, get random number
                Random rand = new Random(System.currentTimeMillis());
                int randomNum = rand.nextInt(count) + 0;


                Log.d("RANDOMNUMBER", Integer.toString(randomNum));

                //Set the current restaurant
                currectResturant = resturants.get(randomNum);

                //Update UI elements
                resturantName.setText(currectResturant.getName());
                restAddress.setText(currectResturant.getAddress());
                goButton.animate().rotation(0).setInterpolator(new AccelerateDecelerateInterpolator());

                //If they have a website then enable the website button
                if(!(currectResturant.getWebsite() == null))
                {
                    websiteButton.setVisibility(View.VISIBLE);
                    websiteButton.setEnabled(true);
                }
                else
                {
                    websiteButton.setVisibility(View.INVISIBLE);
                    websiteButton.setEnabled(false);
                }

                goButtonEnabled = true;

            }
        });

    }

    //This method is used to update the UI if the app can get the location or not
    private void notifyLocationUI()
    {
        try
        {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(latitude == 0.0 || longitude == 0.0)
                    {
                        goButton.setText("NO LOCATION");
                        Log.d("LOCATION", "NO LOCATION");
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


    //This method is for checking and requesting GPS and network provider location
    private void checkGPSPermission()
    {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
            return;
        }
        else
        {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationListener = new myLocationlistener();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d("PERMISSION", "GRANTED");

                    //Restart app
                    Intent i = getBaseContext().getPackageManager().
                            getLaunchIntentForPackage(getBaseContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);

                }
                else
                {

                    Log.d("PERMISSION", "DENIED");

                    //Kill application
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


}
