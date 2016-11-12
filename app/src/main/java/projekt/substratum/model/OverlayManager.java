package projekt.substratum.model;

import android.content.Context;

import java.io.Serializable;

import projekt.substratum.R;

public class OverlayManager implements Serializable {

    private String name;

    private boolean isSelected;

    private int activationValue;

    private Context mContext;

    public OverlayManager(Context context, String name, boolean isActivated) {
        this.mContext = context;
        this.name = name;
        this.isSelected = false;
        try {
            this.activationValue =
                    ((isActivated) ? context.getColor(R.color.overlay_installed_list_entry) :
                            context.getColor(R.color.overlay_not_enabled_list_entry));
        } catch (Exception e) {
            // Suppress warning
        }
    }

    public int getActivationValue() {
        return activationValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public Context getContext() {
        return mContext;
    }
}