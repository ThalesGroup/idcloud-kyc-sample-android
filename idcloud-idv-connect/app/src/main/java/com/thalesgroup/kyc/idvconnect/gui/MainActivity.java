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

package com.thalesgroup.kyc.idvconnect.gui;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.acuant.acuantcamera.camera.AcuantCameraActivity;
import com.acuant.acuantcamera.constant.Constants;
import com.acuant.acuantcommon.exception.AcuantException;
import com.acuant.acuantcommon.initializer.AcuantInitializer;
import com.acuant.acuantcommon.initializer.IAcuantPackage;
import com.acuant.acuantcommon.initializer.IAcuantPackageCallback;
import com.acuant.acuantcommon.model.Credential;
import com.acuant.acuantcommon.model.Error;
import com.acuant.acuantcommon.model.ErrorCodes;
import com.acuant.acuanthgliveness.model.FaceCapturedImage;
import com.acuant.acuantimagepreparation.initializer.ImageProcessorInitializer;
import com.google.android.material.navigation.NavigationView;
import com.thalesgroup.kyc.idvconnect.R;
import com.thalesgroup.kyc.idvconnect.gui.activity.FacialLivenessActivity;
import com.thalesgroup.kyc.idvconnect.gui.fragment.FragmentFaceIdTutorial;
import com.thalesgroup.kyc.idvconnect.gui.fragment.FragmentHome;
import com.thalesgroup.kyc.idvconnect.gui.fragment.FragmentKycOverview;
import com.thalesgroup.kyc.idvconnect.gui.fragment.FragmentMissingPermissions;
import com.thalesgroup.kyc.idvconnect.helpers.AbstractOption;
import com.thalesgroup.kyc.idvconnect.helpers.Configuration;
import com.thalesgroup.kyc.idvconnect.helpers.DataContainer;
import com.thalesgroup.kyc.idvconnect.helpers.KYCManager;
import com.thalesgroup.kyc.idvconnect.helpers.OptionAdapter;
import com.thalesgroup.kyc.idvconnect.helpers.PermissionManager;
import com.thalesgroup.kyc.idvconnect.helpers.util.ImageUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * Main entry point of the application.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    //region Definition

    public static final int ANIM_DURATION_SLOW_MS = 1500;
    public static final String BUNDLE_ARGUMENT_DOC_TYPE = "doc_type";

    private static final int REQUEST_ID_DOC_SCAN = 1;
    private static final int REQUEST_ID_FACE_SCAN = 2;

    private DrawerLayout mDrawer;
    private OptionAdapter mOptionAdapter;
    private ListView mDrawerList;
    private NavigationView mNavigationView;
    private boolean mInited = false;
    private FragmentMissingPermissions mMissingPermissions = null;
    private LinearLayout mProgress;
    private boolean mFrontDocument = true;
    private AbstractOption.DocumentType mDocumentType;

    //endregion

    //region Life Cycle

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get all required ui elements.
        mNavigationView = findViewById(R.id.activity_main_nav_view);
        mDrawer = findViewById(R.id.activity_main_drawer_layout);
        mDrawerList = findViewById(R.id.activity_main_table);
        mProgress = findViewById(R.id.activity_main_progress);

        // Check for permissions or display fragment with information.
        if (!checkMandatoryPermissions(true)) {
            mMissingPermissions = new FragmentMissingPermissions();
            displayFragment(mMissingPermissions, false, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mInited) {
            checkPermissionsAndInit();
        }
    }

    @Override
    public void onActivityResult(final int requestCode,
                                 final int resultCode,
                                 final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ID_DOC_SCAN) {
            onActivityResultDocument(resultCode, data);
        } else {
            onActivityResultFace(resultCode);
        }
    }

    //endregion

    //region Private Helpers

    /**
     * Checks the required runtime permissions and initializes the main SDK.
     */
    private void checkPermissionsAndInit() {
        // In case we don't have permissions yet, simple wait for another call.
        // FragmentMissingPermissions will take care of that.
        if (!checkMandatoryPermissions(false)) {
            return;
        }
        mInited = true;

        // Init all required SDK's
        KYCManager.getInstance().initialise(this);

        // Setup Toolbar, Drawer and related icon.
        mNavigationView.setNavigationItemSelectedListener(this);
        mOptionAdapter = new OptionAdapter(this, KYCManager.getInstance().getOptions());
        mDrawerList.setAdapter(mOptionAdapter);
        mDrawerList.setOnItemClickListener((parent, view, position, id) -> {
            mOptionAdapter.onItemClick(position, view);
        });

        // Remove missing permission fragment if it's present.
        if (mMissingPermissions != null) {
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .remove(mMissingPermissions)
                    .commit();
            mMissingPermissions = null;
        }

        Credential.init(Configuration.ACUANT_USERNAME,
                Configuration.ACUANT_PASSWORD,
                Configuration.ACUANT_SUBSCRIPTION_ID,
                Configuration.ACUANT_FRM_ENDPOINT,
                Configuration.ACUANT_ASSURE_ID_ENDPOINT,
                Configuration.ACUANT_MEDISCAN_ENDPOINT);
        final List<IAcuantPackage> list = new ArrayList<>();
        list.add(new ImageProcessorInitializer());
        try {
            AcuantInitializer.intialize(null, getApplicationContext(), list, new IAcuantPackageCallback() {
                @Override
                public void onInitializeSuccess() {
                    displayFragment(new FragmentHome(), false, false);
                }

                @Override
                public void onInitializeFailed(final List<? extends Error> list) {
                    Toast.makeText(MainActivity.this, "Error: " + list.get(0).errorDescription,
                                   Toast.LENGTH_LONG).show();
                }
            });
        } catch (final AcuantException e) {
            Toast.makeText(this, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handles the {@link this#onActivityResult} method in case of scanning of document.
     *
     * @param resultCode Result code from document scanning.
     * @param data Data received from document scanning.
     */
    private void onActivityResultDocument(final int resultCode, final Intent data) {
        if (resultCode == AcuantCameraActivity.RESULT_SUCCESS_CODE) {
            final String fileUrl = data.getStringExtra(Constants.ACUANT_EXTRA_IMAGE_URL);
            final byte[] imageBytes = ImageUtil.readFile(fileUrl);
            new Thread(()->{
                final Bitmap croppedImage = ImageUtil.cropImage(imageBytes);
                final Bitmap resizedImage = ImageUtil.resize(croppedImage, 640);
                final byte[] imageBytesResized = ImageUtil.bitmapToBytes(resizedImage);
                runOnUiThread(()-> {
                    if (mFrontDocument) {
                        DataContainer.instance().mDocFront = imageBytesResized;
                        if (mDocumentType == AbstractOption.DocumentType.IdCard) {
                            scanBackSide();
                        } else if (KYCManager.getInstance().isFacialRecognition()) {
                            displayFragment(new FragmentFaceIdTutorial(), true, true);
                        } else {
                            displayFragment(new FragmentKycOverview(), true, true);
                        }
                    } else {
                        DataContainer.instance().mDocBack = imageBytesResized;
                        if (KYCManager.getInstance().isFacialRecognition()) {
                            displayFragment(new FragmentFaceIdTutorial(), true, true);
                        } else {
                            displayFragment(new FragmentKycOverview(), true, true);
                        }
                    }
                });
            }).run();
        } else {
            Toast.makeText(this, "Document capture failed.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Begins the scanning of the back side of the document.
     */
    private void scanBackSide() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scan Back Side");
        builder.setMessage("Turn document and scan back side.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            mFrontDocument = false;
            openDocScanActivity(AbstractOption.DocumentType.IdCard);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Handles the {@link this#onActivityResult} method in case of scanning of face.
     *
     * @param resultCode Result code from face scanning.
     */
    private void onActivityResultFace(final int resultCode) {
        if (resultCode == ErrorCodes.ERROR_CAPTURING_FACIAL || resultCode == ErrorCodes.USER_CANCELED_FACIAL) {
            // For demo purposes. On error, start again.
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Face capture failed");
            builder.setMessage("Error");
            builder.setPositiveButton("Try Again", (dialogInterface, i) -> openFaceScanActivity());
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } else {
            final Bitmap image = FaceCapturedImage.Companion.getBitmapImage();
            if (image != null) {
                final Bitmap resizedImage = ImageUtil.resize(image, 480);
                DataContainer.instance().mSelfie = ImageUtil.bitmapToBytes(resizedImage);
                displayFragment(new FragmentKycOverview(), true, true);
            } else {
                Toast.makeText(this, "Error: " + "Cannot retrieve image.", Toast.LENGTH_LONG).show();
            }
        }
    }

    //endregion

    //region OnNavigationItemSelectedListener

    @Override
    public boolean onNavigationItemSelected(final @NonNull MenuItem menuItem) {
        return false;
    }

    //endregion

    //region Public Api

    /**
     * Helper method which displays a {@code Fragment}.
     *
     * @param fragment Fragment to display.
     * @param addToStack {@code True} if {@code Fragment} should be added to backstack.
     * @param animated {@code True} if {@code Fragment} should be added to backstack.
     */
    public void displayFragment(final Fragment fragment,
                                final boolean addToStack,
                                final boolean animated) {
        final FragmentTransaction transition = getSupportFragmentManager().beginTransaction()
                .replace(R.id.app_bar_main_fragment_container, fragment, null);

        if (animated) {
            transition.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        if (addToStack) {
            transition.addToBackStack(null);
        }

        transition.commit();
    }

    public void reloadDrawerData(final List<AbstractOption> navItems) {
        mOptionAdapter.updateWithItems(navItems);
    }

    /**
     * Checks the required runtime permissions.
     *
     * @param askForThem {@code True} if dialog application should request missing permissions, else {@code false}.
     * @return {@code True} if all permissions are present, else {@code false}.
     */
    public boolean checkMandatoryPermissions(final boolean askForThem) {
        return PermissionManager.checkPermissions(this,
                askForThem,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET);
    }

    /**
     * Begins the scanning of the front side of the document.
     *
     * @param docType Document type to scan.
     */
    public void scanFrontSide(final AbstractOption.DocumentType docType) {
        mDocumentType = docType;
        mFrontDocument = true;

        openDocScanActivity(AbstractOption.DocumentType.IdCard);
    }

    /**
     * Opens the activity for document scanning.
     *
     * @param docType Document type.
     */
    public void openDocScanActivity(final AbstractOption.DocumentType docType) {
        final Intent cameraIntent = new Intent(MainActivity.this, AcuantCameraActivity.class);
        cameraIntent.putExtra(Constants.ACUANT_EXTRA_IS_AUTO_CAPTURE, true);
        cameraIntent.putExtra(Constants.ACUANT_EXTRA_BORDER_ENABLED, true);
        startActivityForResult(cameraIntent, REQUEST_ID_DOC_SCAN);
    }

    /**
     * Opens the face scanning activity.
     */
    public void openFaceScanActivity() {
        final Intent cameraIntent = new Intent(MainActivity.this, FacialLivenessActivity.class);
        startActivityForResult(cameraIntent, REQUEST_ID_FACE_SCAN);
    }

    /**
     * Shows the progress bar.
     */
    public void progressBarShow() {
        mProgress.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    /**
     * Hides the progress bar.
     */
    public void progressBarHide() {
        mProgress.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    /**
     * Enables or disables the side menu(drewer).
     *
     * @param enable {@code True} if drawer should be enabled, else {@code false}.
     */
    public void enableDrawer(final boolean enable) {
        final int lockMode = enable ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
        mDrawer.setDrawerLockMode(lockMode);

        final ImageView menuButton = findViewById(R.id.app_bar_main_menu_button);
        if (enable) {
            menuButton.setOnClickListener(view -> onButtonPressedMenu());
            menuButton.setVisibility(View.VISIBLE);
        } else {
            menuButton.setOnClickListener(null);
            menuButton.setVisibility(View.INVISIBLE);
        }
    }

    //endregion

    //region User Interface

    /**
     * On click listener for menu button.
     */
    private void onButtonPressedMenu() {
         mDrawer.openDrawer(Gravity.LEFT);
    }

    //endregion
}
