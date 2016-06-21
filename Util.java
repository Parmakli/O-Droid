package tk.parmclee.o_droid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
    final static float METERS_PER_LATITUDE_DEGREE = 111132.9556f,
        METERS_PER_EQUATOR_LONGITUDE_DEGREE = 111319.491f;
    final static float METERS_FROM_POLE_TO_EQUATOR = 90 * METERS_PER_LATITUDE_DEGREE;

    static double metersPerLongitudeDegree(float latitude){
        return METERS_PER_EQUATOR_LONGITUDE_DEGREE * parallelMultiplier(latitude);
    }

    /**
     * Length of the parallel divided by the length of the equator
     * @param latitude parallel latitude
     * @return The number you must multiply equator length to get parallel length
     */
    static double parallelMultiplier(double latitude){
        return (METERS_FROM_POLE_TO_EQUATOR - latitude) / METERS_FROM_POLE_TO_EQUATOR;
    }

    /**
     * Calculates Earth radius
     * @param theta latitude degrees
     * @return Earth radius on theta latitude
     */
    int r(double theta){
        double thetaRadians = theta * Math.PI / 180; // to radians
        final long rEquatorSquared = Math.round(Math.pow(6378137, 2)),
                rPoleSquared = Math.round(Math.pow(6356752, 2));
        double cosSquared = Math.pow(Math.cos(thetaRadians), 2),
                sinSquared = Math.pow(Math.sin(thetaRadians), 2);
        return (int) Math.round(Math.sqrt(rEquatorSquared * rPoleSquared /
                (rEquatorSquared * sinSquared + rPoleSquared * cosSquared)));
    }

    /**
     * Calculates coordinates of origin or current point
     * @param location x - latitude, y - longitude
     * @param point meters from the origin
     * @param origin if true calculates coordinates of origin, false - current point
     * @return pointF.x - latitude in degrees, pointF.y - longitude in degrees
     */
    PointF calculateGeoCoords(PointF location, Point point, boolean origin){
        int r = r(location.x);
        int sgn = 1;
        if (origin) sgn = -1;
        int x = point.x, y = point.y;
        float theta = location.x,  phi = location.y;
        theta *= Math.PI / 180; // to radians
        phi *= Math.PI / 180;
        double theta0 = theta - sgn * 2 * Math.asin(y / (2 * r));
        double phi0 = phi + sgn * 2 * Math.asin(x / (2 * r * Math.cos(theta)));
        // back to degrees
        return new PointF((float) (theta0 * 180 / Math.PI), (float) (phi0 * 180 / Math.PI));
    }

    /**
     *
     * @param location geographical coordinates, x - latitude, y - longitude
     * @param origin geographical coordinates of the origin, x - latitude, y - longitude
     * @return point on map with coordinates in meters
     */
    Point calculatePoint(PointF location, PointF origin){
        float theta = location.x;
        int r = r(theta);
        float deltaTheta = origin.x - location.x, deltaPhi = location.y - origin.y;
        deltaTheta *= Math.PI / (180 * 2); // to radians and divide by 2
        deltaPhi *= Math.PI / (180 * 2);
        return new Point((int) Math.round(2 * r * Math.cos(theta) * Math.sin(deltaPhi)),
                (int) Math.round(2 * r * Math.sin(deltaTheta)));
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
        float lat1, lon1, lat2, lon2;
        if (location1.y > location2.y){ // (lat1, lon1) - northern point
             lat1 = location1.y;
             lon1 = location1.x;
             lat2 = location2.y;
             lon2 = location2.x;
        } else {
             lat1 = location2.y;
             lon1 = location2.x;
             lat2 = location1.y;
             lon2 = location1.x;
        }
        float xDistance = point1.x - point2.x;
        float yDistance = point1.y - point2.y;
        double scaleLatitudeDegrees = Math.abs((lat2 - lat1) / yDistance); //  degrees per pixel
        // longitude substitution has meaning only on the one latitude
        // so calculate degrees per pixel as it was on the equator
        double scaleLongitudeEquator = Math.abs(
                (lon2 / parallelMultiplier(lat2) - lon1 / parallelMultiplier(lat1)) / xDistance);

        // left+top == west+north corner location
        // pixels increase from top to bottom (opposite to latitude)
        double latitude = lat1 + point1.y * scaleLatitudeDegrees;
        // pixels increase from left to right (like longitude)
        // to get longitude we need to "bring" map from equator
        double longitude = (lon1 / parallelMultiplier(lat1) - point1.x * scaleLongitudeEquator)
                * parallelMultiplier(latitude);
        result.putInt("width", width);
        result.putInt("height", height);
        result.putDouble("scaleLat", scaleLatitudeDegrees);
        result.putDouble("scaleLon", scaleLongitudeEquator);
        result.putDouble("latitude", latitude);
        result.putDouble("longitude", longitude);
        return result;
    }

    static Point getPositionOnMap(double startLatitude, double startLongitude,
                                  double scaleLat, double scaleLonEquator, Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        // left+top == startL..itude
        double latOffset = startLatitude - latitude;
        // "bring" to equator
        double lonOffset = longitude / parallelMultiplier(latitude) -
                startLongitude / parallelMultiplier(startLatitude);

        int y = (int) (latOffset / scaleLat);
        int x = (int) (lonOffset / scaleLonEquator);
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
        Double scaleLat = data.getDouble("scaleLat");
        Double scaleLonEq = data.getDouble("scaleLon");
        String latDegrees = "Latitude degrees per pixel: " + scaleLat;
        String lonDegrees = "Longitude degrees per pixel \"on equator\": " + scaleLonEq;
        content += latDegrees + "\n" + lonDegrees + "\n";
        String quality = "Affixment quality: " + Math.round(100 * Math.min(2 * scaleLat, scaleLonEq)
                                               / Math.max(scaleLonEq, 2 * scaleLat)) + " %";
        content += quality + "\n";

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
