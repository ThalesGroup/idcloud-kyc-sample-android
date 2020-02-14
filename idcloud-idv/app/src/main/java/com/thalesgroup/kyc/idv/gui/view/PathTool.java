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

package com.thalesgroup.kyc.idv.gui.view;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

public class PathTool {

    Path getArrowPath() {
        final Path arrow = new Path();
        arrow.moveTo(32, 322);
        arrow.lineTo(31, 315);
        arrow.lineTo(29, 308);
        arrow.lineTo(34, 302);
        arrow.lineTo(63, 259);
        arrow.lineTo(92, 216);
        arrow.lineTo(120, 173);
        arrow.lineTo(90, 129);
        arrow.lineTo(61, 84);
        arrow.lineTo(31, 39);
        arrow.lineTo(32, 35);
        arrow.lineTo(30, 28);
        arrow.lineTo(33, 25);
        arrow.lineTo(40, 25);
        arrow.lineTo(47, 22);
        arrow.lineTo(53, 27);
        arrow.lineTo(145, 73);
        arrow.lineTo(236, 118);
        arrow.lineTo(327, 165);
        arrow.lineTo(337, 181);
        arrow.lineTo(317, 187);
        arrow.lineTo(306, 192);
        arrow.lineTo(220, 236);
        arrow.lineTo(134, 279);
        arrow.lineTo(47, 322);
        arrow.lineTo(42, 322);
        arrow.lineTo(37, 323);
        arrow.lineTo(32, 322);
        return arrow;
    }

    Path getBlinkPath() {
        final Path blink = new Path();
        blink.moveTo(135, 129);
        blink.lineTo(135, 129);
        blink.lineTo(135, 126);
        blink.lineTo(135, 122);
        blink.lineTo(135, 119);
        blink.lineTo(131, 119);
        blink.lineTo(127, 118);
        blink.lineTo(122, 118);
        blink.lineTo(120, 123);
        blink.lineTo(119, 129);
        blink.lineTo(115, 133);
        blink.lineTo(111, 132);
        blink.lineTo(102, 132);
        blink.lineTo(103, 126);
        blink.lineTo(104, 122);
        blink.lineTo(107, 117);
        blink.lineTo(107, 112);
        blink.lineTo(103, 110);
        blink.lineTo(100, 108);
        blink.lineTo(97, 106);
        blink.lineTo(93, 110);
        blink.lineTo(90, 115);
        blink.lineTo(85, 118);
        blink.lineTo(81, 116);
        blink.lineTo(71, 111);
        blink.lineTo(77, 106);
        blink.lineTo(79, 102);
        blink.lineTo(87, 97);
        blink.lineTo(81, 92);
        blink.lineTo(79, 88);
        blink.lineTo(73, 85);
        blink.lineTo(74, 80);
        blink.lineTo(78, 76);
        blink.lineTo(84, 73);
        blink.lineTo(90, 75);
        blink.lineTo(97, 78);
        blink.lineTo(101, 85);
        blink.lineTo(108, 90);
        blink.lineTo(124, 101);
        blink.lineTo(147, 103);
        blink.lineTo(166, 96);
        blink.lineTo(176, 92);
        blink.lineTo(183, 85);
        blink.lineTo(191, 78);
        blink.lineTo(195, 74);
        blink.lineTo(201, 73);
        blink.lineTo(205, 76);
        blink.lineTo(210, 78);
        blink.lineTo(213, 82);
        blink.lineTo(208, 86);
        blink.lineTo(206, 89);
        blink.lineTo(203, 93);
        blink.lineTo(200, 96);
        blink.lineTo(204, 101);
        blink.lineTo(208, 105);
        blink.lineTo(211, 110);
        blink.lineTo(208, 113);
        blink.lineTo(202, 123);
        blink.lineTo(198, 116);
        blink.lineTo(194, 113);
        blink.lineTo(192, 106);
        blink.lineTo(187, 107);
        blink.lineTo(182, 110);
        blink.lineTo(175, 113);
        blink.lineTo(180, 119);
        blink.lineTo(182, 123);
        blink.lineTo(186, 130);
        blink.lineTo(179, 132);
        blink.lineTo(175, 133);
        blink.lineTo(169, 136);
        blink.lineTo(168, 130);
        blink.lineTo(166, 126);
        blink.lineTo(166, 118);
        blink.lineTo(161, 118);
        blink.lineTo(158, 119);
        blink.lineTo(154, 119);
        blink.lineTo(150, 119);
        blink.lineTo(150, 125);
        blink.lineTo(150, 132);
        blink.lineTo(150, 138);
        blink.lineTo(145, 138);
        blink.lineTo(140, 138);
        blink.lineTo(135, 138);
        blink.lineTo(135, 135);
        blink.lineTo(135, 132);
        blink.lineTo(135, 129);
        return blink;
    }

