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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.thalesgroup.idv.sdk.doc.api.CaptureCallback;
import com.thalesgroup.idv.sdk.doc.api.CaptureResult;
import com.thalesgroup.idv.sdk.doc.api.CaptureSDK;
import com.thalesgroup.idv.sdk.doc.api.Configuration;
import com.thalesgroup.idv.sdk.doc.api.Document;
import com.thalesgroup.kyc.idv.BuildConfig;
import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.gui.view.DocumentStepDetailView;
import com.thalesgroup.kyc.idv.gui.view.DocumentStepView;
import com.thalesgroup.kyc.idv.helpers.AbstractOption;
import com.thalesgroup.kyc.idv.helpers.KYCConfiguration;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCManager;
import com.thalesgroup.kyc.idv.helpers.util.AssetHelper;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;


public abstract class AbstractActivityCaptureDocIDV extends AppCompatActivity
        implements CaptureCallback.InitCallback, CaptureCallback.StartCallback {

    //region Definition

    private static final String TAG = "KYC";

    // UI
    protected LinearLayout mLayoutTutorial = null;
    protected FrameLayout mLayoutPreview = null;
    protected ImageButton mShutterButton = null;
    protected RelativeLayout mCaptureFrame = null;
    protected ImageView mCaptureFrameMrz = null;
    protected Configuration mConfiguration = null;

    protected RelativeLayout mLayoutResult = null;
    protected ImageView mResultImage = null;
    protected Button mResultOkButton = null;
    protected Button mResultKoButton = null;

    protected RelativeLayout mLayoutChecks = null;
    protected TextView mCheckBlur = null;
    protected TextView mCheckGlare = null;
    protected TextView mCheckContrast = null;
    protected TextView mCheckDarkness = null;
    protected TextView mCheckFocus = null;
    protected TextView mCheckBW = null;

    // Logic
    protected AbstractOption.DocumentType mDocumentType = AbstractOption.DocumentType.IdCard;
    protected int mCurrentStep = 0;
    protected boolean mPaused = false;
    protected boolean mAutocapture = false;
    protected CaptureSDK mSdk;
    protected boolean mPausing = false;
    protected boolean mInitializing = false;

    //endregion

    //region Life Cycle
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get argument to determine document type.
        mDocumentType = (AbstractOption.DocumentType) getIntent().getSerializableExtra(MainActivity.BUNDLE_ARGUMENT_DOC_TYPE);

        mSdk = new CaptureSDK();

        // Load basic visual components.
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCurrentStep = 0;
        mPaused = false;
        mAutocapture = false;
        mPausing = false;

        // Initialise SDK with calculated ideal size.
        initSDK();
    }

    @Override
    public void onPause() {
        mPausing = true;

        if (!mInitializing) {
            try {
                if (mSdk != null) {
                    mSdk.stop();
                    mSdk.finish();
                }
            } catch (Throwable e) {
                Log.e("KYC", e.toString());
            }
        }

        mInitializing = false;

        super.onPause();
    }

    //endregion

    //region SDK CaptureCallback.InitCallback
    @Override
    public void onInit(boolean isCompleted, int errorCode) {
        mInitializing = false;

        if (isCompleted) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "SDK init OK!");
            }

            // Handle ID related operations like display step etc...
            runOnUiThread(() -> {
                if (mDocumentType == AbstractOption.DocumentType.IdCard) {
                    onScreenChangedIdCard();
                } else if (mDocumentType == AbstractOption.DocumentType.Passport) {
                    onScreenChangedPassport();
                }
            });
        } else {
            Log.e(TAG, "Error on init: 0x" + Integer.toString(errorCode, 16));
            int errorType = (errorCode & 0x0F00);
            if(errorType == 0x0000){
                String[] errorMsg = { "No error found", "Unknown error", "Library not initialized", "Library is already initialized",
                        "Library init is in progress", "Library stop is in progress", "Library lazy finished",
                        "Error initializing IQA library", "Error initializing Engine", "Invalid cropping points",
                        "Invalid image sign", "Invalid image" };

                Log.w(TAG, " - Init error: " + errorMsg[(errorCode & 0xFF)]);
            }
            if (errorType == 0x0100) {
                String[] errorMsg = { "No error found", "Architecture not supported",
                        "Operating system not supported", "App permissions has been denied" };
                Log.w(TAG, " - Requirement error: " + errorMsg[(errorCode & 0xFF)]);
            }
            if (errorType == 0x0200) {
                String[] errorMsg = { "No error found", "The license is empty", "The license is expired",
                        "The feature selected is not present", "The license format is not correct",
                        "The license has not a valid signing hash", "The license has not a valid version",
                        "The license has not a valid feature list",
                        "The license has not a valid expiration date",
                        "The license has not a valid creation timestamp",
                        "The license has not a valid cache value" };
                Log.w(TAG, " - License error: " + errorMsg[(errorCode & 0xFF)]);
            }
        }
    }

    //endregion

    //region CaptureCallback.StartCallback
    @Override
    public void onProcessedFrame(final CaptureResult result) {
//        if (BuildConfig.DEBUG) {
//            Log.w(TAG, "onProcessedFrame()");
//            Log.i("KYC", "Error Code: " + result.errorCode);
//            Log.i("KYC", "QCR.all: " + result.qualityCheckResults.all);
//            Log.i("KYC", "QCR.noFocused: " + result.qualityCheckResults.noFocused);
//            Log.i("KYC", "QCR.blur: " + result.qualityCheckResults.blur);
//            Log.i("KYC", "QCR.glare: " + result.qualityCheckResults.glare);
//            Log.i("KYC", "QCR.photocopy: " + result.qualityCheckResults.photocopy);
//            Log.i("KYC", "QCR.contrast: " + result.qualityCheckResults.contrast);
//            Log.i("KYC", "QCR.darkness: " + result.qualityCheckResults.darkness);
//        }

        // Update quality checks on UI
        runOnUiThread(() -> {
            // Ignore all messages while SDK is paused and same as last time.
            if (!mPaused) {
                CaptureResult.QualityCheckResults warnings = result.qualityCheckResults;

                if (warnings.blur) {
                    mCheckBlur.setTextColor(Color.RED);
                } else if (KYCManager.getInstance().isEnabledBlurQC()) {
                    mCheckBlur.setTextColor(Color.GREEN);
                }

                if (warnings.glare) {
                    mCheckGlare.setTextColor(Color.RED);
                } else if (KYCManager.getInstance().isEnabledGlareQC()) {
                    mCheckGlare.setTextColor(Color.GREEN);
                }

                if (warnings.contrast) {
                    mCheckContrast.setTextColor(Color.RED);
                } else {
                    mCheckContrast.setTextColor(Color.GREEN);
                }

                if (warnings.darkness) {
                    mCheckDarkness.setTextColor(Color.RED);
                } else if (KYCManager.getInstance().isEnabledDarkQC()) {
                    mCheckDarkness.setTextColor(Color.GREEN);
                }

                if (warnings.noFocused) {
                    mCheckFocus.setTextColor(Color.RED);
                } else {
                    mCheckFocus.setTextColor(Color.GREEN);
                }

                if (warnings.photocopy) {
                    mCheckBW.setTextColor(Color.RED);
                } else if (KYCManager.getInstance().isEnabledBwQC()) {
                    mCheckBW.setTextColor(Color.GREEN);
                }
            }
        });
    }

    @Override
    public void onSuccess(final CaptureResult captureResult) {
        byte[] croppedImage;

        if (BuildConfig.DEBUG) {
            Log.w(TAG, "onSuccess()");
        }

        stopSDK();

        // Get Cropped Image
        if (captureResult.cropFrame != null) {
            croppedImage = captureResult.cropFrame.clone();
        }

        // No cropped image -> try to get full image
        else {
            Log.w(TAG, "No cropped image -> Get full image...");

            croppedImage = captureResult.fullFrame.clone();

            if (croppedImage == null) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "No cropped image -> Restart capture...");
                }

                startSDK();

                return;
            }
        }

        // Front side
        if (mCurrentStep == 1) {
            DataContainer.instance().mDocFront = croppedImage;
        }

        // Back side
        else if (mCurrentStep == 3) {
            DataContainer.instance().mDocBack = croppedImage;
        }

        // Handle ID related operations like display step etc...
        runOnUiThread(() -> {
            mLayoutChecks.setVisibility(View.INVISIBLE);

            mLayoutResult.setVisibility(View.VISIBLE);
            mResultImage.setVisibility(View.VISIBLE);
            showBitmap(croppedImage, mResultImage);
            mResultOkButton.setVisibility(View.VISIBLE);
            mResultKoButton.setVisibility(View.VISIBLE);
        });
    }
    //endregion

    //region Private Helpers

    private void startSDK() {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "startSDK()");
        }

        applyConfig();

        try {
            if (mSdk != null) {
                mSdk.start(mConfiguration, this);
            }
        } catch (Throwable e) {
            Log.e("KYC", e.toString());
        }
    }

    private void stopSDK() {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "stopSDK()");
        }

        try {
            if (mSdk != null) {
                mSdk.stop();
            }
        } catch (Throwable e) {
            Log.e("KYC", e.toString());
        }
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

        // Quality checks: Blurred, hotspot etc...
        mLayoutChecks = findViewById(R.id.capture_quality_checks);
        mCheckBlur = findViewById(R.id.check_blur);
        mCheckGlare = findViewById(R.id.check_glare);
        mCheckContrast = findViewById(R.id.check_contrast);
        mCheckDarkness = findViewById(R.id.check_darkness);
        mCheckFocus = findViewById(R.id.check_focus);
        mCheckBW = findViewById(R.id.check_bw);

        // Manually control shutter button visibility.
        mShutterButton = findViewById(R.id.take_photo_button);

        // Result Layout
        mLayoutResult = findViewById(R.id.capture_result_layout);
        mResultImage = findViewById(R.id.resultDisplay);
        mResultOkButton = findViewById(R.id.result_accept_button);
        mResultKoButton = findViewById(R.id.result_reject_button);

        // Capture frame & Hourglass
        mCaptureFrame = findViewById(R.id.capture_frame);
        mCaptureFrameMrz = findViewById(R.id.capture_frame_mrz);

        resizeCaptureFrame();

        mLayoutChecks.setVisibility(View.INVISIBLE);
        mShutterButton.setAlpha(0.f);
        mShutterButton.setVisibility(View.INVISIBLE);
        mLayoutResult.setVisibility(View.INVISIBLE);
        mResultImage.setVisibility(View.INVISIBLE);
        mResultOkButton.setVisibility(View.INVISIBLE);
        mResultKoButton.setVisibility(View.INVISIBLE);

        // Load side panel with steps based on configuration.
        loadTutorialSteps();

        // Shutter Button
        mShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSdk.triggerCapture();
            }
        });

        // Result Accept Button
        mResultOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDocumentType == AbstractOption.DocumentType.IdCard) {
                    // Front side
                    if (mCurrentStep == 1) {
                        mLayoutResult.setVisibility(View.INVISIBLE);
                        mLayoutChecks.setVisibility(View.INVISIBLE);

                        onScreenChangedIdCard();
                    }

                    // Back side
                    if (mCurrentStep == 3) {
                        mLayoutResult.setVisibility(View.INVISIBLE);
                        mLayoutChecks.setVisibility(View.INVISIBLE);

                        final Intent resultIntent = new Intent();
                        setResult(MainActivity.CAPTURE_RETURN_CODE_OK, resultIntent);
                        finish();
                    }
                }

                else if (mDocumentType == AbstractOption.DocumentType.Passport) {
                    mLayoutResult.setVisibility(View.INVISIBLE);
                    mLayoutChecks.setVisibility(View.INVISIBLE);

                    final Intent resultIntent = new Intent();
                    setResult(MainActivity.CAPTURE_RETURN_CODE_OK, resultIntent);
                    finish();
                }
            }
        });

        // Result Reject Button
        mResultKoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDocumentType == AbstractOption.DocumentType.IdCard) {
                    mCurrentStep--;

                    mLayoutResult.setVisibility(View.INVISIBLE);
                    mLayoutChecks.setVisibility(View.INVISIBLE);

                    onScreenChangedIdCard();
                }

                else if (mDocumentType == AbstractOption.DocumentType.Passport) {
                    mCurrentStep--;

                    mLayoutResult.setVisibility(View.INVISIBLE);
                    mLayoutChecks.setVisibility(View.INVISIBLE);

                    onScreenChangedPassport();
                }
            }
        });
    }

    private void applyConfig() {
        mConfiguration = new Configuration();

        // Detection Mode
        mConfiguration.detectionMode = KYCManager.getInstance().getConfigEdgeMode();

        // Document Type
        if (KYCManager.getInstance().getDocType() == AbstractOption.DocumentType.Passport) {
            mConfiguration.captureDocuments = Document.DocumentModePassport;
        } else {
            mConfiguration.captureDocuments = Document.DocumentModeIdDocument;
        }

        // Enable / Disable QC depending on configuration

        // Blur
        if (!KYCManager.getInstance().isEnabledBlurQC()) {
            mConfiguration.qualityChecks.blurDetectionMode = Configuration.Disabled;
        } else {
            mConfiguration.qualityChecks.blurDetectionMode = Configuration.Strict;
        }

        // Glare
        if (!KYCManager.getInstance().isEnabledGlareQC()) {
            mConfiguration.qualityChecks.glareDetectionMode = Configuration.Disabled;
        } else {
            mConfiguration.qualityChecks.glareDetectionMode = Configuration.Color;
        }

        // Darkness
        if (!KYCManager.getInstance().isEnabledDarkQC()) {
            mConfiguration.qualityChecks.darknessDetectionMode = Configuration.Disabled;
        } else {
            mConfiguration.qualityChecks.darknessDetectionMode = Configuration.Relaxed;
        }

        // BW photocopy
        if (!KYCManager.getInstance().isEnabledBwQC()) {
            mConfiguration.qualityChecks.photocopyDetectionMode = Configuration.Disabled;
        } else {
            mConfiguration.qualityChecks.photocopyDetectionMode = Configuration.BlackAndWhite;
        }

        if (BuildConfig.DEBUG) {
            Log.w(TAG, "SDK Configuration:");
            Log.i(TAG, "Detection Mode: " + mConfiguration.detectionMode);
            Log.i(TAG, "Blur: " + mConfiguration.qualityChecks.blurDetectionMode);
            Log.i(TAG, "Glare: " + mConfiguration.qualityChecks.glareDetectionMode);
            Log.i(TAG, "Darkness: " + mConfiguration.qualityChecks.darknessDetectionMode);
            Log.i(TAG, "BW: " + mConfiguration.qualityChecks.photocopyDetectionMode);
        }

        // Update UI
        runOnUiThread(() -> {
            // Blur
            if (!KYCManager.getInstance().isEnabledBlurQC()) {
                mCheckBlur.setTextColor(Color.GRAY);
            }

            // Glare
            if (!KYCManager.getInstance().isEnabledGlareQC()) {
                mCheckGlare.setTextColor(Color.GRAY);
            }

            // Darkness
            if (!KYCManager.getInstance().isEnabledDarkQC()) {
                mCheckDarkness.setTextColor(Color.GRAY);
            }

            // BW photocopy
            if (!KYCManager.getInstance().isEnabledBwQC()) {
                mCheckBW.setTextColor(Color.GRAY);
            }
        });
    }

    private void initSDK() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (mPausing) {
                return;
            }

            if (!mInitializing) {
                mInitializing = true;

                final KYCManager managerKYC = KYCManager.getInstance();

                // Display/Hide shutter button and do capture manually/automatically
                mAutocapture = !managerKYC.isManualScan();

                // Init the SDK with success handler
                TextureView view = findViewById(R.id.camera);
                mSdk.init(KYCConfiguration.IDV_LICENSE, view, this);
            }
        }, 200);
    }

    protected abstract void resizeCaptureFrame();

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

    private void turnCard() {
        runOnUiThread(() -> {
            mLayoutResult.setVisibility(View.INVISIBLE);
            mLayoutChecks.setVisibility(View.INVISIBLE);
        });

        onScreenChangedIdCard();
    }

    private void pauseSDK() {
        runOnUiThread(() -> {
            mLayoutChecks.setVisibility(View.INVISIBLE);
            mShutterButton.setAlpha(0.f);
            mShutterButton.setVisibility(View.INVISIBLE);
            mCaptureFrame.setVisibility(View.INVISIBLE);
        });

        // Pause tracking SDK callbacks.
        mPaused = true;
    }

    private void resumeSDK() {
        startSDK();

        runOnUiThread(() -> {
            mLayoutResult.setVisibility(View.INVISIBLE);
            mResultImage.setVisibility(View.INVISIBLE);
            mResultOkButton.setVisibility(View.INVISIBLE);
            mResultKoButton.setVisibility(View.INVISIBLE);

            mCaptureFrame.setVisibility(View.VISIBLE);

            // We have to update shutter button manually.
            if (!mAutocapture) {
                mShutterButton.setAlpha(1.f);
                mShutterButton.setVisibility(View.VISIBLE);
            }

            mLayoutChecks.setVisibility(View.VISIBLE);
        });

        // Resume tracking SDK callbacks.
        mPaused = false;
    }

    private void onScreenChangedIdCard() {
        // Scanning front document side.
        if (mCurrentStep == 0 ) {
            // SDK is loaded and start scanning documents. Display first step.
            // Highlight side menu item
            highlightStep(0, true);

            // Display step detail overlay.
            final DocumentStepDetailView stepDetail = new DocumentStepDetailView(this,
                                                                                 R.string.kyc_doc_scan_detail_top,
                                                                                 R.string.kyc_doc_scan_detail_bottom,
                                                                                 AssetHelper.ASSET_DOC_STEP_ID_CARD_FRONT);
            stepDetail.presentInGroupView(mLayoutPreview, mCaptureFrame, this::resumeSDK, true);

            // Pause SDK while detail is visible.
            pauseSDK();
            mCurrentStep++;
        }
        // Turn the document on back side.
        else if (mCurrentStep == 1 ) {
            // SDK did successfully scanned front document side.
            // Highlight next step in side menu. No overlay view at this point.
            highlightStep(0, false);

            // Use cheap method with already flipped image to save dev time.
            highlightAndRotateStep(1, AssetHelper.ASSET_DOC_STEP_ID_CARD_BACK_FLIPPED);

            // Display step detail overlay.
            final DocumentStepDetailView stepDetail = new DocumentStepDetailView(this,
                    R.string.kyc_doc_turn_detail_top,
                    R.string.kyc_doc_turn_detail_bottom,
                    AssetHelper.ASSET_DOC_STEP_ID_CARD_BACK_FLIPPED);
            stepDetail.presentInGroupView(mLayoutPreview, mCaptureFrame, this::turnCard, true);

            // Pause SDK while detail is visible.
            pauseSDK();
            mCurrentStep++;

            // Auto hide Success result
            Timer timer = new Timer(true);

            try
            {
                TimerTask timerTask = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mLayoutResult.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                };

                timer.schedule(timerTask, 3000);
            }
            catch(Exception e)
            {
                Log.e("KYC", e.toString());
            }
        }

        // Scanning back document side.
        else if (mCurrentStep == 2 ) {
            // Scanning back document side.
            // Highlight next step in side menu. No overlay view at this point.
            highlightStep(1, false);
            highlightStep(2, true);

            resumeSDK();
            mCurrentStep++;
        }
    }

    private void onScreenChangedPassport() {
        // Highlight side menu item
        highlightStep(0, true);

        // Display step detail overlay.
        final DocumentStepDetailView stepDetail = new DocumentStepDetailView(this,
                                                                             R.string.kyc_doc_scan_detail_top,
                                                                             R.string.kyc_doc_scan_detail_bottom,
                                                                             AssetHelper.ASSET_DOC_STEP_PASSPORT);
        stepDetail.presentInGroupView(mLayoutPreview, mCaptureFrame, this::resumeSDK, true);

        // Pause SDK while detail is visible.
        pauseSDK();
        mCurrentStep++;
    }

    private void showBitmap(final byte[] data,
                            final ImageView imageView) {
        if (data != null) {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            imageView.setMinimumHeight(displayMetrics.heightPixels);
            imageView.setMinimumWidth(displayMetrics.widthPixels);
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }
    //endregion
}
