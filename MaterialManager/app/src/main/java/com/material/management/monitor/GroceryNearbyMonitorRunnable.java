package com.material.management.monitor;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.material.management.BuildConfig;
import com.material.management.MaterialManagerApplication;
import com.material.management.R;
import com.material.management.data.GroceryListData;
import com.material.management.data.Material;
import com.material.management.output.NotificationOutput;
import com.material.management.service.location.LocationTrackService;
import com.material.management.service.location.LocationUtility;
import com.material.management.utils.DBUtility;
import com.material.management.utils.Utility;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class GroceryNearbyMonitorRunnable implements Runnable {
    public static final String MONITOR_THREAD_NAME = "Grocery_Nearby_Monitor_Thread";
    private static NotificationOutput sNotificationOutput = NotificationOutput.getInstance();
    /*
    /* Maximum  radius for nearby the grocery shop in kilo-meter. Default is 500 meters
     */
    public static final double MAX_NEARBY_RADIUS = 500;

    private DecimalFormat mKmFormat = new DecimalFormat("#.##");
    private DecimalFormat mMeterFormat = new DecimalFormat("###");
    private Context mContext;
    private int mNotifType;

    public GroceryNearbyMonitorRunnable(int notifType) {
        mContext = Utility.getContext();
        mNotifType = notifType;
    }

    @Override
    public void run() {
        if (BuildConfig.DEBUG) {
            Log.d(LocationTrackService.DEBUG, "GroceryNearbyMonitor is running...");
        }
        ArrayList<GroceryListData> groceryListDatas = DBUtility.selectGroceryListInfos();
        Location curLoc = LocationUtility.getsInstance().getLocation();

        for (GroceryListData groceryListData : groceryListDatas) {
            String storeName = groceryListData.getStoreName();
            String lat = groceryListData.getLat();
            String lng = groceryListData.getLong();
            boolean isNeedCheckNearby = (groceryListData.getIsAlertWhenNearBy() == 1) ? true : false;

            if(!isNeedCheckNearby || lat == null || lng == null || !Utility.isValidNumber(Double.class, lat) || !Utility.isValidNumber(Double.class, lng)) {
                return;
            }

            double dist = Utility.getDist(curLoc.getLatitude(), curLoc.getLongitude(), Double.parseDouble(groceryListData.getLat()), Double.parseDouble(groceryListData.getLong()));
            String distStr = (dist >= 1000) ? mContext.getString(R.string.format_dist_kilometer, mKmFormat.format(dist / 1000)): mContext.getString(R.string.format_dist_meter, mMeterFormat.format(dist));
            boolean isNearby = (dist >= MAX_NEARBY_RADIUS) ? false : true ;

            if (BuildConfig.DEBUG) {
                Log.d(MaterialManagerApplication.TAG, "Distance to " + storeName + " is " + dist + " meters.");
            }

            if(isNearby) {
                String address = groceryListData.getAddress();
                String uri = String.format("geo:%s,%s?q=%s,%s(%s)", lat, lng, lat, lng, address);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                sNotificationOutput.outNotif(groceryListData.hashCode(), intent, mContext.getString(R.string.format_nearby_store, storeName, distStr), mNotifType);
            }
        }
    }
}
