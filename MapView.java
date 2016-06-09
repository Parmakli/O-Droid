package tk.parmclee.o_droid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

public class MapView extends View {
    String mPath;
    float mScaleFactor;
    Point mCenter, mMapSize;
    Bitmap mBmp;
    Matrix mInitialMatrix;

    public MapView(Context context) {
        super(context);
    }

    public MapView(Context context, String path) {
        this(context);
        mPath = path;
        mScaleFactor = 1;
        mBmp = BitmapFactory.decodeFile(path);
        int width = mBmp.getWidth();
        int height = mBmp.getHeight();
        mCenter = new Point(width / 2, height / 2);
        mMapSize = new Point(width, height);
        mInitialMatrix = new Matrix();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Point screenSize = getScreenSize();
        RectF displayRect = new RectF(0, 0, screenSize.x, screenSize.y);
        RectF mapRect = new RectF(0, 0, mMapSize.x, mMapSize.y);
        mInitialMatrix.setRectToRect(displayRect, mapRect, Matrix.ScaleToFit.CENTER);
        mInitialMatrix.invert(mInitialMatrix);
        Log.d("Odr", "onAttach\n"+mInitialMatrix.toString()+"\n"+mapRect.toString()+"\n"+displayRect.toString());
    }

    @SuppressWarnings("deprecation") // getWidth() & getHeight() was changed to getSize()
    Point getScreenSize(){
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point p = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(p);
        }else {
            p.x = display.getWidth();
            p.y = display.getHeight();            
        }
        return p;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("Odr", "onDraw");
        canvas.drawBitmap(mBmp, mInitialMatrix, null);
    }
}
