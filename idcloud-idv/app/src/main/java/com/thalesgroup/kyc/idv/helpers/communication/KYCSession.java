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

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Session with verification backend.
 */
public class KYCSession {

    //region Definition

    public static final int RETRY_NONE = 0;
    public static final int RETRY_DOC_SCAN = 1;
    public static final int RETRY_SELFIE_SCAN = 2;
    public static final int RETRY_ABORT = 3;

    /**
     * Callback.
     */
    public interface KYCResponseHandler {
        /**
         * Progress callback.
         *
         * @param nbSteps Number of steps.
         * @param stepNb Current step number.
         * @param response Response from verification server.
         *
         */
        void onProgress(final int nbSteps, final int stepNb, final KYCResponse response);

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

        /**
         * Error during communication with verification server.
         * Manage auto retry.
         *
         * @param error Error received from verification server.
         * @param retryStep Step to retry (Doc scan or Selfie).
         */
        void onFailureRetry(final String error, int retryStep);

        /**
         * Error during communication with verification server.
         * Manage abort.
         *
         * @param error Error received from verification server.
         */
        void onFailureAbort(final String error);
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
        mTryCount = 1;
        mURLBase = baseUrl;
        mHandler = handler;
    }

    //endregion

    //region Public API
    void setHandler(KYCResponseHandler handler) {
        mHandler = handler;
    }

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
    URL getUrlWithSessionId() throws MalformedURLException {
        return new URL(Uri.parse(mURLBase).buildUpon().appendPath(mSessionId).toString());
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
    URL getUrlEnhancedLiveness() throws MalformedURLException {
        final String url = mURLBase + "/" + mSessionId +"/state/steps/enhancedLiveness";
        return new URL(url);
    }

    /**
     * Gets the URL.
     *
     * @return URL.
     * @throws MalformedURLException If URL is malformed.
     */
    URL getUrlEnhancedLivenessPollResult() throws MalformedURLException {
        final String url = mURLBase + "/" + mSessionId;
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
     * Calls the retry error handler.
     *
     * @param error Error received from verification server.
     * @param retryStep Step to retry (Doc scan or Selfie).
     */
    synchronized void handleErrorRetry(final String error, int retryStep) {
        // Call handler in UI thread.
        if (mHandler != null) {
            new Handler(Looper.getMainLooper()).post(() -> mHandler.onFailureRetry(error, retryStep));
        }
    }

    /**
     * Calls the abort error handler.
     *
     * @param error Error received from verification server.
     */
    synchronized void handleErrorAbort(final String error) {
        // Call handler in UI thread.
        if (mHandler != null) {
            new Handler(Looper.getMainLooper()).post(() -> mHandler.onFailureAbort(error));
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
     * Calls the progress handler.
     */
    synchronized void handleProgress(int nbSteps, int stepNb, final KYCResponse response) {
        // Call handler in UI thread.
        if (mHandler != null) {
            new Handler(Looper.getMainLooper()).post(() -> mHandler.onProgress(nbSteps, stepNb, response));
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

