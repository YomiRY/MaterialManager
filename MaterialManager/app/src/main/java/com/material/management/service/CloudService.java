package com.material.management.service;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.material.management.IStatusUpdate;
import com.material.management.MaterialManagerApplication;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.data.BackupRestoreInfo;
import com.material.management.output.NotificationOutput;
import com.material.management.utils.DBUtility;
import com.material.management.utils.FileUtility;
import com.material.management.utils.Utility;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class CloudService extends Service {
    /* You don't need to change these, leave them alone. */
    private static final String ACCOUNT_PREFS_NAME = "dropbox_user_name";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    private static final String APP_KEY = "j9kqmqm0mliz6gy";
    private static final String APP_SECRET = "ojuj4jq94b6ur1z";
    private static final boolean USE_OAUTH1 = false;

    private static boolean sIsBakOrRestoreRunn = false;
    private static boolean sIsServiceUnbinded = true;
    private static DropboxAPI<AndroidAuthSession> sApi;
    private static IStatusUpdate sStatusUpdate = null;

    /* For ICS compatibility */
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case R.id.tv_database_restore:
                    new BackupRestoreTask().execute(R.id.tv_database_restore);
                    break;
                case R.id.tv_database_backup:
                    new BackupRestoreTask().execute(R.id.tv_database_backup);
                    break;
            }
            return false;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();

        initDropBoxConfig();
        disconnectDropBox();
    }

    @Override
    public IBinder onBind(Intent intent) {
        sIsServiceUnbinded = false;
        return new IBackupRestore.Stub() {

            @Override
            public void startRestore(IStatusUpdate statusRemoteHandler) throws RemoteException {
                sStatusUpdate = statusRemoteHandler;
                if (!sIsBakOrRestoreRunn) {
//                    if (VERSION.SDK_INT >= 16) {
//                        new BackupRestoreTask().execute(R.id.tv_database_restore);
//                    } else {
                        mHandler.sendMessage(mHandler.obtainMessage(R.id.tv_database_restore));
//                    }
                }
            }

            @Override
            public void startBackup(IStatusUpdate statusRemoteHandler) throws RemoteException {
                sStatusUpdate = statusRemoteHandler;
                if (!sIsBakOrRestoreRunn) {
//                    if (VERSION.SDK_INT >= 16) {
//                        new BackupRestoreTask().execute(R.id.tv_database_backup);
//                    } else {
                        mHandler.sendMessage(mHandler.obtainMessage(R.id.tv_database_backup));
//                    }
                }
            }

            /* Check if the authentication is successful or fail. */
            @Override
            public void connect() throws RemoteException {
                if (!isLinked()) {
                    connectDropBox();
                }
            }

            public void disConnect() {
                disconnectDropBox();
                sStatusUpdate = null;
            }

            @Override
            public boolean isLinked() throws RemoteException {
                /*
                 * Check if the dropbox is connected or disconnected. The next part must be inserted in the onResume()
                 * method of the activity from which session.startAuthentication() was called, so that Dropbox
                 * authentication completes properly.
                 */
                AndroidAuthSession session = sApi.getSession();

                if (session.authenticationSuccessful()) {
                    try {
                        /* Mandatory call to complete the auth */
                        session.finishAuthentication();

                        /* Store it locally in our app for later use */
                        storeAuth(session);
                        return true;
                    } catch (IllegalStateException e) {
                        Utility.showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
                        Log.i(MaterialManagerApplication.TAG, "Error authenticating", e);
                    }
                }

                return false;
            }
        };
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Log.d(MaterialManagerApplication.TAG, "onUnbind service in CloudService...");
        sStatusUpdate = null;
        sIsServiceUnbinded = true;

        if (!sIsBakOrRestoreRunn)
            disconnectDropBox();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        // Log.d(MaterialManagerApplication.TAG, "onDestroy service in CloudService...");
        super.onDestroy();
    }

    private void initDropBoxConfig() {
        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        sApi = new DropboxAPI<AndroidAuthSession>(session);

        checkAppKeySetup();
    }

    private void connectDropBox() {
        /* Start the remote authentication */
        if (USE_OAUTH1) {
            sApi.getSession().startAuthentication(Utility.getContext());
        } else {
            sApi.getSession().startOAuth2Authentication(Utility.getContext());
        }
    }

    private void disconnectDropBox() {
        /* We let it unlink when user enter the setting activity to ensure authentication every time */
        if (sApi.getSession().isLinked()) {
            logOut();
        }
    }

    private void logOut() {
        /* Remove credentials from the session */
        sApi.getSession().unlink();
        /* Clear our stored keys */
        clearKeys();
    }

    private void checkAppKeySetup() {
        /* Check to make sure that we have a valid app key */
        if (APP_KEY.startsWith("CHANGE") || APP_SECRET.startsWith("CHANGE")) {
            Utility.showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");

            return;
        }

        /* Check if the app has set up its manifest properly. */
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = getPackageManager();
        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
            Utility.showToast("URL scheme in your app's " + "manifest is not set up correctly. You should have a "
                    + "com.dropbox.client2.android.AuthActivity with the " + "scheme: " + scheme);
        }
    }

    /*
     * Shows keeping the access keys returned from Trusted Authenticator in a local store, rather than storing user name
     * & password, and re-authenticating each time (which is not to be done, ever).
     */
    private void loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0)
            return;

        if (key.equals("oauth2:")) {
            /* If the key is set to "oauth2:", then we can assume the token is for OAuth 2. */
            session.setOAuth2AccessToken(secret);
        } else {
            /* Still support using old OAuth 1 tokens. */
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }

    /*
     * Shows keeping the access keys returned from Trusted Authenticator in a local store, rather than storing user name
     * & password, and re-authenticating each time (which is not to be done, ever).
     */
    private void storeAuth(AndroidAuthSession session) {
        /* Store the OAuth 2 access token, if there is one. */
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.commit();
            return;
        }
        /*
         * Store the OAuth 1 access token, if there is one. This is only necessary if you're still using OAuth 1.
         */
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
            edit.commit();
            return;
        }
    }

    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }

    private class BackupRestoreTask extends AsyncTask<Integer, BackupRestoreInfo, String> implements Observer {

        private NotificationOutput mNotifOutput;

        public BackupRestoreTask() {
            mNotifOutput = NotificationOutput.getInstance();
        }

        @Override
        protected void onPreExecute() {
            sIsBakOrRestoreRunn = true;
            super.onPreExecute();
        }

        protected String doInBackground(Integer... params) {
            int id = params[0];
            StringBuilder msg = new StringBuilder("");

            switch (id) {
            case R.id.tv_database_backup:
                msg.append(DBUtility.exportDB(sApi, this));
                msg.append(FileUtility.exportPhoto(sApi, this));
                break;
            case R.id.tv_database_restore:
                msg.append(DBUtility.importDB(sApi, this));
                msg.append(FileUtility.importPhoto(sApi, this));
                break;
            }
            return msg.toString();
        }

        @Override
        public void update(Object data) {
            /* update the progress */
            if (data instanceof BackupRestoreInfo) {
                publishProgress((BackupRestoreInfo) data);
            }
        }

        @Override
        protected void onProgressUpdate(BackupRestoreInfo... values) {
            super.onProgressUpdate(values);

            try {
                BackupRestoreInfo bri = values[0];
                if (sStatusUpdate != null) {
                    sStatusUpdate.updateProgress(bri.getMsg(), bri.getProgress());
                }
                mNotifOutput.outProgress(bri.getMsg(), bri.getProgress(), 100);
            } catch (RemoteException e) {
                mNotifOutput.outProgress(
                        Utility.getContext().getString(R.string.title_progress_notif_backup_restore_fail), 0, 0);
                e.printStackTrace();
            }
        }

        protected void onPostExecute(String msg) {
            try {
                sIsBakOrRestoreRunn = false;

                mNotifOutput
                        .outProgress(
                                Utility.getContext().getString(
                                        R.string.title_progress_notif_backup_restore_successfully), 0, 0);

                if (sStatusUpdate != null) {
                    sStatusUpdate.finishBackupOrRestore(msg);
                }

                if (sIsServiceUnbinded)
                    disconnectDropBox();
            } catch (RemoteException e) {
                mNotifOutput.outProgress(
                        Utility.getContext().getString(R.string.title_progress_notif_backup_restore_fail), 0, 0);
                e.printStackTrace();
            }
        }
    }
}
