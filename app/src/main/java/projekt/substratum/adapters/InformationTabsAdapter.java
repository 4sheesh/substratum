package projekt.substratum.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.tabs.BootAnimation;
import projekt.substratum.tabs.FontInstaller;
import projekt.substratum.tabs.OverlaysList;
import projekt.substratum.tabs.SoundPackager;
import projekt.substratum.tabs.Wallpapers;

public class InformationTabsAdapter extends FragmentStatePagerAdapter {

    private ArrayList package_checker;
    private Integer mNumOfTabs;
    private String theme_mode;
    private String wallpaperUrl;

    @SuppressWarnings("unchecked")
    public InformationTabsAdapter(FragmentManager fm, int NumOfTabs, String theme_mode,
                                  List package_checker, String wallpaperUrl) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
        this.theme_mode = theme_mode;
        try {
            this.package_checker = new ArrayList<>(package_checker);
        } catch (NullPointerException npe) {
            // Suppress this warning for theme_mode launches
        }
        this.wallpaperUrl = wallpaperUrl;
    }

    @Override
    public Fragment getItem(int position) {
        if (theme_mode != null && theme_mode.length() > 0) {
            switch (theme_mode) {
                case "overlays":
                    return new OverlaysList();
                case "bootanimation":
                    return new BootAnimation();
                case "fonts":
                    return new FontInstaller();
                case "audio":
                    return new SoundPackager();
                case "wallpapers":
                    return new Wallpapers();
            }
        }
        return getFragment();
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }

    private Fragment getFragment() {
        if (package_checker.contains("overlays")) {
            package_checker.remove("overlays");
            return new OverlaysList();
        } else if (package_checker.contains("bootanimation")) {
            package_checker.remove("bootanimation");
            return new BootAnimation();
        } else if (package_checker.contains("fonts")) {
            package_checker.remove("fonts");
            return new FontInstaller();
        } else if (package_checker.contains("audio")) {
            package_checker.remove("audio");
            return new SoundPackager();
        } else if (wallpaperUrl != null) {
            return new Wallpapers();
        }
        return null;
    }
}