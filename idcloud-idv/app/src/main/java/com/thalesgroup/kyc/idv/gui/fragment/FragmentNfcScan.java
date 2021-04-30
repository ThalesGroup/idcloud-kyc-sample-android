package com.thalesgroup.kyc.idv.gui.fragment;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.thalesgroup.idv.sdk.nfc.AccessKey;
import com.thalesgroup.idv.sdk.nfc.CaptureListener;
import com.thalesgroup.idv.sdk.nfc.CaptureProgress;
import com.thalesgroup.idv.sdk.nfc.CaptureResult;
import com.thalesgroup.idv.sdk.nfc.CaptureSDK;
import com.thalesgroup.idv.sdk.nfc.ConfigResult;
import com.thalesgroup.kyc.idv.BuildConfig;
import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCConfiguration;
import com.thalesgroup.kyc.idv.helpers.KYCManager;
import com.thalesgroup.kyc.idv.helpers.communication.KYCCommScheduler;
import com.thalesgroup.kyc.idv.helpers.communication.KYCCommunication;
import com.thalesgroup.kyc.idv.helpers.util.AssetHelper;

import java.util.Timer;
import java.util.TimerTask;

public class FragmentNfcScan extends AbstractFragmentBase implements CaptureListener.OnInitListener, CaptureListener.OnStartListener, NfcAdapter.ReaderCallback {
    protected static final int TIMEOUT_NO_CHIP = 62000;
    protected static final int DELAY_GIF = 13000;
    protected static final int NFC_READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;

    protected LinearLayout mLayout;
    protected LinearLayout mErrorLayout;
    protected TextView mCaption;
    protected ProgressBar mHourglass;
    protected LinearLayout mProgress;
    protected ProgressBar mGlobalProgress;
    protected TextView mStepCaption;
    protected Button mButtonCancel;
    protected Button mButtonRetry;
    protected Button mButtonAbort;
    protected CaptureSDK mNfcReader;
    protected boolean mIsTimerAborted = false;
    protected static Timer mTimerTimeout = null;
    protected static Timer mTimerGifs = null;
    protected NfcAdapter mNfcHandler;
    protected int mAnimId = 0;

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View retValue = inflater.inflate(R.layout.fragment_nfc_scan, container, false);

        mLayout = retValue.findViewById(R.id.fragment_nfc_scan_layout);
        mErrorLayout = retValue.findViewById(R.id.fragment_nfc_scan_layout_error);
        mHourglass = retValue.findViewById(R.id.fragment_nfc_scan_hourglass);;
        mProgress = retValue.findViewById(R.id.fragment_nfc_scan_progress);;
        mGlobalProgress = retValue.findViewById(R.id.fragment_nfc_scan_global_progress);;
        mStepCaption = retValue.findViewById(R.id.fragment_nfc_step_description);
        mButtonCancel = retValue.findViewById(R.id.fragment_nfc_scan_button_cancel);
        mButtonRetry = retValue.findViewById(R.id.fragment_nfc_scan_button_retry);
        mButtonAbort = retValue.findViewById(R.id.fragment_nfc_scan_button_abort);

        mHourglass.setVisibility(View.VISIBLE);
        mProgress.setVisibility(View.GONE);

        mNfcHandler = NfcAdapter.getDefaultAdapter(getContext());

        mNfcReader = new CaptureSDK(getMainActivity());
        mNfcReader.init(KYCConfiguration.IDV_LICENSE, this);

