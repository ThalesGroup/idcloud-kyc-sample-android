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

import com.thalesgroup.kyc.idvconnect.helpers.util.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KycVerificationResult {

    //region Definition

    private final String mResult;
    private final String mFirstName;
    private final String mMiddleName;
    private final String mSurname;
    private final String mGender;
    private final String mNationality;
    private final String mExpirationDate;
    private final String mBirthDate;
    private final String mDocumentNumber;
    private final String mDocumentType;
    private final int mTotalVerificationsDone;
    private final KycFields mFields;
    private final KycTemplate mDocTemplate;
    private final List<KycAlert> mAlerts;

    //endregion

    //region Life Cycle

    public KycVerificationResult(final JSONObject response) throws JSONException {
        mResult = JsonUtil.jsonGetString(response, "result", null);
        mFirstName = JsonUtil.jsonGetString(response, "firstName", null);
        mMiddleName = JsonUtil.jsonGetString(response,"middleName", null);
        mSurname = JsonUtil.jsonGetString(response,"surname", null);
        mGender = JsonUtil.jsonGetString(response,"gender", null);
        mNationality = JsonUtil.jsonGetString(response,"nationality", null);
        mExpirationDate = JsonUtil.jsonGetString(response,"expirationDate", null);
        mBirthDate = JsonUtil.jsonGetString(response,"birthDate", null);
        mDocumentNumber = JsonUtil.jsonGetString(response,"documentNumber", null);
        mDocumentType = JsonUtil.jsonGetString(response,"documentType", null);
        mTotalVerificationsDone = JsonUtil.jsonGetInt(response,"totalVerificationsDone", 0);
        mFields = new KycFields(response.getJSONObject("fields"));
        mDocTemplate = new KycTemplate(response.getJSONObject("template"));

        mAlerts = new ArrayList<>();
        final JSONArray alerts = response.getJSONArray("alerts");
        response.getJSONArray("alerts");
        for (int i = 0; i < alerts.length(); i ++) {
            final JSONObject alert = alerts.getJSONObject(i);
            final KycAlert kycAlert = new KycAlert(alert);
            mAlerts.add(kycAlert);
        }
    }

    //endregion

    //region Public API

    public String getResult() {
        return mResult;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getMiddleName() {
        return mMiddleName;
    }

    public String getSurname() {
        return mSurname;
    }

    public String getGender() {
        return mGender;
    }

    public String getNationality() {
        return mNationality;
    }

    public String getExpirationDate() {
        return mExpirationDate;
    }

    public String getBirthDate() {
        return mBirthDate;
    }

    public String getDocumentNumber() {
        return mDocumentNumber;
    }

    public String getDocumentType() {
        return mDocumentType;
    }

    public int getTotalVerificationsDone() {
        return mTotalVerificationsDone;
    }

    public KycFields getFields() {
        return mFields;
    }

    public KycTemplate getDocTemplate() {
        return mDocTemplate;
    }

    public List<KycAlert> getAlerts() {
        return mAlerts;
    }

    //endregion
}
