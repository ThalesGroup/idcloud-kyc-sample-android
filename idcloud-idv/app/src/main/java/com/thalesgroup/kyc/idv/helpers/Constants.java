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

import android.graphics.Color;

public class Constants {

    public static final String DEFAULT_LIVENESS_TEXT_TURN_TO_TARGET = "Turn to target";
    public static final String DEFAULT_LIVENESS_TEXT_TURN_UP = "Turn up";
    public static final String DEFAULT_LIVENESS_TEXT_TURN_DOWN = "Turn down";
    public static final String DEFAULT_LIVENESS_TEXT_TURN_LEFT = "Turn left";
    public static final String DEFAULT_LIVENESS_TEXT_TURN_RIGHT = "Turn right";
    public static final String DEFAULT_LIVENESS_TEXT_TURN_TO_CENTER = "Turn to center";
    public static final String DEFAULT_LIVENESS_TEXT_KEEP_ROTATING = "Keep rotating yaw";
    public static final String DEFAULT_LIVENESS_TEXT_KEEP_ROTATING_WITH_SCORE = "Keep rotating yaw, score: %d";
    public static final String DEFAULT_LIVENESS_TEXT_KEEP_STILL = "Keep still";
    public static final String DEFAULT_LIVENESS_TEXT_KEEP_STILL_WITH_SCORE = "Keep still, score: %d";
    public static final String DEFAULT_LIVENESS_TEXT_BLINK = "Blink";

    public static final String KEY_LIVENESS_TEXT_TURN_TO_TARGET = "KeyTurnToTarget";
    public static final String KEY_LIVENESS_TEXT_TURN_UP = "KeyTurnUp";
    public static final String KEY_LIVENESS_TEXT_TURN_DOWN = "KeyTurnDown";
    public static final String KEY_LIVENESS_TEXT_TURN_LEFT = "KeyTurnLeft";
    public static final String KEY_LIVENESS_TEXT_TURN_RIGHT = "KeyTurnRight";
    public static final String KEY_LIVENESS_TEXT_TURN_TO_CENTER = "KeyTurnToCenter";
    public static final String KEY_LIVENESS_TEXT_KEEP_ROTATING = "KeyKeepRotating";
    public static final String KEY_LIVENESS_TEXT_KEEP_ROTATING_WITH_SCORE = "KeyKeepRotatingWithScore";
    public static final String KEY_LIVENESS_TEXT_KEEP_STILL = "KeyKeepStill";
    public static final String KEY_LIVENESS_TEXT_KEEP_STILL_WITH_SCORE = "KeyKeepStillWithScore";
    public static final String KEY_LIVENESS_TEXT_BLINK = "KeyBlink";

    public static final boolean DEFAULT_SHOW_FACE_RECTANGLE = true;
    public static final int DEFAULT_QUALITY_CHECK_ARROWS_COLOR = Color.RED;
    public static final int DEFAULT_QUALITY_CHECK_ARROWS_STROKE_WIDTH = 4;
    public static final int DEFAULT_QUALITY_CHECK_TEXT_COLOR = Color.RED;
    public static final int DEFAULT_LIVENESS_STATUS_SPACE = 10;
    public static final int DEFAULT_LIVENESS_TEXT_COLOR = Color.YELLOW;
    public static final int DEFAULT_PAINT_TEXT_STROKE_WIDTH = 0;
    public static final int DEFAULT_PAINT_TEXT_SIZE = 15;
    public static final int DEFAULT_LIVENESS_TEXT_SIZE = 15;
    public static final int DEFAULT_PAINT_COLOR = 0xFF3333FF;
    public static final int DEFAULT_LIVENESS_AREA_WIDTH = 2;

    public static final boolean DEFAULT_ROTATE_FACE_RECTANGLE = true;
    public static final int DEFAULT_FACE_RECTANGLE_WIDTH = 2;
    public static final boolean DEFAULT_SHOW_ICAO_ARROWS = true;
    public static final int DEFAULT_QUALITY_CHECK_TEXT_SIZE = 20;

}
