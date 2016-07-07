package projekt.substratum.tabs;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularfillableloaders.CircularFillableLoaders;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.util.BootAnimationHandler;
import projekt.substratum.util.FontHandler;
import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.Root;
import projekt.substratum.util.SoundsHandler;
import projekt.substratum.util.SubstratumBuilder;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class MainScreenTab extends Fragment {

    private final String theme_name = InformationActivity.getThemeName();
    private final String theme_pid = InformationActivity.getThemePID();
    private final List tab_checker = InformationActivity.getListOfFolders();
    private int ALLOWED_AMOUNT_OF_OVERLAYS_TO_TRIGGER_QUICK_APPLY = 5;
    private String versionName;
    private SubstratumBuilder sb;
    private CircularFillableLoaders loader;
    private TextView loader_string;
    private ProgressDialog mProgressDialog;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private PowerManager.WakeLock mWakeLock;
    private boolean has_initialized_cache = false;
    private boolean has_failed = false;
    private int id = 1;
    private ArrayList<String> final_runner, filteredDirectory, to_be_enabled;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> type3overlays;
    private ProgressDialog progress;
    private AlertDialog.Builder builderSingle;
    private String[] overlayList;

    private boolean isSystemPackage(PackageInfo pkgInfo) {
        return ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    private boolean isPackageInstalled(String package_name) {
        PackageManager pm = getContext().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(
                PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(package_name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPackageUpToDate(String package_name) {
        try {
            PackageInfo pinfo = getContext().getPackageManager().getPackageInfo(package_name, 0);
            return pinfo.versionName.equals(versionName);
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e("SubstratumLogger", "Could not find explicit package identifier in " +
                    "package manager list.");
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_1, null);

        int overlayCount = 0;

        try {
            Context otherContext = getContext().createPackageContext(theme_pid, 0);
            AssetManager am = otherContext.getAssets();
            overlayList = am.list("overlays");
            overlayCount = overlayList.length;
        } catch (Exception e) {
            e.printStackTrace();
        }

        CardView quickApplyNoneView = (CardView) root.findViewById(R.id.quickApplyNoneView);
        TextView styleTitle = (TextView) root.findViewById(R.id.styleTitle);
        CardView systemOverlaysCard = (CardView) root.findViewById(R.id.systemOverlayCard);
        CardView tpOverlaysCard = (CardView) root.findViewById(R.id.tpOverlayCard);
        TextView bootAnimTitle = (TextView) root.findViewById(R.id.bootAnimTitle);
        CardView bootAnimCard = (CardView) root.findViewById(R.id.bootAnimCard);
        TextView fontsTitle = (TextView) root.findViewById(R.id.fontsTitle);
        CardView fontsCard = (CardView) root.findViewById(R.id.fontsCard);
        TextView soundsTitle = (TextView) root.findViewById(R.id.soundsTitle);
        CardView soundsCard = (CardView) root.findViewById(R.id.soundsCard);
        quickApplyNoneView.setVisibility(View.GONE);

        int sections_invisible = 0;

        if (overlayCount >= ALLOWED_AMOUNT_OF_OVERLAYS_TO_TRIGGER_QUICK_APPLY && overlayList[0]
                .equals("android")) {
            styleTitle.setVisibility(View.VISIBLE);
            systemOverlaysCard.setVisibility(View.VISIBLE);
            tpOverlaysCard.setVisibility(View.VISIBLE);
        } else {
            styleTitle.setVisibility(View.GONE);
            systemOverlaysCard.setVisibility(View.GONE);
            tpOverlaysCard.setVisibility(View.GONE);
            sections_invisible += 1;
        }

        if (tab_checker.contains("bootanimation")) {
            bootAnimTitle.setVisibility(View.VISIBLE);
            bootAnimCard.setVisibility(View.VISIBLE);
        } else {
            bootAnimTitle.setVisibility(View.GONE);
            bootAnimCard.setVisibility(View.GONE);
            sections_invisible += 1;
        }
        if (tab_checker.contains("fonts")) {
            fontsTitle.setVisibility(View.VISIBLE);
            fontsCard.setVisibility(View.VISIBLE);
        } else {
            fontsTitle.setVisibility(View.GONE);
            fontsCard.setVisibility(View.GONE);
            sections_invisible += 1;
        }
        if (tab_checker.contains("audio")) {
            soundsTitle.setVisibility(View.VISIBLE);
            soundsCard.setVisibility(View.VISIBLE);
        } else {
            soundsTitle.setVisibility(View.GONE);
            soundsCard.setVisibility(View.GONE);
            sections_invisible += 1;
        }

        if (sections_invisible == 4) {
            quickApplyNoneView.setVisibility(View.VISIBLE);
        }

        // System Overlays Dialog

        systemOverlaysCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arrayAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.dialog_listview);
                filteredDirectory = new ArrayList<>();
                type3overlays = new ArrayList<>();

                new LoadTypeThrees().execute("false");
            }
        });

        // Third Party Overlays Dialog

        tpOverlaysCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arrayAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.dialog_listview);
                filteredDirectory = new ArrayList<>();
                type3overlays = new ArrayList<>();

                new LoadTypeThrees().execute("true");
            }
        });

        // Boot Animation Dialog

        bootAnimCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(getContext());
                builderSingle.setTitle(R.string.bootanimation_default_spinner);

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.dialog_listview);
                ArrayList<String> parsedBootAnimations = new ArrayList<>();

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    String[] unparsedBootAnimations = am.list("bootanimation");
                    for (int i = 0; i < unparsedBootAnimations.length; i++) {
                        parsedBootAnimations.add(unparsedBootAnimations[i].substring(0,
                                unparsedBootAnimations[i].length() - 4));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("QuickApply", "There is no bootanimation.zip found within the assets " +
                            "of this theme!");
                }
                for (int i = 0; i < parsedBootAnimations.size(); i++) {
                    arrayAdapter.add(parsedBootAnimations.get(i));
                }
                builderSingle.setNegativeButton(
                        R.string.theme_information_quick_apply_dialog_negative_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builderSingle.setAdapter(
                        arrayAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String strName = arrayAdapter.getItem(which);
                                new BootAnimationHandler().BootAnimationHandler(strName,
                                        getContext(), theme_pid);
                            }
                        });
                builderSingle.show();
            }
        });

        // Font Dialog

        fontsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(getContext());
                builderSingle.setTitle(R.string.font_default_spinner);

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.dialog_listview);
                ArrayList<String> unarchivedFonts = new ArrayList<>();

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    String[] archivedFonts = am.list("fonts");
                    for (int i = 0; i < archivedFonts.length; i++) {
                        unarchivedFonts.add(archivedFonts[i].substring(0,
                                archivedFonts[i].length() - 4));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("QuickApply", "There is no fonts.zip found within the assets " +
                            "of this theme!");
                }
                for (int i = 0; i < unarchivedFonts.size(); i++) {
                    arrayAdapter.add(unarchivedFonts.get(i));
                }
                builderSingle.setNegativeButton(
                        R.string.theme_information_quick_apply_dialog_negative_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builderSingle.setAdapter(
                        arrayAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String strName = arrayAdapter.getItem(which);
                                new FontHandler().FontHandler(strName, getContext(), theme_pid);
                            }
                        });
                builderSingle.show();
            }
        });

        // Sounds Dialog

        soundsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(getContext());
                builderSingle.setTitle(R.string.sounds_default_spinner);

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.dialog_listview);
                ArrayList<String> unarchivedSounds = new ArrayList<>();

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    String[] archivedSounds = am.list("audio");
                    for (int i = 0; i < archivedSounds.length; i++) {
                        unarchivedSounds.add(archivedSounds[i].substring(0,
                                archivedSounds[i].length() - 4));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("QuickApply", "There is no sounds.zip found within the assets " +
                            "of this theme!");
                }
                for (int i = 0; i < unarchivedSounds.size(); i++) {
                    arrayAdapter.add(unarchivedSounds.get(i));
                }
                builderSingle.setNegativeButton(
                        R.string.theme_information_quick_apply_dialog_negative_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builderSingle.setAdapter(
                        arrayAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String strName = arrayAdapter.getItem(which);
                                new SoundsHandler().SoundsHandler(strName, getContext(), theme_pid);
                            }
                        });
                builderSingle.show();
            }
        });
        return root;
    }

    private class LoadTypeThrees extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getContext(), R.style
                    .RestoreDialog);
            progress.setMessage(getContext().getString(R.string.overlay_dialog_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            builderSingle = new AlertDialog.Builder(getContext());
            if (!Boolean.parseBoolean(result)) {
                builderSingle.setTitle(R.string.system_overlay_dialog_title);
            } else {
                builderSingle.setTitle(R.string.tp_overlay_dialog_title);
            }
            builderSingle.setNegativeButton(
                    R.string.theme_information_quick_apply_dialog_negative_button,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builderSingle.setAdapter(
                    arrayAdapter,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                new InitializeCache().execute("");
                            } else {
                                String base3overlay = arrayAdapter.getItem(which);
                                new InitializeCache().execute(base3overlay);
                            }

                        }
                    });
            builderSingle.show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Boolean tp_enabled = Boolean.parseBoolean(sUrl[0]);

            ArrayList<String> systemPackages = new ArrayList<>();
            PackageManager packageManager = getContext().getPackageManager();

            final List<PackageInfo> packageList = packageManager
                    .getInstalledPackages(PackageManager.GET_PERMISSIONS);
            for (PackageInfo packageInfo : packageList) {
                if (isSystemPackage(packageInfo)) {
                    systemPackages.add(packageInfo.packageName);
                }
            }

            try {
                PackageInfo pinfo = getContext().getPackageManager().getPackageInfo(
                        theme_pid, 0);
                versionName = pinfo.versionName;
            } catch (Exception e) {
                // Exception
            }

            try {
                Context otherContext = getContext().createPackageContext(theme_pid, 0);
                AssetManager am = otherContext.getAssets();
                String[] unfilteredDirectory = am.list("overlays");
                String[] type3Directory = am.list("overlays/android");
                for (int i = 0; i < unfilteredDirectory.length; i++) {
                    String current = unfilteredDirectory[i];
                    if (!tp_enabled) {
                        if (systemPackages.contains(current)) {
                            if (isPackageInstalled(current)) {
                                filteredDirectory.add(current);
                                Log.d("SubstratumLogger", "System Overlay: " + current);
                            }
                        }
                    } else {
                        if (!systemPackages.contains(current)) {
                            if (isPackageInstalled(current)) {
                                filteredDirectory.add(current);
                                Log.d("SubstratumLogger", "Third-party Overlay: " + current);
                            }
                        }
                    }
                }
                for (int j = 0; j < type3Directory.length; j++) {
                    String current = type3Directory[j];
                    if (!current.equals("res")) {
                        if (current.length() > 5) {
                            if (current.substring(0, 6).equals("type3_")) {
                                type3overlays.add(current.substring(6));
                            }
                        }
                    } else {
                        type3overlays.add(getString(R.string.overlay_dialog_default_res));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Filter all the type 3's and display it in the arrayAdapter
            for (int i = 0; i < type3overlays.size(); i++) {
                arrayAdapter.add(type3overlays.get(i));
            }
            return sUrl[0];
        }
    }

    private class InitializeCache extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            final_runner = new ArrayList<>();

            Log.d("SubstratumBuilder", "Decompiling and initializing work area with the " +
                    "selected " +

                    "theme's assets...");
            int notification_priority = 2; // PRIORITY_MAX == 2

            // This is the time when the notification should be shown on the user's screen
            mNotifyManager =
                    (NotificationManager) getContext().getSystemService(
                            Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(getContext());
            mBuilder.setContentTitle(getString(R.string.notification_initial_title))
                    .setProgress(100, 0, true)
                    .setSmallIcon(android.R.drawable.ic_popup_sync)
                    .setPriority(notification_priority)
                    .setOngoing(true);
            mNotifyManager.notify(id, mBuilder.build());

            PowerManager pm = (PowerManager)
                    getContext().getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();

            mProgressDialog = null;
            mProgressDialog = new ProgressDialog(getActivity(), R.style
                    .SubstratumBuilder_ActivityTheme);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            mProgressDialog.setContentView(R.layout.compile_dialog_loader);

            loader_string = (TextView) mProgressDialog.findViewById(R.id
                    .loadingTextCreativeMode);
            loader_string.setText(getContext().getResources().getString(
                    R.string.sb_phase_1_loader));
            loader = (CircularFillableLoaders) mProgressDialog.findViewById(
                    R.id.circularFillableLoader);
            loader.setProgress(60);

            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                QuickApplyMainFunction quickApplyMainFunction = new QuickApplyMainFunction();
                quickApplyMainFunction.execute(result);
            } else {
                QuickApplyMainFunction quickApplyMainFunction = new QuickApplyMainFunction();
                quickApplyMainFunction.execute("");
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Initialize Substratum cache with theme
            if (!has_initialized_cache) {
                sb = new SubstratumBuilder();
                sb.initializeCache(getContext(), theme_pid);
                has_initialized_cache = true;
            } else {
                Log.d("SubstratumBuilder", "Work area is ready with decompiled assets " +
                        "already!");
            }
            if (sUrl[0].length() != 0) {
                return sUrl[0];
            } else {
                return null;
            }
        }
    }

    private class QuickApplyMainFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 3", "This phase has started it's asynchronous task.");

            has_failed = false;

            // Change title in preparation for loop to change subtext
            mBuilder.setContentTitle(getString(R.string
                    .notification_compiling_signing_installing))
                    .setContentText(getString(R.string.notification_extracting_assets_text))
                    .setProgress(100, 0, false);
            mNotifyManager.notify(id, mBuilder.build());

            loader_string.setText(getContext().getResources().getString(
                    R.string.sb_phase_2_loader));
            loader.setProgress(20);
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            String final_commands = "";

            for (int j = 0; j < final_runner.size(); j++) {
                final_commands = final_commands + final_runner.get(j) + " ";
            }
            if (final_commands.length() == 0) {
                final_commands += "om disable-all";
            } else {
                final_commands += "&& om disable-all";
            }

            mWakeLock.release();
            mProgressDialog.dismiss();

            Intent notificationIntent = new Intent();
            notificationIntent.putExtra("theme_name", theme_name);
            notificationIntent.putExtra("theme_pid", theme_pid);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent intent =
                    PendingIntent.getActivity(getActivity(), 0, notificationIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

            if (!has_failed) {
                // Closing off the persistent notification
                mBuilder.setAutoCancel(true);
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mBuilder.setContentIntent(intent);
                mBuilder.setSmallIcon(R.drawable.notification_success_icon);
                mBuilder.setContentTitle(getString(R.string.notification_done_title));
                mBuilder.setContentText(getString(R.string.notification_no_errors_found));
                mBuilder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
                mNotifyManager.notify(id, mBuilder.build());

                Toast toast = Toast.makeText(getContext(), getString(R
                                .string.toast_compiled_updated),
                        Toast.LENGTH_SHORT);
                toast.show();
            } else {
                // Closing off the persistent notification
                mBuilder.setAutoCancel(true);
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mBuilder.setContentIntent(intent);
                mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
                mBuilder.setContentTitle(getString(R.string.notification_done_title));
                mBuilder.setContentText(getString(R.string.notification_some_errors_found));
                mBuilder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
                mNotifyManager.notify(id, mBuilder.build());

                Toast toast = Toast.makeText(getContext(), getString(R
                                .string.toast_compiled_updated_with_errors),
                        Toast.LENGTH_SHORT);
                toast.show();
            }

            for (int j = 0; j < to_be_enabled.size(); j++) {
                if (j == 0) {
                    final_commands = final_commands + " && om enable " + to_be_enabled.get(j);
                } else {
                    final_commands = final_commands + " " + to_be_enabled.get(j);
                }
            }
            Log.e("SubstratumLogger", final_commands);
            Root.runCommand(final_commands);

            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {

            String base3overlay = sUrl[0];
            final_runner = new ArrayList<>();
            to_be_enabled = new ArrayList<>();

            String base = base3overlay.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
            for (int i = 0; i < filteredDirectory.size(); i++) {
                String current_overlay = filteredDirectory.get(i);

                // Initialize working notification
                try {
                    ApplicationInfo applicationInfo = getContext().getPackageManager()
                            .getApplicationInfo(current_overlay, 0);
                    String packageTitle = getContext().getPackageManager().getApplicationLabel
                            (applicationInfo).toString();

                    mBuilder.setProgress(100, (int) (((double) (i + 1) / filteredDirectory.size
                            ()) * 100), false);
                    mBuilder.setContentText(getString(R.string.notification_processing) + " " +
                            "\"" +
                            packageTitle + "\"");
                    mNotifyManager.notify(id, mBuilder.build());

                    sb = new SubstratumBuilder();

                    String package_name = filteredDirectory.get(i) + "." +
                            theme_name.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "") +
                            ((base.length() == 0) ? "" : "." + base);

                    if (isPackageInstalled(package_name)) {
                        if (!isPackageUpToDate(package_name)) {
                            sb.beginAction(getContext(), filteredDirectory.get(i), theme_name,
                                    "false", "", null,
                                    ((base3overlay.length() == 0) ? null : base3overlay),
                                    versionName);
                            if (sb.no_install.length() > 0) {
                                final_runner.add(sb.no_install);
                            }
                        }
                        to_be_enabled.add(package_name);
                    } else {
                        sb.beginAction(getContext(), filteredDirectory.get(i), theme_name,
                                "true", "", null,
                                ((base3overlay.length() == 0) ? null : base3overlay),
                                versionName);
                        to_be_enabled.add(package_name);
                    }
                    if (sb.has_errored_out) {
                        has_failed = true;
                    }
                } catch (Exception e) {
                    Log.e("SubstratumLogger", "There was an error trying to run " +
                            "SubstratumBuilder.");
                }
            }

            Root.runCommand("cp /data/system/overlays.xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            String[] commands5 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "5"};

            String parse1_themeName = theme_name.replaceAll("\\s+", "");
            String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

            List<String> state5 = ReadOverlaysFile.main(commands5);
            ArrayList<String> all_installed_overlays = new ArrayList<>(state5);
            List<String> state5overlays = new ArrayList<>(all_installed_overlays);

            for (int i = 0; i < state5overlays.size(); i++) {
                try {
                    ApplicationInfo appInfo = getContext().getPackageManager().getApplicationInfo(
                            state5overlays.get(i), PackageManager.GET_META_DATA);
                    if (appInfo.metaData != null) {
                        if (appInfo.metaData.getString("Substratum_Variant") != null) {
                            if (appInfo.metaData.getString("Substratum_Variant")
                                    .equals(((base3overlay.length() == 0) ? "" : "." +
                                            base3overlay))) {
                                if (appInfo.metaData.getString("Substratum_Parent") != null) {
                                    if (appInfo.metaData.getString("Substratum_Parent")
                                            .equals(parse2_themeName)) {
                                        to_be_enabled.add(state5overlays.get(i));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Exception
                }
            }
            return null;
        }
    }
}