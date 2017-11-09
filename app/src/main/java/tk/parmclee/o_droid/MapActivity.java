package tk.parmclee.o_droid;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


abstract public class MapActivity extends AppCompatActivity {

    MapView mImage;
    RelativeLayout mLayout;
    View mSatImage;
    ImageView mSatFixedImage;
    String mapPath;
    LocationManager locationManager;
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;
    Uri storageUri;
    private long mTrackStartTimeStamp;
    protected GeomagneticField geomagneticField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTrackStartTimeStamp = System.currentTimeMillis();
        deleteOldPoints();
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            RuntimePermissionHelper.checkAndRequestStoragePermission(this, null);
        storageUri = Util.getMapStorageUri(getApplicationContext());

        mapPath = getIntent().getStringExtra("mapPath");
        mImage = new MapView(getApplicationContext(), mapPath);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (RuntimePermissionHelper.checkAndRequestLocationPermission(this, null)) start();
        } else start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case RuntimePermissionHelper.REQUEST_CODE_LOCATION:
                    start();
                    break;
                default:
            }
        } else {
            Toast.makeText(this, "Permissions NOT GRANTED", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void start() {
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
        } catch (IllegalArgumentException iae) {
            Toast.makeText(this, "Please turn on gps", Toast.LENGTH_SHORT).show();
        }

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Don't sleep");
        wakeLock.acquire();
    }

    public void quit(View v) {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("track_dialog", false) &&
                !(this instanceof AffixmentActivity)) {
            makeTrackDialog();
        } else {
            removeGpsAndFinish();
        }
    }

    private void makeTrackDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.save_track)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveTrack();
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeGpsAndFinish();
            }
        }).show();
    }

    private void saveTrack() {
        File directory = new File(mapPath).getParentFile();
        String name = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(new Date(System.currentTimeMillis()));
        File gpx = new File(directory, "0-DroidTrack_".concat(name).concat(".gpx"));
        Gpx.writePath(gpx, name, getPoints());
        makeGoSeeTrackDialog(gpx);
    }

    private List<TrackPoint> getPoints() {
        return DbHelper.getInstance(this).getPoints(mTrackStartTimeStamp);
        /*realm.where(TrackPoint.class)
                .between("mTime", mTrackStartTimeStamp, System.currentTimeMillis())
                .findAll();*/
    }

    private void deleteOldPoints() {
        DbHelper.getInstance(this).deleteOldPoints();
       /* realm.beginTransaction();
        realm.where(TrackPoint.class)
                .lessThan("mTime", mTrackStartTimeStamp - 48 * 3600 * 1000)
                .findAll()
                .deleteAllFromRealm();
        realm.commitTransaction();*/
    }

    private void makeGoSeeTrackDialog(final File file) {
        String message = getString(R.string.gpx_saved).concat(" ").concat(file.getAbsolutePath());
        new AlertDialog.Builder(this)
                .setTitle(R.string.track_saved)
                .setMessage(message)
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeGpsAndFinish();
            }
        })
                .setCancelable(false)
                .show();
    }

    private void showFileInExplorer(File file) { // not working
        Uri selectedUri = Uri.fromFile(file);
        Intent intent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(selectedUri, "*/*");
        if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
            startActivity(Intent.createChooser(intent, "Open folder"));
        } else {
            String message = getString(R.string.gpx_saved).concat(" ").concat(file.getAbsolutePath());
            new AlertDialog.Builder(this)
                    .setTitle(R.string.cannot_open_explorer)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            removeGpsAndFinish();
                        }
                    })
                    .show();
        }
    }

    void removeGPS() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            locationManager.removeUpdates(locationListener);
            locationManager.removeGpsStatusListener(gpsStatusListener);
        } catch (SecurityException se) {
            se.printStackTrace();
            Toast.makeText(this, "security exception", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeGpsAndFinish(){
        removeGPS();
        wakeLock.release();
        finish();
    }

    GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            int satellites = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                RuntimePermissionHelper.checkAndRequestLocationPermission(MapActivity.this, null);
            }
            for (GpsSatellite sat : locationManager.getGpsStatus(null).getSatellites()) {
                if (sat.usedInFix()) satellites++;
            }
            if (mSatImage != null) {
                mSatImage.setVisibility(View.VISIBLE);
                switch (satellites) {
                    case 0:
                       /* mSatImage.setImageResource(R.drawable.ic_power_sat_0);
                        break;*/
                    case 1:
                        /*mSatImage.setImageResource(R.drawable.ic_power_sat_1);
                        break;*/
                    case 2:
                        /*mSatImage.setImageResource(R.drawable.ic_power_sat_2);
                        break;*/
                    case 3:
                        //mSatImage.setImageResource(R.drawable.ic_power_sat_3);
                        mSatFixedImage.startAnimation(AnimationUtils.loadAnimation(MapActivity.this, R.anim.tween));
                        break;
                    default:
                        mSatImage.setVisibility(View.INVISIBLE);
                        break;
                }
            }
            switch (event) {
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    mImage.gpsFixed = true;
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    mImage.gpsFixed = false;
                    break;
            }
        }
    };

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            locationChanged(location);
            geomagneticField = new GeomagneticField(
                    (float) location.getLatitude(),
                    (float) location.getLongitude(),
                    (float) location.getAltitude(),
                    System.currentTimeMillis());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    abstract void locationChanged(Location location);

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
            mImage.checkAndMove(dx, dy);
            mImage.invalidate();
            return true;
        }
    }
}
