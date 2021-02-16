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

import androidx.annotation.NonNull;

import com.thalesgroup.kyc.idvconnect.helpers.util.ImageUtil;
import com.thalesgroup.kyc.idvconnect.helpers.util.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model class representing the face.
 */
public class KycFace {
    //region Definition

    private final String mResult;
    private final byte[] mImage;
    private final int mScore;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code KYCFace} instance.
     *
     * @param response Response received from verification server.
     * @throws JSONException If value not found in passed response.
     */
    KycFace(@NonNull final JSONObject response) throws JSONException {
        mResult = JsonUtil.jsonGetString(response, "result", "Unknown");
        mImage = ImageUtil.imageFromBase64(JsonUtil.jsonGetString(response, "image", null));
        mScore = JsonUtil.jsonGetInt(response, "score", -1);
    }

    //endregion


    //region Public API

    /**
     * Gets the result.
     *
     * @return Result.
     */
    public String getResult() {
        return mResult;
    }

    /**
     * Gets the image.
     *
     * @return Image.
     */
    public byte[] getImage() {
        return mImage.clone();
    }

    /**
     * Gets he score.
     *
     * @return Score.
     */
    public int getScore() {
        return mScore;
    }

    //endregion
}
