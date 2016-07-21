package tk.parmclee.o_droid;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;

public class MapView extends View {
    String mPath;
    float mScaleFactor, mMinFactor;
    Point mMapSize, mScreenSize, mPositionOnMap, mPositionOnScreen;
    PointF mCenter, mLeftTop, mRightBottom; // mapping from screen points to raster map points
    Bitmap mBmp;
    Matrix mMatrix, iMatrix; // "i" means "initial" or "inverse" depends on context
    ScaleGestureDetector mScaleDetector;
    GestureDetector mScrollDetector;
    Paint mPositionPaint, mCPPaint, mTrackPaint, mTextPaint;
    boolean gpsFixed, showTrack, routeMode, hidePos, profiMode, competition;
    Path mTrack, mTransformedTrack, mRoute, mTransformedRoute;
    final int radiusCP = 25, textHeight = 60;
    ArrayList<PointF> cpNumbers, transformedCPNumbers, mRoutePoints;
    float[] numberPoint, transformedNumPt;
    PointF numPoint;

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
        mCPPaint = createPaint("CP");
        mPositionPaint = createPaint("pos");
        mTrackPaint = createPaint("track");
        mTextPaint = createPaint("text");
        mPositionOnScreen = mPositionOnMap = new Point(mScreenSize.x / 2, mScreenSize.y / 2);
        initPaths();
    }

    void initPaths() {
        mTrack = new Path();
        mTransformedTrack = new Path();
        mRoute = new Path();
        mTransformedRoute = new Path();
    }

    Paint createPaint(String what) {
        Paint p = new Paint();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        switch (what) {
            case "text":
                int textColor = parseColor(preferences.getString("CP_color", "red"));
                p.setColor(textColor);
                p.setTextSize(textHeight);
                p.setTextAlign(Paint.Align.CENTER);
                break;
            case "CP":
                int cpColor = parseColor(preferences.getString("CP_color", "red"));
                p.setColor(cpColor);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(5);
                break;
            case "pos":
                int col = parseColor(preferences.getString("track_color", "red"));
                p.setColor(col);
                break;
            case "track":
                int color = parseColor(preferences.getString("track_color", "red"));
                p.setColor(color);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(5);
                break;
            default:
        }
        return p;
    }

    int parseColor(String color) {
        int col = 0;
        switch (color) {
            case "red":
                col = Color.RED;
                break;
            case "green":
                col = Color.GREEN;
                break;
            case "blue":
                col = Color.BLUE;
                break;
            case "black":
                col = Color.BLACK;
                break;
            default:
        }
        return col;
    }

    void addListeners(GestureDetector gesture, ScaleGestureDetector scale) {
        mScaleDetector = scale;
        mScrollDetector = gesture;
    }

    void setCurrentPosition(Point positionOnMap) {
        mPositionOnMap = positionOnMap;
        if (!profiMode) {
            if (!hidePos) {
                if (mTrack.isEmpty()) {
                    mTrack.moveTo(positionOnMap.x, positionOnMap.y);
                } else mTrack.lineTo(positionOnMap.x, positionOnMap.y);
            }
            if (mMatrix != null) {
                calculatePositionOnScreen();
                checkAndMove(mScreenSize.x / 2 - mPositionOnScreen.x,
                        mScreenSize.y / 2 - mPositionOnScreen.y);
            }
            invalidate();
        }
    }

    void calculatePositionOnScreen() {
        float pt[] = {mPositionOnMap.x, mPositionOnMap.y};
        mMatrix.mapPoints(pt);
        mPositionOnScreen.x = Math.round(pt[0]);
        mPositionOnScreen.y = Math.round(pt[1]);
        if (!hidePos) mTransformedTrack.set(mTrack);
    }

    void setScaleFactor(float scale) {
        mScaleFactor = scale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mScrollDetector != null) mScrollDetector.onTouchEvent(event);
        if (mScaleDetector != null) mScaleDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //scaling
        float wRatio = ((float) mScreenSize.x) / ((float) mMapSize.x);
        float hRatio = ((float) mScreenSize.y) / ((float) mMapSize.y);
        mMinFactor = Math.max(wRatio, hRatio);
        Log.d("Odr", "onAttach: " + mScaleFactor + "\n" + mMinFactor);
        if (mScaleFactor < mMinFactor) mScaleFactor = mMinFactor;

        iMatrix.setScale(mScaleFactor, mScaleFactor);

        //calculating new center position
        float newCenter[] = {mMapSize.x / 2, mMapSize.y / 2}; // initial center
        iMatrix.mapPoints(newCenter); // mapped center

        //translating
        float dx = mScreenSize.x / 2 - newCenter[0]; // offset from mapped center to screen center
        float dy = mScreenSize.y / 2 - newCenter[1];
        iMatrix.postTranslate(dx, dy);

        mMatrix = new Matrix(iMatrix);
        transformCoords(mMatrix);
    }

    void transformCoords(final Matrix matrix) {
        if (gpsFixed) calculatePositionOnScreen();
        float pts[] = {mScreenSize.x / 2, mScreenSize.y / 2, 0, 0, mScreenSize.x, mScreenSize.y};
        iMatrix = new Matrix(matrix);
        iMatrix.invert(iMatrix);
        iMatrix.mapPoints(pts);
        mCenter.set(pts[0], pts[1]);
        mLeftTop.set(pts[2], pts[3]);
        mRightBottom.set(pts[4], pts[5]);
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

    /**
     * @param point pixels of the screen
     * @return pixels on the map
     */
    PointF getPointOnMap(PointF point) {
        return new PointF(mLeftTop.x + point.x / mScaleFactor, mLeftTop.y + point.y / mScaleFactor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBmp, mMatrix, null);
        if (routeMode) {
            if (mRoutePoints != null) {
                drawRoute(mRoutePoints);
                if (mRoutePoints.size() > 1) mRoute.addCircle(
                        (mRoutePoints.get(mRoutePoints.size() - 1)).x,
                        (mRoutePoints.get(mRoutePoints.size() - 1)).y,
                        (float) (0.7 * radiusCP / mScaleFactor), Path.Direction.CW);
            }
            mTransformedRoute.set(mRoute);
            mTransformedRoute.transform(mMatrix);
            canvas.drawPath(mTransformedRoute, mCPPaint);
            if (cpNumbers != null && cpNumbers.size() != 0) {
                transformCP(cpNumbers);
                for (int i = 0; i < transformedCPNumbers.size(); i++) {
                    numPoint = transformedCPNumbers.get(i);
                    canvas.drawText(String.valueOf(i + 1), numPoint.x,
                            (float) (numPoint.y + textHeight / 2.82), // don't know why exactly 2.82
                            mTextPaint);
                }
            }
        }
        if (gpsFixed && mPositionOnMap != null && !hidePos) {
            canvas.drawCircle(mPositionOnScreen.x, mPositionOnScreen.y, 10, mPositionPaint);
            if (showTrack) {
                mTransformedTrack.transform(mMatrix);
                canvas.drawPath(mTransformedTrack, mTrackPaint);
            }
        }
    }

    void checkAndMove(float dx, float dy) {
        if ((mLeftTop.x - dx) < 0) dx = mLeftTop.x;
        if ((mRightBottom.x - dx) > mMapSize.x)
            dx = mRightBottom.x - mMapSize.x;
        if ((mLeftTop.y - dy) < 0) dy = mLeftTop.y;
        if ((mRightBottom.y - dy) > mMapSize.y)
            dy = mRightBottom.y - mMapSize.y;
        mMatrix.postTranslate(dx, dy);
        transformCoords(mMatrix);
    }

    public void createRoute(ArrayList<PointF> mRouteList) {
        switch (mRouteList.size()) {
            case 1:
                drawTriangle(mRouteList.get(0), 2 * radiusCP / mScaleFactor, 90, mRoute);
                break;
            case 2:
                drawFirstCP(mRouteList);
                break;
            default:
                drawNextCP(mRouteList);
        }
        invalidate();
    }

    /**
     * @param center cross of medians
     * @param size   edge length
     * @param angle  between one median and x-axe, degrees
     * @param path   where to draw
     */
    void drawTriangle(PointF center, float size, double angle, Path path) {
        final double pi = Math.PI, sqrt3 = Math.sqrt(3);
        double angleRadians = angle * pi / 180;
        float xA = (float) (center.x + size * Math.cos(angleRadians) * sqrt3 / 3);
        float yA = (float) (center.y - size * Math.sin(angleRadians) * sqrt3 / 3);
        float xB = (float) (center.x - size * Math.sin(pi / 6 - angleRadians) * sqrt3 / 3);
        float yB = (float) (center.y + size * Math.cos(pi / 6 - angleRadians) * sqrt3 / 3);
        float xC = (float) (center.x - size * Math.sin(pi / 6 + angleRadians) * sqrt3 / 3);
        float yC = (float) (center.y - size * Math.cos(pi / 6 + angleRadians) * sqrt3 / 3);
        path.moveTo(xA, yA);
        path.lineTo(xB, yB);
        path.lineTo(xC, yC);
        path.close();
    }

    void drawFirstCP(ArrayList<PointF> mRouteList) {
        mRoute = new Path();
        PointF start = mRouteList.get(0);
        PointF firstPoint = mRouteList.get(1);
        double angle = Math.atan2(firstPoint.y - start.y, firstPoint.x - start.x);
        drawTriangle(start, 2 * radiusCP / mScaleFactor, -180 * angle / Math.PI, mRoute);
        float x = (float) (firstPoint.x - radiusCP / mScaleFactor * Math.cos(angle));
        float y = (float) (firstPoint.y - radiusCP / mScaleFactor * Math.sin(angle));
        mRoute.lineTo(x, y);
        mRoute.addCircle(firstPoint.x, firstPoint.y, radiusCP / mScaleFactor, Path.Direction.CW);
        cpNumbers = new ArrayList<>();
        transformedCPNumbers = new ArrayList<>();
        // initialize transient number point
        numPoint = new PointF();
        numberPoint = new float[2];
        transformedNumPt = new float[2];
    }

    private void drawNextCP(ArrayList<PointF> mRouteList) { // call this from the second CP
        PointF previous = mRouteList.get(mRouteList.size() - 3);
        PointF current = mRouteList.get(mRouteList.size() - 2);
        PointF next = mRouteList.get(mRouteList.size() - 1);
        PointF result = calculateNumberPoint(previous, current, next);
        cpNumbers.add(result);
        transformedCPNumbers.add(new PointF(result.x, result.y));
        double angle = Math.atan2(next.y - current.y, next.x - current.x);
        float x0 = (float) (current.x + radiusCP / mScaleFactor * Math.cos(angle));
        float y0 = (float) (current.y + radiusCP / mScaleFactor * Math.sin(angle));
        float x1 = (float) (next.x - radiusCP / mScaleFactor * Math.cos(angle));
        float y1 = (float) (next.y - radiusCP / mScaleFactor * Math.sin(angle));
        mRoute.moveTo(x0, y0);
        mRoute.lineTo(x1, y1);
        mRoute.addCircle(next.x, next.y, radiusCP / mScaleFactor, Path.Direction.CW);
    }

    /**
     * Position of CP number to be drawn
     *
     * @param previous CP
     * @param current  CP
     * @param next     CP
     * @return PointF on Map
     */
    private PointF calculateNumberPoint(PointF previous, PointF current, PointF next) {
        // two segments: between prev & cur and cur & next
        // alphas are angles of segments
        double alpha0 = Math.atan2(previous.y - current.y, previous.x - current.x);
        double alpha2 = Math.atan2(next.y - current.y, next.x - current.x);
        // who has made atan2?
        if (alpha0 < 0) alpha0 = -alpha0;
        else alpha0 = 2 * Math.PI - alpha0;
        if (alpha2 < 0) alpha2 = -alpha2;
        else alpha2 = 2 * Math.PI - alpha2;
        double alpha = (alpha0 + alpha2) / 2;
        if (Math.abs(alpha0 - alpha2) < Math.PI) alpha += Math.PI;
        return new PointF((float) (current.x + 2.5 * radiusCP / mScaleFactor * Math.cos(alpha)),
                (float) (current.y - 2.5 * radiusCP / mScaleFactor * Math.sin(alpha))); // "-" because y-axe down
    }

    private void transformCP(ArrayList<PointF> list) {
        for (int i = 0; i < list.size(); i++) {
            numberPoint[0] = list.get(i).x;
            numberPoint[1] = list.get(i).y;
            mMatrix.mapPoints(transformedNumPt, numberPoint);
            transformedCPNumbers.get(i).x = transformedNumPt[0];
            transformedCPNumbers.get(i).y = transformedNumPt[1];
        }
    }

    void drawRoute(ArrayList<PointF> list) {
        mRoutePoints = list;
        ArrayList<PointF> route = new ArrayList<>(list.size());
        route.add(list.get(0));
        if (list.size() == 1) {
            drawTriangle(list.get(0), 2 * radiusCP / mScaleFactor, 90, mRoute);
            return;
        }
        route.add(list.get(1));
        drawFirstCP(route);
        if (list.size() == 2) return;
        for (int i = 2; i < list.size(); i++) {
            route.add(i, list.get(i));
            drawNextCP(route);
        }
    }
}