        // Animate caption and description
        long delay = KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_nfc_scan_caption), 0);
        mCaption = retValue.findViewById(R.id.fragment_nfc_scan_description);

        mCaption.setText(R.string.fragment_nfc_scan_description_wait);

        delay = KYCManager.animateViewWithDelay(mCaption, delay);

        // Populate layout with gif
        int[] gifs = new int[5];
        int[] texts = new int[5];

        gifs[0] = R.drawable.slide_phone;
        gifs[1] = R.drawable.photo_page;
        gifs[2] = R.drawable.back_page;
        gifs[3] = R.drawable.back_cover;
        gifs[4] = R.drawable.front_page;

        texts[0] = R.string.fragment_nfc_scan_slide;
        texts[1] = R.string.fragment_nfc_scan_photo_page;
        texts[2] = R.string.fragment_nfc_scan_back_page;
        texts[3] = R.string.fragment_nfc_scan_back_cover;
        texts[4] = R.string.fragment_nfc_scan_front_page;

        addGifs(gifs, texts, mLayout);

        KYCManager.animateViewWithDelay(mLayout, delay);

        mButtonCancel.setText(R.string.fragment_nfc_scan_button_cancel);

        mButtonCancel.setOnClickListener(view -> onButtonPressedNext());
        mButtonRetry.setOnClickListener(view -> onButtonPressedRetry());
        mButtonAbort.setOnClickListener(view -> onButtonPressedAbort());

        return retValue;
    }

    @Override
    public void onPause() {
        super.onPause();

        mNfcReader.finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mNfcReader.finish();
    }

    //endregion

    //region Private Helpers

    private long addGif(@DrawableRes final int gif,
                        final LinearLayout parent) {
        final View view = getLayoutInflater().inflate(R.layout.view_gif, null);
        final ImageView image = view.findViewById(R.id.view_gif_image_small);

        Glide.with(this).load(gif).into(image);
        parent.addView(view);

        return KYCManager.animateViewWithDelay(view, 0);
    }

    private long addGifs(@DrawableRes final int[] gifs,
                         @StringRes final int[] texts,
                        final LinearLayout parent) {
        final View view = getLayoutInflater().inflate(R.layout.view_gif, null);
        final ImageView imageSmall = view.findViewById(R.id.view_gif_image_small);
        final ImageView imageBig = view.findViewById(R.id.view_gif_image_big);

        mAnimId = 0;

        try
        {
            // Timer for Gifs
            killTimerGifs();

            mTimerGifs = new Timer(true);

            TimerTask timerGifTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    getMainActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            mLayout.removeAllViews();
                            addChevronSmall(R.drawable.info, texts[mAnimId], parent, 0);

                            if (mAnimId == 0) {
                                Glide.with(getMainActivity()).load(gifs[mAnimId]).into(imageSmall);
                                imageSmall.setVisibility(View.VISIBLE);
                                imageBig.setVisibility(View.GONE);
                            } else {
                                Glide.with(getMainActivity()).load(gifs[mAnimId]).into(imageBig);
                                imageSmall.setVisibility(View.GONE);
                                imageBig.setVisibility(View.VISIBLE);
                            }
                            parent.addView(view);

                            // set next gif to be shown...
                            mAnimId++;

                            if (mAnimId >= gifs.length) {
                                mAnimId = 0;
                            }
                        }
                    });
                }
            };

            mTimerGifs.scheduleAtFixedRate(timerGifTask, 0, DELAY_GIF);
        }
        catch(Exception e)
        {
            Log.e("KYC", e.toString());
        }

        return KYCManager.animateViewWithDelay(view, 0);
    }

    private long addChevron(@DrawableRes final int icon,
                            @StringRes final int caption,
                            final LinearLayout parent,
                            final long delay) {
        final View view = getLayoutInflater().inflate(R.layout.view_step, null);
        final ImageView image = view.findViewById(R.id.view_step_image);
        final TextView text = view.findViewById(R.id.view_step_text);

        image.setImageDrawable(getContext().getDrawable(icon));
        text.setText(caption);
        parent.addView(view);

        return KYCManager.animateViewWithDelay(view, delay);
    }

    private long addChevronSmall(@DrawableRes final int icon,
                            @StringRes final int caption,
                            final LinearLayout parent,
                            final long delay) {
        final View view = getLayoutInflater().inflate(R.layout.view_step_small, null);
        final ImageView image = view.findViewById(R.id.view_step_image);
        final TextView text = view.findViewById(R.id.view_step_text);

        image.setImageDrawable(getContext().getDrawable(icon));
        text.setText(caption);
        parent.addView(view);

        return KYCManager.animateViewWithDelay(view, delay);
    }

    private void onButtonPressedNext() {
        killTimerTimeout();
        killTimerGifs();

        mLayout.removeAllViews();
        addChevron(R.drawable.chevron_nfc, R.string.fragment_nfc_scan_canceled, mLayout, 0);

        mCaption.setText(R.string.fragment_nfc_scan_description_error);
        mHourglass.setVisibility(View.GONE);
        mButtonCancel.setVisibility(View.GONE);
        mErrorLayout.setVisibility(View.VISIBLE);
    }

    private void onButtonPressedRetry() {
        mNfcReader.init(KYCConfiguration.IDV_LICENSE, this);

        mCaption.setText(R.string.fragment_nfc_scan_description_wait);

        mLayout.removeAllViews();

        // Populate layout with gif
        int[] gifs = new int[5];
        int[] texts = new int[5];

        gifs[0] = R.drawable.slide_phone;
        gifs[1] = R.drawable.photo_page;
        gifs[2] = R.drawable.back_page;
        gifs[3] = R.drawable.back_cover;
        gifs[4] = R.drawable.front_page;

        texts[0] = R.string.fragment_nfc_scan_slide;
        texts[1] = R.string.fragment_nfc_scan_photo_page;
        texts[2] = R.string.fragment_nfc_scan_back_page;
        texts[3] = R.string.fragment_nfc_scan_back_cover;
        texts[4] = R.string.fragment_nfc_scan_front_page;

        addGifs(gifs, texts, mLayout);

        KYCManager.animateViewWithDelay(mLayout, 0);

        mErrorLayout.setVisibility(View.GONE);
        mButtonCancel.setVisibility(View.VISIBLE);
        mButtonCancel.setText(R.string.fragment_nfc_scan_button_cancel);
        mHourglass.setVisibility(View.VISIBLE);
        mProgress.setVisibility(View.GONE);
    }

    private void onButtonPressedAbort() {
        killTimerTimeout();
        killTimerGifs();

        getMainActivity().getSupportFragmentManager().popBackStack(1, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void killTimerTimeout() {
        // Timer for Timeout
        if (mTimerTimeout != null)
        {
            mTimerTimeout.purge();
            mTimerTimeout.cancel();

            mTimerTimeout = null;
        }
    }

    private void killTimerGifs() {
        // Timer for Gifs
        if (mTimerGifs != null)
        {
            mTimerGifs.purge();
            mTimerGifs.cancel();

            mTimerGifs =null;
        }
    }

    //endregion

    //region NFC Listeners

    @Override
    public void onInitialized(boolean completed, int errorCode) {
        if(completed) {
            if (BuildConfig.DEBUG) {
                Log.i("KYC", "DOC: " + DataContainer.instance().mDoc);
                Log.i("KYC", "DOB: " + DataContainer.instance().mDob);
                Log.i("KYC", "DOE: " + DataContainer.instance().mDoe);
            }

            AccessKey key = AccessKey.createMRZ(DataContainer.instance().mDoc, DataContainer.instance().mDob, DataContainer.instance().mDoe);

            mNfcReader.start(key, CaptureSDK.Protocol.BAC, this);

            mIsTimerAborted = false;

            // Timer for Timeout
            killTimerTimeout();
            mTimerTimeout = new Timer(true);

            try
            {
                TimerTask timerTask = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        if (!mIsTimerAborted) {
                            getMainActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    mIsTimerAborted = true;

                                    killTimerGifs();

                                    mLayout.removeAllViews();

                                    addChevron(R.drawable.chevron_nfc, R.string.fragment_nfc_scan_no_chip, mLayout, 0);

                                    mCaption.setText(R.string.fragment_nfc_scan_description_error);
                                    mHourglass.setVisibility(View.GONE);
                                    mButtonCancel.setVisibility(View.INVISIBLE);
                                    mErrorLayout.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                };

                mTimerTimeout.schedule(timerTask, TIMEOUT_NO_CHIP);
            }
            catch(Exception e)
            {
                Log.e("KYC", e.toString());
            }
        } else {
            Log.e("KYC", "Error on init: 0x" + Integer.toString(errorCode, 16));
            int errorType = (errorCode & 0x0F00);
            if(errorType == 0x0000){
                String[] errorMsg = { "No error found", "Unknown error", "Library not initialized", "Library is already initialized",
                        "Library init is in progress", "Library stop is in progress", "Library lazy finished",
                        "Error initializing IQA library", "Error initializing Engine", "Invalid cropping points",
                        "Invalid image sign", "Invalid image" };

                Log.w("KYC", " - Init error: " + errorMsg[(errorCode & 0xFF)]);
            }
            if (errorType == 0x0100) {
                String[] errorMsg = { "No error found", "Architecture not supported",
                        "Operating system not supported", "App permissions has been denied" };
                Log.w("KYC", " - Requirement error: " + errorMsg[(errorCode & 0xFF)]);
            }
            if (errorType == 0x0200) {
                String[] errorMsg = { "No error found", "The license is empty", "The license is expired",
                        "The feature selected is not present", "The license format is not correct",
                        "The license has not a valid signing hash", "The license has not a valid version",
                        "The license has not a valid feature list",
                        "The license has not a valid expiration date",
                        "The license has not a valid creation timestamp",
                        "The license has not a valid cache value" };
                Log.w("KYC", " - License error: " + errorMsg[(errorCode & 0xFF)]);
            }
        }
    }

    @Override
    public void onChipFound() {
        if (BuildConfig.DEBUG) {
            Log.i("KYC", "NFC.onChipFound()");
        }

        if (KYCManager.getInstance().isNfcSound()) {
            AssetHelper.playSound(AssetHelper.SOUND_NFC_START, getContext());
        }

        mCaption.setText(R.string.fragment_nfc_scan_description_read);
        mButtonCancel.setVisibility(View.INVISIBLE);
        mHourglass.setVisibility(View.GONE);
        mProgress.setVisibility(View.VISIBLE);

        mLayout.removeAllViews();
        addChevronSmall(R.drawable.info, R.string.fragment_nfc_scan_reading, mLayout, 0);
        addGif(R.drawable.reading_no_move, mLayout);

        mIsTimerAborted = true;

        killTimerTimeout();
        killTimerGifs();
    }

    @Override
    public void onConfiguration(ConfigResult configResult) {
        if (BuildConfig.DEBUG) {
            Log.i("KYC", "NFC.onConfiguration()");
            Log.i("KYC", "BAC  Enabled: " + configResult.BACEnabled);
            Log.i("KYC", "PACE Enabled: " + configResult.PACEEnabled);

            if(configResult.PACEEnabled) {
                Log.i("KYC", "PACE Protocols: ");

                for (ConfigResult.PACEInfo info : configResult.protocols) {
                    if(info != null) {
                        Log.i("KYC", "\t" + info.description);
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onProgress(CaptureProgress captureProgress) {
        mGlobalProgress.setProgress((int)(captureProgress.globalProgress * 100));

        if (captureProgress.step < 100) {
            mStepCaption.setText(getString(R.string.fragment_nfc_scan_description_step) + captureProgress.step);
        }
    }

    @Override
    public void onResult(CaptureResult captureResult) {
        if (BuildConfig.DEBUG) {
            Log.i("KYC", "NFC.onResult()");
        }

        // End SDK
        mNfcReader.finish();

        // To get tag when released by SDK and avoid propagation
        mNfcHandler.enableReaderMode(getMainActivity(), this, NFC_READER_FLAGS, null);

        if (captureResult.parsedData.faceImage != null) {
            if (KYCManager.getInstance().isNfcSound()) {
                AssetHelper.playSound(AssetHelper.SOUND_NFC_END, getContext());
            }

            mLayout.removeAllViews();
            addChevron(R.drawable.chevron_nfc, R.string.fragment_nfc_scan_ok, mLayout, 0);

            mHourglass.setVisibility(View.GONE);
            mProgress.setVisibility(View.VISIBLE);

            DataContainer.instance().mNfcResult = captureResult;

            if (KYCManager.getInstance().isFacialRecognition()) {
                KYCCommScheduler.sendData(KYCCommunication.STEP_START_DOC_VERIFICATION);

                getMainActivity().displayFragment(new FragmentFaceIdTutorial(), true, true);
            } else {
                KYCCommScheduler.sendData(KYCCommunication.STEP_START_DOC_VERIFICATION);

                getMainActivity().displayFragment(new FragmentKycOverview(), true, true);
            }
        }
        else {
            if (KYCManager.getInstance().isNfcSound()) {
                AssetHelper.playSound(AssetHelper.SOUND_NFC_ERROR, getContext());
            }

            mLayout.removeAllViews();
            addChevron(R.drawable.chevron_nfc, R.string.fragment_nfc_scan_ko, mLayout, 0);

            mCaption.setText(R.string.fragment_nfc_scan_description_error);
            mHourglass.setVisibility(View.GONE);
            mButtonCancel.setVisibility(View.GONE);
            mErrorLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        if (BuildConfig.DEBUG) {
            Log.i("KYC", "NFC.onError()" + errorCode + "/" + errorMessage);
        }

        // End SDK
        mNfcReader.finish();

        if (KYCManager.getInstance().isNfcSound()) {
            AssetHelper.playSound(AssetHelper.SOUND_NFC_ERROR, getContext());
        }

        mLayout.removeAllViews();
        addChevron(R.drawable.chevron_nfc, R.string.fragment_nfc_scan_ko, mLayout, 0);

        mCaption.setText(R.string.fragment_nfc_scan_description_error);
        mHourglass.setVisibility(View.GONE);
        mButtonCancel.setVisibility(View.GONE);
        mErrorLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        if (BuildConfig.DEBUG) {
            Log.i("KYC", "NfcAdapter.ReaderCallback.onTagDiscovered(): Avoid tag propagation");
        }
    }


    //endregion
}
