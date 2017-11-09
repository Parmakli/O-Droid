package tk.parmclee.o_droid;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {
    private final static String DB_NAME = "db";
    private final static String TABLE = "points";
    private final static String TIME = "time";
    private final static String LATITUDE = "lat";
    private final static String LONGITUDE = "lng";
    private final static String ALTITUDE = "alt";

    private DbHelper(Context context, int version) {
        super(context, DB_NAME, null, version);
    }

    private static volatile DbHelper instance;

    static DbHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DbHelper.class) {
                if (instance == null) instance = new DbHelper(context, 1);
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE + " ("
                + TIME + " integer,"
                + LATITUDE + " real,"
                + LONGITUDE + " real,"
                + ALTITUDE + " real" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    void addLocation(TrackPoint point) {
        ContentValues cv = new ContentValues();
        SQLiteDatabase db = getWritableDatabase();
        cv.put(TIME, point.getTime());
        cv.put(LATITUDE, point.getLatitude());
        cv.put(LONGITUDE, point.getLongitude());
        db.insert(TABLE, null, cv);
    }

    List<TrackPoint> getPoints(long from) {
        List<TrackPoint> models = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(true,
                    TABLE,
                    new String[]{TIME, LATITUDE, LONGITUDE},
                    TIME + " > ?",
                    new String[]{String.valueOf(from)},
                    null, null, TIME, null);
            if (c.moveToFirst()) {
                int timeIndex = c.getColumnIndex(TIME);
                int latIndex = c.getColumnIndex(LATITUDE);
                int lngIndex = c.getColumnIndex(LONGITUDE);
                do {
                    TrackPoint model = new TrackPoint();
                    model.setTime(c.getLong(timeIndex));
                    model.setLatitude(c.getDouble(latIndex));
                    model.setLongitude(c.getDouble(lngIndex));
                    models.add(model);
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return models;
    }

    void deleteOldPoints() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE, TIME + " < ?", new String[]{String.valueOf(System.currentTimeMillis() - 48 * 3600 * 1000)});
    }

}