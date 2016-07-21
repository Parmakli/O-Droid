package tk.parmclee.o_droid;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class LevelActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.level);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayShowHomeEnabled(true);
    }

    public void startRun(View v) {
        Intent intent = new Intent(getApplicationContext(), CompetitionActivity.class);
        intent.putExtras(getIntent().getExtras());
        switch (v.getId()) {
            case R.id.simple:
                intent.putExtra("level", "simple");
                break;
            case R.id.medium:
                intent.putExtra("level", "medium");
                break;
            case R.id.hard:
                intent.putExtra("level", "hard");
                break;
            default:
        }
        startActivity(intent);
    }

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
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                Intent intent = new Intent(getApplicationContext(), Preferences.class);
                startActivity(intent);
        }
        return true;
    }
}
