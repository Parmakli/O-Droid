package tk.parmclee.o_droid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Util {

    static Uri getMapStorageUri(Context context){
        File storagePath;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            storagePath = Environment.getExternalStorageDirectory();
        } else storagePath = context.getFilesDir();
        storagePath = new File(storagePath.getAbsolutePath() + "/O-Droid Maps");
        storagePath.mkdirs();
        return Uri.parse(storagePath.getAbsolutePath());
    }

    static void createTextFile(Uri uri, String name, String text){
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

    static Bundle mapAffixment(Location location1, Point point1,
                                         Location location2, Point point2, File mapFile){
        Bundle result = new Bundle();
        //Bitmap bmp = BitmapFactory.decodeFile(mapFile.getAbsolutePath());
        //int width = bmp.getWidth();
        //int height = bmp.getHeight();

        float distance[] = new float[1];
        double lat1 = location1.getLatitude();
        double lon1 = location1.getLongitude();
        double lat2 = location2.getLatitude();
        double lon2 = location2.getLongitude();
        Location.distanceBetween(lat1, lon1, lat2, lon2, distance);
        int xDistance = point1.x - point2.x;
        int yDistance = point1.y - point2.y;
        double pixelDistance = Math.hypot(xDistance, yDistance);
        double scaleM = distance[0] / pixelDistance; // meters per pixel
        double scaleLatitudeDegrees = (lat2 - lat1) / yDistance; //  degrees per pxl
        double scaleLongitudeDegrees = (lon2 - lon1) / xDistance; //  degrees per pxl

        // left+top == west+north
        // pixels increase from top to bottom (opposite to latitude)
        double latitude = lat1 + point1.y * Math.abs(scaleLatitudeDegrees);
        // pixels increase from left to right (like longitude)
        double longitude = lon1 - point1.x * Math.abs(scaleLongitudeDegrees);
        //result.putInt("width", width);
        //result.putInt("height", height);
        result.putDouble("scale", scaleM);
        result.putDouble("scaleLatD", Math.abs(scaleLatitudeDegrees));
        result.putDouble("scaleLonD", Math.abs(scaleLongitudeDegrees));
        result.putDouble("latitude", latitude);
        result.putDouble("longitude", longitude);
        return result;
    }

    static Point getPositionOnMap(double startLatitude, double startLongitude,
                                  double scaleLatD, double scaleLonD, Location location){
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        // left+top == startL..itude
        double latOffset = startLatitude - latitude;
        double lonOffset = longitude - startLongitude;

        int y = (int) (latOffset / scaleLatD);
        int x = (int) (lonOffset / scaleLonD);
        return new Point(x, y); //handle x,y < 0 or > w,h after return
    }
}
