package com.martinojones.whattoeat;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dev on 17/02/2016.
 */
public class GetResturants extends GetRawData {

    private String LOG_TAG = GetResturants.class.getSimpleName();
    private List<Resturant> resturants;
    private Uri mDestinationUri;
    private boolean done = false;
    private String LONG;
    private String LAT;
    private String distance;


    //Has GPS location
    public GetResturants(String longitude, String lattitude, String dist) {
        super(null);

        this.LONG = longitude;
        this.LAT = lattitude;
        this.distance = dist;


        String CVEDETAILS_URL = "https://api.locu.com/v1_0/venue/search/?location=" + LAT +"%2C" + LONG + "&category=restaurant&radius=" + distance +"&api_key=ab75f73e558edaf6773aea262208a9dac36a6196";
        mDestinationUri = Uri.parse(CVEDETAILS_URL);

        resturants = new ArrayList<>();
    }


    public void execute() {

        super.setmRawUrl(mDestinationUri.toString());
        DownloadJsonData downloadJsonData = new DownloadJsonData();
        Log.v(LOG_TAG, "Built URI = " + mDestinationUri.toString());
        downloadJsonData.execute(mDestinationUri.toString());

    }


    public void processResult() {
        if(getmDownloadStatus() != DownloadStatus.OK) {
            Log.e(LOG_TAG, "Error downloading raw file");
            return;
        }

        final String RESTURANTS = "objects";
        final String NAME = "name";
        final String ADDRESS = "street_address";
        final String CITY = "locality";
        final String ZIP = "postal_code";



        try {


            JSONObject jsonData = new JSONObject(getmData());
            JSONArray itemsArray = jsonData.getJSONArray(RESTURANTS);


            resturants.clear();

            for(int i=0; i<itemsArray.length(); i++) {

                JSONObject jsonVulnerability = itemsArray.getJSONObject(i);
                String title = jsonVulnerability.getString(NAME);
                String address = jsonVulnerability.getString(ADDRESS);
                String city = jsonVulnerability.getString(CITY);
                String post = jsonVulnerability.getString(ZIP);
                String web = null;




                //Make sure address contains a number to help with validation
                if(address.matches(".*\\d+.*"))
                {
                    resturants.add(new Resturant(title, address, city, post));
                }


            }

        } catch(JSONException jsone) {
            jsone.printStackTrace();
            Log.e(LOG_TAG, "Error processing Json data");
        }


        for(Resturant singlePhoto: resturants) {
            Log.v(LOG_TAG, singlePhoto.toString());
        }

        Log.e(LOG_TAG, "DONE processing vulnerabilities. " + Boolean.toString(done));

    }

    public class DownloadJsonData extends DownloadRawData {

        protected void onPostExecute(String webData) {
            super.onPostExecute(webData);
            processResult();

        }

        protected String doInBackground(String... params) {
            return super.doInBackground(params);
        }

    }


    public boolean getDone()
    {
        return this.done;
    }

    public List<Resturant> getResturants()
    {
        return this.resturants;
    }


}