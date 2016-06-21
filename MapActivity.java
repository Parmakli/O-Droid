package tk.parmclee.o_droid;

import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MapActivity extends AppCompatActivity {

    MapView mImage;
    RelativeLayout mLayout;
    String mapPath;
    LocationManager locationManager;
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;
    Uri storageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        storageUri = Util.getMapStorageUri(getApplicationContext());
        mapPath = getIntent().getStringExtra("mapPath");
        mImage = new MapView(getApplicationContext(), mapPath);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(false);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(criteria, true);

        try {
            locationManager.addGpsStatusListener(gpsStatusListener);
            locationManager.requestLocationUpdates(provider, 1, 1, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            finish();
        } catch (IllegalArgumentException iae){
            Log.d("Odr", iae.getMessage());
            Toast.makeText(this, "Please turn on gps", Toast.LENGTH_SHORT).show();
        }

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Don't sleep");
        wakeLock.acquire();
    }

    @Override
    protected void onStop() {
        super.onStop();
        wakeLock.release();
    }

    public void quit(View v){
        if (locationManager != null)
        try {
            locationManager.removeUpdates(locationListener);
            locationManager.removeGpsStatusListener(gpsStatusListener);
        } catch (SecurityException se) {
            se.printStackTrace();
        }
        finish();
    }

    GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.d("Odr", "first fix");
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.d("Odr", "started");
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    Log.d("Odr", "sat status");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.d("Odr", "stopped");
                    break;
            }
        }
    };

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d("Odr", "location changed:"+location.getLatitude()+","+location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d("Odr", provider + " status changed:"+status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d("Odr", provider + " enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d("Odr", provider + " disabled");
        }
    };


    class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            if (mImage.mScaleFactor * factor > mImage.mMinFactor &&
                    mImage.mScaleFactor * factor < 10 * mImage.mMinFactor) {
                float x = detector.getFocusX();
                float y = detector.getFocusY();
                mImage.mScaleFactor *= factor;
                mImage.mMatrix.postScale(factor, factor);
                mImage.mMatrix.postTranslate(x * (1 - factor), y * (1 - factor));
                mImage.transformCoords(mImage.mMatrix);
                mImage.invalidate();
            }
            return true;
        }
    }

    class ScrollListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float dx = -distanceX;
            float dy = -distanceY;
            if ((mImage.mLeftTop.x - dx) < 0) dx = mImage.mLeftTop.x;
            if ((mImage.mRightBottom.x - dx) > mImage.mMapSize.x)
                dx = mImage.mRightBottom.x - mImage.mMapSize.x;
            if ((mImage.mLeftTop.y - dy) < 0) dy = mImage.mLeftTop.y;
            if ((mImage.mRightBottom.y - dy) > mImage.mMapSize.y)
                dy = mImage.mRightBottom.y - mImage.mMapSize.y;
            mImage.mMatrix.postTranslate(dx, dy);
            mImage.transformCoords(mImage.mMatrix);
            mImage.invalidate();
            return true;
        }
    }
}
