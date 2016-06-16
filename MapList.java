package tk.parmclee.o_droid;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapList extends AppCompatActivity {//TODO remove from stack
    String whatNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_list);

        File mapStorage = new File(StartActivity.sStorageUri.getPath());
        File maps[] = mapStorage.listFiles(filterMime("image/jpeg", "image/png", "image/gif"));
        int quantity = maps.length;
        File affixments[] = mapStorage.listFiles(filterMime("text/plain"));
        ArrayList <String> affixNames = new ArrayList<>(affixments.length);
        for (int i = 0; i < quantity; i++) affixNames.add(reduceExtension(affixments[i].getName()));
        ArrayList<Map<String,String>> data = new ArrayList<>(quantity);
        Map<String, String> map;
        for (File mapFile : maps) {
            map = new HashMap<>();
            String mapName = reduceExtension(mapFile.getName());
            map.put("map", mapFile.getName());
            if (affixNames.contains(mapName)) map.put("affixment", getString(R.string.affix_exist));
            else map.put("affixment", getString(R.string.affix_not_exist));
            data.add(map);
        }
        String from[] = {"map", "affixment"};
        int to[] = {R.id.text1, R.id.text2};
        ListAdapter adapter = new SimpleAdapter(this, data, R.layout.map_list_item, from, to);
        ListView lv = (ListView) findViewById(R.id.list);
        TextView empty = (TextView) findViewById(R.id.empty);
        if (data.isEmpty() && empty != null) empty.setVisibility(View.VISIBLE);
        if (lv != null) {
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(listener);
        }
        whatNext = getIntent().getStringExtra("type");

    }

        String reduceExtension(String fileName){
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    FileFilter filterMime(final String... types) {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String path = pathname.getAbsolutePath();
                String extension, mime;
                try {
                    extension = path.substring(path.lastIndexOf(".") + 1);
                } catch (IndexOutOfBoundsException e) {
                    return false;
                }
                if (extension.length() > 1 && extension.length() < 6) {
                    mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                            extension.toLowerCase());
                    for (String type : types) if (type.equals(mime)) return true;
                }
                return false;
            }
        };
    }

    AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (whatNext){
                case "affixment":
                    Intent intent = new Intent(getApplicationContext(), AffixmentActivity.class);
                    TextView tv = (TextView) view.findViewById(R.id.text1);
                    String path = Util.getMapStorageUri(getApplicationContext()).getPath() +
                            "/" + tv.getText();
                    intent.putExtra("mapPath", path);
                    startActivity(intent);
                    Log.d("Odr", "Path " + path);
                    break;
                case "competition":
                    break;
                case "explore":
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
        Toast.makeText(this, "yes", Toast.LENGTH_SHORT).show();
        return true;
    }
}
