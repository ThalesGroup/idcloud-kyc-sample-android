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
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.auth0.android.jwt.JWT;
import com.thalesgroup.idv.sdk.doc.api.CaptureSDK;
import com.thalesgroup.idv.sdk.doc.api.Configuration;
import com.thalesgroup.kyc.idv.BuildConfig;
import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.gui.animation.EaseInterpolators;
import com.thalesgroup.kyc.idv.gui.fragment.FragmentPrivacyPolicy;
import com.thalesgroup.kyc.idv.gui.fragment.FragmentQRCodeReader;

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
@SuppressWarnings({"WeakerAccess", "SameParameterValue"})
public class KYCManager implements FragmentQRCodeReader.QRCodeReaderDelegate {

    //region Definition
    public final static String LIVENESS_NO = "No liveness";
    public final static String LIVENESS_ENHANCED = "Enhanced Passive";

    private static KYCManager sInstance;

    private Context mContext;
    private SharedPreferences mPreferences;

    private final static String SHARED_PREF_KEY = "KYCOptions";

    // GeneralSettings
    private final static String KEY_FACIAL_RECOGNITION = "KycPreferenceKeyFacalRecognition";
    private final static String KEY_BASIC_CREDENTIALS = "KycPreferenceKeyBasicCredentials";
    private final static String KEY_BASE_URL = "KycPreferenceKeyBaseUrl";
    private final static String KEY_KYC_QR_CODE_VERSION = "KycPreferenceKeyVersion";

    public final static String KYC_QR_CODE_VERSION_KYC2 = "kyc2";

    // DocumentScan
    private final static String KEY_MANUAL_SCAN = "KycPreferenceKeyManualScan";
    private final static String KEY_EDGE_MODE = "KycPreferenceKeyEdgeMode";
    private final static String KEY_BLUR_QC = "KycPreferenceKeyEnabledBlurQC";
    private final static String KEY_GLARE_QC = "KycPreferenceKeyEnabledGlareQC";
    private final static String KEY_DARK_QC = "KycPreferenceKeyEnabledDarkQC";
    private final static String KEY_BW_QC = "KycPreferenceKeyEnabledBwQC";

    // FaceId
    private final static String KEY_FACE_LIVENESS_MODE = "KycPreferenceKeyLivenessMode";

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

    //region Private Helpers

    private List<AbstractOption> createVersionSettings() {
        return new ArrayList<>(Arrays.asList(

                new AbstractOption.SectionHeader(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.STRING_KYC_OPTION_SECTION_VERSION)),

                new AbstractOption.Version(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.STRING_KYC_OPTION_VERSION_APP),
                        BuildConfig.VERSION_NAME),

                new AbstractOption.Version(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.STRING_KYC_OPTION_VERSION_DOC_SDK),
                        CaptureSDK.version),

                new AbstractOption.Version(AbstractOption.OptionSection.Version,
                        mContext.getString(R.string.STRING_KYC_OPTION_VERSION_LIVENESS),
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

    private List<AbstractOption> createDocumentScanSettings() {
        return new ArrayList<>(Arrays.asList(
                new AbstractOption.SectionHeader(AbstractOption.OptionSection.DocumentScan,
                        mContext.getString(R.string.STRING_KYC_OPTION_SECTION_SCAN)),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.DocumentScan,
                        mContext.getString(R.string.STRING_KYC_OPTION_MANUAL_MODE_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_MANUAL_MODE_DES),
                        this::isManualScan,
                        this::setManualScan)
        ));
    }

    private List<AbstractOption> createDocumentScanConfigurationSettings() {
        final HashMap<String, String> edgeMode = new LinkedHashMap<>();
        edgeMode.put(mContext.getString(R.string.STRING_KYC_OPTION_EDGE_MODE_ML), mContext.getString(R.string.STRING_KYC_OPTION_EDGE_MODE_ML));
        edgeMode.put(mContext.getString(R.string.STRING_KYC_OPTION_EDGE_MODE_IP), mContext.getString(R.string.STRING_KYC_OPTION_EDGE_MODE_IP));

        return new ArrayList<>(Arrays.asList(
                new AbstractOption.SectionHeader(AbstractOption.OptionSection.DocumentConfig,
                        mContext.getString(R.string.STRING_KYC_OPTION_SECTION_SCAN_CONFIG)),

                new AbstractOption.Segment(AbstractOption.OptionSection.DocumentConfig,
                        mContext.getString(R.string.STRING_KYC_OPTION_EDGE_MODE_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_EDGE_MODE_DES),
                        edgeMode,
                        this::getEdgeMode,
                        this::setEdgeMode),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.DocumentConfig,
                        mContext.getString(R.string.STRING_KYC_OPTION_ENABLE_BLUR_CHECK_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_ENABLE_BLUR_CHECK_DES),
                        this::isEnabledBlurQC,
                        this::setEnabledBlurQC),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.DocumentConfig,
                        mContext.getString(R.string.STRING_KYC_OPTION_ENABLE_GLARE_CHECK_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_ENABLE_GLARE_CHECK_DES),
                        this::isEnabledGlareQC,
                        this::setEnabledGlareQC),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.DocumentConfig,
                        mContext.getString(R.string.STRING_KYC_OPTION_ENABLE_DARK_CHECK_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_ENABLE_DARK_CHECK_DES),
                        this::isEnabledDarkQC,
                        this::setEnabledDarkQC),

