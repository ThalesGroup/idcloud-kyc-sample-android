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

import org.json.JSONObject;

class KycTemplate {

    //region Definition

    private final String mTemplateId;
    private final String mIssuer;
    private final String mIssuerType;
    private final String mIssuerName;
    private final String mKeesingCode;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code KycTemplate} instance.
     *
     * @param template
     *         JSON response.
     */
    public KycTemplate(final JSONObject template) {
        mTemplateId = JsonUtil.jsonGetString(template, "id", null);
        mIssuer = JsonUtil.jsonGetString(template, "issue", null);
        mIssuerType = JsonUtil.jsonGetString(template, "issuerType", null);
        mIssuerName = JsonUtil.jsonGetString(template, "issuerName", null);
        mKeesingCode = JsonUtil.jsonGetString(template, "keesingCode", null);
    }

    //endregion

    //region Public API

    /**
     * Gets the template id.
     *
     * @return Template id.
     */
    public String getTemplateId() {
        return mTemplateId;
    }

    /**
     * Gets the issuer.
     *
     * @return Issuer.
     */
    public String getIssuer() {
        return mIssuer;
    }

    /**
     * Gets the issuer type.
     *
     * @return Issuer type.
     */
    public String getIssuerType() {
        return mIssuerType;
    }

    /**
     * Gets the issuer name.
     *
     * @return Issuer name.
     */
    public String getIssuerName() {
        return mIssuerName;
    }

    /**
     * Gets the keesing code.
     *
     * @return Keesing code.
     */
    public String getKeesingCode() {
        return mKeesingCode;
    }

    //endregion
}
