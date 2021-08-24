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

package com.thalesgroup.kyc.idvconnect.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.auth0.android.jwt.JWT;
import com.thalesgroup.kyc.idvconnect.BuildConfig;
import com.thalesgroup.kyc.idvconnect.R;
import com.thalesgroup.kyc.idvconnect.gui.MainActivity;
import com.thalesgroup.kyc.idvconnect.gui.animation.EaseInterpolators;
import com.thalesgroup.kyc.idvconnect.gui.fragment.FragmentPrivacyPolicy;
import com.thalesgroup.kyc.idvconnect.gui.fragment.FragmentQRCodeReader;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import androidx.annotation.IdRes;

/**
 * Utility methods.
 */
public class KYCManager implements FragmentQRCodeReader.QRCodeReaderDelegate {

    //region Definition

    private static KYCManager sInstance;

    private Context mContext;
    private SharedPreferences mPreferences;

    private final static String SHARED_PREF_KEY = "KYCOptions";

    private static final String KEY_FACIAL_RECOGNITION = "KycPreferenceKeyFacalRecognition";
    private static final String KEY_MAX_PICTURE_WIDTH = "KycPreferenceKeyMaxPictureWidth";
    private final static String KEY_BASIC_CREDENTIALS = "KycPreferenceKeyBasicCredentials";
    private final static String KEY_BASE_URL = "KycPreferenceKeyBaseUrl";
    private final static String KEY_KYC_QR_CODE_VERSION = "KycPreferenceKeyVersion";

    public final static String KYC_QR_CODE_VERSION_KYC2 = "kyc2";

    private final static String KEY_FACE_LIVENESS_MODE = "KycPreferenceKeyLivenessMode";

