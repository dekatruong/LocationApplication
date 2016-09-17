package com.example.deka.locationapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.*;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.ResultReceiver;

import com.example.deka.locationapplication.service.FetchAddressIntentService;
import com.example.deka.locationapplication.service.GeofenceTransitionsIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;
//import FetchAddressIntentService;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {


    //////////////////
    //Constants

    //To Permission Request-Code
    public static class PermissionRequestCodes {
        private static final int COARSE_LOCATION_TO_GET_LASTKNOWN_LOCATION = 1;
        private static final int ACCESS_FINE_LOCATION_TO_UPDATE_LOCATION = 2;
    }


    //To Other App Request-Code (onActivityResult)
    //private static final int PICK_CONTACT_REQUEST = 11;
    //private static final int CHECK_SETTINGS_REQUEST = 12;
    public static class ActivityRequestCodes {
        private static final int CHECK_SETTINGS_REQUEST = 1;
        private static final int PICK_CONTACT_REQUEST = 2;
    }

    ;

    //Data To Bundle Key (for Resume, Pause)
    public static class StateKeys {
        public static final String CLASS_PATH = MainActivity.class.getCanonicalName();//"com.example.deka.locationapplication.MainActivity";

        private static final String REQUESTING_LOCATION_UPDATES_KEY =
                CLASS_PATH + ".mIsRequestingLocationUpdating";
        private static final String LOCATION_KEY =
                CLASS_PATH + ".mCurrentLocation";
        private static final String LAST_UPDATED_TIME_STRING_KEY =
                CLASS_PATH + ".mLastUpdateTime";
    }

    ////////////////////////////////////////////////////////////////////////////////

    //Data
    private Location mCurrentLocation;
    private String mLastUpdateTime;

    //State Flag
    private boolean mIsRequestingLocationUpdating = false;
    private boolean mIsGeofenceTransitioning = false;
    //private boolean mIsAddressRequestFlag = false;

    //GUI
    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private TextView mLastUpdateTimeTextView;

    /////////
    //Dependency

    //Google Api
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    //Address From Location
    protected Location mLastLocation;
    private ResultReceiver mFetchAddressResultReceiver; //FetchAddress Receiver

    //Geofence Transition
    List<Geofence> mGeofenceList;
    PendingIntent mGeofencePendingIntent;

    /**
     * Note: try @SuppressLint("ParcelCreator") class MyResultReceiver extends ResultReceiver {
     * ... but seriously consider simplified architecture (without two Services) – pskink
     */
    @SuppressLint("ParcelCreator")
    public class AddressResultReceiver extends ResultReceiver {

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            String mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                (MainActivity.this).displayAddressOutput(mAddressOutput);
            } else {
                //Case address not found: show error. TO do
                (MainActivity.this).showToast(mAddressOutput);
            }

        }

    }

    //On-GooogleApi-Connected pending jobs
    Queue<Command> onGoogleApiConnectedPendingJobs = new LinkedList<Command>();

    ////////////////////////////////////////////////////////////////////////////////////////
    //region Event Handler

    ////////////
    //region Activity Event
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MyApp", "onCreate");

        setContentView(R.layout.activity_main);
        ////////////////////////////////
        //View
        this.mLatitudeTextView = (TextView) findViewById(R.id.latitude);
        this.mLongitudeTextView = (TextView) findViewById(R.id.longitude);
        this.mLastUpdateTimeTextView = (TextView) findViewById(R.id.updatetime);
        ///////////////////////////////

        // Create an instance of GoogleApiClient.
        //this.connectGoogleApi();

        // Restore state (Update Location)
        //Log.i("MyApp", "savedInstanceState: "+savedInstanceState);
        //this.updateValuesFromBundle(savedInstanceState);

        // Case no LocationRequest instance: Create LocationRequest
        if (null == this.mLocationRequest) {
            this.createLocationRequest();
        }

        //Fetch Address
        this.mFetchAddressResultReceiver = new AddressResultReceiver(new Handler());

        //Geofence
        //load and Set GeofenceList List
        this.mGeofenceList = this.createGeofenceList();

    }

    /**
     * To do: load from something interesting
     * @return
     */
    private Map<String, LatLng> getPointsFromSomeWhere() {
        Map<String, LatLng> geofences = new LinkedHashMap();

        //YW Location
        geofences.put("youngworld", new LatLng(10.7875581d,106.6503599d));

        //Home Location
        geofences.put("youngworld", new LatLng(106.7875581d,106.6503599d));

        //other Location

        //
        return geofences;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (null != this.mGoogleApiClient) this.mGoogleApiClient.disconnect();

        ///////////////////
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //////////////////////////////////////
        this.stopLocationUpdates();

        //Temporarily only
        //this.stopGeofenceTransitions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MyApp", "onResume");
        ////////////////////////////////
        //LocationUpdate
        if(this.mIsRequestingLocationUpdating) {
            this.startLocationUpdates();
        }
        //GeofenceTransition
        if (this.mIsGeofenceTransitioning) {
            this.startGeofenceTransitions();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i("MyApp", "onSaveInstanceState");

        //UpdateLocationRequest
        savedInstanceState.putBoolean(StateKeys.REQUESTING_LOCATION_UPDATES_KEY,
                this.mIsRequestingLocationUpdating);
        savedInstanceState.putParcelable(StateKeys.LOCATION_KEY,
                this.mCurrentLocation);
        savedInstanceState.putString(StateKeys.LAST_UPDATED_TIME_STRING_KEY,
                mLastUpdateTime);

        //Something else


        //Log.i("MyApp", "savedInstanceState: " + savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.i("MyApp", "onRestoreInstanceState");

        Log.i("MyApp", "savedInstanceState: " + savedInstanceState);

        this.updateValuesFromBundle(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            //Task: get LastKnownLocation
            case PermissionRequestCodes.COARSE_LOCATION_TO_GET_LASTKNOWN_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    this.getLastKnownLocation(); //To do: Not check again


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            //Task: UPDATE_LOCATION
            case PermissionRequestCodes.ACCESS_FINE_LOCATION_TO_UPDATE_LOCATION: {
                // Check if request is accepted/cancelled
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case ActivityRequestCodes.PICK_CONTACT_REQUEST:
                // Make sure the request was successful
                if (resultCode == Activity.RESULT_OK) {
                    // The user picked a contact.
                    // The Intent's data Uri identifies which contact was selected.

                    // Do something with the contact here (bigger example below)
                }
            case ActivityRequestCodes.CHECK_SETTINGS_REQUEST:
                // Make sure the request was successful
                if (resultCode == Activity.RESULT_OK) {
                    //Log
                    Log.i("MyApp", "User has enabled Gps");
                }

        }
    }
    //endregion
    //////////////////////

    //region Google Api EventHandler
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Log.i("MyApp", "GoogleApi-onConnected-event handler");

        //Do pending jobs
        //Log.i("MyApp", "Number of jobs in queue:" + onGoogleApiConnectedPendingJobs.size());
        while (!onGoogleApiConnectedPendingJobs.isEmpty()) {
            Command job = onGoogleApiConnectedPendingJobs.poll();
            //Log.i("MyApp", "do job:" + job);
            job.execute();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //endregion

    //////////////////////

    //region Other Event

    @Override
    public void onLocationChanged(Location location) {
        Log.i("MyApp", "onLocationChanged");

        //update data
        this.mCurrentLocation = location;
        this.mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        //update UI
        updateCurrentLocationToUI();
    }

    //endregion
    //////////////////////
    //region View Action Event

    /**
     * Event Handler
     * Button Click
     * Called when the user clicks the button
     *
     */
    public void onClick_UpdateLocationButton_Handler(View view) {
        Log.i("MyApp", "onClick_UpdateLocationButton_Handler");

        /////////////////////////////////
        //Request Location Updates
        //Get Last Known Location,
        this.getLastKnownLocation();

        //  check and request gps
        this.checkGpsSetting();

        //  Requesting Location Updates
        this.startLocationUpdates();

        //startFetchAddressIntentService
        this.fetchAddressFromLastKnownLocation();

        //start GeofenceTransitions
        this.startGeofenceTransitions();

    }


    //endregion
    //////////////////////


    //endregion
    //////////////////////////////////////////////////////////
    //Own method

    private void connectGoogleApi() {
        // Create an instance of GoogleApiClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        //Connect
        mGoogleApiClient.connect();
    }

    private void connectGoogleApiAndAddPendingTask() {
        String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
        this.addToGoogleApiConnectedPendingJobs(methodName);
        this.connectGoogleApi();
    }

    private boolean isConnectedGoogleApi() {
        if (this.mGoogleApiClient != null
                && this.mGoogleApiClient.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add to Jobs Queue
     * @param methodName
     * @param agrs
     */
    private void addToGoogleApiConnectedPendingJobs(String methodName, Object[]... agrs) {
        try {
            this.onGoogleApiConnectedPendingJobs.add(
                    new Command(this, methodName, agrs)
            );
        } catch (NoSuchMethodException e) {
            //e.printStackTrace(); //To do
            Log.e("MyApp", "NoSuchMethodException: method " + methodName + ", arguments length: " + agrs.length);
        }
    }

    private void addToGoogleApiConnectedPendingJobs(Object[]... agrs) {
        String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
        //String methodName = this.getClass().getEnclosingMethod().getName();

        this.addToGoogleApiConnectedPendingJobs(methodName, agrs);
    }

    /**
     * Check permission, get LastKnownLocation, and update UI
     *
     * @return 0 - not permission, 1 - success. To do
     */
    private int getLastKnownLocation() {
        //Log.i("MyApp", "getLastKnownLocation");

        //Check Permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            //Check if connected GoogleApi
            if (this.isConnectedGoogleApi()) {
                ////
                //Log.i("MyApp", "Connected GoogleApi, getLastKnownLocation");
                //Do
                this.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
                //Log.i("MyApp", "LastKnownLocation: " + this.mLastLocation);

                //show. To do: should be not here
                this.updateLastKnownLocationToUI(this.mLastLocation);
                ////
            } else {
                //Log.i("MyApp", "Connect GoogleApi and add this job To GoogleApiConnectedPendingJobs Queue");

                //Connect GoogleApi
                this.connectGoogleApi();

                //add this job To GoogleApiConnectedPendingJobs Queue
                //Thread.currentThread().getStackTrace()[2].getMethodName()
                this.addToGoogleApiConnectedPendingJobs();
            }

            return 1;
        } else {
            //Case has not permission: request permission. To do: in other thread
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION //Location
                    },
                    PermissionRequestCodes.COARSE_LOCATION_TO_GET_LASTKNOWN_LOCATION);

            //To do: method to re-do this function after grant permission

            return 0;
        }

    }

    private void updateLastKnownLocationToUI(Location mLastLocation) {
        if (mLastLocation != null) {
            //GUI
            TextView mLatitudeText = (TextView) findViewById(R.id.lastknown_latitude);
            TextView mLongitudeText = (TextView) findViewById(R.id.lastknown_longitude);

            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
        }
    }

    /**
     * show popup request if has not yet. To do: should in other thread
     */
    private void checkGpsSetting() {

        Log.i("MyApp", "checkGpsSetting");

        //To do: check if GoogleApi is connected

        ////////////
        //create a LocationSettingsRequest.Builder, and add one or more location requests
        //Next check whether the current location settings are satisfied:
        //When the PendingResult returns, your app can check the location settings by looking at the status code from the LocationSettingsResult object
        LocationServices.SettingsApi.checkLocationSettings(
                this.mGoogleApiClient,
                ((new LocationSettingsRequest.Builder())
                        .addLocationRequest(this.mLocationRequest))
                        .build() //LocationSettingsRequest base on LocationRequest
                )
                .setResultCallback(new ResultCallback<LocationSettingsResult>() {
                    @Override
                    public void onResult(LocationSettingsResult result) {
                        final Status status = result.getStatus();
                        final LocationSettingsStates states = result.getLocationSettingsStates(); //to do
                        switch (status.getStatusCode()) {
                            case LocationSettingsStatusCodes.SUCCESS:
                                // All location settings are satisfied. The client can
                                // initialize location requests here.
                                //...
                                break;
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied, but this can be fixed
                                // by showing the user a dialog.
                                try {
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the checkSettingResult in onActivityResult().
                                    status.startResolutionForResult(
                                            MainActivity.this,
                                            ActivityRequestCodes.CHECK_SETTINGS_REQUEST);

                                } catch (SendIntentException e) {
                                    // Ignore the error.
                                }

                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                // Location settings are not satisfied. However, we have no way
                                // to fix the settings so we won't show the dialog.
                                //...
                                break;
                        }
                    }
                });
    }

    /**
     * setup a LocationRequest
     * @return LocationRequest
     */
    private LocationRequest createLocationRequest() {
        this.mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return mLocationRequest;
    }

    /**
     * register start listening LocationUpdate
     */
    private void startLocationUpdates() {
        Log.i("MyApp", "startLocationUpdates");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.i("MyApp", "startLocationUpdates: request permission");

            //Case has not permission: request permission. To do: in other thread
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION //Location
                    },
                    PermissionRequestCodes.ACCESS_FINE_LOCATION_TO_UPDATE_LOCATION);
        } else {
            //mark State Flag
            this.mIsRequestingLocationUpdating = true;

            //Check GoogleApi
            if (this.isConnectedGoogleApi()) {
                //Do
                PendingResult<Status> status = LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleApiClient, mLocationRequest, this);

            } else {
                //Add this Job and Connect
                this.addToGoogleApiConnectedPendingJobs();
                this.connectGoogleApi();
            }
        }

    }

    /**
     * update current location to UI
     * base on this.mCurrentLocation
     */
    private void updateCurrentLocationToUI() {
        //Log.i("MyApp","update current location to UI");

        if (null != mCurrentLocation) {
            mLatitudeTextView.setText(String.valueOf(mCurrentLocation.getLatitude()));
            mLongitudeTextView.setText(String.valueOf(mCurrentLocation.getLongitude()));
            mLastUpdateTimeTextView.setText(mLastUpdateTime);
        }
    }

    protected void stopLocationUpdates() {
        //mark Flag
        this.mIsRequestingLocationUpdating = false;

        //check if GoogleApiClient has connected
        if (null != this.mGoogleApiClient
                && this.mGoogleApiClient.isConnected()) {

            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        } else {
            //Reconnect
            this.connectGoogleApi();
            //Do when connected
            this.addToGoogleApiConnectedPendingJobs();
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i("MyApp", "updateValuesFromBundle");

        if (savedInstanceState != null) {
            Log.i("MyApp", "savedInstanceState: " + savedInstanceState.toString());
            // Update the value of mIsRequestingLocationUpdating from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(StateKeys.REQUESTING_LOCATION_UPDATES_KEY)) {
                this.mIsRequestingLocationUpdating = savedInstanceState.getBoolean(
                        StateKeys.REQUESTING_LOCATION_UPDATES_KEY);

                //to do
                //setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(StateKeys.LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                this.mCurrentLocation = savedInstanceState.getParcelable(StateKeys.LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(StateKeys.LAST_UPDATED_TIME_STRING_KEY)) {
                this.mLastUpdateTime = savedInstanceState.getString(
                        StateKeys.LAST_UPDATED_TIME_STRING_KEY);
            }

            //UI
            updateCurrentLocationToUI();
        }
    }

    private void setButtonsEnabledState() {
        //do nothing now
    }

    private void fetchAddressFromLastKnownLocation() {
        Log.i("MyApp", "fetchAddressFromLastKnownLocation");

        if (this.mLastLocation != null) {
            // Determine whether a Geocoder is available.
            if (Geocoder.isPresent()) {
                Log.i("MyApp", "start FetchAddress Intent Service");
                this.startFetchAddressIntentService();
            } else {
                Log.i("MyApp", "no geocoder available");
                Toast.makeText(this, R.string.no_geocoder_available,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * start IntentService that FetchAddress
     */
    protected void startFetchAddressIntentService() {
        if (this.isConnectedGoogleApi()) {
            //Create an Intent
            Intent intent = new Intent(this, FetchAddressIntentService.class)
                    .putExtra(Constants.RECEIVER, this.mFetchAddressResultReceiver)
                    .putExtra(Constants.LOCATION_DATA_EXTRA, this.mLastLocation);

            //start
            this.startService(intent);

        } else {
            //GoogleApi
            this.addToGoogleApiConnectedPendingJobs();
            this.connectGoogleApi();
        }
    }

    private void displayAddressOutput(String message) {
        //To do: show in other UI
        Toast.makeText(this, message,
                Toast.LENGTH_LONG).show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message,
                Toast.LENGTH_LONG).show();
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        } else {
            Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
            // calling addGeofences() and removeGeofences().
            return PendingIntent.getService(this, 0, intent, PendingIntent.
                    FLAG_UPDATE_CURRENT);
        }
    }

    /**
     *
     */
    private void startGeofenceTransitions() {
        //mark State Flag
        this.mIsGeofenceTransitioning = true;

        //Check GoogleApi
        if (this.isConnectedGoogleApi()) {
            try {
                //Call GoogleApi
                LocationServices.GeofencingApi.addGeofences(
                        mGoogleApiClient,
                        getGeofencingRequest(), //request
                        getGeofencePendingIntent() //include what service
                ).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        //
                        Toast.makeText(
                                MainActivity.this,
                                MainActivity.this.getString(R.string.geofences_added),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

            } catch (SecurityException securityException) {
                //Case has not permission: request permission. To do: in other thread
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION //Location
                        },
                        PermissionRequestCodes.ACCESS_FINE_LOCATION_TO_UPDATE_LOCATION);

                //To do: re-do this action
            }
        } else {
            //
            this.connectGoogleApiAndAddPendingTask();
        }
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    public void stopGeofenceTransitions() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    getGeofencePendingIntent()
            )// This is the same pending intent that was used in addGeofences().
            .setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    //
                    Toast.makeText(
                            MainActivity.this,
                            MainActivity.this.getString(R.string.geofences_removed),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });

        } catch (SecurityException securityException) {
            //Case has not permission: request permission. To do: in other thread
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION //Location
                    },
                    PermissionRequestCodes.ACCESS_FINE_LOCATION_TO_UPDATE_LOCATION);

            //To do: re-do this job
        }
    }

    /**
     * load and Set CircularRegion List (temp)
     */
    private List<Geofence> createGeofenceList() {
        Map<String, LatLng> points = Constants.AREAS; //to do: should get from sonewhere difference


        List<Geofence> aGeofenceList = new LinkedList<Geofence>();
        for (Map.Entry<String, LatLng> entry: points.entrySet()) {

            aGeofenceList.add(
                    new Geofence.Builder()
                            // Set the request ID of the geofence. This is a string to identify this geofence.
                            .setRequestId(entry.getKey())
                            .setCircularRegion(
                                    entry.getValue().latitude,
                                    entry.getValue().longitude,
                                    Constants.GEOFENCE_RADIUS_IN_METERS
                            )
                            .setExpirationDuration(Geofence.NEVER_EXPIRE) //never expire
                            .setTransitionTypes(
                                    Geofence.GEOFENCE_TRANSITION_ENTER |
                                            Geofence.GEOFENCE_TRANSITION_EXIT
                            )
                            .build()
            );
        }

        return aGeofenceList;
    }
}
