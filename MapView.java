package tk.parmclee.o_droid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Arrays;

public class MapView extends View {
    String mPath;
    float mScaleFactor, mMaxFactor;
    Point mCenter, mMapSize, mScreenSize;
    Bitmap mBmp;
    Matrix mInitialMatrix, mMatrix;
    ScaleGestureDetector mScaleDetector;
    GestureDetector mScrollDetector;

    public MapView(Context context) {
        super(context);
    }

    public MapView(Context context, String path) {
        this(context);
        mPath = path;
        mScaleFactor = 1;
        mScreenSize = getScreenSize();
        mBmp = BitmapFactory.decodeFile(path);
        if (mBmp == null) Toast.makeText(context, "Bmp - nill " + path, Toast.LENGTH_LONG).show();
        int width = mBmp.getWidth();
        int height = mBmp.getHeight();
        mCenter = new Point(width / 2, height / 2);
        mMapSize = new Point(width, height);
        mInitialMatrix = new Matrix();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mScrollDetector = new GestureDetector(context, new ScrollListener());
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
        mMaxFactor = mScaleFactor = Math.max(wRatio, hRatio);
        Log.d("Odr", "onAttach\n" + mInitialMatrix.toString()+ mScaleFactor + "\n"+wRatio+"\n"+hRatio);
        mInitialMatrix.setScale(mScaleFactor, mScaleFactor);

        //calculating center
        centerMoved(mInitialMatrix);

        //translating
        int dx = mScreenSize.x/2 - mCenter.x;
        int dy = mScreenSize.y/2 - mCenter.y;
        mInitialMatrix.postTranslate(dx, dy);
        Log.d("Odr", "dxy: " + dx + " " + dy);

        mMatrix = new Matrix(mInitialMatrix);
    }

    void centerMoved(Matrix matrix){
        float pt[] = {mMapSize.x/2, mMapSize.y/2};
        Log.d("Odr", "pt: " + Arrays.toString(pt));
        matrix.mapPoints(pt);
        Log.d("Odr", "pt: " + Arrays.toString(pt));
        mCenter.x = (int) pt[0];
        mCenter.y = (int) pt[1];
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

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.save();
        //canvas.scale(mScaleFactor, mScaleFactor);
        canvas.drawBitmap(mBmp, mMatrix, null);
        //canvas.restore();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(mMaxFactor, Math.min(mScaleFactor, 10f));
            mMatrix.reset();
            mMatrix.setScale(mScaleFactor, mScaleFactor);
            centerMoved(mMatrix);
            invalidate();
            Log.d("Odr", "scaleListener" );
            return true;
        }
    }

    private class ScrollListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mMatrix.postTranslate(-distanceX, -distanceY);
            centerMoved(mMatrix);
            invalidate();//TODO handle borders of translation and center behaviour
            return true;
        }
    }
}
