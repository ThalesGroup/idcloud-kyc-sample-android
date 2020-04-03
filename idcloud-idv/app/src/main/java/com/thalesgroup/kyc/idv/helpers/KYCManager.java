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

package com.thalesgroup.kyc.idv.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.auth0.android.jwt.JWT;
import com.gemalto.ekyc.EKycLicenseConfigurationListener;
import com.gemalto.ekyc.EKycLicenseManager;
import com.gemalto.ekyc.face_capture.FaceCaptureLivenessMode;
import com.gemalto.ekyc.face_capture.FaceCaptureManager;
import com.thalesgroup.kyc.idv.BuildConfig;
import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.gui.animation.EaseInterpolators;
import com.thalesgroup.kyc.idv.gui.fragment.FragmentPrivacyPolicy;
import com.thalesgroup.kyc.idv.gui.fragment.FragmentQRCodeReader;

import net.gemalto.mcidsdk.CaptureSDK;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AlertDialog;

/**
 * Utility methods.
 */
@SuppressWarnings({"WeakerAccess", "SameParameterValue", "FieldCanBeLocal"})
public class KYCManager implements FragmentQRCodeReader.QRCodeReaderDelegate {

    //region Definition

    private static KYCManager sInstance;

    private Context mContext;
    private SharedPreferences mPreferences;

    private boolean mFaceIdInitSuccess;
    private Exception mFaceIdInitError;
    private EKycLicenseConfigurationListener mFaceCompletion = null;

    private final static String SHARED_PREF_KEY = "KYCOptions";

    // GeneralSettings
    private final static String KEY_FACIAL_RECOGNITION = "KycPreferenceKeyFacalRecognition";
    private static final String KEY_MAX_PICTURE_WIDTH = "KycPreferenceKeyMaxPictureWidth";
    private final static String KEY_JSON_WEB_TOKEN = "KycPreferenceKeyJsonWebTokenV2";
    private final static String KEY_API_KEY = "KycPreferenceKeyApiKeyV2";

    // RiskManagement
    private final static String KEY_EXPIRATION_DATE = "KycPreferenceKeyExpirationDate";

    // DocumentScan
    private final static String KEY_MANUAL_SCAN = "KycPreferenceKeyManualScan";
    private final static String KEY_AUTOMATIC_TYPE = "KycPreferenceKeyAutomaticType";
    private final static String KEY_CAMERA_OTIENTATION = "KycPreferenceKeyCameraOrientation";
    private final static String KEY_DETECTION_ZONE = "KycPreferenceKeyDetectionZone";
    private final static String KEY_BW_PHOTO_COPY_QA = "KycPreferenceKeyBwPhotoCopyQA";

    // FaceId
    private final static String KEY_FACE_LIVENESS_MODE = "KycPreferenceKeyLivenessMode";
    private final static String KEY_FACE_LIVENESS_THRESHOLD = "KycPreferenceKeyLivenessThreshold";
    private final static String KEY_FACE_QUALITY_THRESHOLD = "KycPreferenceKeyQualityThreshold";
    private final static String KEY_FACE_BLINK_TIMEOUT = "KycPreferenceKeyBlinkTimeout";

    //endregion

    //region Life Cycle

    /**
     * Gets the singleton instance.
     *
     * @return Singleton instance of {@code Main}.
     */
    public synchronized static KYCManager getInstance() {
        if (sInstance == null) {
            sInstance = new KYCManager();
        }

        return sInstance;
    }

    /**
     * Initializes the instance.
     *
     * @param context
     *         Android context.
     */
    public void initialise(final Context context) {
        mContext = context;
        mPreferences = context.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
        // Start getting face id license etc...
        initFaceId();
    }

    //endregion

    //region Private Helpers

