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

package com.thalesgroup.kyc.idvconnect.gui.animation;

import android.view.animation.Interpolator;

/**
 * Set of {@link Interpolator} used for animations in the application.
 */
public class EaseInterpolators {

    /**
     * {@code Interpolator} used for moving a view in/out of the screen.
     */
    public static class EaseInOut implements Interpolator {

        @Override
        public float getInterpolation(final float input) {
            float shift = input * 2.f;
            if (input < .5f) {
                return (float) (.5f * Math.pow(shift, 5));
            }
            shift = (input - .5f) * 2 - 1;
            return (float) (.5f * Math.pow(shift, 5) + 1);
        }
    }

    /**
     * {@code Interpolator} used for moving a view out of the screen.
     */
    public static class EaseOut implements Interpolator {

        @Override
        public float getInterpolation(final float input) {
            return (float) -Math.pow(2, -10 * input) + 1;
        }
    }
}


