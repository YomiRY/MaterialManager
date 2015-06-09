package com.material.management.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.material.management.BuildConfig;
import com.material.management.MainActivity;
import com.material.management.MaterialManagerApplication;
import com.material.management.R;
import com.material.management.data.DeviceInfo;
import com.material.management.service.location.LocationUtility;

public class Utility {
    /* Share preference name. */
    private static final String SETTING = "setting_config";
    public static final String FONT_SIZE_SCALE_FACTOR = "font_size_scale_factor";
    public static final String INPUT_TEXT_HISTORY = "input_text_history";
    public static final String DB_UPGRADE_FLAG_1to2 = "db_upgrade_flag_1_to_2";
    public static final String DB_UPGRADE_FLAG_2to3 = "db_upgrade_flag_2_to_3";
    public static final String CATEGORY_IS_INITIALIZED = "category_is_initialized";
    public static final String SHARE_IS_INITIALIZED = "share_is_initialized";
    public static final String SHARE_AUTO_COMPLETE_TEXT = "share_auto_complete_text";
    public static final String MATERIAL_TYPE_GRID_COLUMN_NUM = "grid_column_num";
    public static final String NOTIF_IS_VIBRATE_SOUND = "is_notif_vibrate_sound";
    public static final String NOTIF_FREQUENCY = "notif_freq";

    private static Context sApplicationContext;
    private static Activity sActivity;
    private static SharedPreferences sSpSettings = null;
    private static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat sSimpleDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static File sSdDir = Environment.getExternalStorageDirectory();
    private static Locale sLocale = Locale.getDefault();
    private static DisplayMetrics sScreenDimInPix = null;
    private static PowerManager.WakeLock sWakeLock;
    private static DisplayMetrics sDisplayMetrics = null;
    private static BitmapUtility sBmpUtility;
    private static LocationUtility sLocUtility;
    private static DeviceInfo sDeviceInfo = new DeviceInfo();

    public static void setApplicationContext(Context context) {
        sApplicationContext = context;
        LocationUtility.init(context);
    }

    /* Must be initialize before using the Utility */
    public static void setMainActivity(Activity activity) {
        sActivity = activity;
        BitmapUtility.init(sActivity);
//      LocationUtility.init(sActivity);
        sBmpUtility = BitmapUtility.getInstance();
        sLocUtility = LocationUtility.getsInstance();
        sDeviceInfo.setDevice(Build.MANUFACTURER + " " + Build.MODEL);
        sDeviceInfo.setPlatformVersion("Android " + Build.VERSION.RELEASE);
        sDeviceInfo.setAppVersion(BuildConfig.VERSION_NAME);
        sDeviceInfo.setLanguage(Locale.getDefault().getLanguage());
        sDeviceInfo.setLocale(Locale.getDefault().getCountry());
    }

    public static MainActivity getMainActivity() {
        return (MainActivity) sActivity;
    }

    public static Context getContext() {
        return sApplicationContext;
    }

    public static File getExternalStorageDir() {
        return sSdDir;
    }

    public static String transDateToString(String pattern, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);

