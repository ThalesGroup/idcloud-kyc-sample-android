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

package com.thalesgroup.kyc.idv.gui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.gemalto.ekyc.EKycLicenseConfigurationListener;
import com.gemalto.ekyc.face_capture.FaceCaptureFragment;
import com.gemalto.ekyc.face_capture.FaceCaptureInfo;
import com.gemalto.ekyc.face_capture.FaceCaptureListener;
import com.gemalto.ekyc.face_capture.FaceCaptureLivenessMode;
import com.gemalto.ekyc.face_capture.FaceCaptureManager;
import com.gemalto.ekyc.face_capture.FaceCaptureMetaData;
import com.gemalto.ekyc.face_capture.FaceCaptureSetting;
import com.gemalto.ekyc.face_capture.FaceLivenessAction;
import com.neurotec.face.verification.client.NStatus;
import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.gui.view.FaceCaptureOverlayView;
import com.thalesgroup.kyc.idv.gui.view.LivenessProgressbarView;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCManager;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityCaptureFaceIDV extends AppCompatActivity implements FaceCaptureListener {
    //region Definition

    private TextView mTextAction;
    private TextView mTextScore;
    private FaceCaptureFragment mFaceCaptureFragment;
    private FaceCaptureManager mFaceCaptureManager;
    private FaceCaptureOverlayView mFaceCaptureOverlay;

    private LivenessProgressbarView mProgressScore;
    private boolean mProgressEnabled = true;

    //endregion

    //region Life Cycle

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_face_idv);

        // Get all visual components.
        mProgressScore = findViewById(R.id.activity_capture_face_nt_score_progress);
        mFaceCaptureFragment = (FaceCaptureFragment) getSupportFragmentManager().findFragmentById(R.id.activity_capture_face_idv_face_fragment);
        mTextAction = findViewById(R.id.activity_capture_face_idv_action_label);
        mTextScore = findViewById(R.id.activity_capture_face_idv_score_label);

        // Get current configuration.
        final KYCManager managerKYC = KYCManager.getInstance();
        final int faceLivenessThreshold = managerKYC.getFaceLivenessThreshold();
        final FaceCaptureLivenessMode livenessMode = FaceCaptureLivenessMode.valueOf(managerKYC.getFaceLivenessMode());

        mProgressEnabled = livenessMode == FaceCaptureLivenessMode.PASSIVE;
        mProgressScore.setThreshold(faceLivenessThreshold);

        // Clean up default values
        mProgressScore.setProgress(0);
        mTextScore.setText("");
        mTextAction.setText("");

        // Update SDK configuration.
        final FaceCaptureSetting faceCaptureSetting = new FaceCaptureSetting();
        faceCaptureSetting.setQualityThreshold((byte) managerKYC.getFaceQualityThreshold());
        faceCaptureSetting.setBlinkTimeout(managerKYC.getFaceBlinkTimeout() * 1000);
        faceCaptureSetting.setLivenessThreshold((byte) faceLivenessThreshold);
        faceCaptureSetting.setLivenessMode(livenessMode);

        mFaceCaptureManager = mFaceCaptureFragment.getFaceCaptureManager();
        mFaceCaptureManager.setSetting(faceCaptureSetting);

        managerKYC.initializeFaceIdLicense(new EKycLicenseConfigurationListener() {
            @Override
            public void onLicenseConfigurationSuccess() {
                startFaceCapture();
            }

            @Override
            public void onLicenseConfigurationFailure(final Exception exception) {
                endWithError(exception.getMessage());
            }
        });
    }

    @Override
    protected void onStop() {
        try {
            mFaceCaptureManager.cancel();
        } catch (final Exception exception) {
            // Ignore errors.
        }
        super.onStop();
    }

    //endregion

    //region Private Helpers

    private void startFaceCapture() {
        new Thread(() -> {
            try {
                FaceCaptureManager.start(this);
            } catch (final Exception exception) {
                endWithError(exception.getMessage());
            }
        }).start();
    }

    private void endWithError(final String errMessage) {
        final Intent resultIntent = new Intent();
        if (errMessage != null) {
            resultIntent.putExtra(MainActivity.CAPTURE_EXTRA_ERROR_MESSAGE, errMessage);
        }
        setResult(MainActivity.CAPTURE_RETURN_CODE_ERR, resultIntent);
        finish();
    }

    private int getFaceErrorResultMessage(final int code) {
        if (code == NStatus.TIMEOUT.getValue()) {
            return R.string.face_status_match_timeout;
        } else if (code == NStatus.CANCELED.getValue()) {
            return R.string.face_status_canceled;
        } else if (code == NStatus.BAD_QUALITY.getValue()) {
            return R.string.face_status_bad_quality;
        } else if (code == NStatus.MATCH_NOT_FOUND.getValue()) {
            return R.string.face_status_match_not_found;
        } else if (code == NStatus.CAMERA_NOT_FOUND.getValue()) {
            return R.string.face_status_camera_not_found;
        } else if (code == NStatus.FACE_NOT_FOUND.getValue()) {
            return R.string.face_status_face_not_found;
        } else if (code == NStatus.LIVENESS_CHECK_FAILED.getValue()) {
            return R.string.face_status_liveness_check_failed;
        } else {
            return R.string.face_status_match_none;
        }

    }

    //endregion

    //region FaceCaptureListener

    @Override
    public void captureSuccess(final byte[] bytes,
                               final FaceCaptureMetaData faceCaptureMetaData) {
        DataContainer.instance().mSelfie = bytes.clone();

        final Intent resultIntent = new Intent();
        setResult(MainActivity.CAPTURE_RETURN_CODE_OK, resultIntent);
        finish();
    }

    @Override
    public void captureFailed(final int code) {
        endWithError(getString(getFaceErrorResultMessage(code)));
    }

    @Override
    public void onFaceVerificationCanceled() {
        // nothing to do
    }

    @Override
    public void onLivenessActionChanged(final FaceLivenessAction faceLivenessAction) {
        // nothing to do
    }

    @Override
    public void onFaceDetectedChanged(final Boolean changed) {
        // nothing to do
    }

    @Override
    public void onLivenessScoreChanged(final int score) {
        // nothing to do
    }

    @Override
    public void onFaceCaptureInfo(final FaceCaptureInfo faceCaptureInfo) {
        runOnUiThread(() -> {
            switch (faceCaptureInfo.getLivenessAction()) {

                case faceLivenessActionNone:
                    mTextAction.setText(R.string.STRING_KYC_FACE_ACTION_NONE);
                    break;
                case FaceLivenessActionKeepStill:
                    mTextAction.setText(R.string.STRING_KYC_FACE_ACTION_KEEP_STILL);
                    if (mProgressEnabled) {
                        mTextScore.setText(String.format(Locale.ENGLISH, "Score: %d", faceCaptureInfo.getLivenessScore()));
                        mProgressScore.setProgress(faceCaptureInfo.getLivenessScore());
                    }
                    break;
                case FaceLivenessActionBlink:
                    mTextAction.setText(R.string.STRING_KYC_FACE_ACTION_BLINK);
                    break;
                case FaceLivenessActionMoveUp:
                    mTextAction.setText(R.string.STRING_KYC_FACE_ACTION_MOVE_UP);
                    break;
                case FaceLivenessActionMoveDown:
                    mTextAction.setText(R.string.STRING_KYC_FACE_ACTION_MOVE_DOWN);
                    break;
                case FaceLivenessActionMoveLeft:
                    // It seems to be flipped in SDK.
                    mTextAction.setText(R.string.STRING_KYC_FACE_ACTION_MOVE_RIGHT);
                    break;
                case FaceLivenessActionMoveRight:
                    // It seems to be flipped in SDK.
                    mTextAction.setText(R.string.STRING_KYC_FACE_ACTION_MOVE_LEFT);
                    break;
                case FaceLivenessActionMoveToCenter:
                    mTextAction.setText(R.string.STRING_KYC_FACE_ACTION_MOVE_TO_CENTER);
                    break;
                case FaceLivenessActionTurnSideToSide:
                    mTextAction.setText(R.string.STRING_KYC_FACE_ACTION_SIDE_TO_SIDE);
                    break;
                case FaceLivenessActionRotateYaw:
                    mTextAction.setText(R.string.STRING_KYC_FACE_ACTION_ROTATE_YAW);
                    break;
                default:
                    // nothing to do
            }

            if (mFaceCaptureOverlay != null) {
                mFaceCaptureOverlay.setRegionSize(mFaceCaptureFragment.GetRegionWidth(),
                        mFaceCaptureFragment.GetRegionHeight());
                mFaceCaptureOverlay.update(faceCaptureInfo);
            }
        });
    }

    //endregion
}
