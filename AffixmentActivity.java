package tk.parmclee.o_droid;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;

public class AffixmentActivity extends AppCompatActivity {

    MapView mImage;
    RelativeLayout mLayout;
    String mapPath;
    File affixmentFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.affixment);
        mLayout = (RelativeLayout) findViewById(R.id.layoutAffixment);
        mapPath = getIntent().getStringExtra("mapPath");
        mImage = new MapView(getApplicationContext(), mapPath);
        mImage.addListeners(new GestureDetector(getApplicationContext(), new ScrollListener()),
                new ScaleGestureDetector(getApplicationContext(), new ScaleListener()));
        assert mLayout != null;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mLayout.addView(mImage, params);

        TextView text = (TextView) findViewById(R.id.text);
        assert text != null;
        text.setTextColor(Color.BLACK);
        text.bringToFront();

        ImageButton quitBtn = (ImageButton) findViewById(R.id.quitBtn);
        assert quitBtn != null;
        quitBtn.bringToFront();
        mImage.setTestView(text);

        Log.d("Odr", "onCreate");
        Log.d("Odr", mapPath);
    }

    public void quit(View v) {
        finish();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
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

    private class ScrollListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float dx = -distanceX;
            float dy = -distanceY;
            Log.d("Odr", "Initial dx " + dx + " dy " + dy +
                    "\nlt: " + mImage.mLeftTop.x + "," + mImage.mLeftTop.y +
                    "\nrb: " + mImage.mRightBottom.x + "," + mImage.mRightBottom.y);
            if ((mImage.mLeftTop.x - dx) < 0) dx = mImage.mLeftTop.x;
            if ((mImage.mRightBottom.x - dx) > mImage.mMapSize.x)
                dx = mImage.mRightBottom.x - mImage.mMapSize.x;
            if ((mImage.mLeftTop.y - dy) < 0) dy = mImage.mLeftTop.y;
            if ((mImage.mRightBottom.y - dy) > mImage.mMapSize.y)
                dy = mImage.mRightBottom.y - mImage.mMapSize.y;
            Log.d("Odr", "dx " + dx + " dy " + dy);
            mImage.mMatrix.postTranslate(dx, dy);
            mImage.transformCoords(mImage.mMatrix);
            mImage.invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            ImageView imageView = new ImageView(getApplicationContext());
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_add_location_black_48dp);
            imageView.setImageBitmap(bmp);
            float x = e.getX();
            float y = e.getY();
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int dp = Math.round(36 * metrics.density);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dp, dp);
            params.leftMargin = (int) (x - dp / 2);
            params.topMargin = (int) (y - dp);
            mLayout.addView(imageView, params);
            Log.d("Odr", "longPress");
            createDialog(imageView, x, y);
        }

        void createDialog(final ImageView imageView, final float x, final float y) {
            AlertDialog.Builder builder = new AlertDialog.Builder(AffixmentActivity.this,
                    R.style.DialogTheme);
            final EditText[] coords = new EditText[2];
            builder.setTitle(R.string.coords)
                    .setCancelable(true)
                    .setView(R.layout.dialog)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mLayout.removeView(imageView);
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            float latitude, longitude;
                            try {
                                latitude = Float.parseFloat(coords[0].getText().toString());
                                longitude = Float.parseFloat(coords[1].getText().toString());
                            } catch (NumberFormatException e) {
                                Toast.makeText(getApplicationContext(),
                                        R.string.wrong_coord, Toast.LENGTH_LONG).show();
                                Log.d("Odr", "|" + coords[0].getText() + "|");
                                return;
                            }
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                                    getApplicationContext());
                            if (preferences.getFloat("lat1", 0) == 0) {
                                preferences.edit().putFloat("lat1", latitude)
                                        .putFloat("lon1", longitude)
                                        .putFloat("x1", x)
                                        .putFloat("y1", y).apply();
                                Toast.makeText(getApplicationContext(), R.string.success_1_point,
                                        Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                float lat1 = preferences.getFloat("lat1", 0);
                                float lon1 = preferences.getFloat("lon1", 0);
                                float x1 = preferences.getFloat("x1", 0);
                                float y1 = preferences.getFloat("y1", 0);
                                PointF point1 = mImage.getPointOnMap(new PointF(x1, y1));
                                PointF point2 = mImage.getPointOnMap(new PointF(x, y));
                                File mapFile = new File(mapPath);
                                Bundle data = Util.mapAffixment(new PointF(lon1, lat1), point1,
                                        new PointF(longitude, latitude), point2, mapFile);
                                Log.d("Odr", data.toString());
                                affixmentFile = Util.createAffixmentFile(mapFile, data);
                                Toast.makeText(getApplicationContext(), R.string.success_2_point,
                                        Toast.LENGTH_SHORT).show();
                                Toast.makeText(getApplicationContext(), R.string.success_affixment,
                                        Toast.LENGTH_SHORT).show();
                                preferences.edit().putFloat("lat1", 0).putFloat("lon1", 0)
                                        .putFloat("x1", 0).putFloat("y1", 0).apply();
                                testAffixment();
                                dialog.dismiss();
                            }
                        }
                    }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mLayout.removeView(imageView);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            coords[0] = (EditText) dialog.getWindow().findViewById(R.id.latitudeET);
            coords[1] = (EditText) dialog.getWindow().findViewById(R.id.longitudeET);
        }
    }

    private void testAffixment() {
        Location location = new Location("gps");
        location.setLongitude(10.321138);
        location.setLatitude(63.369010);
        Bundle b = Util.readAffixmentFile(affixmentFile, this);
        double startLatitude = b.getDouble("latitude");
        double startLongitude = b.getDouble("longitude");
        double scaleLat = b.getDouble("scaleLat");
        double scaleLon = b.getDouble("scaleLon");
        Point p = Util.getPositionOnMap(startLatitude, startLongitude, scaleLat, scaleLon, location);
        Log.d("Odr", p.x +","+p.y);
    }
}
