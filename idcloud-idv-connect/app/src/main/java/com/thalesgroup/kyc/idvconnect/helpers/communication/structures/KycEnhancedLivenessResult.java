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

public class KycEnhancedLivenessResult {

    //region Definition
    private final byte[] mCapturedFrame;
    private final boolean mCapturedFrameIsConstructed;
    private final String[] mAutocaptureFeedback;
    private final int mScore;
    private final String[] mFeedback;

    //endregion

    //region Life Cycle

    public KycEnhancedLivenessResult(final JSONObject response) throws JSONException {
        mCapturedFrame = ImageUtil.imageFromBase64(JsonUtil.jsonGetString(response.getJSONObject("video").getJSONObject("autocapture_result"), "captured_frame", null));
        mCapturedFrameIsConstructed = JsonUtil.jsonGetBoolean(response.getJSONObject("video").getJSONObject("autocapture_result"), "captured_frame_is_constructed", false);
        mAutocaptureFeedback = JsonUtil.jsonGetStringArray(response.getJSONObject("video").getJSONObject("autocapture_result"), "feedback", null);
        mScore = JsonUtil.jsonGetInt(response.getJSONObject("video").getJSONObject("liveness_result"), "score", 0);
        mFeedback = JsonUtil.jsonGetStringArray(response.getJSONObject("video").getJSONObject("liveness_result"), "feedback", null);
    }
    //endregion

    //region Public API

    public byte[] getCapturedFrame() {
        return mCapturedFrame;
    }

    public boolean getCapturedFrameIsConstructed() {
        return mCapturedFrameIsConstructed;
    }

    public String[] getAutocaptureaptureFeedback() {
        return mAutocaptureFeedback;
    }

    public int getScore() {
        return mScore;
    }

    public String[] getFeedback() {
        return mFeedback;
    }

    //endregion
}
