package tk.parmclee.o_droid;

import android.graphics.Point;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MapActivity extends AppCompatActivity {

    TextView mLine;
    LocationManager locationManager;
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLine = (TextView) findViewById(R.id.line);
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

        Location l1 = new Location("gps");
        l1.setLatitude(44.932191);
        l1.setLongitude(34.036290);
        Location l2 = new Location("gps");
        l2.setLatitude(44.917771);
        l2.setLongitude(34.045132);
        Point p1 = new Point(1002, 652);
        Point p2 = new Point(2458, 3588);
        Bundle b = Util.mapAffixment(l1,p1,l2,p2,new File("dd"));
        double scale = b.getDouble("scale");
        double scale1 = b.getDouble("scaleLatD");
        double scale2 = b.getDouble("scaleLonD");
        double lat = b.getDouble("latitude");
        double lon = b.getDouble("longitude");
        Location l = new Location("gps");
        l.setLatitude(44.926512);
        l.setLongitude(34.041511);
        Point p = Util.getPositionOnMap(lat,lon,scale1,scale2,l);
        mLine.setText(provider + scale + "\nlat:"+lat+"\nlon:"+lon+
                "\nscale1:"+scale1+"\nscale2:"+scale2+"\nPoint:"+p.x+","+p.y);

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Don't sleep");
        wakeLock.acquire();
    }

    @Override
    protected void onStop() {
        super.onStop();
        wakeLock.release();
    }
}
