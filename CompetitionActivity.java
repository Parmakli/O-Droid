package tk.parmclee.o_droid;

import android.graphics.Point;
import android.graphics.PointF;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;

public class CompetitionActivity extends ExploreActivity {
    ArrayList<PointF> mRoute;
    String level;
    float precision;
    Ringtone ringtone;
    int currentCP;
    boolean finished;

    @Override
    void locationChanged(Location location) {
        super.locationChanged(location);
        if (!finished && gotCP(mRoute.get(currentCP), mPositionOnMap)){
            ringtone.play();
            currentCP++;
            if (mRoute.size() == currentCP) finished = true;
        }
    }

    private boolean gotCP(PointF cp, Point position){
        return Math.hypot(cp.x - position.x, cp.y - position.y) < precision;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentCP = 0;
        precision = 20;
        try {
            precision = Float.parseFloat(preferences.getString("precision", "20"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        mRoute = getIntent().getParcelableArrayListExtra("route");
        mImage.routeMode = true;
        mImage.competition = true;
        mImage.mRoutePoints = mRoute;
        trackBtn.setVisibility(View.INVISIBLE);
        trackBtn.setEnabled(false);
        level = getIntent().getStringExtra("level");
        switch (level){
            case "simple":
                mImage.showTrack = true;
                break;
            case "hard":
                mImage.profiMode = true;
                // no "break" - go next
            case "medium":
                mImage.hidePos = true;
                break;
            default:
        }try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
