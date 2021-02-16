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

import com.thalesgroup.kyc.idv.helpers.util.ImageUtil;
import com.thalesgroup.kyc.idv.helpers.util.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Model class representing the document.
 */
public class KYCDocument {

    //region Definition

    private final String mFirstName;
    private final String mBirthDate;
    private final String mDocumentType;
    private final String mSurname;
    private final byte[] mPortrait;
    private final String mResult;
    private final String mGender;
    private final String mDocumentNumber;
    private final String mExpiryDate;
    private final List<KYCFailedVerification> mFailedVerifications;
    private final String mNationality;
    private final int mTotalVerificationsDone;
    private KYCMRZ mMRZ = null;

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
        mFirstName = JsonUtil.jsonGetString(response, "firstName", "");
        mBirthDate = JsonUtil.jsonGetString(response,"birthDate", "");
        mExpiryDate = JsonUtil.jsonGetString(response,"expiryDate", "");
        mDocumentType = JsonUtil.jsonGetString(response,"documentType", "");
        mSurname = JsonUtil.jsonGetString(response,"surname", "");
        mPortrait = ImageUtil.imageFromBase64(JsonUtil.jsonGetString(response, "portrait", null));
        mResult = JsonUtil.jsonGetString(response,"result", "");
        mGender = JsonUtil.jsonGetString(response,"gender", "");
        mDocumentNumber = JsonUtil.jsonGetString(response,"documentNumber", "");
        mNationality = JsonUtil.jsonGetString(response,"nationality", "");
        mTotalVerificationsDone = JsonUtil.jsonGetInt(response,"totalVerifications", 0);

        if (response.has("mrzTextFields")) {
            mMRZ = new KYCMRZ(response.getJSONObject("mrzTextFields"));
        }

        mFailedVerifications = new ArrayList<>();
        if (response.has("failedVerifications")) {
            final JSONArray verifications = response.getJSONArray("failedVerifications");
            for (int index = 0; index < verifications.length(); index++) {
                final JSONObject loopVerify = verifications.getJSONObject(index);
                mFailedVerifications.add(new KYCFailedVerification(loopVerify));
            }
        }

    }

    //endregion

    //region Public API

    /**
     * Gets the first name.
     *
     * @return First name.
     */
    public String getFirstName() {
        return mFirstName;
    }

    /**
     * Gets the birth date.
     *
     * @return Birth date.
     */
    public String getBirthDate() {
        return mBirthDate;
    }

    /**
     * Gets the document type.
     *
     * @return Document type.
     */
    public String getDocumentType() {
        return mDocumentType;
    }

    /**
     * Gets the surname.
     *
     * @return Surname.
     */
    public String getSurname() {
        return mSurname;
    }

    /**
     * Gets the portrait.
     *
     * @return Portrait.
     */
    public byte[] getPortrait() {
        return mPortrait.clone();
    }

    /**
     * Gets the result.
     *
     * @return Result.
     */
    public String getResult() {
        return mResult;
    }

    /**
     * Gets the gender.
     *
     * @return Gender.
     */
    public String getGender() {
        return mGender;
    }

    /**
     * Gets the nationality.
     *
     * @return Nationality.
     */
    public String getNationality() {
        return mNationality;
    }

    /**
     * Gets the document number.
     *
     * @return Document number.
     */
    public String getDocumentNumber() {
        return mDocumentNumber;
    }

    /**
     * Gets the expiration date.
     *
     * @return Expiration date.
     */
    public String getExpiryDate() {
        return mExpiryDate;
    }

    /**
     * Gets the failed verification list.
     *
     * @return Failed verification list.
     */
    public List<KYCFailedVerification> getFailedVerifications() {
        return mFailedVerifications;
    }

    /**
     * Gets number of total verifications done.
     *
     * @return Number of total verifications done.
     */
    public int getTotalVerificationsDone() {
        return mTotalVerificationsDone;
    }

    /**
     * Gets MRZ.
     *
     * @return MRZ.
     */
    public KYCMRZ getMRZ() {
        return mMRZ;
    }

    //endregion


}

