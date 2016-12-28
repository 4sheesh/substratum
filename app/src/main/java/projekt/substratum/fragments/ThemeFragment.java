package projekt.substratum.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.R;
import projekt.substratum.adapters.ThemeEntryAdapter;
import projekt.substratum.config.References;
import projekt.substratum.model.ThemeInfo;
import projekt.substratum.util.AOPTCheck;
import projekt.substratum.util.ReadOverlays;

public class ThemeFragment extends Fragment {

    private static final int THEME_INFORMATION_REQUEST_CODE = 1;
    private HashMap<String, String[]> substratum_packages;
    private RecyclerView recyclerView;
    private Map<String, String[]> map;
    private Context mContext;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<ApplicationInfo> list;
    private ThemeEntryAdapter adapter;
    private View cardView;
    private ViewGroup root;
    private String home_type = "";
    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (prefs.getBoolean("nougat_style_cards", false)) {
            root = (ViewGroup) inflater.inflate(R.layout.home_fragment_n, container, false);
        } else {
            root = (ViewGroup) inflater.inflate(R.layout.home_fragment, container, false);
        }

        mContext = getActivity();

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            home_type = bundle.getString("home_type");
        }

        substratum_packages = new HashMap<>();
        recyclerView = (RecyclerView) root.findViewById(R.id.theme_list);
        cardView = root.findViewById(R.id.no_entry_card_view);
        cardView.setOnClickListener(v -> {
                    try {
                        String playURL = getString(R.string
                                .search_play_store_url);
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(playURL));
                        startActivity(i);
                    } catch (ActivityNotFoundException
                            activityNotFoundException) {
                        // Suppress warning
                    }
                }
        );
        cardView.setVisibility(View.GONE);

        // Create it so it uses a recyclerView to parse substratum-based themes
        PackageManager packageManager = mContext.getPackageManager();
        list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> refreshLayout());

        if (home_type.equals("")) {
            // We use this because our splash screen is meant to hang (to load) than show a wheel
            refreshLayout();
        } else {
            // This allows it to not hang the nav drawer activity
            LayoutLoader layoutLoader = new LayoutLoader();
            layoutLoader.execute("");
        }

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<>(substratum_packages);

        ArrayList<ThemeInfo> themeInfos = prepareData();
        adapter = new ThemeEntryAdapter(themeInfos);

        // Assign adapter to RecyclerView
        recyclerView.setAdapter(adapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        return root;
    }

    private ArrayList<ThemeInfo> prepareData() {

        ArrayList<ThemeInfo> themes = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            ThemeInfo themeInfo = new ThemeInfo();
            themeInfo.setThemeName(map.keySet().toArray()[i].toString());
            themeInfo.setThemeAuthor(map.get(map.keySet().toArray()[i].toString())[0]);
            themeInfo.setThemePackage(map.get(map.keySet().toArray()[i].toString())[1]);
            themeInfo.setThemeDrawable(
                    References.grabPackageHeroImage(mContext, map.get(map.keySet().toArray()[i]
                            .toString())[1]));
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (prefs.getBoolean("show_theme_ready_indicators", true)) {
                themeInfo.setThemeReadyVariable(References.grabThemeReadyVisibility(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
            }
            if (prefs.getBoolean("show_template_version", false)) {
                themeInfo.setPluginVersion(References.grabPackageTemplateVersion(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
                themeInfo.setSDKLevels(References.grabThemeAPIs(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
                themeInfo.setThemeVersion(References.grabThemeVersion(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
            } else {
                themeInfo.setPluginVersion(null);
                themeInfo.setSDKLevels(null);
                themeInfo.setThemeVersion(null);
            }
            if (home_type.length() == 0) {
                themeInfo.setThemeMode(null);
            } else {
                themeInfo.setThemeMode(home_type);
            }
            themeInfo.setContext(mContext);
            themeInfo.setActivity(getActivity());
            themes.add(themeInfo);
        }
        return themes;
    }

    private void refreshLayout() {
        if (home_type.length() == 0) {
            try {
                new AOPTCheck().injectAOPT(getContext(), false);
            } catch (Exception e) {
                Log.e(References.SUBSTRATUM_LOG,
                        "AOPT could not be checked or injected at this time.");
            }
        }

        MaterialProgressBar materialProgressBar = (MaterialProgressBar) root.findViewById(R.id
                .progress_bar_loader);
        materialProgressBar.setVisibility(View.VISIBLE);
        PackageManager packageManager = mContext.getPackageManager();
        list.clear();
        recyclerView.setAdapter(null);
        substratum_packages = new HashMap<>();
        list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);

        if (prefs.getBoolean("display_old_themes", true)) {
            list.stream().filter(packageInfo ->
                    (packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0).forEach(packageInfo ->
                    References.getSubstratumPackages(
                            mContext,
                            packageInfo.packageName,
                            substratum_packages,
                            home_type,
                            true));
            Log.d(References.SUBSTRATUM_LOG, "Substratum has loaded themes using the pre-499 " +
                    "theme database filter");
        } else {
            References.getSubstratumPackages(
                    mContext,
                    null,
                    substratum_packages,
                    home_type,
                    false);
            Log.d(References.SUBSTRATUM_LOG, "Substratum has loaded themes using the post-499 " +
                    "theme database filter");
        }

        doCleanUp cleanUp = new doCleanUp();
        cleanUp.execute("");

        if (substratum_packages.size() == 0) {
            cardView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            cardView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<>(substratum_packages);
        ArrayList<ThemeInfo> themeInfos = prepareData();
        adapter = new ThemeEntryAdapter(themeInfos);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
        materialProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (prefs.getInt(
                "uninstalled", THEME_INFORMATION_REQUEST_CODE) == THEME_INFORMATION_REQUEST_CODE) {
            prefs.edit().putInt("uninstalled", 0).apply();
            refreshLayout();
        }
        super.onResume();
    }

    private class LayoutLoader extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            refreshLayout();
            if (substratum_packages.size() == 0) {
                cardView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                cardView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                if (prefs.getBoolean("display_old_themes", true)) {
                    list.stream().filter(packageInfo ->
                            (packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0).forEach(
                            packageInfo ->
                                    References.getSubstratumPackages(
                                            mContext,
                                            packageInfo.packageName,
                                            substratum_packages,
                                            home_type,
                                            true));
                    Log.d(References.SUBSTRATUM_LOG, "Substratum has loaded themes using the" +
                            "pre-499 theme database filter");
                } else {
                    References.getSubstratumPackages(
                            mContext,
                            null,
                            substratum_packages,
                            home_type,
                            false);
                    Log.d(References.SUBSTRATUM_LOG, "Substratum has loaded themes using the " +
                            "post-499 theme database filter");
                }
            } catch (Exception e) {
                // Exception
            }
            return null;
        }
    }

    private class doCleanUp extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            List<String> state1 = ReadOverlays.main(1, mContext);
            // Overlays with non-existent targets
            for (int i = 0; i < state1.size(); i++) {
                Log.e("OverlayCleaner", "Target APK not found for \"" + state1.get(i) + "\" and " +
                        "will be removed.");
                References.uninstallOverlay(state1.get(i));
            }
            return null;
        }
    }
}