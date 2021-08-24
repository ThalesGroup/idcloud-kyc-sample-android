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

package com.thalesgroup.kyc.idvconnect.helpers.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Image helpers.
 */
public class ImageUtil {

    //region Public API

    /**
     * Transforms {@code Bitmap} to {@code byte[]}.
     *
     * @param bitmap
     *         Input {@code Bitmap}.
     * @return Output bytes.
     */
    public static byte[] bitmapToBytes(final Bitmap bitmap) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);

        return stream.toByteArray();
    }

    /**
     * Encodes the input byte array as Base64.
     *
     * @param imageData Input byte array.
     *
     * @return Base64 encoded {@code String}.
     */
    public static String base64FromImage(final byte[] imageData) {
        if (imageData == null) {
            return null;
        }

        return new String(Base64.encode(imageData, Base64.NO_WRAP));
    }

    /**
     * Decodes Base64 to image bytes.
     *
     * @param base64 Input Base64 {@code String}.
     *
     * @return Decoded byte array.
     */
    public static byte[] imageFromBase64(final String base64) {
        if (base64 == null) {
            return null;
        }

        return Base64.decode(base64, Base64.DEFAULT);
    }

    /**
     * Deletes the app cache directory
     * @param context app context
     */
    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {}
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    //endregion
}
