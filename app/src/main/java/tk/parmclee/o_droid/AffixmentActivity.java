package tk.parmclee.o_droid;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;

public class AffixmentActivity extends MapActivity {

    File affixmentFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.affixment);
        removeGPS();

        mLayout = (RelativeLayout) findViewById(R.id.layoutAffixment);
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
    }

    private class ScrollAndLongPressListener extends ScrollListener {
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
            createDialog(imageView, x, y);
        }

        void createDialog(final ImageView imageView, final float x, final float y) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                RuntimePermissionHelper.checkAndRequestStoragePermission(AffixmentActivity.this, null);
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
                                return;
                            }
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                                    getApplicationContext());
                            if (preferences.getFloat("lat1", 0) == 0) {
                                PointF point1 = mImage.getPointOnMap(new PointF(x, y));
                                preferences.edit().putFloat("lat1", latitude)
                                        .putFloat("lon1", longitude)
                                        .putFloat("x1", point1.x)
                                        .putFloat("y1", point1.y).apply();
                                Toast.makeText(getApplicationContext(), R.string.success_1_point,
                                        Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                float lat1 = preferences.getFloat("lat1", 0);
                                float lon1 = preferences.getFloat("lon1", 0);
                                float x1 = preferences.getFloat("x1", 0);
                                float y1 = preferences.getFloat("y1", 0);
                                PointF point1 = new PointF(x1, y1);
                                PointF point2 = mImage.getPointOnMap(new PointF(x, y));
                                File mapFile = new File(mapPath);
                                Bundle data = Util.mapAffixment(new PointF(lat1, lon1), point1,
                                        new PointF(latitude, longitude), point2, mapFile);
                                affixmentFile = Util.createAffixmentFile(mapFile, data);
                                Toast.makeText(getApplicationContext(),
                                        getString(R.string.success_2_point)+"\n"+
                                        getString(R.string.success_affixment) + " "
                                        + data.getInt("quality")+" %", Toast.LENGTH_LONG).show();
                                preferences.edit().putFloat("lat1", 0).putFloat("lon1", 0)
                                        .putFloat("x1", 0).putFloat("y1", 0).apply();
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
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
            coords[0] = (EditText) dialog.getWindow().findViewById(R.id.latitudeET);
            coords[1] = (EditText) dialog.getWindow().findViewById(R.id.longitudeET);
            int type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                    | InputType.TYPE_NUMBER_FLAG_SIGNED;
            coords[0].setInputType(type);
            coords[1].setInputType(type);
        }
    }

    @Override
    void locationChanged(Location location) {
        // do nothing
    }

}