        return sdf.format(date);
    }

    public static String transDateToString(Date date) {
        return sSimpleDateFormat.format(date);
    }

    public static Date transStringToDate(String pattern, String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);

        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date transStringToDate(String date) {
        try {
            return sSimpleDateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static SpannableString formatMatchedString(String text, String search) {
        if (text == null || text.isEmpty()) {
            return new SpannableString("");
        }

        SpannableString ss = new SpannableString(text);
        if (search == null || search.length() == 0)
            return ss;

        text = text.toLowerCase(sLocale);
        search = search.toLowerCase(sLocale);

        int beginIndex = text.indexOf(search);
        if (beginIndex >= 0) {

            ss.setSpan(new BackgroundColorSpan(Color.BLUE), beginIndex, beginIndex + search.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ss;
    }

    public static <T extends Number> String convertDecimalFormat(T valObj, String format) {
        DecimalFormat formatter = new DecimalFormat(format);

        return formatter.format(valObj);
    }

    public static Uri getImageUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = Images.Media.insertImage(sApplicationContext.getContentResolver(), inImage, "Title", null);

        return Uri.parse(path);
    }

    public static String getPathFromUri(Uri uri) {
        // just some safety built in
        if (uri == null) {
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = {Images.Media.DATA};
        Cursor cursor = sActivity.managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        // this is our fallback here
        return uri.getPath();
    }

    public static void showToast(String msg) {
        Toast.makeText(sApplicationContext, msg, Toast.LENGTH_LONG).show();
    }

    public static DeviceInfo getDeviceInfo() {
        return sDeviceInfo;
    }

    public static void releaseBitmaps(Bitmap... bitmaps) {
        if (bitmaps != null) {
            for (Bitmap bitmap : bitmaps) {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                    bitmap = null;
                }
            }
            System.gc();
        }
    }

    public static boolean isValidNumber(Class c, String numStr) {
        try {
            if (c == double.class || c == Double.class) {
                Double.parseDouble(numStr);
            } else if (c == int.class || c == Integer.class) {
                Integer.parseInt(numStr);
            } else if (c == float.class || c == Float.class) {
                Float.parseFloat(numStr);
            } else if (c == long.class || c == Long.class) {
                Long.parseLong(numStr);
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static Location getLocation() {
        return sLocUtility.getLocation();
    }

    public static boolean isLocationEnabled() {
        return sLocUtility.isLocationEnabled();
    }

    /* wake lock control */
    public synchronized static void acquire() {
        if (sWakeLock != null)
            sWakeLock.release();

        PowerManager pm = (PowerManager) sApplicationContext.getSystemService(Context.POWER_SERVICE);
        sWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, MaterialManagerApplication.TAG);
        sWakeLock.acquire();
    }

    public synchronized static void release() {
        if (sWakeLock != null)
            sWakeLock.release();
        sWakeLock = null;
    }

    public static DisplayMetrics getDisplayMetrics() {
        if (sDisplayMetrics == null) {
            sDisplayMetrics = new DisplayMetrics();
            sActivity.getWindowManager().getDefaultDisplay().getMetrics(sDisplayMetrics);
        }

        return sDisplayMetrics;
    }

    public static double getDist(double lat1, double lon1, double lat2,
                                 double lon2) {
        double realDistance = 0;
        Location locationA = new Location("A");
        locationA.setLatitude(lat1);
        locationA.setLongitude(lon1);
        Location locationB = new Location("B");
        locationB.setLatitude(lat2);
        locationB.setLongitude(lon2);
        realDistance = locationA.distanceTo(locationB);

        return realDistance;
    }

    public static void setBooleanValueForKey(String key, boolean value) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }
        SharedPreferences.Editor PE = sSpSettings.edit();
        PE.putBoolean(key, value);
        PE.commit();
    }

    public static boolean getBooleanValueForKey(String key) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }
        return sSpSettings.getBoolean(key, false);
    }

    public static void setIntValueForKey(String key, int value) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }
        SharedPreferences.Editor PE = sSpSettings.edit();
        PE.putInt(key, value);
        PE.commit();
    }

    public static int getIntValueForKey(String key) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }

        if (key.equals(MATERIAL_TYPE_GRID_COLUMN_NUM)) {
            return sSpSettings.getInt(key, 2);
        } else if(key.equals(NOTIF_FREQUENCY) || key.equals(NOTIF_FREQUENCY)) {
            return sSpSettings.getInt(key, 1);
        } else {
            return sSpSettings.getInt(key, 0);
        }
    }

    public static void setStringValueForKey(String key, String value) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }
        SharedPreferences.Editor PE = sSpSettings.edit();
        PE.putString(key, value);
        PE.commit();
    }

    public static String getStringValueForKey(String key) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }

        if (key.equals(FONT_SIZE_SCALE_FACTOR)) {
            return sSpSettings.getString(key, "1.0");
        }
        return sSpSettings.getString(key, "");
    }

    public static boolean isNetworkConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static Uri getUriFromPath(String filePath) {
        long photoId;
        Uri photoUri = MediaStore.Images.Media.getContentUri("external");

        String[] projection = {MediaStore.Images.ImageColumns._ID};
        // TODO This will break if we have no matching item in the MediaStore.
        Cursor cursor = sApplicationContext.getContentResolver().query(photoUri, projection, MediaStore.Images.ImageColumns.DATA + " LIKE ?", new String[]{filePath}, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(projection[0]);
        photoId = cursor.getLong(columnIndex);

        cursor.close();
        return Uri.parse(photoUri.toString() + "/" + photoId);
    }
}
