package com.asamm.locus.addon.smartwatch2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class AboutScreen extends Activity {

    // tag for logger
    private static final String TAG = "AboutScreen";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set title
        setTitle(R.string.about_application);

        // set content
        setContentView(R.layout.about_application_screen);

        // set subtitle
        TextView tvSubtitle = (TextView)
                findViewById(R.id.text_view_subtitle);
        tvSubtitle.setText(getString(R.string.version) + ": " + getVersionName());
    }

    /**
     * Get readable name of current version.
     * @return name of version
     */
    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e(TAG, "getVersionName()", e);
            return "";
        }
    }
}