    private List<AbstractOption> createFaceIdSettings() {
        final HashMap<String, String> livenessMode = new LinkedHashMap<>();
        livenessMode.put(FaceCaptureLivenessMode.PASSIVE.toString(),
                mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_PASSIVE));
        livenessMode.put(FaceCaptureLivenessMode.ACTIVE.toString(),
                mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ACTIVE));

        return new ArrayList<>(Arrays.asList(

                new AbstractOption.SectionHeader(AbstractOption.OptionSection.FaceCapture,
                        mContext.getString(R.string.STRING_KYC_OPTION_SECTION_FACEID)),

                new AbstractOption.Segment(AbstractOption.OptionSection.FaceCapture,
                        mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_DES),
                        livenessMode,
                        this::getFaceLivenessMode,
                        this::setFaceLivenessMode),

                new AbstractOption.Number(AbstractOption.OptionSection.FaceCapture,
                        mContext.getString(R.string.STRING_KYC_OPTION_FACE_QUALITY_THRESHOLD_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_FACE_QUALITY_THRESHOLD_DES),
                        0,
                        100,
                        this::getFaceQualityThreshold,
                        this::setFaceQualityThreshold),

                new AbstractOption.Number(AbstractOption.OptionSection.FaceCapture,
                        mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_THRESHOLD_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_THRESHOLD_DES),
                        0,
                        100,
                        this::getFaceLivenessThreshold,
                        this::setFaceLivenessThreshold),

                new AbstractOption.Number(AbstractOption.OptionSection.FaceCapture,
                        mContext.getString(R.string.STRING_KYC_OPTION_FACE_BLINK_TIMEOUT_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_FACE_BLINK_TIMEOUT_DES),
                        0,
                        100,
                        this::getFaceBlinkTimeout,
                        this::setFaceBlinkTimeout)

        ));
    }

    private List<AbstractOption> createVersionSettings() {
        return new ArrayList<>(Arrays.asList(

                new AbstractOption.SectionHeader(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.STRING_KYC_OPTION_SECTION_VERSION)),

                new AbstractOption.Version(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.STRING_KYC_OPTION_VERSION_APP),
                        BuildConfig.VERSION_NAME),

                new AbstractOption.Version(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.STRING_KYC_OPTION_VERSION_ID_SDK),
                        CaptureSDK.version),

                new AbstractOption.Version(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.STRING_KYC_OPTION_VERSION_LIVENESS),
                        FaceCaptureManager.version),

                new AbstractOption.Version(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.STRING_JSON_WEB_TOKEN_EXPIRATION),
                        getJWTExpiration(getJsonWebToken())),

                new AbstractOption.Button(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.STRING_KYC_OPTION_WEB_TOKEN),
                        this::displayQRcodeScannerForInit),

                new AbstractOption.Button(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.legal_privacy_policy),
                        this::openPrivacyPolicy)

        ));
    }

    private List<AbstractOption> createGeneralSettings() {
        return new ArrayList<>(Arrays.asList(
                new AbstractOption.SectionHeader(AbstractOption.OptionSection.General,
                        mContext.getString(R.string.STRING_KYC_OPTION_SECTION_GENERAL)),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.General,
                        mContext.getString(R.string.STRING_KYC_OPTION_FACE_REC_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_FACE_REC_DES),
                        this::isFacialRecognition,
                        this::setFacialRecognition)
        ));
    }

    private List<AbstractOption> createRiskManagementSettings() {
        return new ArrayList<>(Arrays.asList(

                new AbstractOption.SectionHeader(AbstractOption.OptionSection.RiskManagement,
                        mContext.getString(R.string.STRING_KYC_OPTION_SECTION_RISK)),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.RiskManagement,
                        mContext.getString(R.string.STRING_KYC_OPTION_IGNORE_DATE_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_IGNORE_DATE_DES),
                        this::isIgnoreExpirationDate,
                        this::setIgnoreExpirationDate)

        ));
    }

    private List<AbstractOption> createIdentityDocumentScanSettings() {
        return new ArrayList<>(Arrays.asList(

                new AbstractOption.SectionHeader(AbstractOption.OptionSection.IdentityDocumentScan,
                        mContext.getString(R.string.STRING_KYC_OPTION_SECTION_SCAN)),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.IdentityDocumentScan,
                        mContext.getString(R.string.STRING_KYC_OPTION_MANUAL_MODE_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_MANUAL_MODE_DES),
                        this::isManualScan,
                        this::setManualScan),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.IdentityDocumentScan,
                        mContext.getString(R.string.STRING_KYC_OPTION_AUTO_DETECTION_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_AUTO_DETECTION_DES),
                        this::isAutomaticTypeDetection,
                        this::setAutomaticTypeDetection),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.IdentityDocumentScan,
                        mContext.getString(R.string.STRING_KYC_OPTION_CAMERA_ORIENT_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_CAMERA_ORIENT_DES),
                        this::isCameraOrientationPortrait,
                        this::setCameraOrientation),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.IdentityDocumentScan,
                        mContext.getString(R.string.STRING_KYC_OPTION_DETECTION_ZONE_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_DETECTION_ZONE_DES),
                        this::isIdCaptureDetectionZoneReduced,
                        this::setIdCaptureDetectionZone),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.IdentityDocumentScan,
                        mContext.getString(R.string.STRING_KYC_OPTION_BW_PHOTO_COPY_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_BW_PHOTO_COPY_DES),
                        this::isBwPhotoCopyQA,
                        this::setBwPhotoCopyQA)

        ));
    }

    private void initFaceId() {
        mFaceIdInitSuccess = false;
        mFaceIdInitError = null;

        // Important: init the face capture before using it
        EKycLicenseManager.initialize(mContext, Configuration.PRODUCT_KEY, Configuration.SERVER_URL,
                new EKycLicenseConfigurationListener() {
                    @Override
                    public void onLicenseConfigurationSuccess() {
                        // Someone is waiting for init process. Notify it.
                        if (mFaceCompletion != null) {
                            mFaceCompletion.onLicenseConfigurationSuccess();
                            mFaceCompletion = null;
                        }
                        mFaceIdInitSuccess = true;
                        mFaceIdInitError = null;
                    }

                    @Override
                    public void onLicenseConfigurationFailure(final Exception exception) {
                        // Someone is waiting for init process. Notify it.
                        if (mFaceCompletion != null) {
                            mFaceCompletion.onLicenseConfigurationFailure(exception);
                            mFaceCompletion = null;
                        }

                        mFaceIdInitSuccess = false;
                        mFaceIdInitError = exception;
                    }
                });
    }

    private boolean setValue(final String key, final int value) {
        mPreferences.edit().putInt(key, value).apply();
        return true;
    }

    /**
     * Saves the value for a given key.
     *
     * @param value Value.
     * @param key   Key.
     * @return {@code True} if value stored successfully, else {@code false}.
     */
    private boolean setValue(final String key, final boolean value) {
        mPreferences.edit().putBoolean(key, value).apply();
        return true;
    }

    private boolean setValue(final String key, final String value) {
        mPreferences.edit().putString(key, value).apply();
        return true;
    }

    private String getValueString(final String key, final String defaultValue) {
        return mPreferences.getString(key, defaultValue);
    }

    private int getValueInt(final String key, final int defaultValue) {
        return mPreferences.getInt(key, defaultValue);
    }

    /**
     * Retrives the value for a given key.
     *
     * @param key Key.
     * @return Value for given key or default value if value is not saved.
     */
    private boolean getValueBoolean(final String key, final boolean defaultValue) {
        return mPreferences.getBoolean(key, defaultValue);
    }

    private String getJWTExpiration(final String token) {
        try {
            final JWT jwt = new JWT(token);
            return jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : null;
        } catch (final Exception exception) {
            return null;
        }
    }

    //endregion


    //region Public API

    public void initializeFaceIdLicense(final EKycLicenseConfigurationListener completion) {
        if (mFaceIdInitSuccess) {
            // Successfull init already done.
            completion.onLicenseConfigurationSuccess();
        } else {
            // If it's not yet inited. Wait for initializeWithProductKey.
            mFaceCompletion = completion;

            // Something went wrong during init. Try it again.
            if (mFaceIdInitError != null) {
                initFaceId();
            }
        }
    }

    private void openPrivacyPolicy() {
        // Open Terms and Conditions only once.
        final MainActivity activity = (MainActivity) mContext;
        activity.displayFragment(new FragmentPrivacyPolicy(), true, true);
    }

    public void displayQRcodeScannerForInit() {
        // Display QR code reader with current view as delegate.
        final FragmentQRCodeReader fragment = new FragmentQRCodeReader();
        fragment.init(this, 0);

        final MainActivity activity = (MainActivity) mContext;
        activity.displayFragment(fragment, true, true);
    }
    /**
     * Gets the list of side menu options.
     *
     * @return Side menu options.
     */
    public List<AbstractOption> getOptions() {
        final ArrayList<AbstractOption> menuItems = new ArrayList<>();
        menuItems.addAll(createGeneralSettings());
        menuItems.addAll(createRiskManagementSettings());
        menuItems.addAll(createIdentityDocumentScanSettings());
        menuItems.addAll(createFaceIdSettings());
        menuItems.addAll(createVersionSettings());

        return menuItems;
    }

    /**
     * Animates a {@code View} with a given delay.
     *
     * @param view  View to animate.
     * @param delay Delay.
     * @return Delay.
     */
    public static long animateViewWithDelay(final View view, final long delay) {
        final DisplayMetrics metrics = KYCManager.getInstance().mContext.getResources().getDisplayMetrics();
        view.setTranslationX(-metrics.widthPixels);

        final Handler handler = new Handler();
        handler.postDelayed(() -> view.animate().
                translationX(.0f)
                .setDuration(1500)
                .setInterpolator(new EaseInterpolators.EaseOut()), delay);

        return delay + 200;
    }

    @IdRes
    public static int getResId(final String resName, final Class<?> clazz) {
        try {
            final Field idField = clazz.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (final Exception exception) {
            return -1;
        }
    }

    //endregion

    //region Props - GeneralSettings

    /**
     * Sets the facial recognition.
     *
     * @param value Value for facial recognition.
     * @return {@code True} if value stored successfully, else {@code false}.
     */
    public boolean setFacialRecognition(final boolean value) {
        return setValue(KEY_FACIAL_RECOGNITION, value);
    }

    /**
     * Retrieves if facial recognition is set.
     *
     * @return {@code True} if facial recognition is set, else {@code false}.
     */
    public boolean isFacialRecognition() {
        return getValueBoolean(KEY_FACIAL_RECOGNITION, true);
    }

    public int getMaxImageWidth() {
        return getValueInt(KEY_MAX_PICTURE_WIDTH, 1024);
    }

    private boolean setMaxImageWidth(final int value) {
        return setValue(KEY_MAX_PICTURE_WIDTH, value);
    }

    public String getApiKey() {
        return getValueString(KEY_API_KEY, null);
    }

    private void setApiKey(final String apiKey) {
        setValue(KEY_API_KEY, apiKey);
    }

    private boolean setJsonWebToken(final String value) {
        if (getJWTExpiration(value) != null) {
            return setValue(KEY_JSON_WEB_TOKEN, value);
        } else {
            // Do not store invalid token
            return false;
        }
    }

    public String getJsonWebToken() {
        return getValueString(KEY_JSON_WEB_TOKEN, null);
    }

    //endregion

    //region RiskManagement

    public boolean setIgnoreExpirationDate(final boolean value) {
        return setValue(KEY_EXPIRATION_DATE, value);
    }

    public boolean isIgnoreExpirationDate() {
        return getValueBoolean(KEY_EXPIRATION_DATE, false);
    }

    //endregion


    //region Props - DocumentScan

    public boolean setManualScan(final boolean value) {
        return setValue(KEY_MANUAL_SCAN, value);
    }

    public boolean isManualScan() {
        return getValueBoolean(KEY_MANUAL_SCAN, false);
    }

    public boolean setAutomaticTypeDetection(final boolean value) {
        return setValue(KEY_AUTOMATIC_TYPE, value);
    }

    public boolean isAutomaticTypeDetection() {
        return getValueBoolean(KEY_AUTOMATIC_TYPE, true);
    }

    public boolean setCameraOrientation(final boolean value) {
        return setValue(KEY_CAMERA_OTIENTATION, value);
    }

    public boolean isCameraOrientationPortrait() {
        return getValueBoolean(KEY_CAMERA_OTIENTATION, true);
    }

    public boolean setIdCaptureDetectionZone(final boolean value) {
        return setValue(KEY_DETECTION_ZONE, value);
    }

    public boolean isIdCaptureDetectionZoneReduced() {
        return getValueBoolean(KEY_DETECTION_ZONE, false);
    }

    public boolean setBwPhotoCopyQA(final boolean value) {
        return setValue(KEY_BW_PHOTO_COPY_QA, value);
    }

    public boolean isBwPhotoCopyQA() {
        return getValueBoolean(KEY_BW_PHOTO_COPY_QA, false);
    }

    //endregion

    // region Props - FaceId

    public boolean setFaceLivenessMode(final String value) {
        return setValue(KEY_FACE_LIVENESS_MODE, value);
    }

    public String getFaceLivenessMode() {
        return getValueString(KEY_FACE_LIVENESS_MODE, FaceCaptureLivenessMode.PASSIVE.toString());
    }

    public boolean setFaceLivenessThreshold(final int value) {
        return setValue(KEY_FACE_LIVENESS_THRESHOLD, value);
    }

    public int getFaceLivenessThreshold() {
        return getValueInt(KEY_FACE_LIVENESS_THRESHOLD, 0);
    }

    public boolean setFaceQualityThreshold(final int value) {
        return setValue(KEY_FACE_QUALITY_THRESHOLD, value);
    }

    public int getFaceQualityThreshold() {
        return getValueInt(KEY_FACE_QUALITY_THRESHOLD, 50);
    }

    public boolean setFaceBlinkTimeout(final int value) {
        return setValue(KEY_FACE_BLINK_TIMEOUT, value);
    }

    public int getFaceBlinkTimeout() {
        return getValueInt(KEY_FACE_BLINK_TIMEOUT, 15);
    }

    //endregion

    //region QRCodeReaderDelegate

    private void hideQRWithMessage(final String message) {
        final MainActivity activity = (MainActivity) mContext;
        activity.getSupportFragmentManager().popBackStack();
        activity.onDataLayerChanged(getOptions());
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onQRCodeFinished(final FragmentQRCodeReader sender,
                                 final String qrCodeData,
                                 final String error) {
        // Something went wrong on scanner side.
        if (error != null) {
            hideQRWithMessage(error);
        } else {
            // QR Code format is "kyc:<apikey>:<jwt>"
            final String[] elements = qrCodeData.split(":");
            if (elements.length == 3 && "kyc".equals(elements[0])) {
                if (elements[1].isEmpty() || elements[2].isEmpty()) {
                    Log.i("QR Scann", mContext.getString(R.string.STRING_QR_CODE_ERROR_INVALID_DATA));
                    sender.continueScanning();
                } else if (!setJsonWebToken(elements[2])) {
                    Log.i("QR Scann", mContext.getString(R.string.STRING_QR_CODE_ERROR_INVALID_JWT));
                    sender.continueScanning();
                } else {
                    // JWT is already set by previous IF case, now we have to store rest.
                    setApiKey(elements[1]);
                    // Display status information.
                    hideQRWithMessage(mContext.getString(R.string.STRING_QR_CODE_INFO_DONE));
                }
            } else {
                Log.i("QR Scann", mContext.getString(R.string.STRING_QR_CODE_ERROR_FAILED));
                sender.continueScanning();
            }
        }

    }

    //endregion

}
