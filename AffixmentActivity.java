package tk.parmclee.o_droid;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class AffixmentActivity extends AppCompatActivity {

    MapView mImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        String path = getIntent().getStringExtra("mapPath");
        mImage = new MapView(getApplicationContext(), path);
        setContentView(mImage);

        Log.d("Odr", "onCreate");
        Log.d("Odr", path);
    }

}
