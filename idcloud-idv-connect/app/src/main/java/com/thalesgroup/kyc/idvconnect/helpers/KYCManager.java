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
    private final static String KEY_JSON_WEB_TOKEN = "KycPreferenceKeyJsonWebTokenV2";
    private final static String KEY_API_KEY = "KycPreferenceKeyApiKeyV2";

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
                        // Version
                        // #####################
                        new AbstractOption.SectionHeader(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.STRING_KYC_OPTION_SECTION_VERSION)),

                        new AbstractOption.Version(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.STRING_KYC_OPTION_VERSION_APP),
                                BuildConfig.VERSION_NAME),

                        new AbstractOption.Version(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.STRING_ACUANT_OPTION_VERSION),
                                "11.2.5"),

                        new AbstractOption.Version(AbstractOption.OptionSection.Version,
                                mContext.getString(R.string.STRING_JSON_WEB_TOKEN_EXPIRATION),
                                getJWTExpiration(getJsonWebToken())),

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

    /**
     * Retrieves last JSON Web Token or predefined value.
     * @return JWT.
     */
    public String getJsonWebToken() {
        return getValueString(KEY_JSON_WEB_TOKEN, null);
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

    /**
     * Retrieves if automatic detection is set.
     *
     * @return {@code True} if automatic detection is set, else {@code false}.
     */
    public boolean isAutomaticTypeDetection() {
        final String keyAutomaticType = "KycPreferenceKeyAutomaticType"; // NOPMD
        return getValueBoolean(keyAutomaticType, true);
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

    //region Private Helpers

    private boolean setJsonWebToken(final String value) {
        if (getJWTExpiration(value) != null) {
            return setValue(KEY_JSON_WEB_TOKEN, value);
        } else {
            // Do not store invalid token
            return false;
        }
    }

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
            return null;
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
