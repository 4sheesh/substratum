package projekt.substratum;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.mikhaellopez.circularfillableloaders.CircularFillableLoaders;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.util.LayersBuilder;


/**
 * @author Nicholas Chum (nicholaschum)
 */
public class ThemeInformation extends AppCompatActivity {

    public ListView listView;
    public String theme_name, theme_pid;
    public AssetManager am;
    public String[] values;
    public Boolean has_extracted_cache;
    public LayersBuilder lb;
    public CircularFillableLoaders loader;
    public TextView loader_string;
    public List<String> listStrings;
    ProgressDialog mProgressDialog;
    private PowerManager.WakeLock mWakeLock;

    public Drawable grabPackageHeroImage(String package_name) {
        Resources res;
        Drawable hero = null;
        try {
            //I want to use the clear_activities string in Package com.android.settings
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

        has_extracted_cache = false;

        // Handle collapsible toolbar with theme name

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");
        theme_pid = currentIntent.getStringExtra("theme_pid");

        ImageView imageView = (ImageView) findViewById(R.id.preview_image);
        imageView.setImageDrawable(grabPackageHeroImage(theme_pid));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(theme_name);

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById
                (R.id.collapsingToolbarLayout);
        collapsingToolbarLayout.setTitle(theme_name);

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

        // Parse the list of overlay folders inside assets/overlays
        try {
            Context otherContext = createPackageContext(theme_pid, 0);
            am = otherContext.getAssets();
            values = new String[am.list("overlays").length];
            for (int i = 0; i < am.list("overlays").length; i++) {
                values[i] = am.list("overlays")[i];
            }
        } catch (Exception e) {
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, values);

        if (listView != null) {
            listView.setNestedScrollingEnabled(true);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            listView.setAdapter(adapter);
        }

        // Handle the logic for selecting all overlays or not

        Switch toggle_overlays = (Switch) findViewById(R.id.toggle_all_overlays);
        if (toggle_overlays != null) {
            toggle_overlays.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            SparseBooleanArray checked = listView.getCheckedItemPositions();
                            for (int i = 0; i < listView.getAdapter().getCount(); i++) {
                                if (checked.get(i)) {
                                    if (!isChecked == listView.isItemChecked(i)) {
                                        listView.setItemChecked(i, false);
                                    } else {
                                        listView.setItemChecked(i, true);
                                    }
                                } else {
                                    if (!isChecked == listView.isItemChecked(i)) {
                                        listView.setItemChecked(i, true);
                                    } else {
                                        listView.setItemChecked(i, false);
                                    }
                                }
                            }
                        }
                    });
        }

        // Run through phase one - checking whether aapt exists on the device
        Phase1_AAPT_Check phase1_aapt_check = new Phase1_AAPT_Check();
        phase1_aapt_check.execute("");

        Button button = (Button) findViewById(R.id.btnSelection);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    SparseBooleanArray checked = listView.getCheckedItemPositions();
                    listStrings = new ArrayList<String>();
                    for (int i = 0; i < listView.getAdapter().getCount(); i++) {
                        if (checked.get(i)) {
                            listStrings.add(listView.getItemAtPosition(i).toString());
                        }
                    }

                    // Run through phase two - initialize the cache for the specific theme
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute("");
                }
            });
        }

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

        if (id == R.id.rate) {
            String playURL = "https://play.google.com/store/apps/details?id=" + theme_pid;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(playURL));
            startActivity(i);
            return true;
        }
        if (id == R.id.uninstall) {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + theme_pid));
            startActivity(intent);
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
            PowerManager pm = (PowerManager)
                    getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = null;
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.setContentView(R.layout.custom_dialog_loader);
            loader_string = (TextView) mProgressDialog.findViewById(R.id.loadingTextCreativeMode);
            loader_string.setText(getApplicationContext().getResources().getString(
                    R.string.lb_phase_1_loader));
            loader = (CircularFillableLoaders) mProgressDialog.findViewById(
                    R.id.circularFillableLoader);
            loader.setProgress(60);
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
            if (!has_extracted_cache) {
                lb = new LayersBuilder();
                lb.initializeCache(getApplicationContext(), theme_pid);
                has_extracted_cache = true;
            }
            return null;
        }
    }

    private class Phase3_mainFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            loader_string.setText(getApplicationContext().getResources().getString(
                    R.string.lb_phase_2_loader));
            loader.setProgress(40);
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            loader.setProgress(10);
            loader_string.setText(getApplicationContext().getResources().getString(
                    R.string.lb_phase_3_loader));
            mWakeLock.release();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Initialize the cache for this specific app
            for (int i = 0; i < listStrings.size(); i++) {
                lb = new LayersBuilder();
                lb.beginAction(getApplicationContext(), listStrings.get(i), theme_name);
            }
            return null;
        }
    }
}
