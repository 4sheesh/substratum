package projekt.substratum;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gordonwong.materialsheetfab.MaterialSheetFab;
import com.mikhaellopez.circularfillableloaders.CircularFillableLoaders;
import com.stericson.RootTools.RootTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.util.FloatingActionMenu;
import projekt.substratum.util.LayersBuilder;
import projekt.substratum.util.ReadXMLFile;


/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ThemeInformation extends AppCompatActivity {

    public ListView listView;
    public String theme_name, theme_pid;
    public AssetManager am;
    public ArrayList<String> values;
    public Boolean has_extracted_cache;
    public LayersBuilder lb;
    public CircularFillableLoaders loader;
    public TextView loader_string;
    public List<String> listStrings, erroredOverlays, mixAndMatch;
    public Switch toggle_overlays;
    public ProgressBar progressBar;
    ProgressDialog mProgressDialog;
    private PowerManager.WakeLock mWakeLock;
    private ArrayList<String> enabled_overlays;
    private ArrayAdapter<String> adapter;
    private String final_commands;
    private List<String> unapproved_overlays;
    private SharedPreferences prefs;
    private String current_mode;
    private Boolean mixAndMatchMode;
    private String swap_mode_commands;

    private boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void checkEnabledOverlays() {
        try {
            String line;
            enabled_overlays = new ArrayList<String>();
            Process nativeApp = Runtime.getRuntime().exec(
                    "om list");

            OutputStream stdin = nativeApp.getOutputStream();
            InputStream stderr = nativeApp.getErrorStream();
            InputStream stdout = nativeApp.getInputStream();
            stdin.write(("ls\n").getBytes());
            stdin.write("exit\n".getBytes());
            stdin.flush();
            stdin.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                if (line.contains("    [x] ")) {
                    enabled_overlays.add(line.substring(8));
                }
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                Log.e("LayersBuilder", line);
            }
            br.close();
        } catch (IOException ioe) {
        }
    }

    private void checkMixAndMatch() {
        try {
            String line;
            mixAndMatch = new ArrayList<String>();
            Process nativeApp = Runtime.getRuntime().exec(
                    "om list");

            OutputStream stdin = nativeApp.getOutputStream();
            InputStream stderr = nativeApp.getErrorStream();
            InputStream stdout = nativeApp.getInputStream();
            stdin.write(("ls\n").getBytes());
            stdin.write("exit\n".getBytes());
            stdin.flush();
            stdin.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                if (line.contains("    ")) {
                    String[] packageNameParsed = line.substring(8).split("\\.");
                    mixAndMatch.add(packageNameParsed[packageNameParsed.length - 1]);
                }
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                Log.e("LayersBuilder", line);
            }
            br.close();
        } catch (IOException ioe) {
        }
    }

    public Drawable grabAppIcon(String package_name) {
        Drawable icon = null;
        try {
            icon = getPackageManager().getApplicationIcon(package_name);
        } catch (PackageManager.NameNotFoundException nnfe) {
        }
        return icon;
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
        return hero;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_information);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        enabled_overlays = new ArrayList<String>();
        has_extracted_cache = false;

        // Handle collapsible toolbar with theme name

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");
        theme_pid = currentIntent.getStringExtra("theme_pid");

        View sheetView = findViewById(R.id.fab_sheet);
        View overlay = findViewById(R.id.overlay);
        int sheetColor = getColor(R.color.fab_menu_background_card);
        int fabColor = getColor(R.color.colorAccent);

        final FloatingActionMenu floatingActionButton = (FloatingActionMenu) findViewById(R
                .id.apply_fab);
        if (floatingActionButton != null) {
            floatingActionButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                }
            });
            floatingActionButton.hide();
        }

        // Create material sheet FAB
        MaterialSheetFab materialSheetFab = new MaterialSheetFab<>(floatingActionButton,
                sheetView, overlay,
                sheetColor, fabColor);

        Switch enable_swap = (Switch) findViewById(R.id.enable_swap);
        if (prefs.getBoolean("enable_swapping_overlays", true)) {
            mixAndMatchMode = false;
            enable_swap.setChecked(false);
        } else {
            mixAndMatchMode = true;
            enable_swap.setChecked(true);
        }
        enable_swap.setOnCheckedChangeListener(new CompoundButton
                .OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prefs.edit().putBoolean("enable_swapping_overlays", true).apply();
                    mixAndMatchMode = true;
                } else {
                    prefs.edit().putBoolean("enable_swapping_overlays", false).apply();
                    mixAndMatchMode = false;
                }
            }
        });

        TextView compile_selected = (TextView) findViewById(R.id.compile_selected);
        if (compile_selected != null)
            compile_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    current_mode = "compile";
                    progressBar.setVisibility(View.VISIBLE);
                    SparseBooleanArray checked = listView.getCheckedItemPositions();
                    listStrings = new ArrayList<String>();
                    for (int i = 0; i < listView.getAdapter()
                            .getCount(); i++) {
                        if (checked.get(i)) {
                            listStrings.add(listView
                                    .getItemAtPosition(i)
                                    .toString());
                        }
                    }
                    // Run through phase two - initialize the cache for the specific theme
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute("");
                }
            });

        TextView compile_enable_selected = (TextView) findViewById(R.id.compile_enable_selected);
        if (compile_enable_selected != null)
            compile_enable_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays.xml " +
                            Environment
                                    .getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml");
                    String[] commands0 = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "4"};
                    String[] commands1 = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "5"};

                    List<String> state4 = ReadXMLFile.main(commands0);
                    List<String> state5 = ReadXMLFile.main(commands1);

                    unapproved_overlays = new ArrayList<String>(state4);
                    unapproved_overlays.addAll(state5);

                    if (!mixAndMatchMode) {
                        String parse1_themeName = theme_name.replaceAll("\\s+", "");
                        String parse2_themeName = parse1_themeName.replaceAll
                                ("[^a-zA-Z0-9]+", "");

                        checkMixAndMatch();

                        boolean if_all_same_theme = true;
                        for (int i = 0; i < mixAndMatch.size(); i++) {
                            if (!mixAndMatch.get(i).equals(parse2_themeName)) {
                                if_all_same_theme = false;
                            }
                        }
                        if (if_all_same_theme) {
                            mixAndMatchMode = true;
                        }
                    }

                    current_mode = "compile_enable";
                    progressBar.setVisibility(View.VISIBLE);
                    SparseBooleanArray checked = listView.getCheckedItemPositions();
                    listStrings = new ArrayList<String>();
                    for (int i = 0; i < listView.getAdapter()
                            .getCount(); i++) {
                        if (checked.get(i)) {
                            listStrings.add(listView
                                    .getItemAtPosition(i)
                                    .toString());
                        }
                    }
                    // Run through phase two - initialize the cache for the specific theme
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute("");
                }
            });

        TextView disable_selected = (TextView) findViewById(R.id.disable_selected);
        if (disable_selected != null)
            disable_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    current_mode = "disable";
                    progressBar.setVisibility(View.VISIBLE);
                    SparseBooleanArray checked = listView.getCheckedItemPositions();
                    listStrings = new ArrayList<String>();
                    for (int i = 0; i < listView.getAdapter()
                            .getCount(); i++) {
                        if (checked.get(i)) {
                            listStrings.add(listView
                                    .getItemAtPosition(i)
                                    .toString());
                        }
                    }
                    // Run through phase two - initialize the cache for the specific theme
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute("");
                }
            });

        TextView enable_selected = (TextView) findViewById(R.id.enable_selected);
        if (enable_selected != null) enable_selected.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                current_mode = "enable";
                progressBar.setVisibility(View.VISIBLE);
                SparseBooleanArray checked = listView.getCheckedItemPositions();
                listStrings = new ArrayList<String>();
                for (int i = 0; i < listView.getAdapter()
                        .getCount(); i++) {
                    if (checked.get(i)) {
                        listStrings.add(listView
                                .getItemAtPosition(i)
                                .toString());
                    }
                }
                // Run through phase two - initialize the cache for the specific theme
                Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                phase2_initializeCache.execute("");
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.loading_bar);
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        ImageView imageView = (ImageView) findViewById(R.id.preview_image);
        if (imageView != null) imageView.setImageDrawable(grabPackageHeroImage(theme_pid));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setTitle(theme_name);

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById
                (R.id.collapsingToolbarLayout);
        if (collapsingToolbarLayout != null) collapsingToolbarLayout.setTitle(theme_name);

        // From now on parse the theme_name as a package_identifier ready version
        String parse1_themeName = theme_name.replaceAll("\\s+", "");
        theme_name = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Handle all overlays that are located in the APK
        listView = (ListView) findViewById(R.id.overlay_picker);

        LoadOverlays loadOverlays = new LoadOverlays();
        loadOverlays.execute("");

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int counter = 0;
                SparseBooleanArray checked = listView.getCheckedItemPositions();
                for (int i = 0; i < listView.getAdapter()
                        .getCount(); i++) {
                    if (checked.get(i)) {
                        counter += 1;
                    }
                }
                if (counter > 0) {
                    if (counter == listView.getCount()) {
                        if (!toggle_overlays.isChecked()) {
                            toggle_overlays.setChecked(true);
                        }
                    }
                    floatingActionButton.show();
                } else {
                    if (toggle_overlays.isChecked()) {
                        toggle_overlays.setChecked(false);
                    }
                    floatingActionButton.hide();
                }
            }
        });

        // Handle the logic for selecting all overlays or not

        toggle_overlays = (Switch) findViewById(R.id.toggle_all_overlays);
        if (toggle_overlays != null) {
            toggle_overlays.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            try {
                                SparseBooleanArray checked = listView.getCheckedItemPositions();
                                for (int i = 0; i < listView.getAdapter().getCount(); i++) {
                                    if (checked.get(i)) {
                                        if (!isChecked == listView.isItemChecked(i)) {
                                            floatingActionButton.hide();
                                            listView.setItemChecked(i, false);
                                        } else {
                                            floatingActionButton.show();
                                            listView.setItemChecked(i, true);
                                        }
                                    } else {
                                        if (!isChecked == listView.isItemChecked(i)) {
                                            floatingActionButton.show();
                                            listView.setItemChecked(i, true);
                                        } else {
                                            floatingActionButton.hide();
                                            listView.setItemChecked(i, false);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // Deal with app force closing after too many window refreshes
                                Log.e("SubstratumLogger", "ThemeInformation window refreshed too " +
                                        "many times, restarting current activity to preserve app " +
                                        "integrity.");
                                Intent intent = getIntent();
                                finish();
                                startActivity(intent);
                            }
                        }
                    });
        }

        // Run through phase one - checking whether aapt exists on the device
        Phase1_AAPT_Check phase1_aapt_check = new Phase1_AAPT_Check();
        phase1_aapt_check.execute("");

        mProgressDialog = null;
        mProgressDialog = new ProgressDialog(ThemeInformation.this, R.style
                .LayersBuilder_ActivityTheme);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
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
            if (RootTools.isRootAvailable()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ThemeInformation.this);
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
                                try {
                                    String line;
                                    Boolean systemui_found = false;
                                    Process nativeApp = Runtime.getRuntime().exec(
                                            "om list");

                                    OutputStream stdin = nativeApp.getOutputStream();
                                    InputStream stderr = nativeApp.getErrorStream();
                                    InputStream stdout = nativeApp.getInputStream();
                                    stdin.write(("ls\n").getBytes());
                                    stdin.write("exit\n".getBytes());
                                    stdin.flush();
                                    stdin.close();

                                    BufferedReader br = new BufferedReader(new InputStreamReader
                                            (stdout));
                                    while ((line = br.readLine()) != null) {
                                        if (line.contains("    ")) {
                                            String[] packageNameParsed = line.substring(8).split
                                                    ("\\.");
                                            if (packageNameParsed[packageNameParsed.length - 1]
                                                    .equals(parse2_themeName)) {
                                                if (line.substring(8).contains("systemui"))
                                                    systemui_found = true;
                                                Log.d("OverlayCleaner", "Removing overlay \"" +
                                                        line.substring(8) + "\"");
                                                eu.chainfire.libsuperuser.Shell.SU.run(
                                                        "pm uninstall " + line.substring(8));
                                            }
                                        }
                                    }
                                    br.close();
                                    br = new BufferedReader(new InputStreamReader(stderr));
                                    while ((line = br.readLine()) != null) {
                                        Log.e("LayersBuilder", line);
                                    }
                                    br.close();
                                    if (systemui_found)
                                        eu.chainfire.libsuperuser.Shell.SU.run("pkill com.android" +
                                                ".systemui");
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            getString(R.string
                                                    .clean_completion),
                                            Toast.LENGTH_SHORT);
                                    toast.show();
                                } catch (Exception e) {
                                }

                                // Finally close out of the window
                                adapter.notifyDataSetChanged();
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

            } else {
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + theme_pid));
                startActivity(intent);
            }
            return true;
        }
        if (id == R.id.disable) {
            if (RootTools.isRootAvailable()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ThemeInformation.this);
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

                                // Begin uninstalling all overlays based on this package
                                try {
                                    String line;
                                    String commands = "";
                                    ArrayList<String> all_overlays = new ArrayList<String>();

                                    Boolean systemui_found = false;
                                    Process nativeApp = Runtime.getRuntime().exec(
                                            "om list");

                                    OutputStream stdin = nativeApp.getOutputStream();
                                    InputStream stderr = nativeApp.getErrorStream();
                                    InputStream stdout = nativeApp.getInputStream();
                                    stdin.write(("ls\n").getBytes());
                                    stdin.write("exit\n".getBytes());
                                    stdin.flush();
                                    stdin.close();

                                    BufferedReader br = new BufferedReader(new InputStreamReader
                                            (stdout));
                                    while ((line = br.readLine()) != null) {
                                        if (line.contains("    ")) {
                                            String[] packageNameParsed = line.substring(8).split
                                                    ("\\.");
                                            if (packageNameParsed[packageNameParsed.length - 1]
                                                    .equals(parse2_themeName)) {
                                                if (line.substring(8).contains("systemui"))
                                                    systemui_found = true;
                                                Log.d("OverlayDisabler", "Disabling overlay \"" +
                                                        line.substring(8) + "\"");
                                                all_overlays.add(line.substring(8));
                                            }
                                        }
                                    }
                                    for (int i = 0; i < all_overlays.size(); i++) {
                                        if (i == 0) {
                                            commands = commands + "om disable " + all_overlays
                                                    .get(i);
                                        } else {
                                            commands = commands + " && om disable " +
                                                    all_overlays.get(i);
                                        }
                                    }
                                    if (commands.contains("systemui")) {
                                        commands = commands + " && pkill com.android.systemui";
                                    }
                                    br.close();
                                    br = new BufferedReader(new InputStreamReader(stderr));
                                    while ((line = br.readLine()) != null) {
                                        Log.e("LayersBuilder", line);
                                    }
                                    br.close();
                                    eu.chainfire.libsuperuser.Shell.SU.run(commands);
                                    if (systemui_found)
                                        eu.chainfire.libsuperuser.Shell.SU.run("pkill com.android" +
                                                ".systemui");
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            getString(R.string
                                                    .disable_completion),
                                            Toast.LENGTH_SHORT);
                                    toast.show();
                                } catch (Exception e) {
                                }

                                // Finally close out of the window
                                adapter.notifyDataSetChanged();
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

            } else {
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + theme_pid));
                startActivity(intent);
            }
            return true;
        }
        if (id == R.id.enable) {
            if (RootTools.isRootAvailable()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ThemeInformation.this);
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

                                // Begin uninstalling all overlays based on this package
                                try {
                                    String line;
                                    String commands = "";
                                    ArrayList<String> all_overlays = new ArrayList<String>();

                                    Boolean systemui_found = false;
                                    Process nativeApp = Runtime.getRuntime().exec(
                                            "om list");

                                    OutputStream stdin = nativeApp.getOutputStream();
                                    InputStream stderr = nativeApp.getErrorStream();
                                    InputStream stdout = nativeApp.getInputStream();
                                    stdin.write(("ls\n").getBytes());
                                    stdin.write("exit\n".getBytes());
                                    stdin.flush();
                                    stdin.close();

                                    BufferedReader br = new BufferedReader(new InputStreamReader
                                            (stdout));
                                    while ((line = br.readLine()) != null) {
                                        if (line.contains("    ")) {
                                            String[] packageNameParsed = line.substring(8).split
                                                    ("\\.");
                                            if (packageNameParsed[packageNameParsed.length - 1]
                                                    .equals(parse2_themeName)) {
                                                if (line.substring(8).contains("systemui"))
                                                    systemui_found = true;
                                                Log.d("OverlayEnabler", "Enabling overlay \"" +
                                                        line.substring(8) + "\"");
                                                all_overlays.add(line.substring(8));
                                            }
                                        }
                                    }
                                    for (int i = 0; i < all_overlays.size(); i++) {
                                        if (i == 0) {
                                            commands = commands + "om enable " + all_overlays
                                                    .get(i);
                                        } else {
                                            commands = commands + " && om enable " +
                                                    all_overlays.get(i);
                                        }
                                    }
                                    if (commands.contains("systemui")) {
                                        commands = commands + " && pkill com.android.systemui";
                                    }
                                    br.close();
                                    br = new BufferedReader(new InputStreamReader(stderr));
                                    while ((line = br.readLine()) != null) {
                                        Log.e("LayersBuilder", line);
                                    }
                                    br.close();
                                    eu.chainfire.libsuperuser.Shell.SU.run(commands);
                                    if (systemui_found)
                                        eu.chainfire.libsuperuser.Shell.SU.run("pkill com.android" +
                                                ".systemui");
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            getString(R.string
                                                    .enable_completion),
                                            Toast.LENGTH_SHORT);
                                    toast.show();
                                } catch (Exception e) {
                                }

                                // Finally close out of the window
                                adapter.notifyDataSetChanged();
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

            } else {
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + theme_pid));
                startActivity(intent);
            }
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
            if (RootTools.isRootAvailable()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ThemeInformation.this);
                builder.setTitle(theme_name);
                builder.setIcon(grabAppIcon(theme_pid));
                builder.setMessage(R.string.uninstall_dialog_body)
                        .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                eu.chainfire.libsuperuser.Shell.SU.run("pm uninstall " + theme_pid);

                                // Quickly parse theme_name
                                String parse1_themeName = theme_name.replaceAll("\\s+", "");
                                String parse2_themeName = parse1_themeName.replaceAll
                                        ("[^a-zA-Z0-9]+", "");

                                // Begin uninstalling all overlays based on this package
                                try {
                                    String line;
                                    Boolean systemui_found = false;
                                    Process nativeApp = Runtime.getRuntime().exec(
                                            "om list");

                                    OutputStream stdin = nativeApp.getOutputStream();
                                    InputStream stderr = nativeApp.getErrorStream();
                                    InputStream stdout = nativeApp.getInputStream();
                                    stdin.write(("ls\n").getBytes());
                                    stdin.write("exit\n".getBytes());
                                    stdin.flush();
                                    stdin.close();

                                    BufferedReader br = new BufferedReader(new InputStreamReader
                                            (stdout));
                                    while ((line = br.readLine()) != null) {
                                        if (line.contains("    ")) {
                                            String[] packageNameParsed = line.substring(8).split
                                                    ("\\.");
                                            if (packageNameParsed[packageNameParsed.length - 1]
                                                    .equals(parse2_themeName)) {
                                                if (line.substring(8).contains("systemui"))
                                                    systemui_found = true;
                                                Log.d("OverlayCleaner", "Removing overlay \"" +
                                                        line.substring(8) + "\"");
                                                eu.chainfire.libsuperuser.Shell.SU.run(
                                                        "pm uninstall " + line.substring(8));
                                            }
                                        }
                                    }
                                    br.close();
                                    br = new BufferedReader(new InputStreamReader(stderr));
                                    while ((line = br.readLine()) != null) {
                                        Log.e("LayersBuilder", line);
                                    }
                                    br.close();
                                    if (systemui_found)
                                        eu.chainfire.libsuperuser.Shell.SU.run("pkill com.android" +
                                                ".systemui");
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            getString(R.string
                                                    .purge_completion),
                                            Toast.LENGTH_SHORT);
                                    toast.show();
                                } catch (Exception e) {
                                }

                                // Finally close out of the window
                                finish();
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

            } else {
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + theme_pid));
                startActivity(intent);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Destroy the cache if the user leaves the activity
        // Superuser is used due to some files being held hostage by the system

        File[] fileList = new File(getCacheDir().getAbsolutePath() +
                "/LayersBuilder/").listFiles();
        for (int i = 0; i < fileList.length; i++) {
            eu.chainfire.libsuperuser.Shell.SU.run(
                    "rm -r " + getCacheDir().getAbsolutePath() +
                            "/LayersBuilder/" + fileList[i].getName());
        }
        Log.d("LayersBuilder", "The cache has been flushed!");

        super.onBackPressed();
    }

    private class LoadOverlays extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            MaterialProgressBar materialProgressBar = (MaterialProgressBar) findViewById(R.id
                    .progress_bar);
            if (materialProgressBar != null) materialProgressBar.setVisibility(View.GONE);

            if (listView != null) {
                listView.setNestedScrollingEnabled(true);
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                adapter = new ArrayAdapter<String>(ThemeInformation.this, android.R.layout
                        .simple_list_item_multiple_choice, values) {
                    @Override
                    public View getView(final int position, View convertView, ViewGroup parent) {
                        TextView textView = (TextView) super.getView(position, convertView, parent);
                        if (isPackageInstalled(ThemeInformation.this, listView.getItemAtPosition
                                (position).toString() + "." + theme_name)) {
                            if (enabled_overlays.contains(listView.getItemAtPosition(position)
                                    .toString() + "." + theme_name)) {
                                textView.setTextColor(getColor(R.color
                                        .overlay_installed_list_entry));
                            } else {
                                if (unapproved_overlays.contains(listView.getItemAtPosition
                                        (position).toString() + "." + theme_name)) {
                                    SpannableString string = new SpannableString(textView.getText
                                            ());
                                    string.setSpan(new StrikethroughSpan(), 0, string.length(), 0);
                                    textView.setText(string);
                                    textView.setTextColor(getColor(R.color
                                            .overlay_not_approved_list_entry));
                                } else {
                                    textView.setTextColor(getColor(R.color
                                            .overlay_not_enabled_list_entry));
                                }
                            }
                        } else {
                            textView.setTextColor(getColor(R.color
                                    .overlay_not_installed_list_entry));
                        }
                        return textView;
                    }
                };
                listView.setAdapter(adapter);
            }
            toggle_overlays.setClickable(true);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext());

            // Filter out state 0-3 overlays before enabling them

            eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays.xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            String[] commands0 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "0"};
            String[] commands1 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "1"};
            String[] commands2 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "2"};
            String[] commands3 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "3"};

            List<String> state0 = ReadXMLFile.main(commands0);
            List<String> state1 = ReadXMLFile.main(commands1);
            List<String> state2 = ReadXMLFile.main(commands2);
            List<String> state3 = ReadXMLFile.main(commands3);

            unapproved_overlays = new ArrayList<String>(state0);
            unapproved_overlays.addAll(state1);
            unapproved_overlays.addAll(state2);
            unapproved_overlays.addAll(state3);

            // Parse the list of overlay folders inside assets/overlays
            try {
                values = new ArrayList<String>();
                Context otherContext = createPackageContext(theme_pid, 0);
                am = otherContext.getAssets();
                String[] am_list = am.list("overlays");

                for (int i = 0; i < am_list.length; i++) {
                    if (prefs.getBoolean("show_installed_packages", true)) {
                        if (isPackageInstalled(ThemeInformation.this, am_list[i])) {
                            values.add(am_list[i]);
                        }
                    } else {
                        values.add(am_list[i]);
                    }
                }
            } catch (Exception e) {
            }
            checkEnabledOverlays();
            return null;
        }
    }

    private class Phase1_AAPT_Check extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 1", "This phase has started it's asynchronous task.");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Check whether device has AAPT installed
            LayersBuilder aaptCheck = new LayersBuilder();
            aaptCheck.injectAAPT(getApplicationContext());
            return null;
        }
    }

    private class Phase2_InitializeCache extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 2", "This phase has started it's asynchronous task.");
            mWakeLock = null;
            if (!current_mode.equals("enable") && !current_mode.equals("disable")) {
                PowerManager pm = (PowerManager)
                        getApplicationContext().getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
                mWakeLock.acquire();
                mProgressDialog.show();
                mProgressDialog.setContentView(R.layout.theme_information_dialog_loader);
                loader_string = (TextView) mProgressDialog.findViewById(R.id
                        .loadingTextCreativeMode);
                loader_string.setText(getApplicationContext().getResources().getString(
                        R.string.lb_phase_1_loader));
                loader = (CircularFillableLoaders) mProgressDialog.findViewById(
                        R.id.circularFillableLoader);
                loader.setProgress(60);
            }
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Phase3_mainFunction phase3_mainFunction = new Phase3_mainFunction();
            phase3_mainFunction.execute("");
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Initialize the cache for this specific app
            if (!current_mode.equals("enable") && !current_mode.equals("disable")) {
                if (!has_extracted_cache) {
                    lb = new LayersBuilder();
                    lb.initializeCache(getApplicationContext(), theme_pid);
                    has_extracted_cache = true;
                }
            }
            return null;
        }
    }

    private class Phase3_mainFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 3", "This phase has started it's asynchronous task.");
            erroredOverlays = new ArrayList<String>();
            if (!current_mode.equals("enable") && !current_mode.equals("disable")) {
                if (current_mode.equals("compile")) {
                    loader_string.setText(getApplicationContext().getResources().getString(
                            R.string.lb_phase_2_loader_no_enable));
                } else {
                    loader_string.setText(getApplicationContext().getResources().getString(
                            R.string.lb_phase_2_loader));
                }
                loader.setProgress(30);
            }
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Dismiss the dialog first to prevent windows from leaking
            if (!current_mode.equals("enable") && !current_mode.equals("disable")) {
                mProgressDialog.dismiss();
                mWakeLock.release();
            }

            // On non-compiling dialogs, we have the progress bar shown instead
            progressBar.setVisibility(View.GONE);

            if (current_mode.equals("compile_enable")) {
                if (!mixAndMatchMode) {
                    if (swap_mode_commands.length() > 0) {
                        swap_mode_commands = swap_mode_commands + " && " + final_commands + " && " +
                                "cp " +
                                "/data/system/overlays.xml " + Environment
                                .getExternalStorageDirectory().getAbsolutePath() + "/" +
                                ".substratum/overlays" +
                                ".xml";
                        eu.chainfire.libsuperuser.Shell.SU.run(swap_mode_commands);
                        if (swap_mode_commands.contains("systemui"))
                            eu.chainfire.libsuperuser.Shell.SU.run("pkill com.android" +
                                    ".systemui");
                        swap_mode_commands = null;
                    } else {
                        final_commands = final_commands + " && cp /data/system/overlays.xml " +
                                Environment
                                        .getExternalStorageDirectory().getAbsolutePath() + "/" +
                                ".substratum/overlays" +
                                ".xml";
                        eu.chainfire.libsuperuser.Shell.SU.run(final_commands);
                        if (final_commands.contains("systemui"))
                            eu.chainfire.libsuperuser.Shell.SU.run("pkill com.android" +
                                    ".systemui");
                        final_commands = null;
                    }
                } else {
                    if (final_commands.length() > 0) {
                        final_commands = final_commands + " && cp /data/system/overlays.xml " +
                                Environment
                                        .getExternalStorageDirectory().getAbsolutePath() + "/" +
                                ".substratum" +
                                "/overlays" +
                                ".xml";
                        eu.chainfire.libsuperuser.Shell.SU.run(final_commands);
                        if (final_commands.contains("systemui"))
                            eu.chainfire.libsuperuser.Shell.SU.run("pkill com.android" +
                                    ".systemui");
                        final_commands = null;
                    } else {
                        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string
                                        .toast_disabled3),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            } else {
                if (erroredOverlays.size() > 0) {
                    for (int i = 0; i < erroredOverlays.size(); i++) {
                        String toast_text = String.format(getApplicationContext().getResources()
                                .getString(
                                        R.string.failed_to_install_overlay_toast), erroredOverlays
                                .get(i));
                        Toast toast = Toast.makeText(getApplicationContext(), toast_text,
                                Toast.LENGTH_SHORT);
                        toast.show();
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    if (current_mode.equals("compile")) {
                        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string
                                        .toast_compiled),
                                Toast.LENGTH_SHORT);
                        toast.show();
                        adapter.notifyDataSetChanged();
                    } else {
                        if (!current_mode.equals("disable")) {
                            if (final_commands.length() > 0) {
                                Toast toast = Toast.makeText(getApplicationContext(), getString(R
                                                .string.toast_installed),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                                final_commands = final_commands + " && cp /data/system/overlays" +
                                        ".xml " +
                                        Environment
                                                .getExternalStorageDirectory().getAbsolutePath()
                                        + "/" +
                                        ".substratum" +
                                        "/overlays" +
                                        ".xml";
                                eu.chainfire.libsuperuser.Shell.SU.run(final_commands);
                                adapter.notifyDataSetChanged();
                            } else {
                                Toast toast = Toast.makeText(getApplicationContext(), getString(R
                                                .string.toast_disabled3),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        } else {
                            if (final_commands.length() > 0) {
                                Toast toast = Toast.makeText(getApplicationContext(), getString(R
                                                .string.toast_disabled2),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                                final_commands = final_commands + " && cp /data/system/overlays" +
                                        ".xml " +
                                        Environment
                                                .getExternalStorageDirectory().getAbsolutePath()
                                        + "/" +
                                        ".substratum" +
                                        "/overlays" +
                                        ".xml";
                                eu.chainfire.libsuperuser.Shell.SU.run(final_commands);
                                adapter.notifyDataSetChanged();
                            } else {
                                Toast toast = Toast.makeText(getApplicationContext(), getString(R
                                                .string.toast_disabled4),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Filter out state 4 overlays before programming them to enable
            ArrayList<String> approved_overlays = new ArrayList<String>();

            if (!current_mode.equals("enable") && !current_mode.equals("disable")) {
                for (int i = 0; i < listStrings.size(); i++) {
                    lb = new LayersBuilder();
                    lb.beginAction(getApplicationContext(), listStrings.get(i), theme_name);
                    if (lb.has_errored_out) {
                        erroredOverlays.add(listStrings.get(i));
                    } else {
                        approved_overlays.add(listStrings.get(i) + "." + lb.parse2_themeName);
                    }
                }
            }
            // Process the current overlays whether they are enabled or disabled
            eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays.xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            String[] commands = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "4"};
            List<String> approved_disabled_overlays = ReadXMLFile.main(commands);
            if (!current_mode.equals("enable") && !current_mode.equals("disable")) {
                final_commands = "";
                swap_mode_commands = "";
                for (int i = 0; i < approved_overlays.size(); i++) {
                    if (i == 0 && approved_disabled_overlays.contains(approved_overlays.get(i))) {
                        final_commands = final_commands + "om enable " + approved_overlays.get(i);
                    } else {
                        if (i != 0 && final_commands.length() == 0 && approved_disabled_overlays
                                .contains(approved_overlays.get(i))) {
                            final_commands = final_commands + "om enable " + approved_overlays
                                    .get(i);
                        } else {
                            if (approved_disabled_overlays.contains(approved_overlays.get(i))) {
                                final_commands = final_commands + " && om enable " +
                                        approved_overlays.get(i);
                            }
                        }
                    }
                }
            } else {
                // Quickly parse theme_name
                String parse1_themeName = theme_name.replaceAll("\\s+", "");
                String parse2_themeName = parse1_themeName.replaceAll
                        ("[^a-zA-Z0-9]+", "");

                // Process the current overlays whether they are enabled or disabled
                String[] disabled = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml", "4"};
                String[] enabled = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml", "5"};
                List<String> enabled_disabled_overlays = ReadXMLFile.main(disabled);
                enabled_disabled_overlays.addAll(ReadXMLFile.main(enabled));

                final_commands = "";
                swap_mode_commands = "";

                if (current_mode.equals("enable")) {
                    for (int i = 0; i < listStrings.size(); i++) {
                        if (i == 0 && enabled_disabled_overlays.contains(listStrings.get(i) + "."
                                + parse2_themeName)) {
                            final_commands = final_commands + "om enable " +
                                    listStrings.get(i) + "." + parse2_themeName;
                        } else {
                            if (i != 0 && final_commands.length() == 0 &&
                                    enabled_disabled_overlays.contains(listStrings.get(i) + "." +
                                            parse2_themeName)) {
                                final_commands = final_commands + "om enable " +
                                        listStrings.get(i) + "." + parse2_themeName;
                            } else {
                                if (enabled_disabled_overlays.contains(listStrings.get(i) + "." +
                                        parse2_themeName)) {
                                    final_commands = final_commands + " && om enable " +
                                            listStrings.get(i) + "." + parse2_themeName;
                                }
                            }
                        }
                    }
                }
                if (current_mode.equals("disable")) {
                    for (int i = 0; i < listStrings.size(); i++) {
                        if (i == 0 && enabled_disabled_overlays.contains(listStrings.get(i) + "."
                                + parse2_themeName)) {
                            final_commands = final_commands + "om disable " +
                                    listStrings.get(i) + "." + parse2_themeName;
                        } else {
                            if (i != 0 && final_commands.length() == 0 &&
                                    enabled_disabled_overlays.contains(listStrings.get(i) + "." +
                                            parse2_themeName)) {
                                final_commands = final_commands + "om disable " +
                                        listStrings.get(i) + "." + parse2_themeName;
                            } else {
                                if (enabled_disabled_overlays.contains(listStrings.get(i) + "." +
                                        parse2_themeName)) {
                                    final_commands = final_commands + " && om disable " +
                                            listStrings.get(i) + "." + parse2_themeName;
                                }
                            }
                        }
                    }
                }
            }
            if (final_commands.contains("systemui")) {
                final_commands = final_commands + " && pkill com.android.systemui";
            }
            if (final_commands.length() > 0) {
                if (!current_mode.equals("compile")) {
                    if (!mixAndMatchMode) {
                        // Begin uninstalling all overlays based on this package
                        try {
                            String line;
                            ArrayList<String> all_overlays = new ArrayList<String>();
                            Process nativeApp = Runtime.getRuntime().exec(
                                    "om list");

                            OutputStream stdin = nativeApp.getOutputStream();
                            InputStream stderr = nativeApp.getErrorStream();
                            InputStream stdout = nativeApp.getInputStream();
                            stdin.write(("ls\n").getBytes());
                            stdin.write("exit\n".getBytes());
                            stdin.flush();
                            stdin.close();

                            BufferedReader br = new BufferedReader(new InputStreamReader
                                    (stdout));
                            while ((line = br.readLine()) != null) {
                                if (line.contains("    [x] ")) {
                                    if (line.substring(8).contains("systemui"))
                                        Log.d("OverlayDisabler", "Disabling overlay \"" +
                                                line.substring(8) + "\"");
                                    all_overlays.add(line.substring(8));
                                }
                            }
                            for (int i = 0; i < all_overlays.size(); i++) {
                                if (i == 0) {
                                    swap_mode_commands = swap_mode_commands + "om disable " +
                                            all_overlays
                                                    .get(i);
                                } else {
                                    swap_mode_commands = swap_mode_commands + " && om disable " +
                                            all_overlays.get(i);
                                }
                            }
                            br.close();
                            br = new BufferedReader(new InputStreamReader(stderr));
                            while ((line = br.readLine()) != null) {
                                Log.e("LayersBuilder", line);
                            }
                            br.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
            return null;
        }
    }
}
