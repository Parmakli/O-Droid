package tk.parmclee.o_droid;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.io.File;

public class ExploreActivity extends MapActivity {
    Bundle mapData;
    double mpp;
    View trackBtn;
    View trackLayout;
    SharedPreferences mPreferences;
    Point mPositionOnMap;
    Location mPreviousLocation;
    private View mArrowView;
    private SensorEventListener mSensorEventListener;
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explore);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int scale = Integer.parseInt(mPreferences.getString("scale", "100"));
        boolean scroll = mPreferences.getBoolean("scroll", false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            RuntimePermissionHelper.checkAndRequestStoragePermission(this, null);
        File affixFile = new File((Util.affixFileName(mapPath)));
        mapData = Util.readAffixmentFile(affixFile, this);
        mpp = mapData.getDouble("meters per pixel");
        setScale(mPreferences, scale);

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

        View quitBtn = findViewById(R.id.quit);
        if (quitBtn != null) {
            quitBtn.bringToFront();
        }
        mSatImage = findViewById(R.id.sat);
        mSatFixedImage = (ImageView) findViewById(R.id.fixedSat);
        if (mSatImage != null) {
            mSatImage.bringToFront();
        }

        trackBtn = findViewById(R.id.track);
        trackLayout = findViewById(R.id.track_layout);
        if (trackLayout != null) {
            trackLayout.bringToFront();
            trackLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    trackBtn.setActivated(!trackBtn.isActivated());
                    mImage.showTrack = trackBtn.isActivated();
                    mImage.calculatePositionOnScreen();
                    mImage.invalidate();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        quit(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean compass = mPreferences.getBoolean("compass", false);

        View kolba = findViewById(R.id.kolba);
        mArrowView = findViewById(R.id.strelka);
        if (!compass) {
            kolba.setVisibility(View.GONE);
            mArrowView.setVisibility(View.GONE);
        } else {
            kolba.bringToFront();
            mArrowView.bringToFront();
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            // Gravity for accelerometer data
            final float[] gravity = new float[3];
            // magnetic data
            final float[] geomagnetic = new float[3];
            // Rotation data
            final float[] rotation = new float[9];
            // orientation (azimuth, pitch, roll)
            final float[] orientation = new float[3];
            // smoothed values
            final float[][] smoothed = {new float[3]};
            final double[] bearing = {0};
            mSensorEventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {

                    // get accelerometer data
                    // we need to use a low pass filter to make data smoothed
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        smoothed[0] = LowPassFilter.filter(event.values, gravity);
                        gravity[0] = smoothed[0][0];
                        gravity[1] = smoothed[0][1];
                        gravity[2] = smoothed[0][2];
                    } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                        smoothed[0] = LowPassFilter.filter(event.values, geomagnetic);
                        geomagnetic[0] = smoothed[0][0];
                        geomagnetic[1] = smoothed[0][1];
                        geomagnetic[2] = smoothed[0][2];
                    }

                    // get rotation matrix to get gravity and magnetic data
                    SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic);
                    // get bearing to target
                    SensorManager.getOrientation(rotation, orientation);
                    // east degrees of true North
                    bearing[0] = orientation[0];
                    // convert from radians to degrees
                    bearing[0] = Math.toDegrees(bearing[0]);

                    // fix difference between true North and magnetical North
                    if (geomagneticField != null) {
                        bearing[0] += geomagneticField.getDeclination();
                    }

                    // bearing must be in 0-360
                    if (bearing[0] < 0) {
                        bearing[0] += 360;
                    }
                    setBearing((float) bearing[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            };
            mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mSensorManager != null && mSensorEventListener != null)
        mSensorManager.unregisterListener(mSensorEventListener);
    }

    private void setBearing(float bearing){
        mArrowView.setRotation(-bearing);
    }

    @Override
    void locationChanged(Location location) {
        if (mPreviousLocation != null && SphericalUtil.computeDistanceBetween(
                new LatLng(location.getLatitude(), location.getLongitude()),
                new LatLng(mPreviousLocation.getLatitude(), mPreviousLocation.getLongitude())) >= 3) {
            /*realm.beginTransaction();
            realm.copyToRealmOrUpdate(new TrackPoint(location.getTime(), location.getLatitude(), location.getLongitude()));
            realm.commitTransaction();*/
            DbHelper.getInstance(this).addLocation(new TrackPoint(location.getTime(), location.getLatitude(), location.getLongitude()));
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
        }
        mPreviousLocation = location;
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
    }


}
