package com.material.management.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import com.material.management.IStatusUpdate;
import com.material.management.MMFragment;
import com.material.management.MainActivity;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.dialog.LightProgressDialog;
import com.material.management.monitor.MonitorService;
import com.material.management.service.CloudService;
import com.material.management.service.IBackupRestore;
import com.material.management.utils.Utility;

public class SettingsFragment extends MMFragment implements Observer, RadioGroup.OnCheckedChangeListener, AdapterView.OnItemSelectedListener, View.OnClickListener {
    /* Android widgets */
    private static MainActivity sActivity = null;

    private TextView mTvDbRestore;
    private TextView mTvDbBackup;
    private RadioGroup mRgNotifVibrateOrSound;
    private Spinner mSpinNotifFrequency;
    private Spinner mSpinFontSizeChange;
    private View mLayout;
    private CheckedTextView mCbDropBox;

    private int mIsNotifVibrateOrSound;
    private int mNotifFreq;
    private String[] mFontSizeScaleTitles;
    private String[] mFontSizeScaleLeves;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        sActivity = (sActivity == null) ? (MainActivity) getActivity() : sActivity;
        mLayout = inflater.inflate(R.layout.fragment_setting_layout, container, false);

        update(null);
        initView();
        Utility.getContext().bindService(new Intent(sActivity, CloudService.class), mConnection, Context.BIND_AUTO_CREATE);

