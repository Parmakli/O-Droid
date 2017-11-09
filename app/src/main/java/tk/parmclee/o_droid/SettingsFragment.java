package tk.parmclee.o_droid;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.preferences, null);
        boolean hasCompass = getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
        if (!hasCompass) getPreferenceScreen().findPreference("compass").setEnabled(false);
    }
}
