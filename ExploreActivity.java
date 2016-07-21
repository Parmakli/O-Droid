package tk.parmclee.o_droid;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import java.io.File;

public class ExploreActivity extends MapActivity {
    Bundle mapData;
    double mpp;
    ToggleButton trackBtn;
    SharedPreferences preferences;
    Point mPositionOnMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explore);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int scale = Integer.parseInt(preferences.getString("scale", "100"));
        boolean scroll = preferences.getBoolean("scroll", false);

        File affixFile = new File((Util.affixFileName(mapPath)));
        mapData = Util.readAffixmentFile(affixFile, this);
        mpp = mapData.getDouble("meters per pixel");
        setScale(preferences, scale);

        mLayout = (RelativeLayout) findViewById(R.id.layoutAffixment);
        GestureDetector scrollDetector = null;
        ScaleGestureDetector scaleDetector = null;
        if (scroll)
            scrollDetector = new GestureDetector(getApplicationContext(), new ScrollListener());
        if (scale == 0)
            scaleDetector = new ScaleGestureDetector(getApplicationContext(), new ScaleListener());
        mImage.addListeners(scrollDetector, scaleDetector);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mLayout.addView(mImage, params);

        ImageButton quitBtn = (ImageButton) findViewById(R.id.quit);
        quitBtn.bringToFront();
        mSatImage = (ImageView) findViewById(R.id.sat);
        mSatImage.bringToFront();

        trackBtn = (ToggleButton) findViewById(R.id.track);
        trackBtn.setTextColor(Color.BLACK);
        trackBtn.bringToFront();
        trackBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mImage.showTrack = isChecked;
                mImage.calculatePositionOnScreen();
                mImage.invalidate();
            }
        });
    }

    @Override
    void locationChanged(Location location) {
        double startLatitude = mapData.getDouble("latitude");
        double startLongitude = mapData.getDouble("longitude");
        mPositionOnMap = Util.getPositionOnMap(startLatitude, startLongitude, mpp, location);
        int width = mapData.getInt("width");
        int height = mapData.getInt("height");
        if (mPositionOnMap.x < 0) mPositionOnMap.x = 0;
        if (mPositionOnMap.x > width) mPositionOnMap.x = width;
        if (mPositionOnMap.y < 0) mPositionOnMap.y = 0;
        if (mPositionOnMap.y > height) mPositionOnMap.y = height;
        mImage.setCurrentPosition(mPositionOnMap);
        Log.d("Odr", "locationChanged: " + mPositionOnMap.x + "," + mPositionOnMap.y);
    }

    void setScale(SharedPreferences preferences, int scale) {
        if (scale == 0) return;
        float screenWidthCM = (float) 0.1 * Float.parseFloat(preferences.getString("screen_width", "0"));
        float screenHeightCM = (float) 0.1 * Float.parseFloat(preferences.getString("screen_height", "0"));
        Point screenSize = Util.getSizeInPxl(this);
        float screenCMperPxl = Math.max(screenHeightCM, screenWidthCM)
                / Math.max(screenSize.x, screenSize.y);

        float scaleFactor = (float) (mpp / (scale * screenCMperPxl));
        mImage.setScaleFactor(scaleFactor);
        Log.d("Odr", "onCreate mpp " + mpp + " screenCM " + screenCMperPxl);
    }


}
