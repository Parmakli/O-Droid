package tk.parmclee.o_droid;


public class TrackPoint {
    private long mTime;
    private double mLatitude;
    private double mLongitude;
    private double mAltitude; // todo in future for not to do migration

    public TrackPoint(){

    }

    TrackPoint(long time, double latitude, double longitude){
        mTime = time;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }
}