    Path getTargetPath() {
        final Path target = new Path();
        target.addCircle(40, 40, 40, Path.Direction.CW);
        target.addCircle(40, 40, 30, Path.Direction.CW);
        target.addCircle(40, 40, 20, Path.Direction.CW);
        target.addCircle(40, 40, 10, Path.Direction.CW);
        return target;
    }

    Path getRollPath() {
        final Path roll = new Path();
        roll.moveTo(10, 297);
        roll.lineTo(0, 301);
        roll.lineTo(2, 290);
        roll.lineTo(5, 282);
        roll.lineTo(24, 210);
        roll.lineTo(72, 147);
        roll.lineTo(134, 106);
        roll.lineTo(189, 68);
        roll.lineTo(254, 46);
        roll.lineTo(321, 42);
        roll.lineTo(329, 41);
        roll.lineTo(354, 43);
        roll.lineTo(333, 36);
        roll.lineTo(322, 27);
        roll.lineTo(295, 30);
        roll.lineTo(296, 11);
        roll.lineTo(293, 1);
        roll.lineTo(299, 3);
        roll.lineTo(306, 6);
        roll.lineTo(340, 21);
        roll.lineTo(375, 34);
        roll.lineTo(408, 49);
        roll.lineTo(373, 68);
        roll.lineTo(338, 87);
        roll.lineTo(302, 104);
        roll.lineTo(300, 99);
        roll.lineTo(297, 86);
        roll.lineTo(305, 84);
        roll.lineTo(320, 76);
        roll.lineTo(334, 69);
        roll.lineTo(349, 61);
        roll.lineTo(300, 61);
        roll.lineTo(250, 69);
        roll.lineTo(205, 89);
        roll.lineTo(118, 124);
        roll.lineTo(45, 199);
        roll.lineTo(22, 291);
        roll.lineTo(22, 299);
        roll.lineTo(16, 297);
        roll.lineTo(10, 297);
        roll.close();
        return roll;
    }

    Path getYawPath() {
        final Path yaw = new Path();
        yaw.moveTo(21, 102);
        yaw.lineTo(14, 95);
        yaw.lineTo(7, 89);
        yaw.lineTo(1, 81);
        yaw.lineTo(9, 70);
        yaw.lineTo(20, 61);
        yaw.lineTo(29, 51);
        yaw.lineTo(33, 48);
        yaw.lineTo(38, 41);
        yaw.lineTo(42, 40);
        yaw.lineTo(44, 47);
        yaw.lineTo(43, 55);
        yaw.lineTo(43, 62);
        yaw.lineTo(64, 62);
        yaw.lineTo(85, 59);
        yaw.lineTo(106, 53);
        yaw.lineTo(117, 50);
        yaw.lineTo(128, 45);
        yaw.lineTo(136, 36);
        yaw.lineTo(142, 30);
        yaw.lineTo(139, 44);
        yaw.lineTo(140, 48);
        yaw.lineTo(139, 57);
        yaw.lineTo(140, 67);
        yaw.lineTo(134, 73);
        yaw.lineTo(126, 83);
        yaw.lineTo(113, 89);
        yaw.lineTo(101, 92);
        yaw.lineTo(82, 98);
        yaw.lineTo(63, 100);
        yaw.lineTo(43, 101);
        yaw.lineTo(43, 108);
        yaw.lineTo(44, 115);
        yaw.lineTo(42, 121);
        yaw.lineTo(38, 120);
        yaw.lineTo(33, 113);
        yaw.lineTo(29, 110);
        yaw.lineTo(26, 107);
        yaw.lineTo(23, 105);
        yaw.lineTo(21, 102);

        yaw.moveTo(120, 34);
        yaw.lineTo(109, 26);
        yaw.lineTo(96, 23);
        yaw.lineTo(83, 21);
        yaw.lineTo(83, 14);
        yaw.lineTo(83, 7);
        yaw.lineTo(83, 0);
        yaw.lineTo(98, 2);
        yaw.lineTo(114, 6);
        yaw.lineTo(126, 14);
        yaw.lineTo(132, 18);
        yaw.lineTo(134, 27);
        yaw.lineTo(130, 32);
        yaw.lineTo(127, 36);
        yaw.lineTo(124, 38);
        yaw.lineTo(120, 34);
        return yaw;
    }

