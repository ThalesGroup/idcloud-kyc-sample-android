package com.thalesgroup.kyc.idvconnect.gui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.acuant.acuantcommon.model.ErrorCodes;
import com.aware.face_liveness.api.FaceLiveness;
import com.aware.face_liveness.api.exceptions.FaceLivenessException;
import com.thalesgroup.kyc.idvconnect.R;
import com.thalesgroup.kyc.idvconnect.gui.fragment.AwareLivenessFragment;
import com.thalesgroup.kyc.idvconnect.helpers.DataContainer;

import java.lang.ref.WeakReference;



public class AwareLivenessActivity extends ActivityWithSpinner implements FaceLiveness.LivenessActivityPresenter{

    private String LIVENESS_SAMPLE_TAG = "AwareLivenessActivity";



    private InitializeBackgroundTask mInitializeBackgroundTask;
    private boolean mInitComplete = false;
    private String mUsername = "AwareTest";
    private boolean mImageCaptureProperty= false;
    private double mCaptureTimeout = 0;
    private boolean mCaptureOnDevice = true;
    private String workflowName = "Charlie4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aware_liveness);

        String model = "mobile";

        ShowHourglass(getString(R.string.init_selfie));
        mInitializeBackgroundTask = new InitializeBackgroundTask(mInitListener);
        mInitializeBackgroundTask.execute(model);

    }

    @Override
    public void onInitializationComplete(FaceLiveness.InitializationError status) {
        if (status != FaceLiveness.InitializationError.NO_ERROR) {
            String msg = status.toString() + " contact Aware, Inc.";
            ShowDialog(getResources().getString(R.string.error), msg);
            mInitComplete = true;

        } else {
            mInitComplete = true;
            workflowInit();
        }
    }

    @Override
    public WeakReference<FaceLiveness> getLivenessComponentApi() {
        if (mLivenessApi == null) {
            return null;
        }
        return new WeakReference<>(mLivenessApi);
    }

    private void popFragment() {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack();
    }


    public void onCaptureEnd() {
        try {
            DataContainer.instance().mEnhancedSelfieJson = mLivenessApi.getServerPackage();
            DataContainer.instance().mSelfie = mLivenessApi.getCapturedImage();
        } catch (FaceLivenessException e) {
            //getServerPackage can throw an exception on an error
            e.printStackTrace();
        }

        Intent intent = new Intent();
        AwareLivenessActivity.this.setResult(RESPONSE_SUCCESS_CODE, intent);
        AwareLivenessActivity.this.finish();
    }

    public void onCaptureTimedout() {
        Intent intent = new Intent();
        AwareLivenessActivity.this.setResult(ErrorCodes.ERROR_CAPTURING_FACIAL, intent);
        AwareLivenessActivity.this.finish();
    }

    public void onCaptureAbort() {
        Intent intent = new Intent();
        AwareLivenessActivity.this.setResult(ErrorCodes.USER_CANCELED_FACIAL, intent);
        AwareLivenessActivity.this.finish();
    }

    void workflowInit() {
        // Note the following calls must be in the correct order.
        // Must set properties before calling selectWorkflow.
        try {
            mLivenessApi.setProperty(FaceLiveness.PropertyTag.USERNAME, mUsername);
            mLivenessApi.setProperty(FaceLiveness.PropertyTag.CONSTRUCT_IMAGE, mImageCaptureProperty);
            mLivenessApi.setProperty(FaceLiveness.PropertyTag.TIMEOUT, mCaptureTimeout);
            mLivenessApi.setProperty(FaceLiveness.PropertyTag.CAPTURE_ON_DEVICE, mCaptureOnDevice);
        } catch (FaceLivenessException e) {
            String message = "Invalid property setting!!!";
            Toast t = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();

            e.printStackTrace();
        }

        // select a workflow
        try {
            mLivenessApi.selectWorkflow(this, workflowName, "Failed.");
        } catch (FaceLivenessException e) {
            e.printStackTrace();
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = AwareLivenessFragment.newInstance(this);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment, "Execute");
        ft.addToBackStack("Execute");
        ft.commit();

    }


    private FaceLiveness mLivenessApi;
    private static final int SPLASH_TIME = 3000;

    public class InitializeBackgroundTask extends AsyncTask<String, Void, Void> {
        private boolean mCouldNotOpenModel = false;
        private String  mModelName = "";
        private ModelInitializationListener mInitializationListener;

        InitializeBackgroundTask(ModelInitializationListener listener) {
            mInitializationListener = listener;
            mCouldNotOpenModel = false;
            mModelName = "";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final String[] params) {
            mModelName = params[0];
            try {
                FaceLiveness.setStaticProperty(FaceLiveness.StaticPropertyTag.FACE_MODEL, params[0]);
                mLivenessApi = new FaceLiveness( getApplicationContext() );
            } catch (FaceLivenessException e) {
                e.printStackTrace();
            }

            try {
                // initialize the library, wait for callback...
                mLivenessApi.initializeFaceLivenessLibrary(AwareLivenessActivity.this);
            }
            catch (FaceLivenessException e) {
                mCouldNotOpenModel = true;
                return null;
            }

            synchronized (InitializeBackgroundTask.this) {
                while (!mInitComplete && !isCancelled()) {
                    try {
                        Log.i(LIVENESS_SAMPLE_TAG, "Launch starting WAIT");
                        InitializeBackgroundTask.this.wait(SPLASH_TIME);
                        Log.i(LIVENESS_SAMPLE_TAG, "Launch completing WAIT");
                    } catch (InterruptedException e) {
                        Log.e(LIVENESS_SAMPLE_TAG, "Launch INTERRUPTED: " + e.getMessage());
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            super.onPostExecute(o);
            if (isCancelled()) {
                finish();
            }
            HideHourglass();
            mInitializationListener.onInitializationComplete(!mCouldNotOpenModel, mModelName);
        }
    }

    interface ModelInitializationListener {
        void onInitializationComplete(boolean success, String modelName);
    }

    private ModelInitializationListener mInitListener = new ModelInitializationListener() {
        @Override
        public void onInitializationComplete(final boolean success, final String modelName) {
            if (!success) {
                String message = "Could not initialize model " + modelName + " not found!" ;
                Toast t = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
            }
        }
    };

    private static final int RESPONSE_SUCCESS_CODE = 2;

    public void ShowDialog(final String title, final String message) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AwareLivenessActivity.this);
        // Setting Dialog Title
        alertDialogBuilder.setTitle(title);

        // set dialog message
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        finish();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }
}
