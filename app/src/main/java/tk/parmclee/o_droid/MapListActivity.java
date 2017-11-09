package tk.parmclee.o_droid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
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
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapListActivity extends AppCompatActivity {

    String whatNext;
    private final int REQUEST_GET_FILE = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_list);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setIcon(R.drawable.ic_arrow_back_white_24dp);
        }
        String storagePath = getStoragePath(this);
        if (storagePath != null) onStoragePathKnown(storagePath);
    }

    private void onStoragePathKnown(String path) {
        File mapStorage = new File(path);
        File maps[] = mapStorage.listFiles(filterMime("image/jpeg", "image/png", "image/gif"));
        File affixments[] = mapStorage.listFiles(filterMime("text/plain"));
        ArrayList<String> affixNames = new ArrayList<>();
        if (affixments != null) {
            for (File affixment : affixments) affixNames.add(reduceExtension(affixment.getName()));
        }
        ArrayList<Map<String, String>> data = new ArrayList<>();
        Map<String, String> map;
        if (maps != null) {
            for (File mapFile : maps) {
                map = new HashMap<>();
                String mapName = reduceExtension(mapFile.getName());
                map.put("map", mapFile.getName());
                if (affixNames.contains(mapName))
                    map.put("affixment", getString(R.string.affix_exist));
                else map.put("affixment", getString(R.string.affix_not_exist));
                data.add(map);
            }
        }
        String from[] = {"map", "affixment"};
        int to[] = {R.id.text1, R.id.text2};
        ListAdapter adapter = new SimpleAdapter(this, data, R.layout.map_list_item, from, to);
        View empty = findViewById(R.id.empty);
        if (data.isEmpty() && empty != null) {
            empty.setVisibility(View.VISIBLE);
        } else if (empty != null) empty.setVisibility(View.GONE);
        findViewById(R.id.goFindButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_photo)),
                        REQUEST_GET_FILE);
            }
        });
        ListView lv = (ListView) findViewById(R.id.list);
        if (lv != null) {
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(listener);
        }
        whatNext = getIntent().getStringExtra("type");
    }

    public static String getStoragePath(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (RuntimePermissionHelper.checkAndRequestStoragePermission(context, null)) {
                return Util.getMapStorageUri(context).getPath();
            } else return null;
        }
        return Util.getMapStorageUri(context).getPath();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GET_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                View progressBar = findViewById(R.id.progressBar);
                progressBar.setVisibility(View.VISIBLE);
                try {
                    inputStream = getContentResolver().openInputStream(data.getData());
                    File newFile = new File(getStoragePath(this) + File.separator + getFileName(data.getData()));
                    if (!newFile.exists()) newFile.createNewFile();
                    outputStream = new BufferedOutputStream(new FileOutputStream(newFile));
                    copyFile(inputStream, outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "File error", Toast.LENGTH_SHORT).show();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
                String storagePath = getStoragePath(this);
                progressBar.setVisibility(View.GONE);
                if (storagePath != null) onStoragePathKnown(storagePath);
            }
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case RuntimePermissionHelper.REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE:
                    onStoragePathKnown(Util.getMapStorageUri(getApplicationContext()).getPath());
                    break;
                default:
            }
        } else {
            Toast.makeText(this, "Permissions NOT GRANTED", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    String reduceExtension(String fileName) {
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
            switch (whatNext) {
                case "affixment":
                    goNext(AffixmentActivity.class, view);
                    break;
                case "competition":
                    goNext(CreateRouteActivity.class, view);
                    break;
                case "explore":
                    goNext(ExploreActivity.class, view);
                    break;
                default:
            }
        }
    };

    void goNext(Class<?> cls, View view) {
        TextView tv2 = (TextView) view.findViewById(R.id.text2);
        if (tv2.getText() == getString(R.string.affix_not_exist) && cls != AffixmentActivity.class) {
            Toast.makeText(this, getString(R.string.affix_not_exist), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getApplicationContext(), cls);
        TextView tv = (TextView) view.findViewById(R.id.text1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            RuntimePermissionHelper.checkAndRequestStoragePermission(this, null);
        String path = Util.getMapStorageUri(getApplicationContext()).getPath() +
                "/" + tv.getText();
        intent.putExtra("mapPath", path);
        if (cls == CreateRouteActivity.class) intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        Log.d("Odr", "Path " + path);
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
