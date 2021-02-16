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

public final class KYCConfiguration {

    /**
     * Number of attempts before throwing timeout error.
     */
    public static final int IDCLOUD_NUMBER_OF_RETRIES = 30;

    /**
     * Number of seconds between each verification attempt.
     */
    public static final int IDCLOUD_RETRY_DELAY_SEC = 2;

    /**
     * Acuant account username.
     */
    public final static String ACUANT_USERNAME = "Contact your Thales Sales representative for more details on how to obtain this value.";

    /**
     * Acuant account password.
     */
    public final static String ACUANT_PASSWORD = "Contact your Thales Sales representative for more details on how to obtain this value.";

    /**
     * Acuant account subscription id.
     */
    public final static String ACUANT_SUBSCRIPTION_ID = "Contact your Thales Sales representative for more details on how to obtain this value.";

    /**
     * URL to company privacy policy.
     */
    public final static String PRIVACY_POLICY_URL = "Contact your Thales Sales representative for more details on how to obtain this value.";
}
