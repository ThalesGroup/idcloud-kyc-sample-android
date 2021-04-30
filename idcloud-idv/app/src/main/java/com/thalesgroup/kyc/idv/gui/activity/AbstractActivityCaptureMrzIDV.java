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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.thalesgroup.idv.sdk.mrz.api.CaptureCallback;
import com.thalesgroup.idv.sdk.mrz.api.CaptureResult;
import com.thalesgroup.idv.sdk.mrz.api.CaptureSDK;
import com.thalesgroup.idv.sdk.mrz.api.Configuration;
import com.thalesgroup.kyc.idv.BuildConfig;
import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.gui.view.DocumentStepView;
import com.thalesgroup.kyc.idv.gui.view.DrawMrzOverlayView;
import com.thalesgroup.kyc.idv.helpers.AbstractOption;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCConfiguration;
import com.thalesgroup.kyc.idv.helpers.KYCManager;
import com.thalesgroup.kyc.idv.helpers.util.AssetHelper;


public abstract class AbstractActivityCaptureMrzIDV extends AppCompatActivity
        implements CaptureCallback.InitCallback, CaptureCallback.StartCallback {

    //region Definition

    private static final String TAG = "KYC";

    // UI
    protected LinearLayout mLayoutTutorial = null;
    protected FrameLayout mLayoutPreview = null;
    protected RelativeLayout mCaptureFrame = null;
    protected ImageView mCaptureFrameMrz = null;
    protected TextureView mCamera;
    protected DrawMrzOverlayView mDrawOverlayView;
    protected Configuration mConfiguration = null;

    // Logic
    protected AbstractOption.DocumentType mDocumentType = AbstractOption.DocumentType.IdCard;
    protected boolean mPausing = false;
    protected CaptureSDK mSdk;
    protected boolean mInitializing = false;

    //endregion

    //region Life Cycle

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DataContainer.instance().mDoc = "<Unknown>";
        DataContainer.instance().mDob = "<Unknown>";
        DataContainer.instance().mDoe = "<Unknown>";

        // Get argument to determine document type.
        mDocumentType = (AbstractOption.DocumentType) getIntent().getSerializableExtra(MainActivity.BUNDLE_ARGUMENT_DOC_TYPE);

        mSdk = new CaptureSDK();

        // Load basic visual components.
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

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

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDrawOverlayView.requestLayout();
                    mDrawOverlayView.setLayoutParams(mCamera.getLayoutParams());
                    mDrawOverlayView.invalidate();
                    resumeSDK();
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

    private void startSDK() {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "startSDK()");
        }

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
    //endregion

    //region CaptureCallback.StartCallback
    @Override
    public void onProcessedFrame(final CaptureResult partial) {
        if (KYCManager.getInstance().isDisplayMrzArea()) {
            mDrawOverlayView.drawPoints(partial.quadrangle.topLeft, partial.quadrangle.topRight, partial.quadrangle.bottomLeft, partial.quadrangle.bottomRight);
        }

        if (KYCManager.getInstance().isDisplayMrzData()) {
            mDrawOverlayView.drawContours(partial.contours);
        }
    }


    @Override
    public void onSuccess(final CaptureResult captureResult) {
        try {
            stopSDK();
        } catch (Throwable e) {
            Log.e("KYC", e.toString());
        }

        DataContainer.instance().mDoc = captureResult.doc;
        DataContainer.instance().mDob = captureResult.dob;
        DataContainer.instance().mDoe = captureResult.doe;

        final Intent resultIntent = new Intent();
        setResult(MainActivity.CAPTURE_RETURN_CODE_OK, resultIntent);

        try {
            finish();
        } catch (Throwable e) {
            Log.e("KYC", e.toString());
        }
    }
    //endregion

    //region Private Helpers
    protected abstract void resizeCaptureFrame();

    private void initViews() {
        // Run in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Inflate view.
        setContentView(R.layout.activity_capture_mrz_idv);

        // Side menu with steps.
        mLayoutTutorial = findViewById(R.id.mrz_view_capture_tutorial_layout);

        // Camera preview frame. Used as step detail parent.
        mLayoutPreview = findViewById(R.id.mrz_view_capture_preview_layout);

        // Capture Frame
        mCaptureFrame = findViewById(R.id.mrz_capture_frame);
        mCaptureFrameMrz = findViewById(R.id.capture_frame_mrz);

         resizeCaptureFrame();

        // Camera & Overlay for MRZ
        mCamera = findViewById(R.id.mrz_camera);
        mDrawOverlayView = findViewById(R.id.mrz_draw_overlay);

        // Load side panel with steps based on configuration.
        loadTutorialSteps();
    }

    private void initSDK() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (mPausing) {
                return;
            }

            if (!mInitializing) {
                 mInitializing = true;

                mConfiguration = new Configuration();

                // Init the SDK with success handler
                mSdk.init(KYCConfiguration.IDV_LICENSE, mCamera, this);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDrawOverlayView.requestLayout();
                        mDrawOverlayView.setLayoutParams(mCamera.getLayoutParams());
                        mDrawOverlayView.invalidate();
                    }
                });
            }
        }, 200);
    }

    private void loadTutorialSteps() {
        // In case of some reusable view. Remove all current children.
        mLayoutTutorial.removeAllViews();

        mLayoutTutorial.addView(new DocumentStepView(this, R.string.kyc_mrz_scan, AssetHelper.ASSET_DOC_MRZ));
    }

    private void resumeSDK() {
        startSDK();

        // Resume tracking SDK callbacks.
        runOnUiThread(() -> {
            mCaptureFrame.setVisibility(View.VISIBLE);
        });
    }

    //endregion
}