                new AbstractOption.Checkbox(AbstractOption.OptionSection.DocumentConfig,
                        mContext.getString(R.string.STRING_KYC_OPTION_ENABLE_BW_CHECK_CAP),
                        mContext.getString(R.string.STRING_KYC_OPTION_ENABLE_BW_CHECK_DES),
                        this::isEnabledBwQC,
                        this::setEnabledBwQC)
        ));
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
         menuItems.addAll(createDocumentScanSettings());
        menuItems.addAll(createDocumentScanConfigurationSettings());
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

    public String getBaseUrl() {
        return getValueString(KEY_BASE_URL, "");
    }

    private void setBaseUrl(String baseUrl) {
        setValue(KEY_BASE_URL, baseUrl);
    }

    public String getBaseCredentials() {
        return getValueString(KEY_BASIC_CREDENTIALS, null);
    }

    private void setBaseCredentials(String baseUrl) {
        setValue(KEY_BASIC_CREDENTIALS, baseUrl);
    }

    public String getUserAccount() {
        // Default = Base64("Unknown:Unknown")
        String basicAuth = new String(Base64.decode(getValueString(KEY_BASIC_CREDENTIALS, "VW5rbm93OlVua25vd24="), Base64.DEFAULT));

        return basicAuth.substring(0, basicAuth.indexOf(":"));
    }

    public String getKycQRCodeVersion() {
        return getValueString(KEY_KYC_QR_CODE_VERSION, null);
    }

    public void setKycQRCodeVersion(String version) {
        setValue(KEY_KYC_QR_CODE_VERSION, version);
    }

    public void clearCredentials() {
        setValue(KEY_BASE_URL, null);
        setValue(KEY_BASIC_CREDENTIALS, null);
    }
//endregion

    //region QC Management
    public boolean setEdgeMode(final String value) {
        return setValue(KEY_EDGE_MODE, value);
    }

    public String getEdgeMode() {
        return getValueString(KEY_EDGE_MODE, mContext.getString(R.string.STRING_KYC_OPTION_EDGE_MODE_ML));
    }

    public int getConfigEdgeMode() {
        String mode = getValueString(KEY_EDGE_MODE, mContext.getString(R.string.STRING_KYC_OPTION_EDGE_MODE_ML));

        if (mode.equals(mContext.getString(R.string.STRING_KYC_OPTION_EDGE_MODE_ML))) {
            return Configuration.MachineLearning;
        } else {
            return Configuration.ImageProcessing;
        }
    }

    public boolean setEnabledBlurQC(final boolean value) {
        return setValue(KEY_BLUR_QC, value);
    }

    public boolean isEnabledBlurQC() {
        return getValueBoolean(KEY_BLUR_QC, true);
    }

    public boolean setEnabledGlareQC(final boolean value) {
        return setValue(KEY_GLARE_QC, value);
    }

    public boolean isEnabledGlareQC() {
        return getValueBoolean(KEY_GLARE_QC, true);
    }

    public boolean setEnabledDarkQC(final boolean value) {
        return setValue(KEY_DARK_QC, value);
    }

    public boolean isEnabledDarkQC() {
        return getValueBoolean(KEY_DARK_QC, true);
    }

    public boolean setEnabledBwQC(final boolean value) {
        return setValue(KEY_BW_QC, value);
    }

    public boolean isEnabledBwQC() {
        return getValueBoolean(KEY_BW_QC, true);
    }
    //endregion


    //region Props - DocumentScan

    public boolean setManualScan(final boolean value) {
        return setValue(KEY_MANUAL_SCAN, value);
    }

    public boolean isManualScan() {
        return getValueBoolean(KEY_MANUAL_SCAN, false);
    }

    //endregion

    // region Props - FaceId

    public boolean setFaceLivenessMode(final String value) {
        return setValue(KEY_FACE_LIVENESS_MODE, value);
    }

    public String getFaceLivenessMode() {
        return isFacialRecognition() ?
                getValueString(KEY_FACE_LIVENESS_MODE, LIVENESS_ENHANCED)
                : LIVENESS_NO;
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