    private final static String KEY_MANUAL_SCAN = "KycPreferenceKeyManualScan";
    private final static String KEY_SCAN_CHECKS = "KycPreferenceKeyScanChecks";


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
    }

    //endregion

    //region Public API

    /**
     * Gets the list of side menu options.
     *
     * @return Side menu options.
     */
    public List<AbstractOption> getOptions() {
        final HashMap<String, String> livenessMode = new LinkedHashMap<>();
        livenessMode.put(mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ENHANCED_PASSIVE),
                mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ENHANCED_PASSIVE));
        livenessMode.put(mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_PASSIVE),
                mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_PASSIVE));
        livenessMode.put(mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ACTIVE),
                mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ACTIVE));

        return new ArrayList<>(
                Arrays.asList(
                        // #####################
                        // General Settings
                        // #####################
                        new AbstractOption.SectionHeader(AbstractOption.OptionSection.General,
                                mContext.getString(R.string.STRING_KYC_OPTION_SECTION_GENERAL)),

                        new AbstractOption.Checkbox(AbstractOption.OptionSection.General,
                                mContext.getString(R.string.STRING_KYC_OPTION_FACE_REC_CAP),
                                mContext.getString(R.string.STRING_KYC_OPTION_FACE_REC_DES),
                                this::isFacialRecognition, this::setFacialRecognition),

                        // #####################
                        // Face Capture mode
                        // #####################
                        new AbstractOption.SectionHeader(AbstractOption.OptionSection.FaceCapture,
                                mContext.getString(R.string.STRING_KYC_OPTION_SECTION_FACEID)),

                        new AbstractOption.Segment(AbstractOption.OptionSection.FaceCapture,
                                mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_CAP),
                                mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_DES),
                                livenessMode,
                                this::getFaceLivenessMode,
                                this::setFaceLivenessMode),

                        // #####################
                        // Document Capture
                        // #####################
                        new AbstractOption.SectionHeader(AbstractOption.OptionSection.DocCapture,
                                mContext.getString(R.string.STRING_KYC_OPTION_SECTION_DOC_CAPTURE)),

                        new AbstractOption.Checkbox(AbstractOption.OptionSection.DocCapture,
                                mContext.getString(R.string.STRING_KYC_OPTION_MANUAL_MODE_CAP),
                                mContext.getString(R.string.STRING_KYC_OPTION_MANUAL_MODE_DES),
                                this::isManualScan,
                                this::setManualScan),

                        new AbstractOption.Checkbox(AbstractOption.OptionSection.DocCapture,
                                mContext.getString(R.string.STRING_KYC_OPTION_EXT_IMAGE_CHECK_CAP),
                                mContext.getString(R.string.STRING_KYC_OPTION_EXT_IMAGE_CHECK_DES),
                                this::isAdditionalImageChecks,
                                this::setAdditionalImageChecks),

                        // #####################
                        // Version
                        // #####################
                        new AbstractOption.SectionHeader(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.STRING_KYC_OPTION_SECTION_VERSION)),

                        new AbstractOption.Version(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.STRING_KYC_OPTION_VERSION_APP),
                                BuildConfig.VERSION_NAME),

                        new AbstractOption.Version(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.STRING_DOC_SDK_OPTION_VERSION),
                                "11.4.15"),

                        new AbstractOption.Version(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.STRING_ENHANCED_FACE_OPTION_VERSION),
                                "2.9.0"),

                        new AbstractOption.Version(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.STRING_JSON_KYC2_USER_ACCOUNT),
                                getUserAccount()),

                        new AbstractOption.Button(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.STRING_KYC_OPTION_WEB_TOKEN),
                                this::displayQRcodeScannerForInit),

                        new AbstractOption.Button(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.legal_privacy_policy),
                                this::openPrivacyPolicy)
                )
        );
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

    /**
     * Retrieves if facial recognition is set.
     *
     * @return {@code True} if facial recognition is set, else {@code false}.
     */
    public boolean isFacialRecognition() {
        return getValueBoolean(KEY_FACIAL_RECOGNITION, true);
    }

    public boolean setFaceLivenessMode(final String value) {
        return setValue(KEY_FACE_LIVENESS_MODE, value);
    }

    public String getFaceLivenessMode() {
        return getValueString(KEY_FACE_LIVENESS_MODE, mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ENHANCED_PASSIVE));
    }

    public boolean isEnhancedPassiveFaceLivenessMode() {
        return getValueString(KEY_FACE_LIVENESS_MODE, mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ENHANCED_PASSIVE)).equals(mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ENHANCED_PASSIVE));
    }

    public boolean isPassiveFaceLivenessMode() {
        return getValueString(KEY_FACE_LIVENESS_MODE, mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ENHANCED_PASSIVE)).equals(mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_PASSIVE));
    }

    public boolean isActiveFaceLivenessMode() {
        return getValueString(KEY_FACE_LIVENESS_MODE, mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ENHANCED_PASSIVE)).equals(mContext.getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ACTIVE));
    }

    /**
     * Retrieves last JSON Web Token or predefined value.
     * @return JWT.
     */
    public int getMaxImageWidth() {
        return getValueInt(KEY_MAX_PICTURE_WIDTH, 1024);
    }

    private boolean setMaxImageWidth(final int value) {
        return setValue(KEY_MAX_PICTURE_WIDTH, value);
    }

    public String getBaseUrl() {
        return getValueString(KEY_BASE_URL, "");
    }

    private void setBaseUrl(String baseUrl) {
        setValue(KEY_BASE_URL, baseUrl);
    }

    public String getBaseCredentials() {
        return getValueString(KEY_BASIC_CREDENTIALS, null);
    }

    public String getUserAccount() {
        // Default = Base64("Unknown:Unknown")
        String basicAuth = new String(Base64.decode(getValueString(KEY_BASIC_CREDENTIALS, "VW5rbm93OlVua25vd24="), Base64.DEFAULT));

        return basicAuth.substring(0, basicAuth.indexOf(":"));
    }

    private void setBaseCredentials(String basicAuth) {
        setValue(KEY_BASIC_CREDENTIALS, basicAuth);
    }

    public String getKycQRCodeVersion() {
        return getValueString(KEY_KYC_QR_CODE_VERSION, null);
    }

    public void setKycQRCodeVersion(String version) {
        setValue(KEY_KYC_QR_CODE_VERSION, version);
    }

    public void clearCredentials() {
        setValue(KEY_KYC_QR_CODE_VERSION, null);
        setValue(KEY_BASE_URL, null);
        setValue(KEY_BASIC_CREDENTIALS, null);
    }

    /**
     * Return resource if for given string and class (string, assets etc.).
     * @param resName Resource id.
     * @param clazz Resource type class.
     * @return Id of required resource or -1 if does not exists.
     */
    @IdRes
    public static int getResId(final String resName, final Class<?> clazz) {
        try {
            final Field idField = clazz.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (final Exception exception) {
            return -1;
        }
    }

    public void displayQRcodeScannerForInit() {
        // Display QR code reader with current view as delegate.
        final FragmentQRCodeReader fragment = new FragmentQRCodeReader();
        fragment.init(this, 0);

        final MainActivity activity = (MainActivity) mContext;
        activity.displayFragment(fragment, true, true);
    }

    //endregion

    //region Document Capture

    public boolean setManualScan(final boolean value) {
        return setValue(KEY_MANUAL_SCAN, value);
    }

    public boolean isManualScan() {
        return getValueBoolean(KEY_MANUAL_SCAN, false);
    }

    public boolean setAdditionalImageChecks(final boolean value) {
        return setValue(KEY_SCAN_CHECKS, value);
    }

    public boolean isAdditionalImageChecks() {
        return getValueBoolean(KEY_SCAN_CHECKS, true);
    }

    //endregion

    //region Private Helpers

    /**
     * Display privacy policy fragment.
     */
    private void openPrivacyPolicy() {
        // Open Terms and Conditions only once.
        final MainActivity activity = (MainActivity) mContext;
        activity.displayFragment(new FragmentPrivacyPolicy(), true, true);
    }

    /**
     * Sets the facial recognition.
     *
     * @param value Value for facial recognition.
     * @return {@code True} if value stored successfully, else {@code false}.
     */
    private boolean setFacialRecognition(final boolean value) {
        return setValue(KEY_FACIAL_RECOGNITION, value);
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
    private boolean setValue(final String key, final String value) {
        mPreferences.edit().putString(key, value).apply();
        return true;
    }

    /**
     * Retrieves the value for a given key.
     *
     * @param key Key.
     * @return Value for given key or default value if value is not present.
     */
    private boolean getValueBoolean(final String key, final boolean defaultValue) {
        return mPreferences.getBoolean(key, defaultValue);
    }

    /**
     * Retrieves the value for a given key.
     *
     * @param key Key.
     * @return Value for given key or default value if value is not present.
     */
    private String getValueString(final String key, final String defaultValue) {
        return mPreferences.getString(key, defaultValue);
    }

    private int getValueInt(final String key, final int defaultValue) {
        return mPreferences.getInt(key, defaultValue);
    }

    /**
     * Parse entered JWT string and return it's expiration if token is valid. Otherwise null.
     * @param token String of JWT.
     * @return Epiration date or null.
     */
    private String getJWTExpiration(final String token) {
        try {
            final JWT jwt = new JWT(token);
            return jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : null;
        } catch (final Exception exception) {
            return "No JWT";
        }
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
            String separator = qrCodeData.contains("^") ? "\\^" : ":";
            String[] elements = qrCodeData.split(separator);

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "Nb elements: " + elements.length);

                for (int i = 0; i < elements.length; i++) {
                    Log.i("KYC", "#" + i + ": " + elements[i]);
                }
            }

            if(elements.length >= 3) {
                if (elements[0].isEmpty() || elements[1].isEmpty() || elements[2].isEmpty()) {
                    Log.i("QR Scan", mContext.getString(R.string.STRING_QR_CODE_ERROR_INVALID_DATA));
                    sender.continueScanning();
                }

                clearCredentials();

                // Set QR Code version
                setKycQRCodeVersion(elements[0]);

                // QR Code format is "kyc2^<basic credentials(base64encoded)>^<url>"
                if(getKycQRCodeVersion().equals(KYC_QR_CODE_VERSION_KYC2)) {
                    setBaseCredentials(elements[1]);
                    setBaseUrl(elements[2]);
                } else {
                    Log.i("QR Scan", mContext.getString(R.string.STRING_QR_CODE_ERROR_FAILED));
                    sender.continueScanning();
                }

                // Display status information.
                hideQRWithMessage(mContext.getString(R.string.STRING_QR_CODE_INFO_DONE));
            } else {
                Log.i("QR Scan", mContext.getString(R.string.STRING_QR_CODE_ERROR_INVALID_DATA));
                sender.continueScanning();
            }
        }
    }

    //endregion

    public String getErrorMessage(String errorCode, String defaultMessage) {
        String[] errorCodes = mContext.getResources().getStringArray(R.array.error_codes);
        String[] errorMessages = mContext.getResources().getStringArray(R.array.error_messages);

        for (int i = 0; i < errorCodes.length; i++) {
            if (errorCodes[i].equals(errorCode)) {
                return errorMessages[i];
            }
        }

        return defaultMessage;
    }

    public String getErrorMessage(String errorCode) {
        String[] errorCodes = mContext.getResources().getStringArray(R.array.error_codes);
        String[] errorMessages = mContext.getResources().getStringArray(R.array.error_messages);

        for (int i = 0; i < errorCodes.length; i++) {
            if (errorCodes[i].equals(errorCode)) {
                String result = errorMessages[i];
                return result;
            }
        }

        return "Unknown error!";
    }
}
