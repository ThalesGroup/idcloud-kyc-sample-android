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

import android.content.Context;

import java.util.Map;

public abstract class AbstractOption {


    //region Definition

    public enum DocumentType {
        IdCard(0),
        Passport(1),
        PassportBiometric(3);

        private int mValue;

        DocumentType(final int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static DocumentType fromId(final int value) {
            for (final DocumentType documentType : values()) {
                if (documentType.mValue == value) {
                    return documentType;
                }
            }
            return IdCard;
        }

        }

    enum OptionSection {
        General,
        DocumentScan,
        DocumentConfig,
        Version
    }

    enum OptionType {
        Undefined(0),
        Checkbox(1),
        Number(2),
        Version(3),
        Segment(4),
        Button(5),
        SectionHeader(6);

        private int mValue;

        OptionType(final int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static OptionType fromId(final int value) {
            for (final OptionType color : values()) {
                if (color.mValue == value) {
                    return color;
                }
            }
            return Undefined;
        }
    }

    private final OptionSection mSection;
    private final String mCaption;
    private final String mDescription;

    //endregion


    //region Properties

    OptionSection getSection() { return mSection; }

    String getCaption() {
        return mCaption;
    }

    String getDescription() {
        return mDescription;
    }


    public abstract OptionType getType();

    //endregion

    //region Life Cycle

    AbstractOption(final OptionSection section, final String caption, final String description) {
        mSection = section;
        mCaption = caption;
        mDescription = description;
    }

    //endregion

    static class Number extends AbstractOption {

        //region Definition

        interface Get {
            int action();
        }

        interface Set {
            boolean action(final int value);
        }

        private final Get mGetter;
        private final Set mSetter;

        private final int mValueMin;
        private final int mValueMax;

        //endregion

        //region Public API

        int getMinValue() {
            return mValueMin;
        }

        int getMaxValue() {
            return mValueMax;
        }

        boolean setValue(final int value, final Context context) {
            return mSetter.action(value);
        }

        int getValue(final Context context) {
            return mGetter.action();
        }

        @Override
        public OptionType getType() {
            return OptionType.Number;
        }

        //endregion

        //region Life cycle

        Number(final OptionSection section,
               final String caption, final String description,
               final int valueMin, final int valueMax,
               final Get getter, final Set setter) {
            super(section, caption, description);

            mValueMin = valueMin;
            mValueMax = valueMax;
            mGetter = getter;
            mSetter = setter;
        }

        //endregion

    }

    static class Checkbox extends AbstractOption {
        //region Definition

        interface Get {
            boolean action();
        }

        interface Set {
            boolean action(final boolean value);
        }

        private final Get mGetter;
        private final Set mSetter;

        //endregion

        //region Properties

        boolean setValue(final boolean value) {
            return mSetter.action(value);
        }

        boolean getValue() {
            return mGetter.action();
        }

        @Override
        public OptionType getType() {
            return OptionType.Checkbox;
        }

        //endregion

        //region Life cycle

        Checkbox(final OptionSection section,
                 final String caption, final String description,
                 final Get getter, final Set setter) {
            super(section, caption, description);

            mGetter = getter;
            mSetter = setter;
        }

        //endregion

    }

    static class Segment extends AbstractOption {

        //region Definition

        interface Get {
            String action();
        }

        interface Set {
            boolean action(final String value);
        }

        private final Get mGetter;
        private final Set mSetter;

        private final Map<String, String> mOptions;

        //endregion

        //region Life Cycle

        Segment(final OptionSection section,
                final String caption, final String description,
                final Map<String, String> options,
                final Get getter, final Set setter) {
            super(section, caption, description);

            mOptions = options;
            mGetter = getter;
            mSetter = setter;
        }

        //endregion

        //region Properties

        String getValue(final Context context) {
            return mGetter.action();
        }

        boolean setValue(final String value, final Context context) {
            return mSetter.action(value);
        }

        Map<String, String> getOptions() {
            return mOptions;
        }

        @Override
        public OptionType getType() {
            return OptionType.Segment;
        }

        //endregion
    }

    static class Version extends AbstractOption {

        //region Definition

        private final String mValue;

        //endregion

        //region Life cycle

        Version(final OptionSection section,
                final String caption, final String value) {
            super(section, caption, null);

            mValue = value;
        }

        //endregion

        //region Properties

        String getValue() {
            return mValue;
        }

        @Override
        public OptionType getType() {
            return OptionType.Version;
        }

        //endregion
    }

    static class Button extends AbstractOption {
        //region Definition

        interface Action {
            void doAction();
        }

        private final Action mAction;

        //endregion

        //region Properties

        Action getAction() {
            return mAction;
        }

        @Override
        public OptionType getType() {
            return OptionType.Button;
        }

        //endregion

        //region Life cycle

        Button(final OptionSection section,
               final String caption, final Action action) {
            super(section, caption, null);

            mAction = action;
        }

        //endregion
    }

    static class SectionHeader extends AbstractOption {

        //region Properties

        @Override
        public OptionType getType() {
            return OptionType.SectionHeader;
        }

        //endregion

        //region Life cycle

        SectionHeader(final OptionSection section, final String caption) {
            super(section, caption, null);
        }

        //endregion
    }

}
