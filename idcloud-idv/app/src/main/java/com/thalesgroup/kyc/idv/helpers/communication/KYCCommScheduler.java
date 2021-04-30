package com.thalesgroup.kyc.idv.helpers.communication;

import com.thalesgroup.kyc.idv.helpers.DataContainer;

public class KYCCommScheduler {
    public static final int SUCCESS = 0;
    public static final int SENDING_DOC = 1;
    public static final int WAITING_SELFIE = 2;
    public static final int SENDING_SELFIE = 3;
    public static final int FAILURE = 4;
    public static final int FAILURE_RETRY = 5;
    public static final int FAILURE_ABORT = 6;

    private static KYCCommunication mKYCCommunication;
    private static int mState = SUCCESS;
    private static KYCResponse mResponse;
    private static String mError;
    private static int mRetryStep;

    public static void sendData(int step) {
        boolean isIncremental = false;

        // Send data to server and wait for response.
        mKYCCommunication = DataContainer.instance().mKYCCommunication;

        if (step == KYCCommunication.STEP_START_DOC_VERIFICATION) {
            mState = SENDING_DOC;
            isIncremental = true;
        }
        else if (step == KYCCommunication.STEP_SELFIE_VERIFICATION) {
            mState = SENDING_SELFIE;
            isIncremental = false;
        }

        mKYCCommunication.verifyDocument(new KYCSession.KYCResponseHandler() {
            @Override
            public void onProgress(int nbSteps, int stepNb, KYCResponse response) {
                mResponse = response;

                if (stepNb == KYCCommunication.STEP_SELFIE_VERIFICATION) {
                    mState = WAITING_SELFIE;
                }
            }

            @Override
            public void onSuccess(final KYCResponse response) {
                mResponse = response;

                mState = SUCCESS;
            }

            @Override
            public void onFailure(final String error) {
                mError = error;

                mState = FAILURE;
            }

            @Override
            public void onFailureRetry(final String error, int retryStep) {
                mError = error;
                mRetryStep = retryStep;

                mState = FAILURE_RETRY;
            }

            @Override
            public void onFailureAbort(final String error) {
                mError = error;

                mState = FAILURE_ABORT;
            }
        }, isIncremental, step);
    }

    public static int getState() {
        return mState;
    }

    public static KYCResponse getResponse() {
        return mResponse;
    }

    public static String getError() {
        return mError;
    }

    public static int getRetryStep() {
        return mRetryStep;
    }
}
