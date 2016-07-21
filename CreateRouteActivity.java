package tk.parmclee.o_droid;

import android.content.Intent;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;

public class CreateRouteActivity extends MapActivity {
    ArrayList<PointF> mRouteList;

    @Override
    void locationChanged(Location location) {
        // do nothing
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_route);

        mLayout = (RelativeLayout) findViewById(R.id.layoutRoute);
        mImage.addListeners(
                new GestureDetector(getApplicationContext(), new ScrollAndLongPressListener()),
                new ScaleGestureDetector(getApplicationContext(), new ScaleListener()));
        assert mLayout != null;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mLayout.addView(mImage, params);

        ImageButton quitBtn = (ImageButton) findViewById(R.id.quitBtn);
        assert quitBtn != null;
        quitBtn.bringToFront();
        mImage.hidePos = true;
        mImage.routeMode = true;
        mRouteList = new ArrayList<>();
        Button startBtn = (Button) findViewById(R.id.startBtn);
        startBtn.bringToFront();
    }

    class ScrollAndLongPressListener extends ScrollListener {
        @Override
        public void onLongPress(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            PointF pointOnMap = mImage.getPointOnMap(new PointF(x, y));
            mRouteList.add(pointOnMap);
            //mImage.createRoute(mRouteList);
            mImage.drawRoute(mRouteList);
            mImage.invalidate();
        }
    }

    public void chooseLevel(View v) {
        if (mRouteList.size() > 2) {
            Intent intent = new Intent(getApplicationContext(), LevelActivity.class);
            intent.putExtras(getIntent().getExtras());
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putParcelableArrayListExtra("route", mRouteList);
            startActivity(intent);
        } else Toast.makeText(this, getString(R.string.no_route), Toast.LENGTH_SHORT).show();
    }
}
