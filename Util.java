package tk.parmclee.o_droid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
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

    static double associateCoordsWithMap(Location location1, Point point1,
                                Location location2, Point point2, File mapFile){
        //Bitmap bmp = BitmapFactory.decodeFile(mapFile.getAbsolutePath());
        float distance[] = new float[1];
        Location.distanceBetween(location1.getLatitude(), location1.getLongitude(),
                                location2.getLatitude(), location2.getLongitude(), distance);
        int xDistance = point1.x - point2.x;
        int yDistance = point1.y - point2.y;
        double pixelDistance = Math.sqrt(xDistance * xDistance + yDistance * yDistance);
        double scale = distance[0] / pixelDistance; // meters per pixel
        return scale;
    }
}