    Path getMovePath() {
        final Path move = new Path();
        move.moveTo(90, 105);
        move.lineTo(90, 100);
        move.lineTo(90, 95);
        move.lineTo(90, 90);
        move.lineTo(60, 90);
        move.lineTo(30, 90);
        move.lineTo(0, 90);
        move.lineTo(0, 70);
        move.lineTo(0, 50);
        move.lineTo(0, 30);
        move.lineTo(30, 30);
        move.lineTo(60, 30);
        move.lineTo(90, 29);
        move.lineTo(91, 20);
        move.lineTo(91, 10);
        move.lineTo(91, 0);
        move.lineTo(121, 20);
        move.lineTo(152, 39);
        move.lineTo(182, 59);
        move.lineTo(180, 64);
        move.lineTo(170, 68);
        move.lineTo(165, 72);
        move.lineTo(140, 88);
        move.lineTo(116, 104);
        move.lineTo(91, 120);
        move.lineTo(91, 115);
        move.lineTo(91, 110);
        move.lineTo(90, 105);

        move.moveTo(136, 88);
        move.lineTo(150, 79);
        move.lineTo(165, 71);
        move.lineTo(178, 61);
        move.lineTo(176, 55);
        move.lineTo(166, 52);
        move.lineTo(161, 48);
        move.lineTo(138, 33);
        move.lineTo(115, 18);
        move.lineTo(92, 3);
        move.lineTo(91, 12);
        move.lineTo(92, 22);
        move.lineTo(92, 31);
        move.lineTo(62, 31);
        move.lineTo(32, 31);
        move.lineTo(2, 31);
        move.lineTo(2, 50);
        move.lineTo(2, 70);
        move.lineTo(2, 89);
        move.lineTo(32, 89);
        move.lineTo(62, 89);
        move.lineTo(92, 89);
        move.lineTo(92, 98);
        move.lineTo(91, 108);
        move.lineTo(93, 117);
        move.lineTo(107, 108);
        move.lineTo(122, 98);
        move.lineTo(136, 88);
        move.close();
        return move;
    }

