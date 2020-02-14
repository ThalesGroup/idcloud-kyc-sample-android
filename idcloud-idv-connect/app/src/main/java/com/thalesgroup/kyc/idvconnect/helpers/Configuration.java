/*
 MIT License

 Copyright (c) 2019 Thales DIS

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 IMPORTANT: This source code is intended to serve training information purposes only.
 Please make sure to review our IdCloud documentation, including security guidelines.
 */

package com.thalesgroup.kyc.idvconnect.helpers;

public final class Configuration {

    /**
     * IdCloud demo endpoint url.
     */
    public static final String IDCLOUD_URL = "";

    /**
     * Default value for IdCloud communication web token. It can be updated from app menu.
     */
    final static String JSON_WEB_TOKEN_DEFAULT = "";

    /**
     * Acuant account username.
     */
    public final static String ACUANT_USERNAME = "";

    /**
     * Acuant account password.
     */
    public final static String ACUANT_PASSWORD = "";

    /**
     * Acuant account subscription id.
     */
    public final static String ACUANT_SUBSCRIPTION_ID = "";

    /**
     * Acuant facial recognition endpoint url.
     */
    public final static String ACUANT_FRM_ENDPOINT = "";

    /**
     * Acuant Document Authentication & Identity Verification endpoint url.
     */
    public final static String ACUANT_ASSURE_ID_ENDPOINT = "";

    /**
     * Acuant mediscan endpoint url.
     */
    public final static String ACUANT_MEDISCAN_ENDPOINT = "";

    /**
     * URL to company privacy policy.
     */
    public final static String PRIVACY_POLICY_URL = "";
}
