package projekt.substratum.config;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class References {

    // This method is used to determine whether there the system is initiated with OMS

    public static Boolean checkOMS() {
        File om = new File("/system/bin/om");
        return om.exists();
    }

    // This method configures the new devices and their configuration of their vendor folders

    public static Boolean inNexusFilter() {
        String[] nexus_filter = {"angler", "bullhead", "flounder", "marlin", "sailfish"};
        return Arrays.asList(nexus_filter).contains(Build.DEVICE);
    }

    // This string array contains all the SystemUI acceptable overlay packs
    public static Boolean allowedSystemUIOverlay(String current) {
        String[] allowed_overlays = {
                "com.android.systemui.headers",
                "com.android.systemui.navbars"
        };
        return Arrays.asList(allowed_overlays).contains(current);
    }

    // This string array contains all the SystemUI acceptable sound files
    public static Boolean allowedUISound(String targetValue) {
        String[] allowed_themable = {
                "lock_sound",
                "unlock_sound",
                "low_battery_sound"};
        return Arrays.asList(allowed_themable).contains(targetValue);
    }

    // This int controls the notification identifier

    public static int notification_id = 2486;

    // This boolean controls the DEBUG level of the application

    public static Boolean DEBUG = false;

    // This int controls the default priority level for legacy overlays

    public static int DEFAULT_PRIORITY = 50;

    // This method determines whether a specified package is installed

    public static boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // This method obtains the application icon for a specified package

    public static Drawable grabAppIcon(Context context, String package_name) {
        Drawable icon = null;
        try {
            if (References.allowedSystemUIOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon("com.android.systemui");
            } else {
                icon = context.getPackageManager().getApplicationIcon(package_name);
            }
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e("SubstratumLogger", "Could not grab the application icon for \"" + package_name
                    + "\"");
        }
        return icon;
    }
}