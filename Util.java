package tk.parmclee.o_droid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Util {

    static Uri getMapStorageUri(Context context) {
        File storagePath;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            storagePath = Environment.getExternalStorageDirectory();
        } else storagePath = context.getFilesDir();
        storagePath = new File(storagePath.getAbsolutePath() + "/O-Droid Maps");
        storagePath.mkdirs();
        return Uri.parse(storagePath.getAbsolutePath());
    }

    static File createTextFile(Uri uri, String name, String text) {
        File storage = new File(uri.getPath());
        storage.mkdirs();
        File file = new File(storage, name);
        if (file.exists()) file.delete();
        try {
            file.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(text);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    static Bundle mapAffixment(PointF location1, PointF point1,
                               PointF location2, PointF point2, File mapFile) {
        Bundle result = new Bundle();
        Bitmap bmp = BitmapFactory.decodeFile(mapFile.getAbsolutePath());
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        float lat1 = location1.y;
        float lon1 = location1.x;
        float lat2 = location2.y;
        float lon2 = location2.x;
        float xDistance = point1.x - point2.x;
        float yDistance = point1.y - point2.y;
        double scaleLatitudeDegrees = Math.abs((lat2 - lat1) / yDistance); //  degrees per pixel
        double scaleLongitudeDegrees = Math.abs((lon2 - lon1) / xDistance); //  degrees per pixel

        // left+top == west+north corner location
        // pixels increase from top to bottom (opposite to latitude)
        double latitude = lat1 + point1.y * scaleLatitudeDegrees;
        // pixels increase from left to right (like longitude)
        double longitude = lon1 - point1.x * scaleLongitudeDegrees;
        result.putInt("width", width);
        result.putInt("height", height);
        result.putDouble("scaleLat", scaleLatitudeDegrees);
        result.putDouble("scaleLon", scaleLongitudeDegrees);
        result.putDouble("latitude", latitude);
        result.putDouble("longitude", longitude);
        return result;
    }

    static Point getPositionOnMap(double startLatitude, double startLongitude,
                                  double scaleLatD, double scaleLonD, Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        // left+top == startL..itude
        double latOffset = startLatitude - latitude;
        double lonOffset = longitude - startLongitude;

        int y = (int) (latOffset / scaleLatD);
        int x = (int) (lonOffset / scaleLonD);
        return new Point(x, y); //handle x,y < 0 or > w,h after return
    }

    static File createAffixmentFile(File file, Bundle data) {
        String fileName = file.getName();
        String textFileName = fileName.substring(0, fileName.lastIndexOf(".")).concat(".txt");
        File directory = file.getParentFile();

        String content = "AUTO-GENERATED FILE. Please don't modify this.\nThis file was generated "
                + "by O-Droid app.\nDon't modify it by hand. Use O-Droid.\n*********************\n";
        int width = data.getInt("width");
        int height = data.getInt("height");
        content += fileName + " resolution: " + width + "x" + height + "\n";
        String latitude = "Left top corner latitude: " + data.getDouble("latitude");
        String longitude = "Left top corner longitude: " + data.getDouble("longitude");
        content += latitude + "\n" + longitude + "\n";
        String latDegrees = "Latitude degrees per pixel: " + data.getDouble("scaleLat");
        String lonDegrees = "Longitude degrees per pixel: " + data.getDouble("scaleLon");
        content += latDegrees + "\n" + lonDegrees + "\n";

        return createTextFile(Uri.fromFile(directory), textFileName, content);
    }

    static Bundle readAffixmentFile(File file, Context context) {
        if (!file.exists()) {
            Toast.makeText(context, R.string.no_file, Toast.LENGTH_SHORT).show();
            return null;
        }
        Bundle result = new Bundle();
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            do {
                line = br.readLine();
            } while (!line.contains("***"));
            line = br.readLine(); //goto resolution line
            String w = line.substring(line.lastIndexOf(":") + 2, line.lastIndexOf("x"));
            String h = line.substring(line.lastIndexOf("x") + 1);
            result.putInt("width", Integer.valueOf(w));
            result.putInt("height", Integer.valueOf(h));
            line = br.readLine(); //latitude line
            String lat = line.substring(line.lastIndexOf(":") + 2);
            result.putDouble("latitude", Double.valueOf(lat));
            line = br.readLine(); //longitude line
            String lon = line.substring(line.lastIndexOf(":") + 2);
            result.putDouble("longitude", Double.valueOf(lon));
            line = br.readLine(); //latitude scale line
            String scaleLat = line.substring(line.lastIndexOf(":") + 2);
            result.putDouble("scaleLat", Double.valueOf(scaleLat));
            line = br.readLine(); //longitude scale line
            String scaleLon = line.substring(line.lastIndexOf(":") + 2);
            result.putDouble("scaleLon", Double.valueOf(scaleLon));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }
}
