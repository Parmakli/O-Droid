package tk.parmclee.o_droid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_start);

        findViewById(R.id.explore_layout).setOnClickListener(listener);
        findViewById(R.id.compete_layout).setOnClickListener(listener);
        findViewById(R.id.affix_layout).setOnClickListener(listener);
/*
        initialize(explore, R.drawable.explore);
        initialize(compete, R.drawable.competition);
        initialize(affix, R.drawable.affix);*/
        MapListActivity.getStoragePath(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        String width = preferences.getString("screen_width", null);
        String height = preferences.getString("screen_height", null);
        PointF screenSize;
        try {
            //noinspection ResultOfMethodCallIgnored
            Float.parseFloat(width);
            //noinspection ResultOfMethodCallIgnored
            Float.parseFloat(height);
        } catch (NullPointerException | NumberFormatException npe) {
            screenSize = Util.getSizeInCm(this);
            preferences.edit().putString("screen_width", Float.toString(10 * screenSize.x))
                    .putString("screen_height", Float.toString(10 * screenSize.y)).apply();
        }
    }

   /* private void initialize(final ImageView view, int id) {
        view.setOnClickListener(listener);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        AsyncTask<Object, Void, Bitmap> task = new AsyncTask<Object, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Object... objects) {
                int id = (int) objects[0];
                BitmapFactory.Options options = (BitmapFactory.Options) objects[1];
                return BitmapFactory.decodeResource(getResources(), id, options);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                view.setScaleType(ImageView.ScaleType.FIT_XY);
                view.setImageBitmap(bitmap);
            }
        };
        task.execute(id, options);
    }*/

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), MapListActivity.class);
            switch (v.getId()) {
                case R.id.explore_layout:
                    intent.putExtra("type", "explore");
                    startActivity(intent);
                    break;
                case R.id.compete_layout:
                    intent.putExtra("type", "competition");
                    startActivity(intent);
                    break;
                case R.id.affix_layout:
                    intent.putExtra("type", "affixment");
                    startActivity(intent);
                    break;
                default:
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(R.string.options);
        item.setIcon(R.drawable.ic_perm_data_setting_white);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(getApplicationContext(), Preferences.class);
        startActivity(intent);
        return true;
    }
}
