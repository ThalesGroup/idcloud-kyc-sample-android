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

package com.thalesgroup.kyc.idvconnect.helpers.communication.structures;

import com.thalesgroup.kyc.idvconnect.helpers.util.ImageUtil;
import com.thalesgroup.kyc.idvconnect.helpers.util.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;

/**
 * Model class representing the document.
 */
public class KYCDocument {

    //region Definition

    private final KycVerificationResult mKycVerificationResult;
    private final byte[] mPortrait;
    private final byte[] mImageWhiteBack;
    private final byte[] mImageWhiteFront;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code KYCDocument} instance.
     *
     * @param response
     *         JSON response received from verification backend.
     * @throws JSONException If value not found in passed response.
     */
    KYCDocument(@NonNull final JSONObject response) throws JSONException {
        mKycVerificationResult = new KycVerificationResult(response.getJSONObject("verificationResults"));
        mPortrait = ImageUtil.imageFromBase64(JsonUtil.jsonGetString(response, "portrait", null));
        mImageWhiteBack = ImageUtil.imageFromBase64(JsonUtil.jsonGetString(response, "backWhiteImage", null));
        mImageWhiteFront = ImageUtil.imageFromBase64(JsonUtil.jsonGetString(response, "frontWhiteImage", null));
    }

    //endregion

    //region Public API

    /**
     * Gets the selfie image.
     *
     * @return Selfie image.
     */
    public byte[] getPortrait() {
        return mPortrait.clone();
    }

    /**
     * Gets the {@code KycVerificationResult}.
     *
     * @return {@code KycVerificationResult}.
     */
    public KycVerificationResult getKycVerificationResult() {
        return mKycVerificationResult;
    }

    /**
     * Gets the front image of the document.
     *
     * @return Front image of document.
     */
    public byte[] getImageWhiteBack() {
        return mImageWhiteBack.clone();
    }

    /**
     * Gets the back image of the document.
     *
     * @return Back image of the document.
     */
    public byte[] getImageWhiteFront() {
        return mImageWhiteFront.clone();
    }

    //endregion


}

