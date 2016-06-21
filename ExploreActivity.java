package tk.parmclee.o_droid;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class ExploreActivity extends MapActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explore);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        String scale = preferences.getString("scale", "100");
        int scaleFactor;
        mLayout = (RelativeLayout) findViewById(R.id.layoutAffixment);
        mImage.addListeners(
                new GestureDetector(getApplicationContext(), new ScrollListener()),
                new ScaleGestureDetector(getApplicationContext(), new ScaleListener()));
        assert mLayout != null;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mLayout.addView(mImage, params);

        ImageButton quitBtn = (ImageButton) findViewById(R.id.quit);
        assert quitBtn != null;
        quitBtn.bringToFront();
    }
}
