package projekt.substratum.config;

import android.content.Context;
import android.content.om.OverlayInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import projekt.substratum.util.Root;

import static projekt.substratum.config.References.INTERFACE_PACKAGE;
import static projekt.substratum.config.References.checkThemeInterface;
import static projekt.substratum.config.References.checkOMS;

public class ThemeManager {

    /*
        Begin interaction with the OverlayManagerService binaries.

        These methods will concurrently list all the possible functions that is open to Substratum
        for usage with the OMS7 and OMS7-R systems.

        NOTE: Deprecation at the OMS3 level. We no longer support OMS3 commands.
    */

    public static final String listAllOverlays = "cmd overlay list";
    public static final String disableOverlay = "cmd overlay disable";
    public static final String enableOverlay = "cmd overlay enable";
    private static final String disableAllOverlays = "cmd overlay disable-all";
    private static final String setPriority = "cmd overlay set-priority";
    private static final String[] blacklistedPackages = new String[]{
            INTERFACE_PACKAGE
    };

    public static boolean blacklisted(String packageName) {
        List<String> blacklisted = Arrays.asList(blacklistedPackages);
        return blacklisted.contains(packageName);
    }

    public static void enableOverlay(Context context, ArrayList<String> overlays) {
        if (checkThemeInterface(context)) {
            ThemeInterfaceService.enableOverlays(context, overlays, shouldRestartUI(context, overlays));
        } else {
            String commands = enableOverlay;
            for (int i = 0; i < overlays.size(); i++) {
                commands += " " + overlays.get(i);
            }
            new ElevatedCommands.ThreadRunner().execute(commands);
            if (shouldRestartUI(context, overlays)) ThemeManager.restartSystemUI(context);
        }
    }

    public static void disableOverlay(Context context, ArrayList<String> overlays) {
        if (checkThemeInterface(context)) {
            ThemeInterfaceService.disableOverlays(context, overlays, shouldRestartUI(context,
                    overlays));
        } else {
            String commands = disableOverlay;
            for (int i = 0; i < overlays.size(); i++) {
                commands += " " + overlays.get(i);
            }
            new ElevatedCommands.ThreadRunner().execute(commands);
            if (shouldRestartUI(context, overlays)) restartSystemUI(context);
        }
    }

    public static void setPriority(Context context, ArrayList<String> overlays) {
        if (checkThemeInterface(context)) {
            ThemeInterfaceService.setPriority(context, overlays);
        } else {
            String commands = "";
            for (int i = 0; i < overlays.size() - 1; i++) {
                String parentName = overlays.get(i);
                String packageName = overlays.get(i + 1);
                commands += (commands.isEmpty() ? "" : " && ") + setPriority + " " + packageName +
                        " " + parentName;
            }
            new ElevatedCommands.ThreadRunner().execute(commands);
            if (shouldRestartUI(context, overlays)) restartSystemUI(context);
        }
    }

    public static void disableAll(Context context) {
        if (checkThemeInterface(context)) {
            List<String> list = ThemeManager.listOverlays(5);
            ThemeInterfaceService.disableOverlays(context, new ArrayList<>(list),
                    shouldRestartUI(context, new ArrayList<>(list)));
        } else {
            new ElevatedCommands.ThreadRunner().execute(disableAllOverlays);
        }
    }

    public static void restartSystemUI(Context context) {
        if (checkThemeInterface(context)) {
            ThemeInterfaceService.restartSystemUI(context);
        } else {
            Root.runCommand("pkill -f com.android.systemui");
        }
    }

    public static void restartService(Context context) {
        if (checkThemeInterface(context)) {
            ThemeInterfaceService.restartService(context);
        }
    }

    public static List<String> listOverlays(int state) {
        List<String> list = new ArrayList<>();
        Map<String, List<OverlayInfo>> allOverlays = OverlayManagerService.getAllOverlays();
        if (allOverlays != null) {
            Set<String> set = allOverlays.keySet();
            for (String targetPackageName : set) {
                for (OverlayInfo oi : allOverlays.get(targetPackageName)) {
                    if (state == 5 && oi.isEnabled()) {
                        list.add(oi.packageName);
                    } else if (state == 4 && !oi.isEnabled()) {
                        list.add(oi.packageName);
                    } else if (state <= 3 && !oi.isApproved()) {
                        list.add(oi.packageName);
                    }
                }
            }
        }
        return list;
    }

    public static boolean isOverlayEnabled(String overlayName) {
        List<String> enabledOverlays = ThemeManager.listOverlays(5);
        for (String o : enabledOverlays) {
            if (o.equals(overlayName)) return true;
        }
        return false;
    }

    /*
        Begin interaction with the ThemeInterfaceService or the PackageManager binaries.

        These methods will handle all possible commands to be sent to PackageManager when handling
        an overlay, such as installing and uninstalling APKs directly on the device.
     */

    public static void installOverlay(Context context, String overlay) {
        if (checkThemeInterface(context)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(overlay);
            ThemeInterfaceService.installOverlays(context, list);
        } else {
            new ElevatedCommands.ThreadRunner().execute("pm install -r " + overlay);
        }
    }

    public static void installOverlay(Context context, ArrayList<String> overlays) {
        if (checkThemeInterface(context)) {
            ThemeInterfaceService.installOverlays(context, overlays);
        } else {
            String packages = "";
            for (String o : overlays) {
                packages += o + " ";
            }
            new ElevatedCommands.ThreadRunner().execute("pm install -r " + packages);
        }
    }

    public static void uninstallOverlay(Context context, ArrayList<String> overlays) {
        if (checkThemeInterface(context)) {
            ThemeInterfaceService.uninstallOverlays(context, overlays, shouldRestartUI(context,
                    overlays));
        } else {
            String command = "pm uninstall ";
            for (String packageName : overlays) {
                command += packageName + " ";
            }
            new ElevatedCommands.ThreadRunner().execute(command);
            if (checkOMS(context) && shouldRestartUI(context, overlays)) restartSystemUI(context);
        }
    }

    public static boolean shouldRestartUI(Context context, ArrayList<String> overlays) {
        if (checkOMS(context)) {
            for (String o : overlays) {
                if (o.startsWith("com.android.systemui")) return true;
            }
        }
        return false;
    }
}
