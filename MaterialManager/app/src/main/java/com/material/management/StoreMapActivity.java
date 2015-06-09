package com.material.management;

import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.RequestParams;
import com.material.management.api.module.ConnectionControl;
import com.material.management.data.StoreData;
import com.material.management.utils.Utility;
import com.material.management.service.location.LocationTrackService;
import com.picasso.Callback;
import com.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class StoreMapActivity extends MMActivity implements FragmentManager.OnBackStackChangedListener {
    // if zoom level is bigger than DEFAULT_ZOOM_LEVEL, modify google map camera zoom level to DEFAULT_ZOOM_LEVEL
    private final static int DEFAULT_ZOOM_LEVEL = 16;
    private final static int CAMERA_BOUNDS_PADDING = 80;
    private static final String REQ_PLACE_SEARCH = "place_search";
    private static final String REQ_PLACE_DETAIL = "place_detail";

    private View mLayout;
    private RelativeLayout mRlEmpty;
    private RelativeLayout mRlOnLoading;
    private RelativeLayout mRlNoNetwork;
    private RelativeLayout mRlSearchStoreLayout;
    private FrameLayout mFlMapContainer;
    private SeekBar mSbNearbyDist;
    private TextView mTvNearbyDist;
    private ListView mLvNearbyStore;
    private EditText mEtStoreSearch;
    private ImageView mIvMapToggle;
    private Button mBtnSearch;

    private FragmentManager mFm;
    private MapFragment mMapFragment;
    private GoogleMap mGoogleMap;
    /* TODO: Maybe it don't need to use ArrayList instead of the LinkedHashMap */
    private LinkedHashMap<String, StoreData> mRefStoreDataMap = null;
    private HashMap<Marker, StoreData> mMarkerStoreMap = null;
    private StoreResultAdapter mStoreResultAdapter = null;
    private StoreData mSelectedStoreData = null;
    private Location mCurLocation;
    //    private Messenger mMessenger;
    /* TODO: It maybe need to be refactor. */
    private Bitmap mUserPinBitmap;
    private Bitmap mStorePinBitmap;
    private Bitmap mCornerListBitmap;
    private Bitmap mCornerMapBitmap;
    private boolean mIsLoadingFinished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayout = mInflater.inflate(R.layout.activity_pick_location_search, null);

        setContentView(mLayout);

        Intent intent = getIntent();
        setTitle(intent.getStringExtra("title"));

        getActionBar().setDisplayHomeAsUpEnabled(true);
        changeLayoutConfig(mLayout);
        findView();
        init();
        setListener();
    }

    private void findView() {
        mRlEmpty = (RelativeLayout) findViewById(R.id.rl_empty_data);
        mRlOnLoading = (RelativeLayout) findViewById(R.id.rl_on_loading);
        mRlNoNetwork = (RelativeLayout) findViewById(R.id.rl_no_network);
        mFlMapContainer = (FrameLayout) findViewById(R.id.fl_store_map_container);
        mRlSearchStoreLayout = (RelativeLayout) findViewById(R.id.rl_search_store_layout);
        mSbNearbyDist = (SeekBar) findViewById(R.id.sb_nearby_dist);
        mTvNearbyDist = (TextView) findViewById(R.id.tv_nearby_dist_text);
        mLvNearbyStore = (ListView) findViewById(R.id.lv_nearby_store);
        mEtStoreSearch = (EditText) findViewById(R.id.et_search);
        mIvMapToggle = (ImageView) findViewById(R.id.iv_toggleView);
        mBtnSearch = (Button) findViewById(R.id.btn_search);
    }

    private void init() {
        showProgressDialog(null, getString(R.string.on_loading));

        mIsLoadingFinished = false;
        mFm = getFragmentManager();
        mMapFragment = MapFragment.newInstance();
        mRefStoreDataMap = new LinkedHashMap<String, StoreData>();
        mMarkerStoreMap = new HashMap<Marker, StoreData>();
        mUserPinBitmap = initPinBitmap(R.drawable.icon_map_pin_user, 62, 78, 1080, 1920);
        mStorePinBitmap = initPinBitmap(R.drawable.icon_map_pin_store, 62, 78, 1080, 1920);
        mCornerListBitmap = initPinBitmap(R.drawable.dashboard_button_map_white, 284, 378, 1080, 1920);
        mCornerMapBitmap = initPinBitmap(R.drawable.dashboard_button_map, 284, 378, 1080, 1920);
//        mHandler = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                LocationTrackService.LocationUpdateStatus updateStatus = LocationTrackService.LocationUpdateStatus.values()[msg.what];
//
//                switch (updateStatus) {
//                    case MSG_UPDATE_TRACK_LOCATION: {
//                        showProgressDialog(null, getString(R.string.store_map_msg_location_updating));
//                        doSearch(mEtStoreSearch.getText().toString());
//                    }
//                    break;
//                }
//            }
//        };
//        mMessenger = new Messenger(mHandler);

        mSbNearbyDist.setProgress(0);
        mTvNearbyDist.setText(Integer.toString(mSbNearbyDist.getProgress() + 2));
        mIvMapToggle.setImageBitmap(mCornerMapBitmap);
        mFm.addOnBackStackChangedListener(this);
        mFm.beginTransaction().replace(R.id.fl_store_map_container, mMapFragment).addToBackStack(null).commit();
    }

    /* TODO: It need to be refactore.
    *  1.Handling bitmap in UI thread.
    *  2.Bitmap handling need to be in utility.
    * */
    private Bitmap initPinBitmap(int resourceId, int standIconWidth, int standIconHeight, int standScreenWidth, int standScreenHeight) {
        Bitmap bmp = null;
        Bitmap tmpBmp = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        int scaledWidth;
        int scaledHeight;

        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inScaled = false;
        options.inDensity = mMetrics.densityDpi;

        scaledWidth = (int) (((double) mMetrics.widthPixels / standScreenWidth) * standIconWidth);
        scaledHeight = (int) (((double) mMetrics.heightPixels / standScreenHeight) * standIconHeight);

        /* TODO: Need to be refactor */
        try {
            tmpBmp = BitmapFactory.decodeResource(getResources(), resourceId, options);
        } catch (OutOfMemoryError err) {
            err.printStackTrace();
        }
        try {
            bmp = Bitmap.createScaledBitmap(tmpBmp, scaledWidth, scaledHeight, false);
        } catch (OutOfMemoryError err) {
            err.printStackTrace();
        }

        if (tmpBmp != bmp) {
            Utility.releaseBitmaps(tmpBmp);
            tmpBmp = null;
        }

        return bmp;
    }

    private void setListener() {
        mBtnSearch.setOnClickListener(this);
        mIvMapToggle.setOnClickListener(this);
        mLvNearbyStore.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedStoreData = (StoreData) mStoreResultAdapter.getItem(position);
                RequestParams params = new RequestParams();

                params.put("key", getString(R.string.place_api_key));
                params.put("reference", mSelectedStoreData.getStoreRef());
                params.put("sensor", Boolean.toString(true));
                params.put("language", mDeviceInfo.getLanguage() + "-" + mDeviceInfo.getLocale());

                mControl.getData(ConnectionControl.PLACE_DETAIL_SEARCH, StoreMapActivity.this, params, REQ_PLACE_DETAIL);
                showProgressDialog(null, getString(R.string.data_progressing));
            }
        });
        mSbNearbyDist.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mTvNearbyDist.setText(Integer.toString(progress + 2));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(mFlMapContainer.getVisibility() == View.VISIBLE) {
//            Intent locationTrackIntent = new  Intent(StoreMapActivity.this, LocationTrackService.class);
//
//            locationTrackIntent.putExtra("ui_update_messenger", mMessenger);
//            locationTrackIntent.putExtra("is_location_track_on", true);
//            startService(locationTrackIntent);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        stopService(new Intent(StoreMapActivity.this, LocationTrackService.class));
    }


    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!mIsLoadingFinished) {
            return;
        }

        if (mRlSearchStoreLayout.getVisibility() == View.GONE) {
            mFlMapContainer.setVisibility(View.GONE);
            mIvMapToggle.setImageBitmap(mCornerMapBitmap);
            mRlSearchStoreLayout.setVisibility(View.VISIBLE);
        } else {
            mFm.beginTransaction().remove(mMapFragment).commit();
            mGoogleMap = null;
            finish();
        }
    }

    @Override
    public void onBackStackChanged() {
        /* Only searching in initial stage. */
        doSearch("");
    }

    private void doSearch(String keyword) {
        mLvNearbyStore.setVisibility(View.GONE);
        mRlEmpty.setVisibility(View.GONE);

        if (Utility.isNetworkConnected(mContext)) {
            mRlNoNetwork.setVisibility(View.GONE);
        } else {
            mRlNoNetwork.setVisibility(View.VISIBLE);
            closeProgressDialog();

            return;
        }

        if (!Utility.isLocationEnabled()) {
            showAlertDialog(null, getString(R.string.store_map_msg_err_location_disabled), getString(R.string.title_positive_btn_label), null, null, null);
        }

        RequestParams params = new RequestParams();
        /* When press search, update current position again. */
        mCurLocation = Utility.getLocation();

        params.put("key", getString(R.string.place_api_key));
        params.put("location", mCurLocation.getLatitude() + "," + mCurLocation.getLongitude());
        params.put("radius", Integer.toString(Integer.parseInt(mTvNearbyDist.getText().toString()) * 1000));
        params.put("sensor", Boolean.toString(true));
        params.put("keyword", keyword.replace(' ', ','));
        params.put("language", mDeviceInfo.getLanguage() + "-" + mDeviceInfo.getLocale());
        mControl.getData(ConnectionControl.PLACE_NEARBY_SEARCH, this, params, REQ_PLACE_SEARCH);

        mRlOnLoading.setVisibility(View.VISIBLE);
    }

    private void initMapMarker() {
        mGoogleMap = mMapFragment.getMap();

        mGoogleMap.clear();

        LatLng curLatLng = new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude());
        Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(curLatLng));
        Collection<StoreData> storeCollection = mRefStoreDataMap.values();

        marker.setTitle(getString(R.string.title_current_location));
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(mUserPinBitmap));
        mGoogleMap.getUiSettings().setZoomControlsEnabled(false);
        mGoogleMap.getUiSettings().setCompassEnabled(true);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curLatLng, 16));

        for (StoreData storeData : storeCollection) {
            String lat = storeData.getStoreLat();
            String lon = storeData.getStoreLong();

            if (!Utility.isValidNumber(Double.class, lat) || !Utility.isValidNumber(Double.class, lon)) {
                continue;
            }

            LatLng latLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
            marker = mGoogleMap.addMarker(new MarkerOptions().position(latLng));

            marker.setTitle(storeData.getStoreName());
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(mStorePinBitmap));
            mMarkerStoreMap.put(marker, storeData);
        }

        mGoogleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                fitZoomAndPositionToMapAtLeastOneMarker();
            }
        });

        mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                mSelectedStoreData = mMarkerStoreMap.get(marker);

                if (mSelectedStoreData == null) {
                    return;
                }

                RequestParams params = new RequestParams();

                params.put("key", getString(R.string.place_api_key));
                params.put("reference", mSelectedStoreData.getStoreRef());
                params.put("sensor", Boolean.toString(true));
                params.put("language", mDeviceInfo.getLanguage() + "-" + mDeviceInfo.getLocale());

                mControl.getData(ConnectionControl.PLACE_DETAIL_SEARCH, StoreMapActivity.this, params, REQ_PLACE_DETAIL);
                showProgressDialog(null, getString(R.string.data_progressing));
            }
        });

        mGoogleMap.setInfoWindowAdapter(new StoreInfoWindowAdapter());
    }

    private class StoreInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private final DecimalFormat mKmFormat;
        private final DecimalFormat mMeterFormat;
        private final View mInfoWindowLayout;
        private Marker mMarker;

        public StoreInfoWindowAdapter() {
            mKmFormat = new DecimalFormat("#.##");
            mMeterFormat = new DecimalFormat("###");
            mInfoWindowLayout = mInflater.inflate(R.layout.view_map_window_info, null);
        }

        @Override
        public View getInfoWindow(final Marker marker) {
            StoreData storeData = mMarkerStoreMap.get(marker);
            mMarker = marker;

            if (storeData == null) {
                return null;
            }

            final ImageView ivPlacePhoto = (ImageView) mInfoWindowLayout.findViewById(R.id.iv_place_photo);
            TextView tvStoreName = (TextView) mInfoWindowLayout.findViewById(R.id.tv_store_name);
            TextView tvStoreAddress = (TextView) mInfoWindowLayout.findViewById(R.id.tv_store_address);
            TextView tvStoreDist = (TextView) mInfoWindowLayout.findViewById(R.id.tv_store_dist);
            String storeLat = storeData.getStoreLat();
            String storeLong = storeData.getStoreLong();
            final String photoRef = storeData.getPhotoReference();

            tvStoreName.setText(storeData.getStoreName());
            tvStoreAddress.setText(storeData.getStoreAddress());
            ivPlacePhoto.setImageResource(R.drawable.ic_no_image_available);

            if (Utility.isValidNumber(Double.class, storeLat) && Utility.isValidNumber(Double.class, storeLong)) {
                double calculateDistance = Utility.getDist(mCurLocation.getLatitude(), mCurLocation.getLongitude(), Double.parseDouble(storeLat), Double.parseDouble(storeLong));

                if (calculateDistance >= 1000) {
                    tvStoreDist.setText(getString(R.string.title_marker_dist_info_kilo_meter, mKmFormat.format(calculateDistance / 1000)));
                } else {
                    tvStoreDist.setText(getString(R.string.title_marker_dist_info_meter, mMeterFormat.format(calculateDistance)));
                }
            }

            if (!photoRef.equals("N/A")) {
                final String photoWidth = (int) Math.max(Double.parseDouble(storeData.getPhotoWidth()), Double.parseDouble(storeData.getPhotoHeight())) + "";

                Picasso.with(mContext).load(ConnectionControl.PLACE_PHOTO_SEARCH_URL + "maxwidth=" + photoWidth + "&photoreference=" + photoRef + "&sensor=true&key=" + getString(R.string.place_api_key)).placeholder(R.drawable.ic_no_image_available).into(ivPlacePhoto, new Callback() {
                    @Override
                    public void onSuccess() {
                        getInfoContents(mMarker);
                    }

                    @Override
                    public void onError() {

                    }
                });
            }

            return mInfoWindowLayout;
        }

        @Override
        public View getInfoContents(final Marker marker) {
            if (mMarker != null
                    && mMarker.isInfoWindowShown()) {
                mMarker.hideInfoWindow();
                mMarker.showInfoWindow();
            }
            return null;
        }
    }

    public void fitZoomAndPositionToMapAtLeastOneMarker() {
        double myLat = mCurLocation.getLatitude();
        double myLon = mCurLocation.getLongitude();
        double lat = 0;
        double lon = 0;
        double latDiff = 0;
        double lonDiff = 0;
        double shortestDistance = Double.MAX_VALUE;
        Collection<StoreData> storeCollection = mRefStoreDataMap.values();

        for (StoreData data : storeCollection) {
            String storeLat = data.getStoreLat();
            String storeLong = data.getStoreLong();

            if (storeLat == null || storeLat.isEmpty() || storeLat == "N/A") {
                continue;
            }
            if (storeLong == null || storeLong.isEmpty() || storeLong == "N/A") {
                continue;
            }

            lat = Double.valueOf(storeLat);
            lon = Double.valueOf(storeLong);
            double calculateDistance = Utility.getDist(myLat, myLon, lat, lon);

            if (shortestDistance > calculateDistance) {
                shortestDistance = calculateDistance;
                latDiff = Math.abs(myLat - lat);
                lonDiff = Math.abs(myLon - lon);
            }
        }
        double maxLat = myLat + latDiff;
        double minLat = myLat - latDiff;
        double maxLon = myLon + lonDiff;
        double minLon = myLon - lonDiff;
        LatLng southWestLatLon = new LatLng(minLat, minLon);
        LatLng northEastLatLon = new LatLng(maxLat, maxLon);
        zoomInUntilAllMarkersAreStillVisible(southWestLatLon, northEastLatLon);
    }

    private void zoomInUntilAllMarkersAreStillVisible(final LatLng southWestLatLon, final LatLng northEastLatLon) {
        mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

            @Override
            public void onCameraChange(CameraPosition arg0) {
                LatLng myLatLng = new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude());

                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestLatLon, northEastLatLon), CAMERA_BOUNDS_PADDING));
                mGoogleMap.setOnCameraChangeListener(null);
                if (mGoogleMap.getCameraPosition().zoom > DEFAULT_ZOOM_LEVEL) {
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, DEFAULT_ZOOM_LEVEL));
                }
            }
        });
    }

    @Override
    public void callbackFromController(JSONObject jsonObj, String req) {
        if (req.equals(REQ_PLACE_SEARCH)) {
            try {
                if (jsonObj.getString("status").equals("OK")) {
                    JSONArray resultAry = jsonObj.getJSONArray("results");

                    mRefStoreDataMap.clear();
                    if (resultAry.length() >= 0) {
                        for (int i = 0, len = resultAry.length(); i < len; i++) {
                            JSONObject resultJson = resultAry.getJSONObject(i);
                            JSONObject geoJsonObj = resultJson.getJSONObject("geometry").getJSONObject("location");
                            JSONObject photoJsonObj = resultJson.has("photos") ? resultJson.getJSONArray("photos").getJSONObject(0) : null;
                            StoreData storeData = new StoreData();

                            storeData.setStoreName(resultJson.has("name") ? resultJson.getString("name") : "N/A");
                            storeData.setStoreRef(resultJson.has("reference") ? resultJson.getString("reference") : "N/A");
                            storeData.setStoreLat(geoJsonObj.has("lat") ? geoJsonObj.getString("lat") : "N/A");
                            storeData.setStoreLong(geoJsonObj.has("lng") ? geoJsonObj.getString("lng") : "N/A");
                            storeData.setStoreAddress((resultJson.has("vicinity")) ? resultJson.getString("vicinity") : "N/A");
                            storeData.setStoreRate((resultJson.has("rating")) ? resultJson.getString("rating") : "N/A");

                            if (photoJsonObj != null) {
                                storeData.setPhotoReference(photoJsonObj.getString("photo_reference"));
                                storeData.setPhotoWidth(photoJsonObj.getString("width"));
                                storeData.setPhotoHeight(photoJsonObj.getString("height"));
                            } else {
                                storeData.setPhotoReference("N/A");
                                storeData.setPhotoWidth("N/A");
                                storeData.setPhotoHeight("N/A");
                            }

                            mRefStoreDataMap.put(storeData.getStoreRef(), storeData);
                        }

                        if (mStoreResultAdapter == null) {
                            mStoreResultAdapter = new StoreResultAdapter(mRefStoreDataMap.values());
                            mLvNearbyStore.setAdapter(mStoreResultAdapter);
                        } else {
                            mStoreResultAdapter.clear();
                            mStoreResultAdapter.addAll(mRefStoreDataMap.values());
                            mStoreResultAdapter.notifyDataSetChanged();
                        }

                        mLvNearbyStore.setVisibility(View.VISIBLE);
                        mRlOnLoading.setVisibility(View.GONE);
                        initMapMarker();
                        closeProgressDialog();

                        if (mStoreResultAdapter.getCount() == 0) {
                            mRlEmpty.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    /* Other error status
                       ZERO_RESULTS 表示搜索成功，但未返回任何结果。如果在搜索中传递了一个偏远位置的 latlng，就可能会出现这种情况。
                       OVER_QUERY_LIMIT 表示您超出了配额。
                       REQUEST_DENIED 表示您的请求遭到拒绝，这通常是由于缺少 sensor 参数造成的。
                       INVALID_REQUEST 通常表示缺少必填的查询参数（location 或 radius）。
                    */
                    mLvNearbyStore.setVisibility(View.GONE);
                    mRlOnLoading.setVisibility(View.GONE);
                    mRlEmpty.setVisibility(View.VISIBLE);

                    showAlertDialog(null, getString(R.string.store_map_msg_err_retry), getString(R.string.title_positive_btn_label), null, null, null);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (req.equals(REQ_PLACE_DETAIL)) {
            try {
                if (jsonObj.getString("status").equals("OK")) {
                    JSONObject resultJsonObj = jsonObj.getJSONObject("result");
                    Intent resultIntent = new Intent();

                    mSelectedStoreData.setStoreAddress(resultJsonObj.has("formatted_address") ? resultJsonObj.getString("formatted_address") : "");
                    mSelectedStoreData.setStorePhone(resultJsonObj.has("formatted_phone_number") ? resultJsonObj.getString("formatted_phone_number") : "");
                    if (resultJsonObj.has("opening_hours")) {
                        JSONObject openingTimeInfoJson = resultJsonObj.getJSONObject("opening_hours");

                        if (openingTimeInfoJson.has("periods")) {
                            JSONArray daysJsonAry = openingTimeInfoJson.getJSONArray("periods");
                            StringBuilder openDaysHours = new StringBuilder("");

                            for (int i = 0, len = daysJsonAry.length(); i < len; i++) {
                                JSONObject dayJsonObj = daysJsonAry.getJSONObject(i);
                                /* TODO: I assume it is pair-exist*/
                                JSONObject openJsonObj = dayJsonObj.getJSONObject("open");
                                JSONObject closeJsonObj = dayJsonObj.getJSONObject("close");
                                /* FORMAT : [DAY1]#[open-time],[close-time]|[DAY2]#[open-time],[close-time]...*/
                                String day = openJsonObj.getString("day") + "#";
                                String hourTimes = openJsonObj.getString("time") + "," + closeJsonObj.getString("time");

                                openDaysHours.append(day + hourTimes + "|");
                            }

                            mSelectedStoreData.setStoreServiceTime(openDaysHours.substring(0, openDaysHours.length() - 1));
                        }
                    }

                    resultIntent.putExtra("store_data", mSelectedStoreData);
                    setResult(0, resultIntent);
                    finish();
                } else {
                    showAlertDialog(null, getString(R.string.store_map_msg_err_retry), getString(R.string.title_positive_btn_label), null, null, null);
                }
                closeProgressDialog();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mIsLoadingFinished = true;
        closeProgressDialog();
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        switch (id) {
            case R.id.btn_search: {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_press_bounce));
                doSearch(mEtStoreSearch.getText().toString());
            }
            break;

            case R.id.iv_toggleView: {
                int mapVisibility = mFlMapContainer.getVisibility();
//                Intent locationTrackIntent = new Intent(StoreMapActivity.this, LocationTrackService.class);

//                locationTrackIntent.putExtra("ui_update_messenger", mMessenger);
                if (mapVisibility == View.VISIBLE) {
//                    locationTrackIntent.putExtra("is_location_track_on", false);
                    mFlMapContainer.setVisibility(View.GONE);
                    mRlSearchStoreLayout.setVisibility(View.VISIBLE);

                    mIvMapToggle.setImageBitmap(mCornerMapBitmap);
                } else if (mapVisibility == View.GONE) {
//                    locationTrackIntent.putExtra("is_location_track_on", true);
                    mFlMapContainer.setVisibility(View.VISIBLE);
                    mRlSearchStoreLayout.setVisibility(View.GONE);

                    mIvMapToggle.setImageBitmap(mCornerListBitmap);
//                  initMapMarker();
                }
//                startService(locationTrackIntent);
            }
            break;
        }

    }


    private class StoreResultAdapter extends BaseAdapter {
        private ArrayList<StoreData> mStoreDataList = null;
        private LayoutInflater mInflater;

        public StoreResultAdapter(Collection<StoreData> collection) {
            mStoreDataList = new ArrayList<StoreData>();
            mInflater = LayoutInflater.from(StoreMapActivity.this);

            mStoreDataList.addAll(collection);
        }

        public void clear() {
            mStoreDataList.clear();
        }

        public void addAll(Collection<StoreData> collection) {
            mStoreDataList.addAll(collection);
        }

        @Override
        public int getCount() {
            return mStoreDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return mStoreDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup view = null;
            StoreData data = mStoreDataList.get(position);

            if (convertView == null) {
                view = (ViewGroup) mInflater.inflate(R.layout.view_store_location_list_item, parent, false);

                changeLayoutConfig(view);
            } else {
                view = (ViewGroup) convertView;
            }

            TextView tvStoreName = (TextView) view.findViewById(R.id.tv_store_name);
            TextView tvStoreAddress = (TextView) view.findViewById(R.id.tv_store_address);

            tvStoreName.setText(data.getStoreName());
            tvStoreAddress.setText(data.getStoreAddress());

            return view;
        }
    }
}
