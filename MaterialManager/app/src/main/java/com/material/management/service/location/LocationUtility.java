package com.material.management.service.location;


import android.content.Context;

import android.location.Location;

import android.os.Bundle;


public class LocationUtility extends BestLocationListener {
    private static Context sContext;
    private static LocationUtility sInstance = null;

    private Location mLastKnowLocation = null;
    private BestLocationProvider mBestLocationProvider = null;

    private LocationUtility(){
    }

    public static void init(Context context) {
        sContext = context;
    }

    public static LocationUtility getsInstance() {
        if(sInstance == null) {
            sInstance = new LocationUtility();
            sInstance.mBestLocationProvider = new BestLocationProvider(sContext, true, true, 0, 100, 2, 0);
        }
        return sInstance;
    }

    public boolean isLocationEnabled() {
        /*
        * Another provider type LocationManager.NETWORK_PROVIDER, maybe we can switch different
        * provider type for different purpose. e.g: power saving, exactly location and so on.
        * */
        return sInstance.mBestLocationProvider.isLocationEnabled();
    }

    public Location getLocation() {
        mLastKnowLocation = mBestLocationProvider.getLastKnowLocation();
        mBestLocationProvider.initLocationManager();
        mBestLocationProvider.startLocationUpdatesWithListener(sInstance);

        /* TODO: Use fake location if we can't retrieve any address, then we give it taipei station location*/
        if (mLastKnowLocation == null) {
            mLastKnowLocation = new Location("");

            mLastKnowLocation.setLatitude(25.048346);
            mLastKnowLocation.setLongitude(121.516396);
        }

        return mLastKnowLocation;
    }

    @Override
    public void onLocationUpdate(Location location, BestLocationProvider.LocationType type, boolean isFresh) {
        mLastKnowLocation = location;
        /*
        * If we get a new location, then we stop update.
        * Either the location is from GPS or Network.
        * */
        mBestLocationProvider.stopLocationUpdates();
    }

    @Override
    public void onLocationUpdateTimeoutExceeded(BestLocationProvider.LocationType type) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
