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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class KycFields {

    //region Definition

    private final List<KycNameValues> mOcr;
    private final List<KycNameValues> mMrz;
    private final List<KycNameValues> mMagStripe;
    private final List<KycNameValues> mBarcode2d;
    private final List<KycNameValues> mNative;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code KycFields} instance.
     *
     * @param field JSON response.
     * @throws JSONException
     */
    public KycFields(final JSONObject field) throws JSONException {
        mOcr = parseNameValueArray(field, "OCR");
        mMrz = parseNameValueArray(field, "MRZ");
        mMagStripe = parseNameValueArray(field, "MAGSTRIPE");
        mBarcode2d = parseNameValueArray(field, "BARCODE_2D");
        mNative = parseNameValueArray(field, "NATIVE");
    }

    //endregion

    //region Public API

    /**
     * Gets the OCR - readable texts.
     *
     * @return OCR - readable texts.
     */
    public List<KycNameValues> getOcr() {
        return mOcr;
    }

    /**
     * Gets the MRZ - Fields from the machine Readable Zone.
     *
     * @return MRZ - Fields from the machine Readable Zone.
     */
    public List<KycNameValues> getMrz() {
        return mMrz;
    }

    /**
     * Gets the Magstripe data.
     *
     * @return Magstripe data.
     */
    public List<KycNameValues> getMagStripe() {
        return mMagStripe;
    }

    /**
     * Gets the 2D barcode data.
     *
     * @return 2D barcode data.
     */
    public List<KycNameValues> getBarcode2d() {
        return mBarcode2d;
    }

    /**
     * Gets the Non-latin version of the field.
     *
     * @return Non-latin version of the field.
     */
    public List<KycNameValues> getNative() {
        return mNative;
    }

    //endregion

    //region Private Helpers

    private List<KycNameValues> parseNameValueArray(final JSONObject object, final String key)
            throws JSONException {
        final List<KycNameValues> list = new ArrayList<>();
        final JSONArray jsonArray = object.getJSONArray(key);
        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject jsonObject = jsonArray.getJSONObject(i);
            final KycNameValues kycNameValues = new KycNameValues(jsonObject);
            list.add(kycNameValues);
        }

        return list;
    }

    //endregion
}
