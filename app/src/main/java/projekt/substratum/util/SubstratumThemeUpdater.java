package projekt.substratum.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.sql.Ref;
import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.InformationActivity;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.config.References;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class SubstratumThemeUpdater {

    private Context mContext;
    private SharedPreferences prefs;
    private String packageName;
    private boolean showNotification;

    public void initialize(Context context, String packageName, boolean notification) {
        this.mContext = context;
        this.packageName = packageName;
        this.showNotification = notification;

        prefs = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_updating", true).apply();
        // Ensure permissions are set on the SharedPreference for WORLD_READABLE
        References.restoreSharedPrefWorldReadable(context);

        new SubstratumThemeUpdate().execute("");
    }

    private String getThemeName(String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (References.checkOMS()) {
                    if (appInfo.metaData.getString("Substratum_Theme") != null) {
                        if (appInfo.metaData.getString("Substratum_Author") != null) {
                            return appInfo.metaData.getString("Substratum_Theme");
                        }
                    }
                } else {
                    if (appInfo.metaData.getBoolean("Substratum_Legacy", false)) {
                        if (appInfo.metaData.getString("Substratum_Theme") != null) {
                            if (appInfo.metaData.getString("Substratum_Author") != null) {
                                return appInfo.metaData.getString("Substratum_Theme");
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
        return null;
    }

    private class SubstratumThemeUpdate extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {

            final int id = ThreadLocalRandom.current().nextInt(0, 1000);
            final int notification_priority = 2; // PRIORITY_MAX == 2

            if (showNotification) {
                Intent notificationIntent;
                PendingIntent intent;
                if (References.checkOMS()) {
                    notificationIntent = new Intent(mContext, InformationActivity.class);
                    notificationIntent.putExtra("theme_name", getThemeName(packageName));
                    notificationIntent.putExtra("theme_pid", packageName);
                    notificationIntent.putExtra("refresh_back", true);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent = PendingIntent.getActivity(mContext, 0, notificationIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);
                } else {
                    notificationIntent = new Intent(mContext, MainActivity.class);
                    intent = PendingIntent.getActivity(mContext, 0, notificationIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);
                }

                // This is the time when the notification should be shown on the user's screen
                NotificationManager mNotifyManager =
                        (NotificationManager) mContext.getSystemService(
                                Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                        (mContext);
                mBuilder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
                mBuilder.setContentTitle(getThemeName(packageName) + " " + mContext.getString(
                        R.string.notification_theme_updated))
                        .setContentText(mContext.getString(R.string
                                .notification_theme_updated_content))
                        .setAutoCancel(true)
                        .setContentIntent(intent)
                        .setSmallIcon(R.drawable.notification_updated)
                        .setLargeIcon(BitmapFactory.decodeResource(
                                mContext.getResources(), R.mipmap
                                        .restore_launcher))
                        .setPriority(notification_priority);
                mNotifyManager.notify(id, mBuilder.build());
            } else {
                Intent notificationIntent;
                PendingIntent intent;
                if (References.checkOMS()) {
                    notificationIntent = new Intent(mContext, InformationActivity.class);
                    notificationIntent.putExtra("theme_name", getThemeName(packageName));
                    notificationIntent.putExtra("theme_pid", packageName);
                    notificationIntent.putExtra("refresh_back", true);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent = PendingIntent.getActivity(mContext, 0, notificationIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);
                } else {
                    notificationIntent = new Intent(mContext, MainActivity.class);
                    intent = PendingIntent.getActivity(mContext, 0, notificationIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);
                }

                // This is the time when the notification should be shown on the user's screen
                NotificationManager mNotifyManager =
                        (NotificationManager) mContext.getSystemService(
                                Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                        (mContext);
                mBuilder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
                mBuilder.setContentTitle(getThemeName(packageName) + " " +
                        mContext.getString(R.string
                                .notification_theme_installed))
                        .setContentIntent(intent)
                        .setContentText(mContext.getString(R.string
                                .notification_theme_installed_content))
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setLargeIcon(BitmapFactory.decodeResource(
                                mContext.getResources(), R.mipmap
                                        .main_launcher))
                        .setPriority(notification_priority);
                mNotifyManager.notify(id, mBuilder.build());
            }
            prefs.edit().putBoolean("is_updating", false).apply();
            References.restoreSharedPrefWorldReadable(mContext);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            new CacheCreator().initializeCache(mContext, packageName);
            return null;
        }
    }
}
