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

package com.thalesgroup.kyc.idv.helpers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

/**
 * Class to handle runtime permissions.
 */
public class PermissionManager {

    //region Public API

    /**
     * Checks for runtime permission.
     *
     * @param activity Calling activity.
     * @param askForThem {@code True} if missing permission should be requested, else {@code false}.
     * @param permissions List of permissions.
     *
     * @return {@code True} if permissions are present, else {@code false}.
     */
    @TargetApi(23)
    public static boolean checkPermissions(final Activity activity,
                                           final boolean askForThem,
                                           final String... permissions) {

        // Old SDK version does not have dynamic permissions.
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        final List<String> permissionsToCheck = new ArrayList<>();

        for (final String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PermissionChecker.PERMISSION_GRANTED) {
                permissionsToCheck.add(permission);
            }
        }

        if (!permissionsToCheck.isEmpty() && askForThem) {
            ActivityCompat
                    .requestPermissions(activity, permissionsToCheck.toArray(new String[0]), 0);
        }

        return permissionsToCheck.isEmpty();
    }

    //endregion
}
