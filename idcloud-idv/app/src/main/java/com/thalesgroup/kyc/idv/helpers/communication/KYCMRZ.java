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

import androidx.annotation.NonNull;

import com.thalesgroup.kyc.idv.helpers.util.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model class representing the document.
 */
public class KYCMRZ {

    //region Definition

    private final String mRawData;
    private final String mDocumentType;
    private final String mIssuingState;
    private final String mLastName;
    private final String mFirstName;
    private final String mDocumentNumber;
    private final String mNationality;
    private final String mBirthDate;
    private final String mSex;
    private final String mExpiryDate;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code KYCDocument} instance.
     *
     * @param response
     *         JSON response received from verification backend.
     * @throws JSONException If value not found in passed response.
     */
    KYCMRZ(@NonNull final JSONObject response) throws JSONException {
        mRawData = JsonUtil.jsonGetString(response, "MrzRawData", "");
        mDocumentType = JsonUtil.jsonGetString(response,"DocumentType", "");
        mIssuingState = JsonUtil.jsonGetString(response,"IssuingState", "");
        mLastName = JsonUtil.jsonGetString(response, "LastName", "");
        mFirstName = JsonUtil.jsonGetString(response, "FirstName", "");
        mDocumentNumber = JsonUtil.jsonGetString(response,"DocumentNumber", "");
        mBirthDate = JsonUtil.jsonGetString(response,"DateOfBirth", "");
        mNationality = JsonUtil.jsonGetString(response,"Nationality", "");
        mSex = JsonUtil.jsonGetString(response,"Sex", "");
        mExpiryDate = JsonUtil.jsonGetString(response,"DateOfExpiry", "");
    }

    //endregion

    //region Public API

    /**
     * Gets the raw data.
     *
     * @return Raw data.
     */
    public String getRawData() {
        return mRawData;
    }

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
     * Gets the last name.
     *
     * @return Last name.
     */
    public String getLastName() {
        return mLastName;
    }

    /**
     * Gets the gender.
     *
     * @return Gender.
     */
    public String getSex() {
        return mSex;
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
     * Gets the issuing date.
     *
     * @return Issuing date.
     */
    public String getIssuingDate() {
        return mIssuingState;
    }

    //endregion


}

