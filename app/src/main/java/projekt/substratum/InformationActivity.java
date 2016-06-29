package projekt.substratum;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.flaviofaria.kenburnsview.KenBurnsView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import projekt.substratum.adapters.InformationTabsAdapter;
import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class InformationActivity extends AppCompatActivity {

    public static String theme_name, theme_pid;

    private final int THEME_INFORMATION_REQUEST_CODE = 1;

    private Boolean uninstalled = false;

    public static String getThemeName() {
        return theme_name;
    }

    public static String getThemePID() {
        return theme_pid;
    }

    public static int getDominantColor(Bitmap bitmap) {
        return bitmap.getPixel(0, 0);
    }

    public Drawable grabPackageHeroImage(String package_name) {
        Resources res;
        Drawable hero = null;
        try {
            res = getPackageManager().getResourcesForApplication(package_name);
            int resourceId = res.getIdentifier(package_name + ":drawable/heroimage", null, null);
            if (0 != resourceId) {
                hero = getPackageManager().getDrawable(package_name, resourceId, null);
            }
            return hero;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Drawable grabAppIcon(String package_name) {
        Drawable icon = null;
        try {
            icon = getPackageManager().getApplicationIcon(package_name);
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e("SubstratumLogger", "Could not grab the application icon for \"" + package_name
                    + "\"");
        }
        return icon;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_activity_tabs);

        boolean dynamicActionBarColors = getResources().getBoolean(R.bool.dynamicActionBarColors);

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");
        theme_pid = currentIntent.getStringExtra("theme_pid");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setTitle(theme_name);

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById
                (R.id.collapsing_toolbar_tabbed_layout);
        if (collapsingToolbarLayout != null) collapsingToolbarLayout.setTitle(theme_name);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Drawable heroImage = grabPackageHeroImage(theme_pid);
        Bitmap heroImageBitmap = ((BitmapDrawable) heroImage).getBitmap();

        int dominantColor = getDominantColor(heroImageBitmap);

        KenBurnsView kenBurnsView = (KenBurnsView) findViewById(R.id.kenburnsView);
        if (kenBurnsView != null) kenBurnsView.setImageDrawable(heroImage);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                    .theme_information_tab_one)));
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                    .theme_information_tab_two)));
            try {
                Context otherContext = getApplicationContext().createPackageContext(theme_pid, 0);
                AssetManager am = otherContext.getAssets();
                if (Arrays.asList(am.list("")).contains("bootanimation")) {
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                            .theme_information_tab_three)));
                }
                if (Arrays.asList(am.list("")).contains("fonts")) {
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                            .theme_information_tab_four)));
                }
                if (Arrays.asList(am.list("")).contains("audio")) {
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                            .theme_information_tab_five)));
                }
            } catch (Exception e) {
                Log.e("SubstratumLogger", "Could not refresh list of asset folders.");
            }
            tabLayout.setTabGravity(TabLayout.MODE_SCROLLABLE);
            if (dynamicActionBarColors) tabLayout.setBackgroundColor(dominantColor);
        }

        if (collapsingToolbarLayout != null && dynamicActionBarColors) {
            collapsingToolbarLayout.setStatusBarScrimColor(dominantColor);
            collapsingToolbarLayout.setContentScrimColor(dominantColor);
        }

        Root.requestRootAccess();

        final InformationTabsAdapter adapter = new InformationTabsAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount(), getApplicationContext(),
                        theme_pid);
        if (viewPager != null) {
            viewPager.setOffscreenPageLimit(tabLayout.getTabCount());
            viewPager.setAdapter(adapter);
            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener
                    (tabLayout));
            tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    viewPager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_information_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.clean) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(grabAppIcon(theme_pid));
            builder.setMessage(R.string.clean_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin uninstalling all overlays based on this package
                            Root.runCommand("cp /data/system/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");

                            String[] commands = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "4"};

                            String[] commands1 = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "5"};

                            List<String> stateAll = ReadOverlaysFile.main(commands);
                            stateAll.addAll(ReadOverlaysFile.main(commands1));

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager()
                                            .getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null) {
                                        if (appInfo.metaData.getString("Substratum_Parent") !=
                                                null) {
                                            if (appInfo.metaData.getString("Substratum_Parent")
                                                    .equals(parse2_themeName)) {
                                                all_overlays.add(current);
                                            }
                                        }
                                    }
                                } catch (PackageManager.NameNotFoundException nnfe) {
                                    Log.e("SubstratumLogger", "Could not find explicit package " +
                                            "for " +
                                            "this overlay...");
                                }
                            }

                            String commands2 = "";
                            for (int i = 0; i < all_overlays.size(); i++) {
                                if (i == 0) {
                                    commands2 = commands2 + "pm uninstall " + all_overlays
                                            .get(i);
                                } else {
                                    commands2 = commands2 + " && pm uninstall " +
                                            all_overlays.get(i);
                                }
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .clean_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            Root.runCommand(commands2);
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return true;
        }
        if (id == R.id.disable) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(grabAppIcon(theme_pid));
            builder.setMessage(R.string.disable_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin disabling all overlays based on this package
                            Root.runCommand("cp /data/system/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");

                            String[] commands = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "4"};

                            String[] commands1 = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "5"};

                            List<String> stateAll = ReadOverlaysFile.main(commands);
                            stateAll.addAll(ReadOverlaysFile.main(commands1));

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager()
                                            .getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null) {
                                        if (appInfo.metaData.getString("Substratum_Parent") !=
                                                null) {
                                            if (appInfo.metaData.getString("Substratum_Parent")
                                                    .equals(parse2_themeName)) {
                                                all_overlays.add(current);
                                            }
                                        }
                                    }
                                } catch (PackageManager.NameNotFoundException nnfe) {
                                    Log.e("SubstratumLogger", "Could not find explicit package " +
                                            "for " +
                                            "this overlay...");
                                }
                            }

                            String commands2 = "om disable ";
                            for (int i = 0; i < all_overlays.size(); i++) {
                                commands2 = commands2 + all_overlays.get(i) + " ";
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .disable_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            Log.e("commands", commands2);

                            Root.runCommand(commands2);
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return true;
        }
        if (id == R.id.enable) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(grabAppIcon(theme_pid));
            builder.setMessage(R.string.enable_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin enabling all overlays based on this package
                            Root.runCommand("cp /data/system/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");

                            String[] commands = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "4"};

                            String[] commands1 = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "5"};

                            List<String> stateAll = ReadOverlaysFile.main(commands);
                            stateAll.addAll(ReadOverlaysFile.main(commands1));

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager()
                                            .getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null) {
                                        if (appInfo.metaData.getString("Substratum_Parent") !=
                                                null) {
                                            if (appInfo.metaData.getString("Substratum_Parent")
                                                    .equals(parse2_themeName)) {
                                                all_overlays.add(current);
                                            }
                                        }
                                    }
                                } catch (PackageManager.NameNotFoundException nnfe) {
                                    Log.e("SubstratumLogger", "Could not find explicit package " +
                                            "for " +
                                            "this overlay...");
                                }
                            }

                            String commands2 = "om enable ";
                            for (int i = 0; i < all_overlays.size(); i++) {
                                commands2 = commands2 + all_overlays.get(i) + " ";
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .enable_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            Log.e("commands", commands2);

                            Root.runCommand(commands2);
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return true;
        }
        if (id == R.id.rate) {
            String playURL = "https://play.google.com/store/apps/details?id=" + theme_pid;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(playURL));
            startActivity(i);
            return true;
        }
        if (id == R.id.uninstall) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(grabAppIcon(theme_pid));
            builder.setMessage(R.string.uninstall_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Root.runCommand("pm uninstall " + theme_pid);

                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin uninstalling all overlays based on this package
                            Root.runCommand("cp /data/system/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");

                            String[] commands = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "4"};

                            String[] commands1 = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "5"};

                            List<String> stateAll = ReadOverlaysFile.main(commands);
                            stateAll.addAll(ReadOverlaysFile.main(commands1));

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager()
                                            .getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null) {
                                        if (appInfo.metaData.getString("Substratum_Parent") !=
                                                null) {
                                            if (appInfo.metaData.getString("Substratum_Parent")
                                                    .equals(parse2_themeName)) {
                                                all_overlays.add(current);
                                            }
                                        }
                                    }
                                } catch (PackageManager.NameNotFoundException nnfe) {
                                    Log.e("SubstratumLogger", "Could not find explicit package " +
                                            "for " +
                                            "this overlay...");
                                }
                            }

                            String commands2 = "";
                            for (int i = 0; i < all_overlays.size(); i++) {
                                if (i == 0) {
                                    commands2 = commands2 + "pm uninstall " + all_overlays
                                            .get(i);
                                } else {
                                    commands2 = commands2 + " && pm uninstall " +
                                            all_overlays.get(i);
                                }
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .clean_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            Root.runCommand(commands2);

                            // Finally close out of the window
                            uninstalled = true;
                            onBackPressed();
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        if (uninstalled) {
            intent.putExtra("Uninstalled", true);
        } else {
            intent.putExtra("Uninstalled", false);
        }
        setResult(THEME_INFORMATION_REQUEST_CODE, intent);
        // Destroy the cache if the user leaves the activity
        super.onBackPressed();
        clearCache clear = new clearCache();
        clear.execute("");
    }

    private class clearCache extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("SubstratumBuilder", "The cache has been flushed!");
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Superuser is used due to some files being held hostage by the system
            File cacheFolder = new File(getCacheDir().getAbsolutePath() + "/SubstratumBuilder/");
            if (cacheFolder.exists()) {
                Root.runCommand(
                        "rm -r " + getCacheDir().getAbsolutePath() +
                                "/SubstratumBuilder/");
            }
            return null;
        }
    }
}