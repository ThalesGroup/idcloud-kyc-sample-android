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

package com.thalesgroup.kyc.idvconnect.helpers.communication;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.thalesgroup.kyc.idvconnect.helpers.communication.structures.KYCResponse;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Session with verification backend.
 */
public class KYCSession {

    //region Definition

    static final String COMMON_STATE_FAILED = "Failed";
    static final String COMMON_STATE_ERROR = "Error";

    /**
     * Callback.
     */
    public interface KYCResponseHandler {
        /**
         * Success callback.
         *
         * @param response Response from verification server.
         */
        void onSuccess(final KYCResponse response);

        /**
         * Error during communication with verification server.
         *
         * @param error Error received from verification server.
         */
        void onFailure(final String error);
    }

    private int mTryCount;
    private final String mURLBase;
    private String mSessionId;
    private KYCResponseHandler mHandler;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code KYCFailedVerification} instance.
     *
     * @param baseUrl Verification server URL.
     * @param handler Callback.
     */
    KYCSession(final String baseUrl,
               final KYCResponseHandler handler) {
        mSessionId = null;
        mTryCount = 1;
        mURLBase = baseUrl;
        mHandler = handler;
    }

    //endregion

    //region Public API

    /**
     * Gets the try counter.
     *
     * @return Try counter.
     */
    int getTryCount() {
        return mTryCount;
    }

    /**
     * Sets the try counter.
     *
     * @param tryCount Try counter.
     */
    void setTryCount(final int tryCount) {
        mTryCount = tryCount;
    }

    /**
     * Gets the URL.
     *
     * @return URL.
     * @throws MalformedURLException If URL is malformed.
     */
    URL getBaseUrl() throws MalformedURLException {
        return new URL(mURLBase);
    }

    /**
     * Gets the URL.
     *
     * @return URL.
     * @throws MalformedURLException If URL is malformed.
     */
    URL getUrlDocumennt() throws MalformedURLException {
        final String url = mURLBase + "/" + mSessionId + "/state/steps/verifyResults";
        return new URL(url);
    }

    /**
     * Gets the URL.
     *
     * @return URL.
     * @throws MalformedURLException If URL is malformed.
     */
    URL getUrlSelfie() throws MalformedURLException {
        final String url = mURLBase + "/" + mSessionId +"/state/steps/faceMatch";
        return new URL(url);
    }

    /**
     * Updates the session with the session id.
     *
     * @param sessionId Session id.
     */
    void updateWithSessionId(final String sessionId) {
        mSessionId = sessionId;
    }

    /**
     * Calls the error handler.
     *
     * @param error Error received from verification server.
     */
    synchronized void handleError(final String error) {
        // Call handler in UI thread.
        if (mHandler != null) {
            new Handler(Looper.getMainLooper()).post(() -> mHandler.onFailure(error));
        }
    }

    /**
     * Calls the success handler.
     *
     * @param response Response received from verification server.
     */
    synchronized void handleResult(final KYCResponse response) {
        // Call handler in UI thread.
        if (mHandler != null) {
            new Handler(Looper.getMainLooper()).post(() -> mHandler.onSuccess(response));
        }
    }

    /**
     * Removes the UI listener.
     */
    synchronized void removeListener() {
        mHandler = null;
    }

    /**
     * Checks if UI listener is present.
     *
     * @return {@code True} if present, else {@code false}.
     */
    synchronized boolean isListenerRegistered() {
        return mHandler != null;
    }

    //endregion

}

