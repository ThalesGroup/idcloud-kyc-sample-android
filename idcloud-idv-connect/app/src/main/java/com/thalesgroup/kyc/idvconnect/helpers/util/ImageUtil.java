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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.acuant.acuantcommon.model.Image;
import com.acuant.acuantimagepreparation.AcuantImagePreparation;
import com.acuant.acuantimagepreparation.model.CroppingData;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Image helpers.
 */
public class ImageUtil {

    //region Public API

    /**
     * Reads file from given location.
     *
     * @param fileUrl
     *         Location.
     * @return Bytes or {code null} if error occures.
     */
    public static byte[] readFile(final String fileUrl) {
        final File file = new File(fileUrl);
        BufferedInputStream bufferedInputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            outputStream = new ByteArrayOutputStream();
            final byte[] buffer = new byte[4096];
            int read = bufferedInputStream.read(buffer, 0, buffer.length);
            while (read != -1) {
                outputStream.write(buffer, 0, read);
                read = bufferedInputStream.read(buffer, 0, buffer.length);
            }
        } catch (IOException e) {
            return null;
        } finally {
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    // nothing to do
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    // nothing to do
                }
            }
        }

        return outputStream.toByteArray();
    }

    /**
     * Crops the image.
     *
     * @param imageBytes
     *         Input image.
     * @return Output image.
     */
    public static Image cropImage(final byte[] imageBytes) {
        final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        final CroppingData data = new CroppingData();
        data.image = bitmap;
        return AcuantImagePreparation.crop(data);
    }

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
        bitmap.recycle();

        return stream.toByteArray();
    }

    /**
     * Resizes image based on the width.
     *
     * @param bitmap Input image.
     * @param width Desired width of resized image.
     *
     * @return Resized image.
     */
    public static Bitmap resize(final Bitmap bitmap, final int width) {
        final float ratio = (float) width / bitmap.getWidth();
        final int newHeight = Math.round(ratio * bitmap.getHeight());
        final int newWidth = Math.round(ratio * bitmap.getWidth());
        final Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        bitmap.recycle();

        return resizedBitmap;
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

    //endregion
}
