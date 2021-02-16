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

package com.thalesgroup.kyc.idvconnect.helpers.util;

import android.util.Log;

import com.thalesgroup.kyc.idvconnect.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for JSON operations.
 */
public class JsonUtil {

    //region Public API

    /**
     * Gets JSON {@code String} value from {@code JSONObject}.
     *
     * @param json
     *         Input {@code JSONObject} object to parse.
     * @param key
     *         Key.
     * @param defaultValue
     *         Default value if key is not present in {@code JSONObject} object.
     * @return Parsed value or default value if not present.
     */
    public static String jsonGetString(final JSONObject json,
                                       final String key,
                                       final String defaultValue) {
        if (json == null) {
            return defaultValue;
        }

        try {
            return json.getString(key);
        } catch (final JSONException e) {
            return defaultValue;
        }
    }

    /**
     * Gets JSON {@code int} value from {@code JSONObject}.
     *
     * @param json
     *         Input {@code JSONObject} object to parse.
     * @param key
     *         Key.
     * @param defaultValue
     *         Default value if key is not present in {@code JSONObject} object.
     * @return Parsed value or default value if not present.
     */
    public static int jsonGetInt(final JSONObject json, final String key, final int defaultValue) {
        try {
            return json.getInt(key);
        } catch (final JSONException e) {
            return defaultValue;
        }
    }

    /**
     * Gets JSON {@code boolean} value from {@code JSONObject}.
     *
     * @param json
     *         Input {@code JSONObject} object to parse.
     * @param key
     *         Key.
     * @param defaultValue
     *         Default value if key is not present in {@code JSONObject} object.
     * @return Parsed value or default value if not present.
     */
    public static boolean jsonGetBoolean(final JSONObject json, final String key, final boolean defaultValue) {
        try {
            return json.getBoolean(key);
        } catch (final JSONException e) {
            return defaultValue;
        }
    }

    /**
     * Gets JSON {@code String[]} value from {@code JSONObject}.
     *
     * @param json
     *         Input {@code JSONObject} object to parse.
     * @param key
     *         Key.
     * @param defaultValue
     *         Default value if key is not present in {@code JSONObject} object.
     * @return Parsed value or default value if not present.
     */
    public static String[] jsonGetStringArray(final JSONObject json, final String key, final String[] defaultValue) {
        if (json == null) {
            return null;
        }

        try {
            JSONArray strArray = json.getJSONArray(key);
            String[] strings = new String[strArray.length()];

                for (int i = 0; i < strArray.length(); i++) {
                    strings[i] = strArray.getString(i);
                }

            return strings;
        } catch (final JSONException e) {
            return defaultValue;
        }
    }

    public static void logJson(String jsonString, String name) {
        if (BuildConfig.DEBUG) {
            Log.w("KYC", name + " (" + jsonString.length() + " bytes)");

            String[] strs = jsonString.split(",");

            for (int i = 0; i < strs.length; i++) {
                Log.i("KYC", strs[i] + ",");
            }
        }
    }
    //endregion
}
