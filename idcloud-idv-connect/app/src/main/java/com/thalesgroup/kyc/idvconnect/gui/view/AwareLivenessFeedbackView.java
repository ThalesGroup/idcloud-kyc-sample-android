package com.thalesgroup.kyc.idvconnect.gui.view;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.aware.face_liveness.api.interfaces.CanvasUpdateCallback;
import com.aware.face_liveness.api.interfaces.DevicePositionCallback;
import com.aware.face_liveness.api.interfaces.FeedbackCallback;

import com.thalesgroup.kyc.idvconnect.gui.fragment.AwareLivenessFragment;


public class AwareLivenessFeedbackView implements FeedbackCallback, DevicePositionCallback, CanvasUpdateCallback {
    private static final String CUSTOM_VIEW_TAG = "custom_view_tag";
    private static final int BACKGROUND_ALPHA = 200;
    private static final float sTextSizeBase = 40.f;
    private static final int   GAP = 10;

    private  float mDensityMultiplier = 0.0f;

    //private Paint mCommandPaint;  // draws text
    private Paint mOvalPaint;  // draws the oval outline
    private Paint mPorterDuffPaint;
    private float mLastPositionState = 0.0f;
    private Activity mActivity;
    private AwareLivenessFragment ui;
    private int mLastBoxId = 0;
    private int mSelectedBox = -1;
    final private float mOvalHeightScale = 0.85f;
    private Paint mCommandPaint;
    private Rect mCommandTextBounds;
    private int mBoxId;
    private Canvas mCanvas;
    private RectF mAreadOfInterest;
    private float mDisplayWidthScale;
	private int mPositionMode = 1;

    public AwareLivenessFeedbackView(final Activity activity, AwareLivenessFragment act) {
        mDensityMultiplier = activity.getResources().getDisplayMetrics().density;
        mActivity = activity;

        ui = act;
        ui.setCanvasUpdateCallback(this);
    }


    private int getBoxId(float positionIndicator) {
        int boxId = 0;

        if (positionIndicator < -0.85  && positionIndicator >= -1.0) {
            boxId = 0;
        }
        else if (positionIndicator < -0.70 && positionIndicator >= -0.85) {
            boxId = 1;
        }
        else if (positionIndicator < -0.55 && positionIndicator >= -0.70) {
            boxId = 2;
        }
        else if (positionIndicator < -0.40 && positionIndicator >= -0.55) {
            boxId = 3;
        }
        // Center:
        // NOTE: Center means device is in position
        else if (positionIndicator > -0.40 && positionIndicator <= 0.40) {
            boxId = 4;
        }
        else if (positionIndicator >= 0.40 && positionIndicator < 0.55) {
            boxId = 5;
        }
        else if (positionIndicator >= 0.55 && positionIndicator < 0.70) {
            boxId = 6;
        }
        else if (positionIndicator >= 0.70 && positionIndicator < 0.85) {
            boxId = 7;
        }
        else if (positionIndicator >= 0.85 && positionIndicator <= 1.0) {
            boxId = 8;
        }

        mSelectedBox = boxId;
        return boxId;
    }

