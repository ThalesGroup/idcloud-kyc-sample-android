package com.thalesgroup.kyc.idvconnect.helpers;

import com.thalesgroup.kyc.idvconnect.helpers.communication.KYCCommunication;

/**
 * Class to hold verification data.
 */
public final class DataContainer {

    //region Definition

    static private DataContainer sInstance = null;

    // Doc data
    public byte[] mDocFront;
    public byte[] mDocBack;

    // Selfie data
    public byte[] mSelfie;
    public String mEnhancedSelfieJson;

    // Server session data
    public int mVerificationStep = KYCCommunication.STEP_START_VERIFICATION;
    public KYCCommunication mKYCCommunication;

    //endregion

    //region Life Cycle

    /**
     * Private constructor.
     */
    private DataContainer() {
    }

    /**
     * Retrieves the singleton instance.
     *
     * @return Singleton instance of {@code DataContainer}.
     */
    public synchronized static DataContainer instance() {
        if (sInstance == null) {
            sInstance = new DataContainer();
            sInstance.mKYCCommunication = new KYCCommunication();
        }

        return sInstance;
    }

    //endregion

    //region Public API

    /**
     * Clears all the data.
     */
    public void clearDocData() {
        mDocFront = null;
        mDocBack = null;
        mSelfie = null;
        mEnhancedSelfieJson = null;
        mVerificationStep = KYCCommunication.STEP_START_VERIFICATION;
    }

    //endregion

}
