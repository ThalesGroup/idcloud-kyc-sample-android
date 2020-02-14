package com.thalesgroup.kyc.idvconnect.helpers;

/**
 * Class to hold image data - document and face.
 */
public final class DataContainer {

    //region Definition

    static private DataContainer sInstance = null;

    // doc data IDV
    public byte[] mDocFront;
    public byte[] mDocBack;
    public boolean mDocAutoSnapshot;
    public byte[] mSelfie;

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
        mDocAutoSnapshot = false;
    }

    //endregion

}