        return mLayout;
    }

    @Override
    public void onResume() {
        try {
            if (mService != null && mService.isLinked()) {
                mCbDropBox.setChecked(true);
            } else
                mCbDropBox.setChecked(false);
        } catch (RemoteException e) {
            e.printStackTrace();
            mCbDropBox.setChecked(false);
        }
        sendScreenAnalytics(getString(R.string.ga_app_view_settings_fragment));
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        Utility.getContext().unbindService(mConnection);
        super.onDestroyView();
    }

    private void initView() {
        mRgNotifVibrateOrSound = (RadioGroup) mLayout.findViewById(R.id.rg_notif_types);
        mSpinNotifFrequency = (Spinner) mLayout.findViewById(R.id.spin_notification_frequency);
        mSpinFontSizeChange = (Spinner) mLayout.findViewById(R.id.spin_font_size_scale_factor);
        RadioButton rbNotifVibrate = (RadioButton) mLayout.findViewById(R.id.rb_notif_type_vibrate);
        RadioButton rbNotifSound = (RadioButton) mLayout.findViewById(R.id.rb_notif_type_sound);
        mCbDropBox = (CheckedTextView) mLayout.findViewById(R.id.cb_dropbox_enable);
        mTvDbBackup = (TextView) mLayout.findViewById(R.id.tv_database_backup);
        mTvDbRestore = (TextView) mLayout.findViewById(R.id.tv_database_restore);
        /* default is vibrate */
        mIsNotifVibrateOrSound = Utility.getIntValueForKey(Utility.NOTIF_IS_VIBRATE_SOUND);
        /* default frequency is 1 hour */
        mNotifFreq = Utility.getIntValueForKey(Utility.NOTIF_FREQUENCY);
        ArrayAdapter<String> notifSpinAdapter = new SpinnerSettingsAdapter<String>(Utility.getContext(), R.layout.view_spinner_item_layout,
                getResources().getStringArray(R.array.default_notification_frequency));
        ArrayAdapter<String> fontSizeSpinAdapter = new SpinnerSettingsAdapter<String>(Utility.getContext(), R.layout.view_spinner_item_layout,
                getResources().getStringArray(R.array.font_size_scale_level_title));
        notifSpinAdapter.setDropDownViewResource(R.layout.view_spinner_item_layout);
        fontSizeSpinAdapter.setDropDownViewResource(R.layout.view_spinner_item_layout);
        mFontSizeScaleTitles = getResources().getStringArray(R.array.font_size_scale_level_title);
        mFontSizeScaleLeves = getResources().getStringArray(R.array.font_size_scale_level);
        String defaultScaleFact = Utility.getStringValueForKey(Utility.FONT_SIZE_SCALE_FACTOR);

        if (mIsNotifVibrateOrSound == 0) {
            rbNotifVibrate.setChecked(true);
        } else {
            rbNotifSound.setChecked(true);
        }

        mSpinFontSizeChange.setAdapter(fontSizeSpinAdapter);
        mSpinFontSizeChange.setOnItemSelectedListener(this);
        for(int i = 0, len = mFontSizeScaleTitles.length ; i < len ; i++) {
            if(defaultScaleFact.equals(mFontSizeScaleLeves[i])) {
                mSpinFontSizeChange.setSelection(i);
            }
        }
        mSpinNotifFrequency.setAdapter(notifSpinAdapter);
        mSpinNotifFrequency.setOnItemSelectedListener(this);
        mSpinNotifFrequency.setSelection(notifSpinAdapter.getPosition(Integer.toString(mNotifFreq)));
        mRgNotifVibrateOrSound.setOnCheckedChangeListener(this);
        mCbDropBox.setOnClickListener(this);
        mTvDbBackup.setOnClickListener(this);
        mTvDbRestore.setOnClickListener(this);
    }

    /* Radio Button */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_notif_type_vibrate:
                mIsNotifVibrateOrSound = 0;
                break;
            case R.id.rb_notif_type_sound:
                mIsNotifVibrateOrSound = 1;
                break;
        }


        Utility.setIntValueForKey(Utility.NOTIF_IS_VIBRATE_SOUND, mIsNotifVibrateOrSound);
        Utility.setIntValueForKey(Utility.NOTIF_FREQUENCY, mNotifFreq);
        Intent intent = new Intent();

        intent.setClass(Utility.getContext(), MonitorService.class);
        intent.putExtra("monitor_type", MonitorService.MonitorType.MONITOR_TYPE_EXPIRE_NOITFICATION.value());
        intent.putExtra("notif_type", mIsNotifVibrateOrSound);
        /* if immeditly_triggered== false , it will delay one ExpireMonitorRunnable checking */
        intent.putExtra("immeditly_triggered", false);
        Utility.getContext().sendBroadcast(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapter, View selectedView, int pos, long id) {
        int vId = adapter.getId();

        if(vId == R.id.spin_notification_frequency) {
            mNotifFreq = Integer.parseInt(((TextView) selectedView).getText().toString().trim());
            Intent intent = new Intent();
            Utility.setIntValueForKey(Utility.NOTIF_IS_VIBRATE_SOUND, mIsNotifVibrateOrSound);
            Utility.setIntValueForKey(Utility.NOTIF_FREQUENCY, mNotifFreq);

            intent.setClass(Utility.getContext(), MonitorService.class);
            intent.putExtra("monitor_type", MonitorService.MonitorType.MONITOR_TYPE_EXPIRE_NOITFICATION.value());
            intent.putExtra("notif_freq", mNotifFreq);
            /* if immeditly_triggered== false , it will delay one ExpireMonitorRunnable checking */
            intent.putExtra("immeditly_triggered", false);
            Utility.getContext().sendBroadcast(intent);
        } else if(vId == R.id.spin_font_size_scale_factor) {
            Utility.setStringValueForKey(Utility.FONT_SIZE_SCALE_FACTOR, mFontSizeScaleLeves[pos]);
            sActivity.updateLayoutConfig();
            changeLayoutConfig(mLayout);
        }
    }

    public void onClick(View view) {
        int id = view.getId();

        if ((id == R.id.tv_database_backup) || (id == R.id.tv_database_restore)) {
            if (mService != null) {
                mProgressDialog = LightProgressDialog.getInstance(sActivity);

                mProgressDialog.setMessage(sActivity.getString(R.string.title_progress_startup));
                mProgressDialog.show();

                try {
                    if (id == R.id.tv_database_backup) {
                        mService.startBackup(mStatusUpdate);
                    } else if (id == R.id.tv_database_restore) {
                        mService.startRestore(mStatusUpdate);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else if(id == R.id.cb_dropbox_enable) {
            boolean isChecked = !mCbDropBox.isChecked();

            mCbDropBox.setChecked(isChecked);
            try {
                if (mService != null && (isChecked && !mService.isLinked()))
                    mService.connect();
                else if (mService != null && (!isChecked && mService.isLinked()))
                    mService.disConnect();
            } catch (RemoteException e) {
                e.printStackTrace();
                mCbDropBox.setChecked(false);
            }
        }
    }

    @Override
    public void update(Object data) {
        if (sActivity == null) {
            return;
        }

        sActivity.setMenuItemVisibility(R.id.action_search, false);
        sActivity.setMenuItemVisibility(R.id.menu_action_add, false);
        sActivity.setMenuItemVisibility(R.id.menu_action_cancel, false);
        sActivity.setMenuItemVisibility(R.id.menu_sort_by_date, false);
        sActivity.setMenuItemVisibility(R.id.menu_sort_by_name, false);
        sActivity.setMenuItemVisibility(R.id.menu_sort_by_place, false);
        sActivity.setMenuItemVisibility(R.id.menu_grid_1x1, false);
        sActivity.setMenuItemVisibility(R.id.menu_grid_2x1, false);
        sActivity.setMenuItemVisibility(R.id.menu_clear_expired_items, false);

    }

    private class SpinnerSettingsAdapter<String> extends ArrayAdapter<String> {

        public SpinnerSettingsAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            changeLayoutConfig(v);

            return v;
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = super.getDropDownView(position, convertView, parent);

            ((TextView) v).setGravity(Gravity.CENTER);
            changeLayoutConfig(v);

            return v;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Log.d(MaterialManagerApplication.TAG, "onServiceConnected");
            mService = IBackupRestore.Stub.asInterface((IBinder) service);
        }

        public void onServiceDisconnected(ComponentName className) {
            // Log.d(MaterialManagerApplication.TAG, "onServiceDisconnected");
            mService = null;
        }
    };

    private LightProgressDialog mProgressDialog = null;
    private IBackupRestore mService;
    private IStatusUpdate.Stub mStatusUpdate = new IStatusUpdate.Stub() {

        @Override
        public void updateProgress(final String msg, final int progress) throws RemoteException {
            if (mProgressDialog != null) {
                sActivity.runOnUiThread(new Runnable() {

                       @Override
                       public void run() {
                           mProgressDialog.setMessage(msg);
                           mProgressDialog.setProgress(progress);
                       }
                   });
            }
        }

        @Override
        public void finishBackupOrRestore(final String msg) throws RemoteException {
            Utility.getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(msg);
                    mProgressDialog.setShowState(false);
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            });
        }
    };

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }
}
