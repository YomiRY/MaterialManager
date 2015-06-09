package com.material.management;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.material.management.api.module.ConnectionControl;
import com.material.management.api.module.ViewCallbackListener;
import com.material.management.data.DeviceInfo;
import com.material.management.utils.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by randyhsu on 2014/9/6.
 */
public class MMFragment extends Fragment implements ViewCallbackListener, View.OnClickListener {
    protected Dialog mProgressDialog;
    protected Handler mHandler;
    protected Resources mResources;
    protected Activity mOwnerActivity;
    protected LayoutInflater mInflater;
    protected DisplayMetrics mMetrics;
    protected InputMethodManager mImm = null;
    protected ConnectionControl mControl;
    protected DeviceInfo mDeviceInfo;
    protected HashMap<Integer, Float> mOrigFontSizeMap = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mHandler = new Handler();
        mResources = getResources();
        mOwnerActivity = getActivity();
        mMetrics = new DisplayMetrics();
        mInflater = (LayoutInflater) mOwnerActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImm = (InputMethodManager) mOwnerActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        mControl = ConnectionControl.getInstance();
        mDeviceInfo = Utility.getDeviceInfo();

        mOwnerActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /* It's been pair using*/
    public void showProgressDialog(String title, String content) {
        /* to avoid windowLeaked*/
        closeProgressDialog();
        mProgressDialog = ProgressDialog.show(mOwnerActivity, title, content, true);
    }

    public void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    /* TODO: We that it use common layout and only setup message. But maybe it need to be custom in the future. */
    public void showToast(final String msg) {
        mOwnerActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = new Toast(mOwnerActivity);
                View layout = mInflater.inflate(R.layout.view_default_toast, null);
                TextView tvToastMsg = (TextView) layout.findViewById(R.id.tv_toast_msg);

                tvToastMsg.setText(msg);
                toast.setView(layout);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public void showAlertDialog(final String title, final String msg,
                                final String btnPositText, final String btnNegatText,
                                final DialogInterface.OnClickListener positListener, final DialogInterface.OnClickListener negatListener) {
        mOwnerActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mOwnerActivity, R.style.AlertDialogTheme);

                builder.setTitle(title);
                builder.setMessage(msg);
                builder.setPositiveButton(btnPositText, positListener);
                builder.setNegativeButton(btnNegatText, negatListener);

                Dialog dialog = builder.show();
                /* Use the id inside framework to get the message TextView*/
                TextView tvMsgText = (TextView) dialog.findViewById(android.R.id.message);

                tvMsgText.setGravity(Gravity.CENTER);
            }
        });
    }

    protected void sendScreenAnalytics(String screenName) {
        //Get a Tracker (should auto-report)
        Tracker t =  ((MaterialManagerApplication) mOwnerActivity.getApplication()).getTracker(MaterialManagerApplication.TrackerName.APP_TRACKER);

        t.setScreenName(screenName);
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    protected void sendEventAnalytics(String screenName) {
        //Get a Tracker (should auto-report)
        Tracker t =  ((MaterialManagerApplication) mOwnerActivity.getApplication()).getTracker(MaterialManagerApplication.TrackerName.APP_TRACKER);

        t.setScreenName(screenName);
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    public void callbackFromController(JSONObject result) {

    }

    @Override
    public void callbackFromController(JSONObject result, String tag) {

    }

    @Override
    public void callbackFromController(JSONObject result, Throwable throwable, String error) {

    }

    @Override
    public void onClick(View v) {
        mImm.hideSoftInputFromWindow(getView().getApplicationWindowToken(), 0);
    }

    /* Change layout configuration e.g.: Font size...*/
    protected void changeLayoutConfig(View layout) {
        if(mOrigFontSizeMap == null) {
            mOrigFontSizeMap = new HashMap<Integer, Float>();
        }
        storeViewFontSize(layout);
        changeFontSize(layout);
    }

    protected void storeViewFontSize(View view) {
        if(mOrigFontSizeMap == null) {
            return;
        }

        if(view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for(int i = 0, len = viewGroup.getChildCount() ; i < len ; i++) {
                storeViewFontSize(viewGroup.getChildAt(i));
            }
        } else if(view instanceof TextView && !mOrigFontSizeMap.containsKey(view.getId())){
            mOrigFontSizeMap.put(view.getId(), ((TextView) view).getTextSize());
        }
    }

    protected void changeFontSize(View view) {
        float adjustFontFactor = Float.parseFloat(Utility.getStringValueForKey(Utility.FONT_SIZE_SCALE_FACTOR));

        if(view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for(int i = 0, len = viewGroup.getChildCount() ; i < len ; i++) {
                changeFontSize(viewGroup.getChildAt(i));
            }
        } else if(view instanceof TextView){
            TextView targetView = (TextView) view;
            String ignoreTag = (String)targetView.getTag();

            if(ignoreTag != null && ignoreTag.equals("font_size_change_ignore"))
                return;

            targetView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mOrigFontSizeMap.get(view.getId()) * adjustFontFactor);
        }
    }
}
