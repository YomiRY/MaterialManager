package com.material.management.output;

import com.material.management.MainActivity;
import com.material.management.R;
import com.material.management.utils.Utility;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;

public class NotificationOutput {
    private static final int PROGRESS_NOTIFICATION_ID = 0;
    private static final Uri SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private static final long[] VIBRATE = { 0, 100, 200, 300 };
    private static NotificationOutput sNotOutput = null;

    private NotificationManager mNotMgr = null;
    private Context mContext;
    private Notification.Builder mProgressNotif = null;
    private Notification.Builder mNotif = null;

    private NotificationOutput(Context context) {
        mNotMgr = (NotificationManager) context.getSystemService("notification");
        mContext = context;
        mNotif = new Notification.Builder(mContext);
        mProgressNotif = new Notification.Builder(mContext);
    }

    public static void initInstance(Context context) {
        if (sNotOutput == null)
            sNotOutput = new NotificationOutput(context);
    }

    public static NotificationOutput getInstance() {
        return sNotOutput;
    }

    public void outProgress(String msg, int progress, int max) {
        mProgressNotif.setSmallIcon(R.drawable.ic_launcher);
        mProgressNotif.setContentTitle(Utility.getContext().getString(R.string.app_name));
        mProgressNotif.setContentText(msg);
        mProgressNotif.setProgress(max, progress, false);

        if (VERSION.SDK_INT >= 16) {
            mNotMgr.notify(PROGRESS_NOTIFICATION_ID, mProgressNotif.build());
        } else {
            mNotMgr.notify(PROGRESS_NOTIFICATION_ID, mProgressNotif.getNotification());
        }
    }

    /* output may be refined ,this is a temporary implementation */
    public void outNotif(int objId, String msg, int notifType, Bundle bundle) {
        Intent actionIntent = new Intent(mContext, MainActivity.class);
        actionIntent.putExtras(bundle);

        outNotif(objId, actionIntent, msg, notifType);
    }

    public void outNotif(int objId, Intent actionIntent, String msg, int notifType) {
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, objId, actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Ringtone r = null;

        mNotif.setContentTitle(mContext.getString(R.string.app_name)).setContentText(msg)
                .setSmallIcon(R.drawable.ic_launcher).setContentIntent(contentIntent).setTicker(msg)
                .setAutoCancel(true);

        if(VERSION.SDK_INT >= 16) {
            mNotif.setPriority(Notification.PRIORITY_HIGH);
        }

        /*
         * 0: Vibrate 1: Sound
         */
        if (notifType == 0) {
            mNotif.setVibrate(VIBRATE);
        } else {
            r = RingtoneManager.getRingtone(Utility.getContext(), SOUND);
        }

        /* use wake lock to wake up devie */
        Utility.acquire();
        if (VERSION.SDK_INT >= 16) {
            mNotMgr.notify(objId, mNotif.build());
        } else {
            mNotMgr.notify(objId, mNotif.getNotification());
        }
        if (r != null && notifType == 1) {
            r.play();
        }
        /* release wake lock to wake up devie */
        Utility.release();
    }
}
