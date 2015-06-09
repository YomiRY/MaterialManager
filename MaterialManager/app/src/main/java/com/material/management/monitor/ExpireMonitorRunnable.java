package com.material.management.monitor;

import android.content.Context;
import android.os.Bundle;

import com.material.management.R;
import com.material.management.data.BundleInfo;
import com.material.management.data.Material;
import com.material.management.output.NotificationOutput;
import com.material.management.utils.DBUtility;
import com.material.management.utils.Utility;
import java.util.ArrayList;
import java.util.Calendar;

public class ExpireMonitorRunnable implements Runnable {
    public static final String MONITOR_THREAD_NAME = "Expire_Monitor_Thread";
    private static NotificationOutput sNotificationOutput = NotificationOutput.getInstance();

    private Context mContext;
    private int mNotifType;

    public ExpireMonitorRunnable(int notifType) {
        mContext = Utility.getContext();
        mNotifType = notifType;
    }

    @Override
    public void run() {
        ArrayList<Material> materialList = DBUtility.selectMaterialInfos();
        Calendar today = Calendar.getInstance();
        long todayTimeInMillis = today.getTimeInMillis();

        for (Material material : materialList) {
            int notificationDays = material.getNotificationDays();
            Calendar validateDate = material.getValidDate();
            String validateDateStr = Utility.transDateToString(validateDate.getTime());

            if (validateDate.getTimeInMillis() < todayTimeInMillis) {
                sNotificationOutput.outNotif(material.hashCode(), mContext.getString(R.string.format_expired_msg, material.getName(), validateDateStr), mNotifType, createNotificationBundle(material));
            } else {
                validateDate.add(Calendar.DAY_OF_MONTH, notificationDays * -1);

                if (validateDate.getTimeInMillis() <= todayTimeInMillis) {
                    sNotificationOutput.outNotif(material.hashCode(), mContext.getString(R.string.format_before_expired_msg, material.getName(), validateDateStr), mNotifType, createNotificationBundle(material));
                }
            }
        }
    }

    private Bundle createNotificationBundle(Material material) {
        Bundle bundle = new Bundle();

        bundle.putInt(BundleInfo.BUNDLE_KEY_BUNDLE_TYPE, BundleInfo.BundleType.BUNDLE_TYPE_NOTIFICATION.value());
        bundle.putString(BundleInfo.BUNDLE_KEY_MATERIAL_TYPE, material.getMaterialType());
        bundle.putString(BundleInfo.BUNDLE_KEY_MATERIAL_NAME, material.getName());

        return bundle;
    }
}
