package tk.parmclee.o_droid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

public class StartActivity extends AppCompatActivity {

    static Uri sStorageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.ic_launcher);
        }
        ImageView explore = (ImageView) findViewById(R.id.explore);
        ImageView compete = (ImageView) findViewById(R.id.compete);
        ImageView affix = (ImageView) findViewById(R.id.affix);

        initialize(explore, R.drawable.explore);
        initialize(compete, R.drawable.competition);
        initialize(affix, R.drawable.affix);
        sStorageUri = Util.getMapStorageUri(getApplicationContext());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        String width = preferences.getString("screen_width", null);
        String height = preferences.getString("screen_height", null);
        PointF screenSize;
        try {
            Float.parseFloat(width);
            Float.parseFloat(height);
        } catch (NullPointerException | NumberFormatException npe) {
            screenSize = Util.getSizeInCm(this);
            preferences.edit().putString("screen_width", Float.toString(10 * screenSize.x))
                    .putString("screen_height", Float.toString(10 * screenSize.y)).apply();
        }
    }

    private void initialize(ImageView view, int id) {
        view.setOnClickListener(listener);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bm = BitmapFactory.decodeResource(getResources(), id, options);
        view.setScaleType(ImageView.ScaleType.FIT_XY);
        view.setImageBitmap(bm);
    }

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), MapList.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            switch (v.getId()) {
                case R.id.explore:
                    intent.putExtra("type", "explore");
                    startActivity(intent);
                    break;
                case R.id.compete:
                    intent.putExtra("type", "competition");
                    startActivity(intent);
                    break;
                case R.id.affix:
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