    private void reportValues(final float positionIndicator) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int boxId = getBoxId(positionIndicator);
                if (boxId != mLastBoxId) {
                    mLastBoxId = boxId;
                    mBoxId = boxId;
                    ui.reportOrietnationValues(boxId );
                }
            }
        });
    }


    @Override
    public void onCanvasUpdateCallback(Canvas canvas, RectF areaOfInterest, float displayWidthScale) {
        mCanvas = canvas;
        mAreadOfInterest = areaOfInterest;
        mDisplayWidthScale = displayWidthScale;
    }

    @Override
    public void onDevicePositionCallback(float positionState) {
        //!!TODO: fill in device indicator
      //  Log.v(CUSTOM_VIEW_TAG, "onDevicePositionCallback positionState: " + positionState);
        mLastPositionState = positionState;
        reportValues(mLastPositionState);

    }

    @Override
    public void onFeedbackResultCallback(FeedbackResult feedbackResult) {

        //!!TODO: update the canvas
        Log.v(CUSTOM_VIEW_TAG, "onFeedbackResultCallback feedbackResult: " + feedbackResult.getFeedback());
        updateFeedback(feedbackResult);

    }

    private void updateFeedback(final FeedbackResult feedbackResult) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mOvalPaint == null) {
                    initializePaint();
                }

                updateTextDirections(feedbackResult);

                updateFaceLocationDisplay(feedbackResult);
            }
        });
    }

    private void updateFaceLocationDisplay(FeedbackResult feedbackResult) {
        float raceTrackScale = 1.0f;

        drawRaceTrackNew(feedbackResult, raceTrackScale, false, BACKGROUND_ALPHA);
    }

    private void drawRaceTrackNew(FeedbackResult feedbackResult, float scaleValue, boolean useColor, int backgroundAlpha) {
        Canvas canvas = mCanvas;
        RectF initialAOI = mAreadOfInterest;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        int color= Color.GREEN;;

        RectF aoi = new RectF();

        aoi = mAreadOfInterest;

        if (useColor == false) {
            int positionMode = mPositionMode;

            boolean greenOval = false;
            switch(positionMode)
            {
                case 1:
                    greenOval = feedbackResult.isCompliant() && (mSelectedBox == 4);
                    break;
                case 2:
                    greenOval = feedbackResult.isCompliant() && (mSelectedBox >= 3 && mSelectedBox <= 5);
                    break;
                case 3:
                    greenOval = feedbackResult.isCompliant() && (mSelectedBox >= 2 && mSelectedBox <= 6);
                    break;
                case 4:
                    greenOval = feedbackResult.isCompliant() && (mSelectedBox >= 1 && mSelectedBox <= 7);
                    break;
                case 5:
                    greenOval = feedbackResult.isCompliant() && (mSelectedBox >= 0 && mSelectedBox <= 8);
                    break;
            }
            color =  greenOval ? Color.GREEN : Color.RED;
        }
        else {
            if (feedbackResult.getFeedback().toString().contains("FAR"))
                mOvalPaint.setColor(Color.BLUE);
            else if (feedbackResult.getFeedback().toString().contains("CLOSE"))
                mOvalPaint.setColor(Color.RED);
            else if (feedbackResult.isCompliant())
                mOvalPaint.setColor(Color.GREEN);
            else {

                if (mBoxId == 0 || mBoxId == 8)
                    color = Color.RED;
                if (mBoxId == 1 || mBoxId == 7)
                    color = Color.MAGENTA;
                if (mBoxId == 2 || mBoxId == 6)
                    color = Color.YELLOW;
                if (mBoxId == 3 || mBoxId == 5)
                    color = Color.CYAN;
                if (mBoxId == 4)
                    color = Color.GREEN;
            }
        }

        paintRaceTrack(canvas,aoi,color,backgroundAlpha);
    }

    private void paintRaceTrack(Canvas canvas, RectF aoi, int color, int backgroundAlpha) {

        int alpha = mOvalPaint.getAlpha();
        mOvalPaint.setColor(Color.GRAY);
        mOvalPaint.setAlpha(backgroundAlpha);
        mOvalPaint.setStyle(Paint.Style.FILL);

        canvas.drawRect(0,0,canvas.getWidth(), canvas.getHeight(),mOvalPaint);

        //
        // Clear the inner part of the racetrack
        //

        if (mPorterDuffPaint == null)
            mPorterDuffPaint = new Paint();

        mPorterDuffPaint.setAlpha(alpha);
        mPorterDuffPaint.setStyle(Paint.Style.FILL);
        mPorterDuffPaint.setColor(Color.TRANSPARENT);
        mPorterDuffPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));

        RectF topOval = new RectF(aoi.left, aoi.top, aoi.right, aoi.top + aoi.width());
        canvas.drawArc(topOval, 0, -180, false, mPorterDuffPaint);

        topOval.top = aoi.top + aoi.width()/2;
        topOval.bottom = aoi.bottom - aoi.width()/2;
        canvas.drawRect(topOval,mPorterDuffPaint);

        RectF bottomOval = new RectF(aoi.left, aoi.bottom - aoi.width(), aoi.right, aoi.bottom);
        canvas.drawArc(bottomOval, 0, 180, false, mPorterDuffPaint);

        //
        // Draw the racetrack outline
        //
        mOvalPaint.setAlpha(alpha);
        mOvalPaint.setColor(color);
        mOvalPaint.setStyle(Paint.Style.STROKE);
        topOval = new RectF(aoi.left, aoi.top, aoi.right, aoi.top + aoi.width());

        canvas.drawArc(topOval, 0, -180, false, mOvalPaint);

        canvas.drawLine(aoi.left,aoi.top + (aoi.width())/2, aoi.left, aoi.bottom - (aoi.width())/2, mOvalPaint);
        canvas.drawLine(aoi.right,aoi.top + (aoi.width())/2, aoi.right, aoi.bottom - (aoi.width())/2, mOvalPaint);

        bottomOval = new RectF(aoi.left, aoi.bottom - aoi.width(), aoi.right, aoi.bottom);
        canvas.drawArc(bottomOval, 0, 180, false, mOvalPaint);

    }

    private void updateTextDirections(FeedbackResult feedbackResult) {
        com.aware.face_liveness.api.FaceLiveness.AutoFeedback command = null;
        command = feedbackResult.getFeedback();

        // Display feedback
        final com.aware.face_liveness.api.FaceLiveness.AutoFeedback finalCommand = command;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ui.reportFeedback(finalCommand);
            }
        });

        Log.v(CUSTOM_VIEW_TAG, "Display feedback: " + command);

    }

    private void initializePaint() {
        mOvalPaint = new Paint(Paint.LINEAR_TEXT_FLAG);
        mOvalPaint.setColor(Color.RED);
        mOvalPaint.setStyle(Paint.Style.STROKE);
        mOvalPaint.setStrokeWidth(10.0f);
        mOvalPaint.setAlpha(192);

        mCommandPaint = new Paint(Paint.LINEAR_TEXT_FLAG);
        mCommandPaint.setColor(Color.BLACK);
        mCommandPaint.setStyle(Paint.Style.FILL);
        mCommandPaint.setTextSize(sTextSizeBase * mDensityMultiplier);

        mCommandTextBounds = new Rect();
        mCommandPaint.getTextBounds("a", 0, 1, mCommandTextBounds);

    }

}
