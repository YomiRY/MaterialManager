package com.material.management.utils;

import android.util.Log;
import com.material.management.BuildConfig;

public class LogUtility {

    public static void printLogD(String tag, String msg) {
        if(BuildConfig.DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void printLogE(String tag, String msg) {
        if(BuildConfig.DEBUG) {
            Log.e(tag, msg);
        }
    }

    public static void printStackTrace(Exception e) {
        if(BuildConfig.DEBUG) {
            e.printStackTrace();
        }
    }
}
