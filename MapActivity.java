package tk.parmclee.o_droid;

import android.content.Intent;
import android.graphics.Point;
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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MapActivity extends AppCompatActivity {

    TextView mLine;
    LocationManager locationManager;
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;
    Uri storageUri;
    File map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLine = (TextView) findViewById(R.id.line);
        assert mLine != null;
        mLine.setOnClickListener(listener);
        storageUri = Util.getMapStorageUri(getApplicationContext());
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
            locationManager.addGpsStatusListener(new GpsStatus.Listener() {
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
            });
            locationManager.requestLocationUpdates(provider, 1, 1, new LocationListener() {
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
            });
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            finish();
        } catch (IllegalArgumentException iae){
            Log.d("Odr", iae.getMessage());
            Toast.makeText(this, "Please turn on gps", Toast.LENGTH_SHORT).show();
        }
        map = new File(storageUri.getPath(), "map_womenf_middle.jpg");
        Bundle getB = testMap(map);

        mLine.setText(storageUri.getPath()+getB.toString());

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Don't sleep");
        wakeLock.acquire();
    }

    @Override
    protected void onStop() {
        super.onStop();
        wakeLock.release();
    }

    Bundle testMap(File map){
        Location l1 = new Location("gps"); l1.setLatitude(63.379120); l1.setLongitude(10.316691);
        Location l2 = new Location("gps"); l2.setLatitude(63.361495); l2.setLongitude(10.297033);
        Point p1 = new Point(839, 25);
        Point p2 = new Point(95, 1590);
        Bundle b = Util.mapAffixment(l1,p1,l2,p2,map);
        double scale1 = b.getDouble("scaleLat");
        double scale2 = b.getDouble("scaleLon");
        double lat = b.getDouble("latitude");
        double lon = b.getDouble("longitude");
        Location l = new Location("gps");
        l.setLatitude(63.366497);
        l.setLongitude(10.319914);
        Point p = Util.getPositionOnMap(lat,lon,scale1,scale2,l);

        File f = Util.createAffixmentFile(map, b);
        return Util.readAffixmentFile(f);
    }

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), AffixmentActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra("mapPath", map.getAbsolutePath());
            startActivity(intent);
        }
    };
}
