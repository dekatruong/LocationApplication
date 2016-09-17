package com.example.deka.locationapplication.service;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.example.deka.locationapplication.Constants;
import com.example.deka.locationapplication.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Deka on 13/09/2016.
 */
public class FetchAddressIntentService extends IntentService {

    private static final String TAG = "FetchAddressIS";
    private static final String NAME = "FetchAddressIS";

    protected ResultReceiver mReceiver;


    public FetchAddressIntentService() {
        super(FetchAddressIntentService.NAME);
    }

    //@Override
    protected void onHandleIntent(Intent intent) {
        //Get receiver
        this.mReceiver = intent.getParcelableExtra(
                Constants.RECEIVER);

        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra(
                Constants.LOCATION_DATA_EXTRA);

        /////////////////////////////////////
        String resultMessage = "";

        //geocoder
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        //...
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1 // In this sample, get just a single address.
            );
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            resultMessage = getString(R.string.service_not_available);

            Log.e(TAG, resultMessage + ":" + ioException.getMessage());
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            resultMessage = getString(R.string.invalid_lat_long_used);

            Log.e(TAG, resultMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size() <= 0) {
            //Case no error
            if(resultMessage.isEmpty()){
                resultMessage = getString(R.string.no_address_found);

                Log.i(TAG, resultMessage);
            }

            //deliver
            deliverResultToReceiver(Constants.FAILURE_RESULT, resultMessage);
        } else {
            Address address = addresses.get(0); //get 1st
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }

            Log.i(TAG, getString(R.string.address_found));

            //deliver
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"),
                            addressFragments));
        }
    }


    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);

        this.mReceiver.send(resultCode, bundle);
    }
}
