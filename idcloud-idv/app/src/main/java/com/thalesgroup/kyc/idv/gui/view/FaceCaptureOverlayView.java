/*
 * MIT License
 *
 * Copyright (c) 2020 Thales DIS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * IMPORTANT: This source code is intended to serve training information purposes only.
 *            Please make sure to review our IdCloud documentation, including security guidelines.
 */

package com.thalesgroup.kyc.idv.gui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.os.ConditionVariable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.WindowManager;

import com.gemalto.ekyc.face_capture.FaceCaptureInfo;
import com.gemalto.ekyc.face_capture.FaceQualityCheckWarnings;
import com.thalesgroup.kyc.idv.helpers.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;


@SuppressWarnings("unused")
public class FaceCaptureOverlayView extends TextureView implements SurfaceTextureListener  {

    private static final Matrix IMAGE_TRANSFORMATION_MATRIX = null;
    private static final int ROTATION = 0;

    private static final String TAG = "FaceOverlayView";
    private static final boolean DEBUG = false;
    private int mIcaoArrowsColor = Constants.DEFAULT_QUALITY_CHECK_ARROWS_COLOR;
    private int mIcaoArrowsWidth = Constants.DEFAULT_QUALITY_CHECK_ARROWS_STROKE_WIDTH;
    private boolean mShowIcaoArrows = Constants.DEFAULT_SHOW_ICAO_ARROWS;
    private boolean mShowIcaoText = Constants.DEFAULT_SHOW_ICAO_ARROWS;

    private Map<FaceQualityCheckWarnings, String> mFaceQualityCheckWarningMap;
    private int mRegionWidth;
    private int mRegionHeight;
    private int mOrientation = 0;
    private Display mDisplay;
    private FaceCaptureInfo mCaptureInfo;
    private int mImageRotateFlipType;
    private Matrix mAttributeMatrix = null;
    private final Object lockObject = new Object();
    private OrientationEventListener mOrientationListener;
    private DrawTool drawTool;

    RectF parentBounds = new RectF();
    RectF imageBounds = new RectF();
    RectF rotatedImageBounds = new RectF();
    RectF childBoundsF = new RectF();
    RectF rotatedScaledChildLayout = new RectF();
    Matrix layoutMatrix = new Matrix();
    Matrix attributesViewTransformationMatrix = new Matrix();
    Matrix imageViewTransformationMatrix = new Matrix();

    // ===========================================================
    // Public constructors
    // ===========================================================

