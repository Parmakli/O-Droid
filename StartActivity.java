package tk.parmclee.o_droid;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity {

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
    }

    private void initialize(ImageView view, int id){
        view.setOnClickListener(listener);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bm = BitmapFactory.decodeResource(getResources(), id, options);
        view.setImageBitmap(bm);//TODO bitmap as background

    }

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.explore:
                    break;
                case R.id.compete:
                    break;
                case R.id.affix:
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
        Toast.makeText(this, "yes",Toast.LENGTH_SHORT).show();
        return true;
    }
}
