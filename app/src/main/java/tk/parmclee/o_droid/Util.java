package tk.parmclee.o_droid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Util {

    /**
     * Calculates Earth radius
     *
     * @param theta latitude degrees
     * @return Earth radius on theta latitude
     */
    static long r(double theta) {
        double thetaRadians = theta * Math.PI / 180; // to radians
        long r2 = Math.round(Math.pow(6378137, 2));     // equator radius squared
        final double r2Relation = r2 / Math.pow(6356752, 2);  // divided by pole radius squared
        double cosSquared = Math.pow(Math.cos(thetaRadians), 2),
                sinSquared = Math.pow(Math.sin(thetaRadians), 2);
        return Math.round(Math.sqrt(r2 / (r2Relation * sinSquared + cosSquared)));
    }

    /**
     * Calculates coordinates of origin
     *
     * @param location x - latitude, y - longitude
     * @param point    meters from the origin
     * @return result.x - latitude in degrees, result.y - longitude in degrees
     */
    static PointF calculateOrigin(PointF location, PointF point) {
        long r = r(location.x);
        float x = point.x, y = point.y;
        float theta = location.x, phi = location.y;
        theta *= Math.PI / 180; // to radians
        phi *= Math.PI / 180;
        double theta0 = theta + 2 * Math.asin(y / (2 * r));
        double phi0 = phi - 2 * Math.asin(x / (r * (Math.cos(theta0) + Math.cos(theta))));
        // back to degrees
        return new PointF((float) (theta0 * 180 / Math.PI), (float) (phi0 * 180 / Math.PI));
    }

    /**
     * @param location geographical coordinates, x - latitude, y - longitude (degrees)
     * @param origin   geographical coordinates of the origin, x - latitude, y - longitude (degrees)
     * @return point on map with coordinates in meters
     */
    static PointF calculatePoint(PointF location, PointF origin) {
        float theta = location.x;
        long r = r(theta);
        float theta0 = origin.x, deltaPhi = location.y - origin.y;
        theta *= Math.PI / 180;  // to radians
        theta0 *= Math.PI / 180;
        deltaPhi *= Math.PI / (180 * 2); // to radians and divide by 2
        return new PointF((float) (r * ((Math.cos(theta) + Math.cos(theta0)) * Math.sin(deltaPhi))),
                (float) (2 * r * Math.sin((theta0 - theta) / 2)));
    }

    static Uri getMapStorageUri(Context context) {
        File storagePath;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            storagePath = Environment.getExternalStorageDirectory();
        } else storagePath = context.getFilesDir();
        storagePath = new File(storagePath.getAbsolutePath() + "/O-Droid Maps");
        storagePath.mkdirs();
        return Uri.parse(storagePath.getAbsolutePath());
    }

    private static File createTextFile(Uri uri, String name, String text) {
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
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inSampleSize = MapView.getSampleSize(mapFile.getAbsolutePath());
        options.inDither = true;
        Bitmap bmp = BitmapFactory.decodeFile(mapFile.getAbsolutePath(), options);
        int width = bmp.getWidth(), height = bmp.getHeight();

        PointF pointsDistanceMeters = calculatePoint(location1, location2);

        float xDistance = point1.x - point2.x;
        float yDistance = point1.y - point2.y;
        double scaleX = Math.abs(pointsDistanceMeters.x / xDistance); // meters per pixel
        double scaleY = Math.abs(pointsDistanceMeters.y / yDistance);
        int quality = (int) Math.round(100 * Math.min(scaleX, scaleY) / Math.max(scaleX, scaleY));
        double mpp = (scaleX + scaleY) / 2;

        PointF point1meters = new PointF((float) (point1.x * mpp), (float) (point1.y * mpp));

        PointF origin = calculateOrigin(location1, point1meters);
        double latitude = origin.x, longitude = origin.y;

        result.putInt("width", width);
        result.putInt("height", height);
        result.putDouble("meters per pixel", mpp);
        result.putInt("quality", quality);
        result.putDouble("latitude", latitude);
        result.putDouble("longitude", longitude);
        return result;
    }

    static Point getPositionOnMap(double startLatitude, double startLongitude,
                                  double mpp, Location location) {
        PointF origin = new PointF((float) startLatitude, (float) startLongitude);
        PointF current = new PointF((float) location.getLatitude(), (float) location.getLongitude());
        PointF currentPointMeters = calculatePoint(current, origin);
        int x = (int) (currentPointMeters.x / mpp);
        int y = (int) (currentPointMeters.y / mpp);
        return new Point(x, y); //handle x,y < 0 or > w,h after return
    }

    static File createAffixmentFile(File file, Bundle data) {
        String fileName = file.getName();
        String textFileName = affixFileName(fileName);
        File directory = file.getParentFile();

        String content = "AUTO-GENERATED FILE. Please don't modify this.\nThis file was generated "
                + "by O-Droid app.\nDon't modify it by hand. Use O-Droid.\n*********************\n";
        int width = data.getInt("width");
        int height = data.getInt("height");
        content += fileName + " resolution: " + width + "x" + height + "\n";
        String latitude = "Left top corner latitude: " + data.getDouble("latitude");
        String longitude = "Left top corner longitude: " + data.getDouble("longitude");
        content += latitude + "\n" + longitude + "\n";
        Double mpp = data.getDouble("meters per pixel");
        String latDegrees = "Meters per pixel: " + mpp;
        content += latDegrees + "\n";
        int quality = data.getInt("quality");
        String qty = "Affixment quality: " + quality + " %";
        content += qty + "\n";

        return createTextFile(Uri.fromFile(directory), textFileName, content);
    }

    static String affixFileName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf(".")).concat(".txt");
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
            line = br.readLine(); //scale line
            String mpp = line.substring(line.lastIndexOf(":") + 2);
            result.putDouble("meters per pixel", Double.valueOf(mpp));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    @SuppressWarnings("deprecation") // getWidth & getHeight have been changed to getSize
    static Point getSizeInPxl(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            wm.getDefaultDisplay().getSize(size);
        } else {
            size.x = wm.getDefaultDisplay().getWidth();
            size.y = wm.getDefaultDisplay().getHeight();
        }
        return size;
    }

    static PointF getSizeInCm(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = getSizeInPxl(context);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        final float CM_PER_INCH = 2.54f;
        return new PointF(CM_PER_INCH * size.x / dm.xdpi, CM_PER_INCH * size.y / dm.ydpi);
    }
}
