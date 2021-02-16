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

package com.thalesgroup.kyc.idv.helpers.communication;

import com.thalesgroup.kyc.idv.helpers.util.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;

/**
 * Model class for a failed verification.
 */
public class KYCFailedVerification {
    //region Definition

    private final String mCategory;
    private final String mName;
    private final String mType;
    private final int mScore;
    private final int mThreshold;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code KYCFailedVerification} instance.
     *
     * @param response Response received from verification server.
     * @throws JSONException If value not found in passed response.
     */
    KYCFailedVerification(@NonNull final JSONObject response) throws JSONException {
        mCategory = JsonUtil.jsonGetString(response, "category", null);
        mName = JsonUtil.jsonGetString(response, "name", null);
        mType = JsonUtil.jsonGetString(response, "type", null);
        mScore = JsonUtil.jsonGetInt(response, "score", -1);
        mThreshold = JsonUtil.jsonGetInt(response, "threshold", -1);
    }

    //endregion


    //region Public API


    /**
     * Gets the category.
     *
     * @return Category
     */
    public String getCategory() {
        return mCategory;
    }

    /**
     * Gets the name.
     *
     * @return Name.
     */
    public String getName() {
        return mName;
    }

    /**
     * Gets the name.
     *
     * @return Name.
     */
    public String getType() {
        return mType;
    }

    /**
     * Gets the score.
     *
     * @return Score.
     */
    public int getScore() {
        return mScore;
    }

    /**
     * Gets the threshold.
     *
     * @return Threshold.
     */
    public int getThreshold() {
        return mThreshold;
    }

    //endregion
}
