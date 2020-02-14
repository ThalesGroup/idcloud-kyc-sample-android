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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.TextureView;

import com.gemalto.ekyc.face_capture.FaceCaptureInfo;
import com.gemalto.ekyc.face_capture.FaceLivenessAction;
import com.gemalto.ekyc.face_capture.FaceQualityCheckWarnings;
import com.thalesgroup.kyc.idv.helpers.Constants;

import java.util.HashMap;
import java.util.Map;

class DrawTool {

    //region Definition

    private final static Matrix ARROW_TRANSFORM_MATRIX = new Matrix();
    private final static Matrix SCALE_MATRIX = new Matrix();
    private final static boolean ROTATE_FACE_RECTANGLE = Constants.DEFAULT_ROTATE_FACE_RECTANGLE;

    private FaceCaptureInfo mCaptureInfo;
    private PathTool mPathTool;
    private Paint mFaceQualityCheckArrowsPaint;
    private Paint mLinePaint;
    private final Context mContext;
    private Paint mLivenessTextPaint;
    private Paint mLivenessAreaPaint;
    private final TextureView mView;
    private final int mImageRotateFlipType = -1;
    private Map<String, String> mTextMap;

    //endregion

    //region Life Cycle

    DrawTool(final Context context, final TextureView view) {
        super();

        this.mContext = context;
        this.mView = view;
        initComponents();
    }