    Path getPitchPath() {
        final Path pitch = new Path();
        pitch.moveTo(92, 45);
        pitch.lineTo(89, 45);
        pitch.lineTo(87, 45);
        pitch.lineTo(84, 45);
        pitch.lineTo(83, 72);
        pitch.lineTo(80, 100);
        pitch.lineTo(72, 126);
        pitch.lineTo(69, 134);
        pitch.lineTo(64, 141);
        pitch.lineTo(57, 147);
        pitch.lineTo(50, 151);
        pitch.lineTo(42, 149);
        pitch.lineTo(34, 149);
        pitch.lineTo(29, 150);
        pitch.lineTo(27, 146);
        pitch.lineTo(32, 144);
        pitch.lineTo(41, 132);
        pitch.lineTo(43, 117);
        pitch.lineTo(46, 102);
        pitch.lineTo(49, 84);
        pitch.lineTo(51, 65);
        pitch.lineTo(51, 45);
        pitch.lineTo(46, 45);
        pitch.lineTo(37, 47);
        pitch.lineTo(34, 43);
        pitch.lineTo(36, 37);
        pitch.lineTo(42, 32);
        pitch.lineTo(46, 26);
        pitch.lineTo(53, 17);
        pitch.lineTo(59, 8);
        pitch.lineTo(67, 0);
        pitch.lineTo(78, 12);
        pitch.lineTo(88, 26);
        pitch.lineTo(98, 40);
        pitch.lineTo(103, 46);
        pitch.lineTo(97, 45);
        pitch.lineTo(92, 45);

        pitch.moveTo(51, 22);
        pitch.lineTo(45, 30);
        pitch.lineTo(40, 37);
        pitch.lineTo(35, 44);
        pitch.lineTo(40, 44);
        pitch.lineTo(46, 44);
        pitch.lineTo(52, 44);
        pitch.lineTo(52, 72);
        pitch.lineTo(49, 100);
        pitch.lineTo(42, 127);
        pitch.lineTo(40, 135);
        pitch.lineTo(36, 143);
        pitch.lineTo(30, 148);
        pitch.lineTo(38, 148);
        pitch.lineTo(47, 149);
        pitch.lineTo(55, 146);
        pitch.lineTo(65, 140);
        pitch.lineTo(69, 128);
        pitch.lineTo(73, 117);
        pitch.lineTo(80, 93);
        pitch.lineTo(81, 68);
        pitch.lineTo(83, 44);
        pitch.lineTo(88, 44);
        pitch.lineTo(94, 44);
        pitch.lineTo(99, 44);
        pitch.lineTo(89, 30);
        pitch.lineTo(78, 15);
        pitch.lineTo(67, 1);
        pitch.lineTo(62, 8);
        pitch.lineTo(56, 15);
        pitch.lineTo(51, 22);

        pitch.moveTo(28, 138);
        pitch.lineTo(23, 146);
        pitch.lineTo(12, 140);
        pitch.lineTo(11, 133);
        pitch.lineTo(4, 120);
        pitch.lineTo(2, 106);
        pitch.lineTo(1, 92);
        pitch.lineTo(3, 86);
        pitch.lineTo(13, 89);
        pitch.lineTo(17, 90);
        pitch.lineTo(20, 95);
        pitch.lineTo(19, 102);
        pitch.lineTo(22, 108);
        pitch.lineTo(24, 116);
        pitch.lineTo(27, 124);
        pitch.lineTo(31, 132);
        pitch.lineTo(3, 135);
        pitch.lineTo(29, 137);
        pitch.lineTo(28, 138);

        pitch.moveTo(27, 128);
        pitch.lineTo(21, 117);
        pitch.lineTo(20, 104);
        pitch.lineTo(17, 92);
        pitch.lineTo(13, 89);
        pitch.lineTo(6, 90);
        pitch.lineTo(2, 91);
        pitch.lineTo(4, 106);
        pitch.lineTo(7, 123);
        pitch.lineTo(15, 137);
        pitch.lineTo(18, 144);
        pitch.lineTo(31, 139);
        pitch.lineTo(29, 132);
        pitch.lineTo(28, 130);
        pitch.lineTo(27, 129);
        pitch.lineTo(27, 128);
        return pitch;
    }

    Path preparePath(final Path path, final Rect area, final boolean mirror) {
        final Matrix matrix = new Matrix();
        final RectF pathRect = new RectF();
        path.computeBounds(pathRect, true);
        path.offset(-pathRect.left, -pathRect.top);

        if (mirror) {
            matrix.postConcat(getMirrorMatrix());
            matrix.postTranslate(pathRect.width(), 0);
        }

        final float scale = Math.min(area.width() / pathRect.width(), area.height() / pathRect.height());
        matrix.postScale(scale, scale);

        matrix.postTranslate(area.left, area.top);

        final Path result = new Path();
        result.addPath(path, matrix);
        return result;
    }

    private Matrix getMirrorMatrix() {
        final float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
        final Matrix matrixMirrorY = new Matrix();
        matrixMirrorY.setValues(mirrorY);
        return matrixMirrorY;
    }
}
