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
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.gui.animation.EaseInterpolators;
import com.thalesgroup.kyc.idv.gui.view.DocumentScanWarningView;
import com.thalesgroup.kyc.idv.gui.view.DocumentStepDetailView;
import com.thalesgroup.kyc.idv.gui.view.DocumentStepView;
import com.thalesgroup.kyc.idv.helpers.AbstractOption;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCManager;
import com.thalesgroup.kyc.idv.helpers.util.AssetHelper;

import net.gemalto.mcidsdk.CaptureListener;
import net.gemalto.mcidsdk.CaptureResult;
import net.gemalto.mcidsdk.CaptureSDK;
import net.gemalto.mcidsdk.Metadata;
import net.gemalto.mcidsdk.ui.CaptureFragment;
import net.gemalto.mcidsdk.ui.Document;
import net.gemalto.mcidsdk.ui.PreviewEdgesImageView;

import java.util.EnumSet;
import java.util.Locale;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;


public abstract class AbstractActivityCaptureDocIDV extends AppCompatActivity
        implements CaptureListener, CaptureListener.DetectionWarningListener {

    //region Definition

    // Tutorial steps side panel minimum and maximum size to fit.
    protected static final int SIDE_MENU_MIN_SIZE_DP = 100;
    protected static final int SIDE_MENU_MAX_SIZE_DP = 180;

    private static final String TAG = AbstractActivityCaptureDocIDV.class.getName();

    // UI
    protected LinearLayout mLayoutTutorial = null;
    protected FrameLayout mLayoutPreview = null;
    protected DocumentScanWarningView mDocScanWarning;
    protected PreviewEdgesImageView mSDKEdgePreview = null;
    protected CaptureFragment mSDKCaptureView = null;
    protected ImageButton mShutterButton = null;
    protected RelativeLayout mLayoutResultBackground = null;
    protected ImageView mCaptureFrame = null;

    // Logic
    protected AbstractOption.DocumentType mDocumentType = AbstractOption.DocumentType.IdCard;
    protected int mCurrentStep = 0;
    protected boolean mCurrentStepProcessed = false;
    protected boolean mPaused = false;
    protected boolean mAutocapture = false;
    protected boolean mLimitDetectionZone = false;
    @StringRes
    protected int mLastWarningMessage = 0;
    protected CaptureSDK mSdk;

    //endregion

    //region Life Cycle

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get argument to determine document type.
        mDocumentType = (AbstractOption.DocumentType) getIntent().getSerializableExtra(MainActivity.BUNDLE_ARGUMENT_DOC_TYPE);
        mLimitDetectionZone = KYCManager.getInstance().isIdCaptureDetectionZoneReduced();

        // Load basic visual components.
        initViews();

        // SDK Handle camera permission in case we don't have them already.
        // Ideally we should update ideal size them, but for demo purposes this is enough.
        final Camera.Size idealCameraResolution = initIdealCameraResolutionAndUpdateLayout();

        // Initialise SDK with calculated ideal size.
        initSDK(idealCameraResolution);
    }


    @Override
    public void onDestroy() {
        if (mSdk != null) {
            mSdk.releaseMemory();
        }

        super.onDestroy();
    }

    //endregion


    //region CaptureListener

    @Override
    public void onSuccess(final byte[] capturedSide1, final byte[] capturedSide2, final Metadata capturedMetadata) {
        Log.d(TAG, "deprecated onSuccess() method called");
        // Empty method as the result is handled in new onSuccess method
    }

    @Override
    public void onSuccess(final CaptureResult captureResult) {
        Log.d(TAG, "new onSuccess() method called");

        mSdk.stop();
        DataContainer.instance().mDocFront = captureResult.getSide1() != null ? captureResult.getSide1().clone() : null;
        DataContainer.instance().mDocBack = captureResult.getSide2() != null ? captureResult.getSide2().clone() : null;

        if (mDocumentType == AbstractOption.DocumentType.IdCard && captureResult.getSide2() != null ||
            mDocumentType == AbstractOption.DocumentType.Passport && captureResult.getSide1() != null) {

            final Intent resultIntent = new Intent();
            setResult(MainActivity.CAPTURE_RETURN_CODE_OK, resultIntent);
            finish();
        }
    }

    @Override
    public void onError(final int errorCode) {
        Log.d(TAG, "onError(): " + errorCode);

        mSdk.stop();

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(MainActivity.CAPTURE_EXTRA_ERROR_CORE, errorCode);
        setResult(MainActivity.CAPTURE_RETURN_CODE_ERR, resultIntent);
        finish();
    }

    @Override
    public void onScreenChanged(final CaptureScreen captureScreen) {
        // Handle ID related operations like display step etc...
        runOnUiThread(() -> {
            if (mDocumentType == AbstractOption.DocumentType.IdCard) {
                onScreenChangedIdCard(captureScreen);
            } else if (mDocumentType == AbstractOption.DocumentType.Passport) {
                onScreenChangedPassport(captureScreen);
            }
        });
    }

    //endregion

    //region DetectionWarningListener

    @Override
    public void onDetectionWarnings(final EnumSet<CaptureSDK.DetectionWarning> warnings) {
        // Handle ID related operations like display step etc...
        runOnUiThread(() -> {
            @StringRes final int message = getMessageResource(warnings);

            // Ignore all messages while SDK is paused and same as last time.
            if (!mPaused && message != mLastWarningMessage) {
                if (message != 0) {
                    mDocScanWarning.display(message, true);
                } else {
                    mDocScanWarning.hide(true);
                }
            }

            // Update last message all the time so we can update sdk on resume.
            mLastWarningMessage = message;
        });
    }

    //endregion

    //region Private Helpers

    private int getMessageResource(final EnumSet<CaptureSDK.DetectionWarning> warnings) {
        int message = 0;

        if (warnings.contains(CaptureSDK.DetectionWarning.FitDocument)) {
            message = R.string.doc_scan_warning_fit;
        }
        if (warnings.contains(CaptureSDK.DetectionWarning.LowContrast)) {
            message = R.string.doc_scan_warning_light;
        }
        if (warnings.contains(CaptureSDK.DetectionWarning.Hotspot)) {
            message = R.string.doc_scan_warning_hotspot;
        }
        if (warnings.contains(CaptureSDK.DetectionWarning.FocusInProgress)) {
            message = R.string.doc_scan_warning_focusing;
        }
        if (warnings.contains(CaptureSDK.DetectionWarning.LowContrast)) {
            message = R.string.doc_scan_warning_contrast;
        }
        if (warnings.contains(CaptureSDK.DetectionWarning.Blur)) {
            message = R.string.doc_scan_warning_blur;
        }
        if (warnings.contains(CaptureSDK.DetectionWarning.None)) {
            message = 0;
        }

        return message;
    }

    private void initViews() {
        // Run in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Inflate view.
        setContentView(R.layout.activity_capture_doc_idv);

        // Side menu with steps.
        mLayoutTutorial = findViewById(R.id.view_capture_tutorial_layout);

        // Camera preview frame. Used as step detail parent.
        mLayoutPreview = findViewById(R.id.view_capture_preview_layout);

        // Custom warning toast. Blurred, hotspot etc...
        mDocScanWarning = findViewById(R.id.warning_toast);

        // Manually control shutter button visibility.
        mShutterButton = findViewById(R.id.take_photo_button);

        // Result background to fade transition
        //mLayoutResultBackground = findViewById(R.id.result_screen_background);
        mLayoutResultBackground = findViewById(R.id.mcid_capture_result_layout_custom);

        // Hide original SDK overlay and use custom one.
        findViewById(R.id.overlay_preview).setAlpha(.0f);

        mCaptureFrame = findViewById(R.id.capture_frame);

        if (mCaptureFrame != null) {
            mCaptureFrame.setAlpha(.0f);
        }

        // Document edge detection overlay. We want to force hide it.
        mSDKEdgePreview = findViewById(R.id.edges_preview);

        resizeCaptureFrame();

        // Load side panel with steps based on configuration.
        loadTutorialSteps();
    }

    private void initSDK(final Camera.Size idealCameraResolution) {
        final KYCManager managerKYC = KYCManager.getInstance();

        // Get main SDK capture view fragment. It must be present.
        mSDKCaptureView = (CaptureFragment) getSupportFragmentManager().findFragmentById(R.id.capture_view);
        if (mSDKCaptureView == null) {
            throw new IllegalStateException("Failed to get SDK fragment.");
        }

        // Update resolution to fit screen ideally.
        mSDKCaptureView.setDesiredLiveStreamRes(idealCameraResolution.height, idealCameraResolution.width);
        mSDKCaptureView.setDesiredProcessStreamRes(idealCameraResolution.height, idealCameraResolution.width);

        // Configure the SDK
        mSdk = mSDKCaptureView.getSDK();

        // Document will define size as well as number of pages.
        if (mDocumentType == AbstractOption.DocumentType.IdCard) {
            mSdk.setCaptureDocuments(Document.DocumentModeIdDocument);
        } else if (mDocumentType == AbstractOption.DocumentType.Passport) {
            mSdk.setCaptureDocuments(Document.DocumentModePassport);
        }

        // Display/Hide shutter button and do capture manually/automatically
        mAutocapture = !managerKYC.isManualScan();
        mSdk.setAutoSnapshot(mAutocapture);

        // Auto crop image based on document edges.
        // It might still force display cropping in case of failed detection.
        mSdk.setAutoCropping(managerKYC.isAutomaticTypeDetection());

        // Hide result screen after each step.
        mSdk.setQACheckResultTimeout(3);

        // Use faster machine learning.
        mSdk.setEdgeDetectionMode(CaptureSDK.EEdgeDetectionMode.MachineLearning);

        // To display custom toast overlay.
        mSdk.setDetectionWarningsListener(this);

        // Limit detection zone to 80%
        if (mLimitDetectionZone) {
            mSdk.setDetectionZone(80, 1.4204f);
        }

        // Set whenever we want also black and white copy of photo.
        mSdk.setBWPhotocopyQAEnabled(managerKYC.isBwPhotoCopyQA());

        // Hide the UI elements for each step
        //mSdk.hideUIElementsForStep(CaptureSDK.CaptureStep.ResultOK);
        mSdk.hideUIElementsForStep(CaptureSDK.CaptureStep.Detecting);

        // Init the SDK with success handler
        mSdk.init((final boolean isCompleted, final int errorCode) -> {
            if (isCompleted) {
                mSdk.start(this);
            } else {
                Log.d(TAG, String.format(Locale.getDefault(), "Error on init (%d)", errorCode));
            }
        });
    }

    protected abstract void resizeCaptureFrame();

    protected int dpToPx(final int densityIndependentPixel) {
        return Math.round((float) densityIndependentPixel * getResources().getDisplayMetrics().density);
    }

    private void loadTutorialSteps() {
        // In case of some reusable view. Remove all current children.
        mLayoutTutorial.removeAllViews();

        if (mDocumentType == AbstractOption.DocumentType.IdCard) {
            mLayoutTutorial.addView(new DocumentStepView(this, R.string.kyc_doc_scan_step_01, AssetHelper.ASSET_DOC_STEP_ID_CARD_FRONT));
            mLayoutTutorial.addView(new DocumentStepView(this, R.string.kyc_doc_scan_step_02, AssetHelper.ASSET_DOC_STEP_ID_CARD_FRONT));
            mLayoutTutorial.addView(new DocumentStepView(this, R.string.kyc_doc_scan_step_03, AssetHelper.ASSET_DOC_STEP_ID_CARD_BACK));
        } else if (mDocumentType == AbstractOption.DocumentType.Passport) {
            mLayoutTutorial.addView(new DocumentStepView(this, R.string.kyc_doc_scan_step_01, AssetHelper.ASSET_DOC_STEP_PASSPORT));
        }
    }

    private void highlightAndRotateStep(final int stepIndex, final String iconName) {
        if (stepIndex >= 0 && stepIndex < mLayoutTutorial.getChildCount()) {
            final DocumentStepView stepView = (DocumentStepView) mLayoutTutorial.getChildAt(stepIndex);
            stepView.highlightAndRotateToIcon(this, iconName);
        } else {
            Log.e(TAG, "highlightStep: Invalid step index.");
        }
    }

    private void highlightStep(final int stepIndex, final boolean highlight) {
        if (stepIndex >= 0 && stepIndex < mLayoutTutorial.getChildCount()) {
            final DocumentStepView stepView = (DocumentStepView) mLayoutTutorial.getChildAt(stepIndex);
            stepView.highlight(highlight, true);
        } else {
            Log.e(TAG, "highlightStep: Invalid step index.");
        }
    }

    private void pauseSDK() {
        // By disabling auto-snapshot we will prevent SDK to continue automatically.
        mSDKCaptureView.getSDK().setAutoSnapshot(false);
        // Make sure, that edges will stay hidden.
        mSDKEdgePreview.setAlpha(.0f);

        // Pause tracking SDK callbacks.
        mPaused = true;

    }

    private void resumeSDK() {
        // Continue auto-capture.
        mSDKCaptureView.getSDK().setAutoSnapshot(mAutocapture);

        // We have to update shutter button manually.
        if (!mAutocapture) {
            mShutterButton.setAlpha(1.f);
            mShutterButton.setVisibility(View.VISIBLE);
        }

        // Make sure, that edges will stay hidden.
        mSDKEdgePreview.setAlpha(.0f);

        // Display any warning message triggered by SDK while processing was paused.
        if (mLastWarningMessage != 0) {
            mDocScanWarning.display(mLastWarningMessage, true);
        }

        // Resume tracking SDK callbacks.
        mPaused = false;
    }

    private void onScreenChangedIdCard(final CaptureScreen captureScreen) {
        if (captureScreen == CaptureScreen.InResultOK) {
            mLayoutResultBackground.animate().alpha(1.0f)
                                   .setDuration(1000)
                                   .setInterpolator(new EaseInterpolators.EaseInOut()).start();
        } else if (captureScreen == CaptureScreen.OutResultOK) {
            // Image is automatically hidden by SDK. We want to animate it with parent.
            findViewById(R.id.resultDisplay).setVisibility(View.VISIBLE);
            mLayoutResultBackground.animate().alpha(.0f)
                    .setDuration(1000)
                    .setInterpolator(new EaseInterpolators.EaseInOut()).start();
        }

        // Initial step. Scanning front document side.
        if (mCurrentStep == 0 && captureScreen == CaptureScreen.InDetecting
            && !mCurrentStepProcessed) {
            // SDK is loaded and start scanning documents. Display first step.
            // Highlight side menu item
            highlightStep(0, true);

            // Display step detail overlay.
            final DocumentStepDetailView stepDetail = new DocumentStepDetailView(this,
                                                                                 R.string.kyc_doc_scan_detail_top,
                                                                                 R.string.kyc_doc_scan_detail_bottom,
                                                                                 AssetHelper.ASSET_DOC_STEP_ID_CARD_FRONT);
            stepDetail.presentInGroupView(mLayoutPreview, mCaptureFrame, this::resumeSDK);

            // Pause SDK while detail is visible.
            pauseSDK();
            mCurrentStepProcessed = true;
        } else if (mCurrentStep == 0 && captureScreen == CaptureScreen.OutResultOK) {
            // SDK did successfully scanned front document side.
            // Highlight next step in side menu. No overlay view at this point.
            highlightStep(0, false);
            // Use cheap method with already flipped image to save dev time.
            highlightAndRotateStep(1, AssetHelper.ASSET_DOC_STEP_ID_CARD_BACK_FLIPPED);
            ++mCurrentStep;
            mCurrentStepProcessed = false;

            // Do we want overlay here or not?
            // Animate overlay with same progress
            if (mCaptureFrame != null) {
                mCaptureFrame.animate().alpha(.0f).setDuration(500)
                             .setInterpolator(new EaseInterpolators.EaseInOut()).start();
            }
        } else if (mCurrentStep == 1 && captureScreen == CaptureScreen.InDetecting
                   && !mCurrentStepProcessed) {
            // Scanning back document side.
            // Highlight next step in side menu. No overlay view at this point.
            highlightStep(1, false);
            highlightStep(2, true);

            // Display step detail overlay.
            final DocumentStepDetailView stepDetail = new DocumentStepDetailView(this,
                                                                                 R.string.kyc_doc_scan_detail_top,
                                                                                 R.string.kyc_doc_scan_detail_bottom,
                                                                                 AssetHelper.ASSET_DOC_STEP_ID_CARD_BACK);
            stepDetail.presentInGroupView(mLayoutPreview, mCaptureFrame, this::resumeSDK);

            // Pause SDK while detail is visible.
            pauseSDK();
            mCurrentStepProcessed = true;
        }
    }

    private void onScreenChangedPassport(final CaptureScreen captureScreen) {
        // Initial step. Scanning front document side.
        // SDK is loaded and start scanning documents. Display first step.
        if (mCurrentStep == 0 && captureScreen == CaptureScreen.InDetecting
            && !mCurrentStepProcessed) {
            // Highlight side menu item
            highlightStep(0, true);

            // Display step detail overlay.
            final DocumentStepDetailView stepDetail = new DocumentStepDetailView(this,
                                                                                 R.string.kyc_doc_scan_detail_top,
                                                                                 R.string.kyc_doc_scan_detail_bottom,
                                                                                 AssetHelper.ASSET_DOC_STEP_PASSPORT);
            stepDetail.presentInGroupView(mLayoutPreview, mCaptureFrame, this::resumeSDK);

            // Pause SDK while detail is visible.
            pauseSDK();
            mCurrentStepProcessed = true;
        }
    }

    //endregion

    //region Abstract methods

    abstract Camera.Size initIdealCameraResolutionAndUpdateLayout();

    //endregion
}
