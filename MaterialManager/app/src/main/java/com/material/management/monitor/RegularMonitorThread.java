package com.material.management.monitor;

import java.util.ArrayList;
import java.util.Calendar;
import com.material.management.data.BundleInfo;
import com.material.management.data.Material;
import com.material.management.data.BundleInfo.BundleType;
import com.material.management.output.NotificationOutput;
import com.material.management.utils.DBUtility;
import com.material.management.utils.Utility;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

public class RegularMonitorThread extends HandlerThread {
    public static final String MONITOR_THREAD_NAME = "Monitor_Thread";
    /*
     * 0:vibrate
     * 1:sound
     * 0 is default 
     * */
    private static int sNotifType = 0;

    /*
     * default is notificated per 1 hr. 
     * */
    private static long sNotifFreq = 1000 * 60 * 60;

    private boolean mIsRunning;
    private MonitorRunnable mMonitorRunnable;
    private Handler mHandler;
    private NotificationOutput mNotificationOutput;

    public RegularMonitorThread(int priority) {
        super(MONITOR_THREAD_NAME, priority);
        mIsRunning = false;
        mMonitorRunnable = new MonitorRunnable();
        mNotificationOutput = NotificationOutput.getInstance();
    }

    public void startMonitor() {
        mHandler = new Handler(this.getLooper());
        mHandler.postDelayed(mMonitorRunnable, sNotifFreq);
    }

    public void stopMonitor() {
        if (mHandler == null)
            return;

        mHandler.removeCallbacks(mMonitorRunnable);
        this.quit();
    }

    private void refreshRunnableState() {
        mHandler.removeCallbacks(mMonitorRunnable);

        mMonitorRunnable = new MonitorRunnable();

        mHandler.postDelayed(mMonitorRunnable, sNotifFreq);
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public void changeNotifType(int notifType) {
        sNotifType = notifType;
    }

    public void changeNotifFreq(long notifFreq) {
        sNotifFreq = notifFreq;
        refreshRunnableState();
    }

    private class MonitorRunnable implements Runnable {

        public void run() {
            ArrayList<Material> materialList = DBUtility.selectMaterialInfos();
            Calendar today = Calendar.getInstance();

            for (Material material : materialList) {
                int notificationDays = material.getNotificationDays();
                Calendar validateDate = material.getValidDate();
                long todayTimeInMillis = today.getTimeInMillis();
                String validateDateStr = Utility.transDateToString(validateDate.getTime());
                if (validateDate.getTimeInMillis() < todayTimeInMillis) {
                    mNotificationOutput.outNotif(material.hashCode(), material.getName() + "已經過期，過期時間為" + validateDateStr + "，請注意物品狀態", sNotifType, createNotificationBundle(material));
                } else {
                    validateDate.add(Calendar.DAY_OF_MONTH, notificationDays * -1);

                    if (validateDate.getTimeInMillis() <= todayTimeInMillis) {
                        mNotificationOutput.outNotif(material.hashCode(), material.getName() + "即將過期，過期時間為" + validateDateStr + "，請注意物品狀態", sNotifType, createNotificationBundle(material));
                    }
                }
            }

            mHandler.postDelayed(this, sNotifFreq);
        }

        private Bundle createNotificationBundle(Material material) {
            Bundle bundle = new Bundle();

            bundle.putInt(BundleInfo.BUNDLE_KEY_BUNDLE_TYPE, BundleType.BUNDLE_TYPE_NOTIFICATION.value());
            bundle.putString(BundleInfo.BUNDLE_KEY_MATERIAL_TYPE, material.getMaterialType());
            bundle.putString(BundleInfo.BUNDLE_KEY_MATERIAL_NAME, material.getName());

            return bundle;
        }
    }
}
