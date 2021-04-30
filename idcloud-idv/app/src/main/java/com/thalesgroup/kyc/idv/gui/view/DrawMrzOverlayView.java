package com.thalesgroup.kyc.idv.gui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DrawMrzOverlayView extends View {
    private final Handler handler = new Handler();
    private final Runnable mRunnableClearPoints;
    private final Runnable mRunnableClearContours;

    private final Path mPath;
    private final Path mContoursPath;

    private final Paint mBorderPaint;
    private final Paint mFillPaint;
    private final Paint mBorderContours;

    private final AtomicBoolean mInvalidateAlreadyFired;

    private final AtomicInteger mWidth;
    private final AtomicInteger mHeight;
    private final Rect mQuadrangle;

    private final int mTimeoutDraw = 1000;

    // To customizer
    // MRZ
    // -- common
    private final float mStroke    = 10.0f;
    private final int mPathRadius  = 10;
    // -- outer
    private final int mBorderColor = 0xFF0000FF;
    private final int mPadding     = 20;
    // -- inner
    private final int mFillColor   = 0x939DFFCE;
    private final int mAlpha       = 100;

    public DrawMrzOverlayView(Context context) {
        this(context, null);
    }

    public DrawMrzOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawMrzOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DrawMrzOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mPath = new Path();
        mContoursPath = new Path();

        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mStroke);
        mBorderPaint.setStrokeJoin(Paint.Join.ROUND);
        mBorderPaint.setStrokeCap(Paint.Cap.ROUND);
        mBorderPaint.setPathEffect(new CornerPathEffect(mPathRadius));
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setDither(true);

        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(mFillColor);
        mFillPaint.setAlpha(mAlpha);
        mFillPaint.setAntiAlias(true);
        mFillPaint.setDither(true);

        mBorderContours = new Paint();
        mBorderContours.setStyle(Paint.Style.FILL_AND_STROKE);
        mBorderContours.setStrokeWidth(1.0f);
        mBorderContours.setStrokeJoin(Paint.Join.ROUND);
        mBorderContours.setStrokeCap(Paint.Cap.ROUND);
        mBorderContours.setPathEffect(new CornerPathEffect(1));
        mBorderContours.setColor(0xFF00FF00);
        mBorderContours.setAntiAlias(true);
        mBorderContours.setDither(true);

        mWidth = new AtomicInteger(0);
        mHeight = new AtomicInteger(0);

        mInvalidateAlreadyFired = new AtomicBoolean(false);

        mQuadrangle = new Rect();

        mRunnableClearPoints = new Runnable() {
            @Override
            public void run() {
                clearPoints();
            }
        };

        mRunnableClearContours = new Runnable() {
            @Override
            public void run() {
                clearContours();
            }
        };
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO: Manage sizes to avoid specific ones. It always must be parent size (match_parent)
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        mWidth.set(widthSize);
        mHeight.set(heightSize);
        mQuadrangle.set(0, 0, widthSize, heightSize);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!mPath.isEmpty()) {
            canvas.drawPath(mPath, mFillPaint);
            canvas.drawPath(mPath, mBorderPaint);
        }

        if (!mContoursPath.isEmpty()) {
            canvas.drawPath(mContoursPath, mBorderContours);
        }

        mInvalidateAlreadyFired.set(false);
    }

    public void drawPoints(
            final PointF topLeft, final PointF topRight,
            final PointF bottomLeft, final PointF bottomRight
    ) {
        Objects.requireNonNull(topLeft, "topLeft must have a value");
        Objects.requireNonNull(topRight, "topRight must have a value");
        Objects.requireNonNull(bottomLeft, "bottomLeft must have a value");
        Objects.requireNonNull(bottomRight, "bottomRight must have a value");

        if (isEmpty(topLeft, topRight, bottomLeft, bottomRight)) {
            cleanPath(mPath);
            return;
        }

        handler.removeCallbacks(mRunnableClearPoints);

        post(new Runnable() {
            @Override
            public void run() {

                PointF[] withPadding = padding(normalize(topLeft), normalize(topRight), normalize(bottomLeft), normalize(bottomRight));
                mPath.rewind();
                mPath.moveTo(withPadding[0].x, withPadding[0].y); // topLeft
                mPath.lineTo(withPadding[1].x, withPadding[1].y); // topRight
                mPath.lineTo(withPadding[3].x, withPadding[3].y); // bottomRight
                mPath.lineTo(withPadding[2].x, withPadding[2].y); // bottomLeft
                mPath.lineTo(withPadding[0].x, withPadding[0].y); // topLeft
                mPath.close();

                reDraw();

                handler.postDelayed(mRunnableClearPoints, mTimeoutDraw);
            }
        });
    }

    public void drawContours(final ArrayList<ArrayList<PointF>> contours){

        if(contours != null) {

            handler.removeCallbacks(mRunnableClearContours);

            post(new Runnable() {
                @Override
                public void run() {

                    mContoursPath.rewind();

                    for (ArrayList<PointF> contourPoints : contours) {

                        boolean firstItem = true;
                        for (PointF pointF : contourPoints) {

                            PointF normPoint = normalize(pointF);

                            if (firstItem) {
                                firstItem = false;

                                mContoursPath.moveTo(normPoint.x, normPoint.y);
                            } else {
                                mContoursPath.lineTo(normPoint.x, normPoint.y);
                            }

                        }
                        mContoursPath.close();
                    }

                    reDraw();

                    handler.postDelayed(mRunnableClearContours, mTimeoutDraw);
                }
            });
        }
    }

    public void clear() {
        clearPoints();
        clearContours();
    }

    public void clearPoints() {
        cleanPath(mPath);
    }

    public void clearContours() {
        cleanPath(mContoursPath);
    }

    private PointF normalize(final PointF point) {

        float x = point.x * mWidth.get();
        float y = point.y * mHeight.get();

        return new PointF(x, y);
    }

    private PointF[] padding(
            final PointF topLeft, final PointF topRight,
            final PointF bottomLeft, final PointF bottomRight
    ) {
        float pTopLeftX = topLeft.x - mPadding;
        float pTopLeftY = topLeft.y - mPadding;

        float pTopRightX = topRight.x + mPadding;
        float pTopRightY = topRight.y - mPadding;

        float pBottomLeftX = bottomLeft.x - mPadding;
        float pBottomLeftY = bottomLeft.y + mPadding;

        float pBottomRightX = bottomRight.x + mPadding;
        float pBottomRightY = bottomRight.y + mPadding;

        PointF fTopLeft     = new PointF(pTopLeftX, pTopLeftY);
        PointF fTopRight    = new PointF(pTopRightX, pTopRightY);
        PointF fBottomLeft  = new PointF(pBottomLeftX, pBottomLeftY);
        PointF fBottomRight = new PointF(pBottomRightX, pBottomRightY);

        return new PointF[] {
                fTopLeft,
                fTopRight,
                fBottomLeft,
                fBottomRight
        };
    }

    public boolean isEmpty(
            final PointF topLeft, final PointF topRight,
            final PointF bottomLeft, final PointF bottomRight
    ) {
        return (
                topLeft.x == 0 && topLeft.y == 0 &&
                        topRight.x == 0 && topRight.y == 0 &&
                        bottomLeft.x == 0 && bottomLeft.y == 0.0f &&
                        bottomRight.x == 0.0f && bottomRight.y == 0.0f
        );
    }

    private void cleanPath(Path path) {
        if(path != null) {
            path.rewind();
            reDraw();
        }
    }

    private void reDraw(){
        if(!mInvalidateAlreadyFired.get()){
            post(new Runnable() {
                @Override
                public void run() {
                    invalidate();
                }
            });
            mInvalidateAlreadyFired.set(true);
        }
    }
}