package com.example.deka.locationapplication;

import com.google.android.gms.maps.model.LatLng;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Deka on 13/09/2016.
 */
public final class Constants {
    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;
    public static final String PACKAGE_NAME =
            "com.example.deka.locationapplication";

    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME +
            ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME +
            ".LOCATION_DATA_EXTRA";
    ////////
    //Geofence
    public static final float GEOFENCE_RADIUS_IN_METERS = 100;
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = 1000;

    /**
     * Map for storing information Center-LatLng of areas.
     */
    public static final Map<String, LatLng> AREAS = new LinkedHashMap<String, LatLng>();
    static {
        //YW Location
        AREAS.put("youngworld", new LatLng(10.7875581d, 106.6503599d));
        //Home Location
        AREAS.put("youngworld", new LatLng(106.7875581d, 106.6503599d));
    }
}