    private void initComponents() {

        mPathTool = new PathTool();

        mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setColor(Constants.DEFAULT_PAINT_COLOR);
        mLinePaint.setStrokeWidth(dipToPx(Constants.DEFAULT_FACE_RECTANGLE_WIDTH));

        mFaceQualityCheckArrowsPaint = new Paint();
        mFaceQualityCheckArrowsPaint.setColor(Constants.DEFAULT_QUALITY_CHECK_ARROWS_COLOR);
        mFaceQualityCheckArrowsPaint.setStyle(Paint.Style.STROKE);
        mFaceQualityCheckArrowsPaint.setStrokeWidth(Constants.DEFAULT_QUALITY_CHECK_ARROWS_STROKE_WIDTH);

        mLivenessTextPaint = new Paint();
        mLivenessTextPaint.setColor(Constants.DEFAULT_LIVENESS_TEXT_COLOR);
        mLivenessTextPaint.setTextSize(dipToPx(Constants.DEFAULT_LIVENESS_TEXT_SIZE));
        mLivenessTextPaint.setStrokeWidth(dipToPx(Constants.DEFAULT_PAINT_TEXT_STROKE_WIDTH));

        mLivenessAreaPaint = new Paint();
        mLivenessAreaPaint.setStyle(Paint.Style.STROKE);
        mLivenessAreaPaint.setColor(Constants.DEFAULT_LIVENESS_TEXT_COLOR);
        mLivenessAreaPaint.setStrokeWidth(dipToPx(Constants.DEFAULT_LIVENESS_AREA_WIDTH));

        final Paint mTextPaint = new Paint();
        mTextPaint.setColor(Constants.DEFAULT_PAINT_COLOR);
        mTextPaint.setTextSize(dipToPx(Constants.DEFAULT_PAINT_TEXT_SIZE));
        mTextPaint.setStrokeWidth(dipToPx(Constants.DEFAULT_PAINT_TEXT_STROKE_WIDTH));

        final Paint mFeaturePointPaint = new Paint();
        mFeaturePointPaint.setColor(Constants.DEFAULT_PAINT_COLOR);
        mFeaturePointPaint.setStyle(Paint.Style.FILL);

        mTextMap = new HashMap<>();
        {
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_BLINK, Constants.DEFAULT_LIVENESS_TEXT_BLINK);
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_KEEP_ROTATING, Constants.DEFAULT_LIVENESS_TEXT_KEEP_ROTATING);
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_KEEP_ROTATING_WITH_SCORE, Constants.DEFAULT_LIVENESS_TEXT_KEEP_ROTATING_WITH_SCORE);
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_KEEP_STILL, Constants.DEFAULT_LIVENESS_TEXT_KEEP_STILL);
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_KEEP_STILL_WITH_SCORE, Constants.DEFAULT_LIVENESS_TEXT_KEEP_STILL_WITH_SCORE);
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_TURN_DOWN, Constants.DEFAULT_LIVENESS_TEXT_TURN_DOWN);
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_TURN_LEFT, Constants.DEFAULT_LIVENESS_TEXT_TURN_LEFT);
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_TURN_TO_CENTER, Constants.DEFAULT_LIVENESS_TEXT_TURN_TO_CENTER);
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_TURN_RIGHT, Constants.DEFAULT_LIVENESS_TEXT_TURN_RIGHT);
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_TURN_UP, Constants.DEFAULT_LIVENESS_TEXT_TURN_UP);
            mTextMap.put(Constants.KEY_LIVENESS_TEXT_TURN_TO_TARGET, Constants.DEFAULT_LIVENESS_TEXT_TURN_TO_TARGET);
        }
    }

    //endregion

    //region Private Helpers

    /**
     * Gets action text.
     *
     * @param key Action key.
     * @return Action text.
     */
    private String getLivenessText(final String key) {
        if (mTextMap.containsKey(key)) {
            return mTextMap.get(key);
        }

        throw new IllegalStateException("Key is not defined");
    }

    private float dipToPx(final int value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, mContext.getResources().getDisplayMetrics());
    }

    void drawFaceRectangle(final Canvas canvas, final Matrix rotationMatrix) {

        if (Constants.DEFAULT_SHOW_FACE_RECTANGLE) {
            final int preRotatedMatrix = canvas.save();
            if (ROTATE_FACE_RECTANGLE) {
                canvas.concat(rotationMatrix);
            }

            final Rect rect = mCaptureInfo.getBoundingRect();
            final Path path = new Path();
            path.moveTo(rect.left, rect.top);
            path.lineTo(rect.right, rect.top);
            if (mCaptureInfo.getYaw() < 0) {
                path.lineTo(rect.right - (rect.width() / 5 * mCaptureInfo.getYaw()) / 45, rect.top + (rect.height() / 2));
            }
            path.lineTo(rect.right, rect.bottom);
            path.lineTo(rect.left, rect.bottom);
            if (mCaptureInfo.getYaw() > 0) {
                path.lineTo(rect.left - (rect.width() / 5 * mCaptureInfo.getYaw()) / 45, rect.top + (rect.height() / 2));
            }
            path.lineTo(rect.left, rect.top);
            path.lineTo(rect.right, rect.top);

            canvas.drawPath(path, mLinePaint);
            canvas.restoreToCount(preRotatedMatrix);
        }
    }

    private void drawRollArrows(final Canvas canvas, final boolean isFlip) {

        ARROW_TRANSFORM_MATRIX.reset();
        SCALE_MATRIX.reset();

        final Rect faceRect = mCaptureInfo.getBoundingRect();
        final RectF rollPathBounds = new RectF();
        final Path rollArrowPath = mPathTool.getRollPath();
        rollArrowPath.computeBounds(rollPathBounds, true);
        final float scale = faceRect.width() / 5f / rollPathBounds.width();
        SCALE_MATRIX.postScale(scale, scale);
        rollArrowPath.transform(SCALE_MATRIX);
        ARROW_TRANSFORM_MATRIX.postTranslate(faceRect.left, faceRect.top);
        if (isFlip) {
            rollArrowPath.transform(getMirrorMatrix());
            ARROW_TRANSFORM_MATRIX
                    .postTranslate(faceRect.width() + rollPathBounds.width() * scale, 0);
        }
        ARROW_TRANSFORM_MATRIX.postTranslate(-(rollPathBounds.width() * scale / 2), -(rollPathBounds.height() * scale / 2));
        final int canvasSave = canvas.save();
        canvas.concat(ARROW_TRANSFORM_MATRIX);
        canvas.drawPath(rollArrowPath, mFaceQualityCheckArrowsPaint);
        canvas.restoreToCount(canvasSave);
    }


    private void drawYawArrows(final Canvas canvas, final boolean isFlip) {

        ARROW_TRANSFORM_MATRIX.reset();
        SCALE_MATRIX.reset();

        final Rect faceRect = mCaptureInfo.getBoundingRect();
        final RectF yawPathBounds = new RectF();
        final Path yawArrowPath = mPathTool.getYawPath();
        yawArrowPath.computeBounds(yawPathBounds, true);
        final float scale = (float)faceRect.width() / 5.0f / yawPathBounds.width();
        SCALE_MATRIX.postScale(-scale, scale);
        SCALE_MATRIX.postTranslate(yawPathBounds.width() * scale, 0);
        yawArrowPath.transform(SCALE_MATRIX);
        final float centerX = (yawPathBounds.left + yawPathBounds.width()) / 2 * scale;
        final float centerY = (yawPathBounds.top + yawPathBounds.height()) / 2 * scale;

        final float offset = (float)faceRect.width() / 5.0f * mCaptureInfo.getYaw() / 45.0f;

        ARROW_TRANSFORM_MATRIX.postTranslate(faceRect.left, faceRect.top);
        if (isFlip) {
            yawArrowPath.transform(getMirrorMatrix());
            ARROW_TRANSFORM_MATRIX.postTranslate(faceRect.width() + yawPathBounds.width() * scale, 0);
        }
        ARROW_TRANSFORM_MATRIX.postTranslate(offset - centerX, faceRect.height() / 2 - centerY);

        final int canvasSave = canvas.save();
        canvas.concat(ARROW_TRANSFORM_MATRIX);
        canvas.drawPath(yawArrowPath, mFaceQualityCheckArrowsPaint);
        canvas.restoreToCount(canvasSave);
    }


    private void drawPitchArrows(final Canvas canvas, final boolean isFlip) {

        ARROW_TRANSFORM_MATRIX.reset();
        SCALE_MATRIX.reset();

        final Rect faceRect = mCaptureInfo.getBoundingRect();
        final Path pitchPath = mPathTool.getPitchPath();
        final RectF pitchPathBounds = new RectF();
        pitchPath.computeBounds(pitchPathBounds, true);
        final float coordinateX = faceRect.width() / 2;
        final float scale = faceRect.width() / 5f / pitchPathBounds.width();

        SCALE_MATRIX.postScale(scale, -scale);
        SCALE_MATRIX.postTranslate(0, pitchPathBounds.height() * scale);
        pitchPath.transform(SCALE_MATRIX);

        ARROW_TRANSFORM_MATRIX.postTranslate(faceRect.left, faceRect.top);

        final float centerX = (pitchPathBounds.left + pitchPathBounds.width()) / 2 * scale;
        final float centerY = (pitchPathBounds.top + pitchPathBounds.height()) / 2 * scale;

        if (isFlip) {
            final Matrix temp = new Matrix();
            temp.postScale(1, -1);
            temp.postTranslate(0, pitchPathBounds.height() * scale);
            pitchPath.transform(temp);
            ARROW_TRANSFORM_MATRIX.postTranslate(0, faceRect.height());
        }

        ARROW_TRANSFORM_MATRIX.postTranslate(coordinateX - centerX, -centerY);
        final int canvasSave = canvas.save();
        canvas.concat(ARROW_TRANSFORM_MATRIX);
        canvas.drawPath(pitchPath, mFaceQualityCheckArrowsPaint);
        canvas.restoreToCount(canvasSave);
    }


    private void drawMoveArrow(final Canvas canvas, final float rotate) {

        ARROW_TRANSFORM_MATRIX.reset();
        SCALE_MATRIX.reset();

        final Rect faceRect = mCaptureInfo.getBoundingRect();
        final RectF movePathBounds = new RectF();
        final Path moveArrowPath = mPathTool.getMovePath();

        moveArrowPath.computeBounds(movePathBounds, true);
        final float scale = faceRect.width() / 5f / movePathBounds.width();
        SCALE_MATRIX.postScale(-scale, scale);
        moveArrowPath.transform(SCALE_MATRIX);

        final float midY = movePathBounds.height() / 2 * scale;

        final int startingCanvas = canvas.save();
        final float coordinateX = faceRect.width() / 2;
        final float coordinateY = faceRect.height() / 2;
        ARROW_TRANSFORM_MATRIX.postTranslate(faceRect.left, faceRect.top);

        final float diffX = dipToPx(10);
        final float diffY = coordinateY - midY;
        ARROW_TRANSFORM_MATRIX.postTranslate(-diffX, diffY);
        canvas.concat(ARROW_TRANSFORM_MATRIX);

        final int tCanvas = canvas.save();
        ARROW_TRANSFORM_MATRIX.reset();
        ARROW_TRANSFORM_MATRIX.postRotate(rotate, diffX + coordinateX, midY);
        canvas.concat(ARROW_TRANSFORM_MATRIX);
        canvas.drawPath(moveArrowPath, mFaceQualityCheckArrowsPaint);
        canvas.restoreToCount(tCanvas);

        canvas.restoreToCount(startingCanvas);
    }

    //endregion

    //region Public API

    void drawFaceQualityCheckArrows(final Canvas canvas, final Matrix rotationMatrix, final boolean isShowIcaoArrows) {
        final int warnings = mCaptureInfo.getFaceQualityCheckWarnings();
        if (warnings == 0 || !isShowIcaoArrows) {
            return;
        }
        final int icaoMatrix = canvas.save();
        canvas.concat(rotationMatrix);

        final boolean isRollLeft  = (warnings & FaceQualityCheckWarnings.ROLL_LEFT.getValue()) != 0;
        final boolean isRollRight = (warnings & FaceQualityCheckWarnings.ROLL_RIGHT.getValue()) != 0;
        final boolean isYawLeft  = (warnings & FaceQualityCheckWarnings.YAW_LEFT.getValue()) != 0;
        final boolean isYawRight = (warnings & FaceQualityCheckWarnings.YAW_RIGHT.getValue()) != 0;
        final boolean isTooEast  = (warnings & FaceQualityCheckWarnings.TOO_EAST.getValue()) != 0;
        final boolean isTooWest  = (warnings & FaceQualityCheckWarnings.TOO_WEST.getValue()) != 0;
        final boolean isTooNorth = (warnings & FaceQualityCheckWarnings.TOO_NORTH.getValue()) != 0;
        final boolean isTooSouth = (warnings & FaceQualityCheckWarnings.TOO_SOUTH.getValue()) != 0;
        final boolean isTooNear  = (warnings & FaceQualityCheckWarnings.TOO_NEAR.getValue()) != 0;
        final boolean isPitchUp   = (warnings & FaceQualityCheckWarnings.PITCH_UP.getValue()) != 0;
        final boolean isPitchDown = (warnings & FaceQualityCheckWarnings.PITCH_DOWN.getValue()) != 0;

        if (isRollLeft || isRollRight) {
            drawRollArrows(canvas, isFlipY(mImageRotateFlipType) ^ isRollLeft);
        }

        //Yaw
        if ((isYawLeft && !isTooEast) || (isYawRight && !isTooWest) || isTooNear) {
            drawYawArrows(canvas, isFlipY(mImageRotateFlipType)^isYawLeft);
        }

        //Pitch
        if ((isPitchDown && !isTooSouth) || (isPitchUp && !isTooNorth) || isTooNear) {

            drawPitchArrows( canvas, isPitchDown);
        }

        //Move
        if (isTooWest) {
            drawMoveArrow(canvas, 0);
        }
        if (isTooEast) {
            drawMoveArrow(canvas, 180);
        }
        if (isTooNorth) {
            drawMoveArrow(canvas, 90);
        }
        if (isTooSouth) {
            drawMoveArrow(canvas, 270);
        }

        canvas.restoreToCount(icaoMatrix);
    }

    void drawQualityCheckText(final Canvas canvas, final boolean mShowIcaoText, final String icaoWarningText) {
        if(mShowIcaoText) {
            final Paint textPaint = new Paint();
            textPaint.setColor(Constants.DEFAULT_QUALITY_CHECK_TEXT_COLOR);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setTextSize(dipToPx(Constants.DEFAULT_QUALITY_CHECK_TEXT_SIZE));


            if (icaoWarningText != null) {
                final int xCord = mView.getWidth() / 10;
                final int width = xCord * 8;
                final int height = xCord;

                final Rect bounds = new Rect();
                mLivenessTextPaint.getTextBounds(icaoWarningText, 0, icaoWarningText.length(), bounds);
                final Rect area = new Rect(xCord, mView.getHeight() - height * 2, xCord + width, mView.getHeight() - height);

                final int oldCanvas = canvas.save();
                canvas.drawText(icaoWarningText, area.centerX() - (bounds.width() / 2), area.top - dipToPx(
                        Constants.DEFAULT_LIVENESS_STATUS_SPACE), textPaint);
                canvas.restoreToCount(oldCanvas);
            }
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable", "SuspiciousNameCombination"})
    void drawLivenessComponent(final Canvas canvas, final int rotationFix) {

        if (mCaptureInfo.getLivenessAction() != null && mCaptureInfo.getLivenessAction() != FaceLivenessAction.faceLivenessActionNone) {
            final int oldCanvas = canvas.save();

            int newRotateFix = 0;
            if (isFlipY(mImageRotateFlipType)) {
                newRotateFix = (360 - rotationFix) % 360;
            }
            if (newRotateFix != 0) {
                final Matrix liveness = new Matrix();
                liveness.postRotate(newRotateFix, mView.getWidth() / 2, mView.getHeight() / 2);
                liveness.postTranslate((float) (Math.sin(Math.toRadians(newRotateFix)) * (mView.getHeight() - mView
                        .getWidth()) / 2), 0);
                canvas.concat(liveness);
            }

            final boolean blink        = mCaptureInfo.getLivenessAction() == FaceLivenessAction.FaceLivenessActionBlink;
            final boolean rotate       = mCaptureInfo.getLivenessAction() == FaceLivenessAction.FaceLivenessActionRotateYaw;
            final boolean keepStill    = mCaptureInfo.getLivenessAction() == FaceLivenessAction.FaceLivenessActionKeepStill;
            final boolean keepRotating = mCaptureInfo.getLivenessAction() == FaceLivenessAction.FaceLivenessActionTurnSideToSide;
            final boolean toCenter     = mCaptureInfo.getLivenessAction() == FaceLivenessAction.FaceLivenessActionMoveToCenter;
            final boolean toLeft       = mCaptureInfo.getLivenessAction() == FaceLivenessAction.FaceLivenessActionMoveLeft;
            final boolean toRight      = mCaptureInfo.getLivenessAction() == FaceLivenessAction.FaceLivenessActionMoveRight;
            final boolean toUp         = mCaptureInfo.getLivenessAction() == FaceLivenessAction.FaceLivenessActionMoveUp;
            final boolean toDown       = mCaptureInfo.getLivenessAction() == FaceLivenessAction.FaceLivenessActionMoveDown;

            final byte score = mCaptureInfo.getLivenessScore();
            final StringBuilder status = new StringBuilder();
            String actionKey = null;

            final int maxYaw = 35;

            final float yaw = mCaptureInfo.getYaw() * -1;
            final float targetYaw = mCaptureInfo.getLivenessTargetYaw() * -1;

            final int xCord = mView.getWidth() / 10;
            final int width = xCord * 8;
            final int height = xCord;

            final Rect area = new Rect(xCord, mView.getHeight() - height * 2, xCord + width, mView.getHeight() - height);

            if (keepStill) {
                if (score <= 100) {
                    actionKey = Constants.KEY_LIVENESS_TEXT_KEEP_STILL_WITH_SCORE;
                } else {
                    actionKey = Constants.KEY_LIVENESS_TEXT_KEEP_STILL;
                }
            } else if (rotate) {
                if (!blink) {
                    actionKey = Constants.KEY_LIVENESS_TEXT_TURN_TO_TARGET;

                    final int targetOffset = (int) ((targetYaw / maxYaw) * (width / 2));
                    final Rect targetArea = new Rect(area.centerX() - (height / 2) + targetOffset,
                            area.top,
                            area.centerX() - (height / 2) + targetOffset + height,
                            area.top + height);

                    canvas.drawPath(mPathTool.preparePath(mPathTool.getTargetPath(), targetArea, targetYaw < yaw), mLivenessAreaPaint);

                    final int curentYawOffset = (int) ((yaw / maxYaw) * (width / 2));

                    final int left = area.centerX() - (height / 2);

                    final Rect curentYawArea = new Rect(left + curentYawOffset,
                            area.top,
                            left + curentYawOffset + height,
                            area.top + height);

                    canvas.drawPath(mPathTool.preparePath(mPathTool.getArrowPath(), curentYawArea, targetYaw < yaw), mLivenessAreaPaint);
                } else {
                    actionKey = Constants.KEY_LIVENESS_TEXT_BLINK;

                    final int curentYawOffset = (int) ((yaw / maxYaw) * (width / 2));
                    final int left = area.centerX() - (height / 2);

                    final Rect curentYawArea = new Rect(left + curentYawOffset,
                            area.top,
                            left + curentYawOffset + height,
                            area.top + height);
                    canvas.drawPath(mPathTool.preparePath(mPathTool.getBlinkPath(), curentYawArea, targetYaw < yaw), mLivenessAreaPaint);
                }
            } else if (blink) {
                actionKey = Constants.KEY_LIVENESS_TEXT_BLINK;
            } else if (keepRotating) {
                if (score <= 100) {
                    actionKey = Constants.KEY_LIVENESS_TEXT_KEEP_ROTATING_WITH_SCORE;
                } else {
                    actionKey = Constants.KEY_LIVENESS_TEXT_KEEP_ROTATING;
                }
            } else if (toCenter) {
                actionKey = Constants.KEY_LIVENESS_TEXT_TURN_TO_CENTER;
            } else if (toLeft) {
                actionKey = Constants.KEY_LIVENESS_TEXT_TURN_LEFT;
            } else if (toRight) {
                actionKey = Constants.KEY_LIVENESS_TEXT_TURN_RIGHT;
            } else if (toUp) {
                actionKey = Constants.KEY_LIVENESS_TEXT_TURN_UP;
            } else if (toDown) {
                actionKey = Constants.KEY_LIVENESS_TEXT_TURN_DOWN;
            }
            // if actionsKey string will not have format specifications (%d), score will be ignored
            status.append(String.format(getLivenessText(actionKey), score));
            final Rect bounds = new Rect();
            mLivenessTextPaint.getTextBounds(status.toString(), 0, status.length(), bounds);

            canvas.drawText(status.toString(), area.centerX() - (bounds.width() / 2), area.top - dipToPx(
                    Constants.DEFAULT_LIVENESS_STATUS_SPACE), mLivenessTextPaint);
            canvas.restoreToCount(oldCanvas);
        }
    }

    Matrix getMirrorMatrix() {
        final float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
        final Matrix matrixMirrorY = new Matrix();
        matrixMirrorY.setValues(mirrorY);
        return matrixMirrorY;
    }


    boolean isFlipY(final int rotation) {
        if (rotation != -1 ) {
            return (rotation & 4) == 4;
        } else {
            return false;
        }
    }



    /**
     * Gets liveness text color.
     *
     * @return Color code.
     */
     int getLivenessTextColor() {
        return mLivenessTextPaint.getColor();
    }

    /**
     * Sets liveness text color.
     *
     * @param livenessTextColor Color code.
     */
    void setLivenessTextColor(final int livenessTextColor) {
        mLivenessTextPaint.setColor(livenessTextColor);
    }

    void updateCaptureInfo(final FaceCaptureInfo captureInfo){
        mCaptureInfo = captureInfo;
    }

    //endregion
}