    public FaceCaptureOverlayView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        initComponents(context);
    }

    public FaceCaptureOverlayView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initComponents(context);
    }

    public FaceCaptureOverlayView(final Context context) {
        super(context);
        initComponents(context);
    }

    // ===========================================================
    // Entry point
    // ===========================================================

    public void setRegionSize(final int width, final int height) {
        if(mRegionWidth != width || mRegionHeight != height) {
            mRegionWidth = width;
            mRegionHeight = height;

            post(() -> requestLayout());
        }
    }

    public void update(final FaceCaptureInfo info)
    {
        mCaptureInfo = info;
        drawTool.updateCaptureInfo(info);
        internalUpdate();
    }

    // ===========================================================
    // Private methods
    // ===========================================================

    private void initComponents(final Context context) {
        mOrientationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(final int rotation) {
                int newRotation;
                if (rotation >= 315 || rotation >= 0 && rotation < 45) {
                    newRotation = 0;
                } else if (rotation >= 45 && rotation < 135) {
                    newRotation = 90;
                } else if (rotation >= 135 && rotation < 225) {
                    newRotation = 180;
                } else { //if (rotation >= 225 && rotation < 315) {
                    newRotation = 270;
                }

                if (newRotation != mOrientation) {
                    mOrientation = newRotation;
//					updateView();
                }
            }
        };

        drawTool = new DrawTool(context, this);

        mFaceQualityCheckWarningMap = new HashMap<>();
        {
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.FACE_NOT_DETECTED, "Face not detected");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.EYES_CLOSED, "Eyes closed");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.MOUTH_OPEN, "Mouth open");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.LOOKING_AWAY, "Looking away");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.GLASSES_REFLECTION, "Glasses reflection");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.ROLL_LEFT, "Roll left");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.ROLL_RIGHT, "Roll right");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.YAW_LEFT, "Yaw left");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.YAW_RIGHT, "Yaw right");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.PITCH_UP, "Pitch up");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.PITCH_DOWN, "Pitch down");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.TOO_NEAR, "Too near");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.TOO_FAR, "Too far");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.TOO_NORTH, "Too north");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.TOO_SOUTH, "Too south");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.TOO_WEST, "Too west");
            mFaceQualityCheckWarningMap.put(FaceQualityCheckWarnings.TOO_EAST, "Too east");
        }

        final WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = windowManager != null ? windowManager.getDefaultDisplay() : null;

        initComponents2();
    }


    private int displayRotationToAngle(final int displayRotation) {
        int angle;
        switch (displayRotation) {
            case Surface.ROTATION_0:
                angle = 0;
                break;
            case Surface.ROTATION_90:
                angle = 90;
                break;
            case Surface.ROTATION_180:
                angle = 180;
                break;
            case Surface.ROTATION_270:
                angle = 270;
                break;
            default:
                throw new AssertionError("Not recognised display rotation");
        }
        return angle;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mOrientationListener.enable();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mOrientationListener.disable();
    }

    @Override
    public void onLayout(final boolean changed, final int xLeftCord, final int yTopCord, final int xRightCord, final int yBottomCord) {
        if (DEBUG) {
            Log.i(TAG, "layoutSurface");
        }

        float scale;
        final boolean mirroredY = drawTool.isFlipY(mImageRotateFlipType);
        int rotation = getRotation(mImageRotateFlipType);
        int rotationFix;

        rotationFix = displayRotationToAngle(mDisplay.getRotation());

        if (mirroredY) {
            rotationFix = (360 - rotationFix) % 360;
        }

        rotation = rotation - rotationFix;
        parentBounds.set(xLeftCord, yTopCord, xRightCord, yBottomCord);
        imageBounds.set(0, 0, mRegionWidth, mRegionHeight);

        layoutMatrix.postRotate(rotation, imageBounds.width() / 2, imageBounds.height() / 2);
        layoutMatrix.mapRect(rotatedImageBounds, imageBounds);

        if (imageBounds.width() != 0 && imageBounds.height() != 0) {
            scale = Math.min( parentBounds.width() / rotatedImageBounds.width(), parentBounds.height() / rotatedImageBounds.height());
        } else {
            scale = 1;
        }

        layoutMatrix.postScale(scale, scale);
        layoutMatrix.mapRect(childBoundsF, imageBounds);

        attributesViewTransformationMatrix.postRotate(rotation, imageBounds.width() / 2, imageBounds.height() / 2);
        attributesViewTransformationMatrix.postTranslate((rotatedImageBounds.width() - imageBounds.width()) / 2, (rotatedImageBounds.height() - imageBounds.height()) / 2);
        attributesViewTransformationMatrix.postScale(scale, scale);

        if(mirroredY) {
            attributesViewTransformationMatrix.postConcat(drawTool.getMirrorMatrix());
            attributesViewTransformationMatrix.postTranslate(childBoundsF.width(), 0);
        }
        mAttributeMatrix = attributesViewTransformationMatrix;
    }

    // ===========================================================
    // Public methods
    // ===========================================================

    /**
     * Gets liveness text color.
     * @return Color code.
     */
    public int getLivenessTextColor() {

        return drawTool.getLivenessTextColor();
    }


    /**
     * Sets liveness text color.
     *
     * @param livenessTextColor Color code.
     */
    public void setLivenessTextColor(final int livenessTextColor) {
        final int oldLivenessTextColor = getLivenessTextColor();
        if (oldLivenessTextColor != livenessTextColor) {
            drawTool.setLivenessTextColor(livenessTextColor);
            update(mCaptureInfo);
        }
    }

    public boolean isShowIcaoArrows() {
        return mShowIcaoArrows;
    }

    public void setShowIcaoArrows(final boolean showIcaoArrows) {
        final boolean oldShowIcaoArrows = isShowIcaoArrows();
        mShowIcaoArrows = showIcaoArrows;
        if (showIcaoArrows != oldShowIcaoArrows) {
            update(mCaptureInfo);
        }
    }

    public boolean isShowIcaoTextWarnings() {
        return mShowIcaoText;
    }

    public void setShowIcaoTextWarnings(final boolean showIcaoTextWarnings) {
        final boolean oldShowIcaoTextWarnings = isShowIcaoTextWarnings();
        mShowIcaoText = showIcaoTextWarnings;
        if (showIcaoTextWarnings != oldShowIcaoTextWarnings) {
            update(mCaptureInfo);
        }
    }

    public int getIcaoArrowsColor() {
        return mIcaoArrowsColor;
    }

    public void setIcaoArrowsColor(final int icaoArrowsColor) {
        final int oldIcaoArrowsColor = getIcaoArrowsColor();
        mIcaoArrowsColor = icaoArrowsColor;
        if (icaoArrowsColor != oldIcaoArrowsColor) {
            update(mCaptureInfo);
        }
    }

    public int getIcaoArrowsWidth() {
        return mIcaoArrowsWidth;
    }

    public void setIcaoArrowsWidth(final int icaoArrowsWidth) {
        final int oldIcaoArrowsWidth = getIcaoArrowsWidth();
        mIcaoArrowsWidth = icaoArrowsWidth;
        if (icaoArrowsWidth != oldIcaoArrowsWidth) {
            update(mCaptureInfo);
        }
    }

    // ===========================================================
    // Private fields
    // ===========================================================

    private boolean mAttributesTextureViewAvailable = false;
    private ConditionVariable mAttributesWaitingLock;
    private Semaphore mTextureViewAvailabilityLock;

    // ===========================================================
    // Attribute rendering thread
    // ===========================================================

    private class AttributeRenderThread extends Thread {

        @Override
        public void run() {
            while (mAttributesTextureViewAvailable) {
                mAttributesWaitingLock.block();
                mAttributesWaitingLock.close();

                try {
                    mTextureViewAvailabilityLock.acquire();
                    if (!mAttributesTextureViewAvailable) {
                        mTextureViewAvailabilityLock.release();
                        break;
                    }

                    final Canvas canvas = lockCanvas();
                    if (canvas != null) {
                        canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);

                        final int state = canvas.save();
                        canvas.concat(mAttributeMatrix);
                        if(mCaptureInfo != null) {
                            final Rect rect = mCaptureInfo.getBoundingRect();
                            final Matrix rotatedMatrix = new Matrix();

                            rotatedMatrix.postRotate(mCaptureInfo.getRoll(),
                                    (float) (rect.left * 2 + rect.width()) / 2.0f,
                                    (float) (rect.top * 2 + rect.height()) / 2.0f);
                            drawTool.drawFaceRectangle(canvas, rotatedMatrix);

                            drawTool.drawFaceQualityCheckArrows(canvas, rotatedMatrix, isShowIcaoArrows());
                            canvas.restoreToCount(state);

                            final int rotationFix = displayRotationToAngle(mDisplay.getRotation()) + mOrientation;
                            drawTool.drawLivenessComponent(canvas, rotationFix);

                            final String icaoWarningText = getIcaoWarningText(mCaptureInfo.getFaceQualityCheckWarnings());
                            drawTool.drawQualityCheckText(canvas, mShowIcaoText, icaoWarningText);
                        }
                        unlockCanvasAndPost(canvas);
                    }
                    mTextureViewAvailabilityLock.release();
                } catch (final InterruptedException exception) {
                    mTextureViewAvailabilityLock.release();
                    break;
                }
            }
        }
    }

    // ===========================================================
    // Private methods
    // ===========================================================

    private void initComponents2() {
        setSurfaceTextureListener(this);
        mAttributesWaitingLock = new ConditionVariable();
        mTextureViewAvailabilityLock = new Semaphore(1);
    }

    private void internalUpdate() {
        if (DEBUG) {
            Log.i(TAG, "internalPostImageAndEvent mAttributesWaitingLock.open");
        }

        mAttributesWaitingLock.open();
    }

    private void startRendering(final SurfaceTexture surface) {
        if (DEBUG) {
            Log.i(TAG, "AttributeRenderThread startRendering");
        }

        try {
            mTextureViewAvailabilityLock.acquire();
            mAttributesTextureViewAvailable = true;
            mTextureViewAvailabilityLock.release();
            new AttributeRenderThread().start();

            if (DEBUG) {
                Log.i(TAG, "AttributeRenderThread mAttributesWaitingLock.open()");
            }

            mAttributesWaitingLock.open();
        } catch (final InterruptedException exception) {
            // nothing to do
        }
    }

    private void stopRendering(final SurfaceTexture surface) {
        if (DEBUG) {
            Log.i(TAG, "AttributeRenderThread stopRendering");
        }

        try {
            mTextureViewAvailabilityLock.acquire();
            mAttributesTextureViewAvailable = false;
            mTextureViewAvailabilityLock.release();

            if (DEBUG) {
                Log.i(TAG, "AttributeRenderThread mAttributesWaitingLock.open()");
            }

            mAttributesWaitingLock.open();
        } catch (final InterruptedException exception) {
            // nothing to do
        }
    }

    // ===========================================================
    // Listener methods
    // ===========================================================

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
        if (DEBUG) {
            Log.i(TAG, "onSurfaceTextureAvailable");
        }
        startRendering(surface);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
        if (DEBUG) {
            Log.i(TAG, "onSurfaceTextureDestroyed");
        }
        stopRendering(surface);
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
        if (DEBUG) {
            Log.i(TAG, "onSurfaceTextureSizeChanged");
        }
    }

    @Override
    public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
        if (DEBUG) {
            Log.i(TAG, "onSurfaceTextureUpdated");
        }
    }

    public String getIcaoWarningText(final int warnings) {
        String name = null;
        final FaceQualityCheckWarnings [] all = FaceQualityCheckWarnings.values();
        for (final FaceQualityCheckWarnings warning: FaceQualityCheckWarnings.values()) {
            if((warnings & warning.getValue()) != 0)
            {
                name = mFaceQualityCheckWarningMap.get(warning);
                break;
            }
        }
        return name;
    }

    private int getRotation(final int rotation) {
        if (rotation != -1) {
            return (rotation & 3) * 90;
        } else {
            return 0;
        }
    }
}

