package tk.parmclee.o_droid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class MapView extends View {
    String mPath;
    float mScaleFactor, mMinFactor;
    Point mMapSize, mScreenSize;
    PointF mCenter, mLeftTop, mRightBottom; // mapping from screen points to raster map points
    Bitmap mBmp;
    Matrix mMatrix, iMatrix; // "i" means initial or inverse
    ScaleGestureDetector mScaleDetector;
    GestureDetector mScrollDetector;
    TextView testView;

    void setTestView(TextView tv) {
        testView = tv;
    }

    public MapView(Context context) {
        super(context);
    }

    public MapView(Context context, String path) {
        this(context);
        mPath = path;
        mScaleFactor = 1;
        mScreenSize = getScreenSize();
        mBmp = BitmapFactory.decodeFile(path);
        if (mBmp == null) Toast.makeText(context, "Bmp - null " + path, Toast.LENGTH_LONG).show();
        int width = mBmp.getWidth();
        int height = mBmp.getHeight();
        mMapSize = new Point(width, height);
        mCenter = new PointF(width / 2, height / 2);
        mLeftTop = new PointF(0, 0);
        mRightBottom = new PointF(width, height);
        iMatrix = new Matrix();
    }

    void addListeners(GestureDetector gesture, ScaleGestureDetector scale){
        mScaleDetector = scale;
        mScrollDetector = gesture;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        mScrollDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //scaling
        float wRatio = ((float) mScreenSize.x) / ((float) mMapSize.x);
        float hRatio = ((float) mScreenSize.y) / ((float) mMapSize.y);
        mMinFactor = mScaleFactor = Math.max(wRatio, hRatio);
        Log.d("Odr", "onAttach\n" + iMatrix.toString() + mScaleFactor + "\n" + wRatio + "\n" + hRatio +
                "\nMapSize" + mMapSize.x + "," + mMapSize.y);
        iMatrix.setScale(mScaleFactor, mScaleFactor);

        //calculating new center position
        float newCenter[] = {mMapSize.x / 2, mMapSize.y / 2}; // initial center
        iMatrix.mapPoints(newCenter); // mapped center

        //translating
        float dx = mScreenSize.x / 2 - newCenter[0]; // offset from mapped center to screen center
        float dy = mScreenSize.y / 2 - newCenter[1];
        iMatrix.postTranslate(dx, dy);

        mMatrix = new Matrix(iMatrix);
        transformCoords(iMatrix);
    }

    void transformCoords(final Matrix matrix) {
        float pts[] = {mScreenSize.x / 2, mScreenSize.y / 2, 0, 0, mScreenSize.x, mScreenSize.y};
        iMatrix = new Matrix(matrix);
        iMatrix.invert(iMatrix);
        iMatrix.mapPoints(pts);
        mCenter.set(pts[0], pts[1]);
        mLeftTop.set(pts[2], pts[3]);
        mRightBottom.set(pts[4], pts[5]);
        Log.d("Odr", matrix.toString());
        Log.d("Odr", iMatrix + "\ncenter: " + mCenter.x + "," + mCenter.y +
                "\nlt: " + mLeftTop.x + "," + mLeftTop.y +
                "\nrb: " + mRightBottom.x + "," + mRightBottom.y);
        if (testView != null) testView.setText(getCenterString());
    }

    @SuppressWarnings("deprecation")
        // getWidth() & getHeight() was changed to getSize()
    Point getScreenSize() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point p = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(p);
        } else {
            p.x = display.getWidth();
            p.y = display.getHeight();
        }
        return p;
    }

    String getCenterString() {
        return "{" + (int) mCenter.x + "," + (int) mCenter.y + "}\n"+
                "{" + (int) (mLeftTop.x + mScreenSize.x/2 / mScaleFactor) + "," +
                (int) (mLeftTop.y + mScreenSize.y/2 / mScaleFactor) + "}\n"+
                "{" + (int) (mRightBottom.x - mScreenSize.x/2 / mScaleFactor) + "," +
                (int) (mRightBottom.y - mScreenSize.y/2 / mScaleFactor)+ "}";
    }

    /**
     *
     * @param point pixels of the screen
     * @return pixels on the map
     */

    PointF getPointOnMap(PointF point){
        return new PointF(mLeftTop.x + point.x / mScaleFactor, mLeftTop.y + point.y / mScaleFactor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBmp, mMatrix, null);
    }

}
