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

package com.thalesgroup.kyc.idv.gui;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.activity.ActivityCaptureDocIDVLandscape;
import com.thalesgroup.kyc.idv.gui.activity.ActivityCaptureDocIDVPortrait;
import com.thalesgroup.kyc.idv.gui.activity.ActivityCaptureFaceIDV;
import com.thalesgroup.kyc.idv.gui.fragment.FragmentFaceIdTutorial;
import com.thalesgroup.kyc.idv.gui.fragment.FragmentHome;
import com.thalesgroup.kyc.idv.gui.fragment.FragmentKycOverview;
import com.thalesgroup.kyc.idv.gui.fragment.FragmentMissingPermissions;
import com.thalesgroup.kyc.idv.helpers.AbstractOption;
import com.thalesgroup.kyc.idv.helpers.KYCManager;
import com.thalesgroup.kyc.idv.helpers.OptionAdapter;
import com.thalesgroup.kyc.idv.helpers.PermissionManager;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    //region Definition

    public static final int ANIM_DURATION_SLOW_MS = 1500;
    public static final String BUNDLE_ARGUMENT_DOC_TYPE = "doc_type";

    private static final int REQUEST_ID_DOC_SCAN = 1;
    private static final int REQUEST_ID_FACE_SCAN = 2;

    public static final int CAPTURE_RETURN_CODE_OK = 1;
    public static final int CAPTURE_RETURN_CODE_ERR = 2;
    public static final String CAPTURE_EXTRA_ERROR_CORE = "ERR_CODE";
    public static final String CAPTURE_EXTRA_ERROR_MESSAGE = "ERR_MESSAGE";

    private DrawerLayout mDrawer;
    private OptionAdapter mOptionAdapter;
    private ListView mDrawerList;
    private NavigationView mNavigationView;
    private boolean mInited = false;
    private FragmentMissingPermissions mMissingPermissions = null;
    private LinearLayout mProgress;

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
        } else if (requestCode == REQUEST_ID_FACE_SCAN) {
            onActivityResultFace(resultCode, data);
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

        // Drawer Item click listeners
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

        displayFragment(new FragmentHome(), false, false);
    }

    private void onActivityResultDocument(final int resultCode, final Intent data) {
        if (resultCode == CAPTURE_RETURN_CODE_OK) {
            if (KYCManager.getInstance().isFacialRecognition()) {
                displayFragment(new FragmentFaceIdTutorial(), true, true);
            } else {
                displayFragment(new FragmentKycOverview(), true, true);
            }
        } else if (resultCode == CAPTURE_RETURN_CODE_ERR) {
            final int errCode = data.getIntExtra(CAPTURE_EXTRA_ERROR_CORE, -1);
            Toast.makeText(this, "Document capture failed. Error code: " + errCode, Toast.LENGTH_LONG).show();
        }
    }

    private void onActivityResultFace(final int resultCode, final Intent data) {
        if (resultCode == CAPTURE_RETURN_CODE_OK) {
            displayFragment(new FragmentKycOverview(), true, true);
        } else if (resultCode == CAPTURE_RETURN_CODE_ERR) {
            final String errMessage = data.getStringExtra(CAPTURE_EXTRA_ERROR_MESSAGE);

            // For demo purposes. On error, start again.
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Face capture failed");
            builder.setMessage(errMessage);
            builder.setPositiveButton("Try Again", (dialogInterface, i) -> openFaceScanActivity());
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
    }

    //endregion

    //region OnNavigationItemSelectedListener

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
        return false;
    }

    //endregion

    //region Public Api

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

    public void openDocScanActivity(final AbstractOption.DocumentType docType) {
        Intent intent = null;
        if (KYCManager.getInstance().isCameraOrientationPortrait()) {
            intent = new Intent(MainActivity.this, ActivityCaptureDocIDVPortrait.class);
        } else {
            intent = new Intent(MainActivity.this, ActivityCaptureDocIDVLandscape.class);
        }

        intent.putExtra(MainActivity.BUNDLE_ARGUMENT_DOC_TYPE, docType);
        startActivityForResult(intent, REQUEST_ID_DOC_SCAN);
    }

    public void openFaceScanActivity() {
        final Intent intent = new Intent(MainActivity.this, ActivityCaptureFaceIDV.class);
        startActivityForResult(intent, REQUEST_ID_FACE_SCAN);
    }


    public void progressBarShow() {
        mProgress.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

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
            menuButton.setOnClickListener(thisview -> onButtonPressedMenu());
            menuButton.setVisibility(View.VISIBLE);
        } else {
            menuButton.setOnClickListener(null);
            menuButton.setVisibility(View.INVISIBLE);
        }
    }

    //endregion

    //region User Interface

    private void onButtonPressedMenu() {
        mDrawer.openDrawer(Gravity.LEFT);
    }

    //endregion
}
