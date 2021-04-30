package com.thalesgroup.kyc.idv.gui.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aware.face_liveness.api.FaceLiveness;
import com.aware.face_liveness.api.exceptions.FaceLivenessException;
import com.aware.face_liveness.api.interfaces.WorkflowStateCallback;
import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.activity.AwareLivenessActivity;
import com.thalesgroup.kyc.idv.gui.view.AwareLivenessFeedbackView;


public class AwareLivenessFragment extends Fragment implements WorkflowStateCallback {

    private final static String WORKFLOW_FRAGMENT_TAG = "workflow_fragment";

    private FaceLiveness.LivenessActivityPresenter mWorkflowActiveListener;
    private FrameLayout mFrameLayout;
    private AwareLivenessActivity mUI;
    private WorkflowState mWorkflowState;
    private String mEvent = "No Event";
    private FaceLiveness mLivenessApi;

    private ImageView[] mPositionViews = new ImageView[9];
    private boolean[] mPositionColor = new boolean[9];
    private TextView mFeedback;

    public static AwareLivenessFragment newInstance(AwareLivenessActivity ui) {
        AwareLivenessFragment instance = new AwareLivenessFragment();
        instance.mUI = ui;
        return  instance;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mWorkflowActiveListener = (FaceLiveness.LivenessActivityPresenter) context;
        } catch (ClassCastException e) {
            Log.e(WORKFLOW_FRAGMENT_TAG, "WorkflowActiveListener not implemented");
            throw new ClassCastException(e.toString() + " implement WorkflowActiveListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mLivenessApi = mWorkflowActiveListener.getLivenessComponentApi().get();

        View view = inflater.inflate(R.layout.fragment_aware_liveness_view, container, false);
        AwareLivenessFeedbackView feedbackView = new AwareLivenessFeedbackView(getActivity(), this);

        mFrameLayout = view.findViewById(R.id.liveness_component_imp);
        mLivenessApi.AllocateFaceLivenessView(getContext());
        mLivenessApi.setWorkflowStateCallback(this);
        mLivenessApi.setFeedbackCallback(feedbackView);
        mLivenessApi.setDevicePositionCallback(feedbackView);
        try {
            mLivenessApi.bindFaceLivenessApi(mFrameLayout, getActivity());
            mLivenessApi.onCreateView(getActivity(), mFrameLayout);
        } catch (FaceLivenessException e) {
            Log.e(WORKFLOW_FRAGMENT_TAG, "Error: " + e.getMessage());
        }

        // the following is for the device orientation indicator. It is used during the face capture
        int index = 0;
        mPositionViews[index++] =  view.findViewById(R.id.positionIndicator0);
        mPositionViews[index++] =  view.findViewById(R.id.positionIndicator1);
        mPositionViews[index++] =  view.findViewById(R.id.positionIndicator2);
        mPositionViews[index++] =  view.findViewById(R.id.positionIndicator3);
        mPositionViews[index++] =  view.findViewById(R.id.positionIndicator4);
        mPositionViews[index++] =  view.findViewById(R.id.positionIndicator5);
        mPositionViews[index++] =  view.findViewById(R.id.positionIndicator6);
        mPositionViews[index++] =  view.findViewById(R.id.positionIndicator7);
        mPositionViews[index] =  view.findViewById(R.id.positionIndicator8);

        for(int i=0;i<9;i++) {
            mPositionColor[index] = false;
        }

        mFeedback = (TextView) view.findViewById(R.id.tv_feedback);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mLivenessApi.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            mLivenessApi.onResume();
        } catch (Exception e) {
            Log.e(WORKFLOW_FRAGMENT_TAG, "No Liveness API: " + e.getMessage());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            mLivenessApi.onPause();
        } catch (Exception e) {
            Log.e(WORKFLOW_FRAGMENT_TAG, "No Liveness API: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLivenessApi.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mWorkflowActiveListener = null;
        mLivenessApi.onDetach();
    }

    public void setCanvasUpdateCallback(AwareLivenessFeedbackView feedbackView) {
        mLivenessApi.setCanvasUpdateCallback(feedbackView);
    }

    //
    // WorkflowStateListener
    //

    /**
     * Watch the workflow state for a
     * {@link WorkflowState#COMPLETE }
     * messeage upon which the  {@link AwareLivenessFragment} will end and we report results.
     *
     * @param workflowState the current {@link WorkflowState}
     * @param event         An event request if the statue is {@link WorkflowState#EVENT}
     */
    @Override
    public void workflowStateCallback(WorkflowState workflowState, String event) {
        mWorkflowState = workflowState;

        if (!this.isVisible())
            return;

        if ( (workflowState == WorkflowState.COMPLETE) ) {
            mUI.onCaptureEnd();
            Log.d(WORKFLOW_FRAGMENT_TAG, "Complete");
        }
        else if ( (workflowState == WorkflowState.TIMEDOUT) ) {
            mUI.onCaptureTimedout();
            Log.d(WORKFLOW_FRAGMENT_TAG, "Timed Out");
        }
        else if (workflowState == WorkflowState.ABORT) {

            mUI.onCaptureAbort();
            Log.d(WORKFLOW_FRAGMENT_TAG, "Abort");

        } else if (workflowState == WorkflowState.DEVICE_IN_POSITION) {

            Log.d(WORKFLOW_FRAGMENT_TAG, "Device In position");

        } else if (workflowState == WorkflowState.PREPARING) {
            if (!this.isVisible())
                return;
            displayMessage(getResources().getString(R.string.position_device_vertically));
            Log.d(WORKFLOW_FRAGMENT_TAG, "Preparing");

        } else if (workflowState == WorkflowState.EVENT) {
            if (!this.isVisible())
                return;
            mEvent = event;

            if(mEvent.equals("NONE"))
                displayMessage(getResources().getString(R.string.compliant));
            else {
                displayMessage(mEvent);
            }
            Log.d(WORKFLOW_FRAGMENT_TAG, "Event = " + event);

        } else if (workflowState == WorkflowState.HOLD_STEADY) {
            if (!this.isVisible())
                return;
            displayMessage(getResources().getString(R.string.hold));
            Log.d(WORKFLOW_FRAGMENT_TAG, "Hold");

        }
    }

    private void displayMessage(final String msg){

        mUI.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFeedback.setText(msg);
            }
        });
    }

    // this is called by feedbackview
    public void reportOrietnationValues(int boxId ) {
        int color = Color.TRANSPARENT;


        for(int i=0;i<9;i++) {
            if(mPositionColor[i])
                mPositionViews[i].setBackgroundColor(color);
        }

        if (boxId == 0 || boxId == 8)
            color = Color.RED;
        if (boxId == 1 || boxId == 7)
            color = Color.MAGENTA;
        if (boxId == 2 || boxId == 6)
            color = Color.YELLOW;
        if (boxId == 3 || boxId == 5)
            color = Color.CYAN;
        if (boxId == 4)
            color = Color.GREEN;

        mPositionViews[boxId].setBackgroundColor(color);
        mPositionColor[boxId] = true;
    }

    // this is called by LivenessFeedbackView
    public void reportFeedback(FaceLiveness.AutoFeedback feedback ) {
        if (!this.isVisible())
            return;
        if(mWorkflowState == WorkflowState.DEVICE_IN_POSITION) {
            if (mFeedback != null && (feedback != null)) {
                mFeedback.setText(getAutoCaptureString(feedback));
                Log.d(WORKFLOW_FRAGMENT_TAG, "Device In Position.");
            } else {
                mFeedback.setText("");
            }
        }
    }

    private String getAutoCaptureString(FaceLiveness.AutoFeedback code) {
        String result = "";
        switch (code) {
            case OFF:
                result = (getContext().getResources().getString(R.string.off));
                break;
            case COMPLIANT_IMAGE:
                result = (getContext().getResources().getString(R.string.compliant));
                break;
            case NO_FACE_DETECTED:
                result = (getContext().getResources().getString(R.string.no_face));
                break;
            case MULTIPLE_FACES_DETECTED:
                result = (getContext().getResources().getString(R.string.multiple_faces));
                break;
            case INVALID_POSE:
                result = (getContext().getResources().getString(R.string.invalid_pose));
                break;
            case FACE_TOO_FAR:
                result = (getContext().getResources().getString(R.string.face_too_far));
                break;
            case FACE_TOO_CLOSE:
                result = (getContext().getResources().getString(R.string.face_too_close));
                break;
            case FACE_ON_LEFT:
                result = (getContext().getResources().getString(R.string.face_too_left));
                break;
            case FACE_ON_RIGHT:
                result = (getContext().getResources().getString(R.string.face_too_right));
                break;
            case FACE_TOO_HIGH:
                result = (getContext().getResources().getString(R.string.face_too_high));
                break;
            case FACE_TOO_LOW:
                result = (getContext().getResources().getString(R.string.face_too_low));
                break;
            case INSUFFICIENT_LIGHTING:
                result = (getContext().getResources().getString(R.string.lighting_too_dark));
                break;
            case LIGHT_TOO_BRIGHT:
                result = (getContext().getResources().getString(R.string.lighting_too_bright));
                break;
            case TOO_MUCH_BLUR:
                result = (getContext().getResources().getString(R.string.out_of_focus));
                break;
            case SMILE_PRESENT:
                result = (getContext().getResources().getString(R.string.smile_present));
                break;
            case FOREHEAD_COVERING:
                result = (getContext().getResources().getString(R.string.forehead_covered));
                break;
            case BACKGROUND_TOO_BRIGHT:
                result = (getContext().getResources().getString(R.string.bright_background));
                break;
            case BACKGROUND_TOO_DARK:
                result = (getContext().getResources().getString(R.string.dark_background));
                break;
            case LEFT_EYE_CLOSED:
                result = (getContext().getResources().getString(R.string.left_eye_closed));
                break;
            case RIGHT_EYE_CLOSED:
                result = (getContext().getResources().getString(R.string.right_eye_closed));
                break;
            case LEFT_EYE_OBSTRUCTED:
                result = (getContext().getResources().getString(R.string.left_eye_covered));
                break;
            case RIGHT_EYE_OBSTRUCTED:
                result = (getContext().getResources().getString(R.string.right_eye_covered));
                break;
            case HEAVY_FRAMES:
                result = (getContext().getResources().getString(R.string.heavy_frames));
                break;
            case GLARE:
                result = (getContext().getResources().getString(R.string.glare));
                break;
            case DARK_GLASSES:
                result = (getContext().getResources().getString(R.string.dark_glasses));
                break;
            case FACIAL_SHADOWING:
                result = (getContext().getResources().getString(R.string.face_shadows));
                break;
            case UNNATURAL_LIGHTING_COLOR:
                result = (getContext().getResources().getString(R.string.lighting_unnatural));
                break;
            default:
                Log.d(WORKFLOW_FRAGMENT_TAG, "Un-monitored Feedback: " + code);
                break;
        }
        return result;
    }
}
