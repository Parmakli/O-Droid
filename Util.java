package tk.parmclee.o_droid;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Util {

    Uri getMapStorageUri(Context context){
        File storagePath;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            storagePath = Environment.getExternalStorageDirectory();
        } else storagePath = context.getFilesDir();
        storagePath = new File(storagePath.getAbsolutePath() + "/O-Droid Maps");
        storagePath.mkdirs();
        return Uri.parse(storagePath.getAbsolutePath());
    }

    void createTextFile(Uri uri, String name, String text){
        File storage = new File(uri.getPath());
        File file = new File(storage, name);
        file.mkdirs();
        try {
            RandomAccessFile raFile = new RandomAccessFile(file, "rw");
            raFile.writeUTF(text);
            raFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
