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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.thalesgroup.kyc.idv.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.LayoutRes;

public class OptionAdapter extends BaseAdapter {

    /**
     * Helper class to reduce findViewById calls for reusable cells.
     */
    private static class TableCellNumberView extends FrameLayout {

        final TextView mTitle;
        final TextView mSubtitle;
        final TextView mValue;
        final SeekBar mValueSlider;

        @SuppressLint("ClickableViewAccessibility")
        private TableCellNumberView(final Context context) {
            super(context);

            // Load visuals.
            final View contentView = inflate(context, R.layout.table_cell_number, null);

            mTitle = contentView.findViewById(R.id.table_cell_number_title);
            mSubtitle = contentView.findViewById(R.id.table_cell_number_subtitle);
            mValue = contentView.findViewById(R.id.table_cell_number_value);
            mValueSlider = contentView.findViewById(R.id.table_cell_number_value_slider);

            // Help drawer to not take over touch.
            mValueSlider.setOnTouchListener((v, event) -> {
                final int action = event.getAction();

                if (action == MotionEvent.ACTION_DOWN) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                } else if (action == MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }

                // Handle seekbar touch events.
                v.onTouchEvent(event);
                return true;
            });

            addView(contentView);
        }
    }

    /**
     * Helper class for spinner so we can store both key and value.
     */
    private static class SegmentKeyCaption {

        private final String mKey;
        private final String mCaption;

        public SegmentKeyCaption(final String key, final String caption) {
            mKey = key;
            mCaption = caption;
        }


        public String getKey() {
            return mKey;
        }

        public String getCaption() {
            return mCaption;
        }

        @Override
        public String toString() {
            return mCaption;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof SegmentKeyCaption) {
                final SegmentKeyCaption segment = (SegmentKeyCaption) obj;
                return segment.getCaption().equals(mCaption) && segment.getKey().equals(mKey);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mCaption, mKey);
        }

    }

    private final Context mContext;
    private List<AbstractOption> mNavItems;

    public OptionAdapter(final Context context, final List<AbstractOption> navItems) {
        super();

        mContext = context;
        mNavItems = navItems;
    }

    public void updateWithItems(final List<AbstractOption> navItems) {
        mNavItems = navItems;
        notifyDataSetChanged();
    }

    public void onItemClick(final int position, final View convertView) {
        switch (mNavItems.get(position).getType()) {
            case Undefined:
                throw new IllegalArgumentException("Undefined table cell type.");
            case Checkbox:
                final Switch switchValue = convertView.findViewById(R.id.table_cell_boolean_switch);
                switchValue.setChecked(!switchValue.isChecked());
                break;
            case Button:
                ((AbstractOption.Button) mNavItems.get(position)).getAction().doAction();
                break;
            case Segment:
                final Spinner spinner = convertView.findViewById(R.id.table_cell_segment_spinner);
                spinner.performClick();
                break;
            case Number:
            case Version:
            case SectionHeader:
                // No action needed
                break;
            default:
                // nothing to do
        }
    }

    private View inflateView(final View convertView, @LayoutRes final int layout) {
        if (convertView != null) {
            return convertView;
        } else {
            final LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            return inflater.inflate(layout, null);
        }
    }

    //region BaseAdapter

    @Override
    public int getCount() {
        return mNavItems.size();
    }

    @Override
    public Object getItem(final int position) {
        return mNavItems.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return AbstractOption.OptionType.values().length;
    }

    @Override
    public int getItemViewType(final int position) {
        return mNavItems.get(position).getType().getValue();
    }

    @Override
    public boolean isEnabled(final int position) {
        final AbstractOption.OptionSection section = mNavItems.get(position).getSection();
        boolean retValue = super.isEnabled(position);

        return retValue;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {

        final AbstractOption option = mNavItems.get(position);
        final boolean isEnabled = isEnabled(position);
        switch (option.getType()) {
            case Undefined:
                throw new IllegalArgumentException("Undefined table cell type.");
            case Checkbox:
                return checkboxLoad((AbstractOption.Checkbox) option, convertView, isEnabled);
            case Number:
                return numberLoad((AbstractOption.Number) option, convertView, isEnabled);
            case Version:
                return versionLoad((AbstractOption.Version) option, convertView);
            case Segment:
                return segmentLoad((AbstractOption.Segment) option, convertView, isEnabled);
            case Button:
                return buttonLoad((AbstractOption.Button) option, convertView, isEnabled);
            case SectionHeader:
                return sectionHeaderLoad((AbstractOption.SectionHeader) option, convertView);
            default:
                // nothing to do
        }

        return null;
    }

    //endregion

    //region Option Types

    private View checkboxLoad(final AbstractOption.Checkbox option,
                              final View convertView,
                              final boolean enabled) {
        final View retValue = inflateView(convertView, R.layout.table_cell_boolean);

        // Load caption and description.
        ((TextView) retValue.findViewById(R.id.table_cell_boolean_title)).setText(option.getCaption());
        ((TextView) retValue.findViewById(R.id.table_cell_boolean_subtitle)).setText(option.getDescription());

        // Bind switch to data layer.
        final Switch switchValue = retValue.findViewById(R.id.table_cell_boolean_switch);
        // Remove listener before changing value to prevent change on older item.
        switchValue.setEnabled(enabled);
        switchValue.setOnCheckedChangeListener(null);
        switchValue.setChecked(option.getValue());
        switchValue.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!option.setValue(switchValue.isChecked())) {
                switchValue.setChecked(!isChecked);
            } else {
                this.notifyDataSetChanged();
            }
        });

        return retValue;
    }

    private View sectionHeaderLoad(final AbstractOption.SectionHeader option,
                                   final View convertView) {
        final View retValue = inflateView(convertView, R.layout.table_cell_section_header);

        // Load caption.
        ((TextView) retValue.findViewById(R.id.table_cell_section_header_title)).setText(option.getCaption());

        return retValue;
    }

    private View numberLoad(final AbstractOption.Number option,
                            final View convertView,
                            final boolean enabled) {
        final TableCellNumberView retValue = convertView != null ? (TableCellNumberView) convertView : new TableCellNumberView(mContext);

        // Load caption, description and value.
        retValue.mTitle.setText(option.getCaption());
        retValue.mSubtitle.setText(option.getDescription());
        retValue.mValueSlider.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(final SeekBar seekBar) {
                        final int value = option.getMinValue() + seekBar.getProgress();
                        final int originalValue = option.getValue(mContext);
                        if (originalValue != value) {
                            if (!option.setValue(value, mContext)) {
                                retValue.mValueSlider.setProgress(originalValue);
                            } else {
                                OptionAdapter.this.notifyDataSetChanged();
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(final SeekBar seekBar) {
                        // nothing to do
                    }

                    @Override
                    public void onProgressChanged(final SeekBar seekBar, final int progress,
                                                  final boolean fromUser) {
                        final int value = option.getMinValue() + progress;
                        retValue.mValue.setText(String.valueOf(value));
                    }
                }
        );
        retValue.mValueSlider.setMax(option.getMaxValue() - option.getMinValue());
        retValue.mValueSlider.setProgress(option.getValue(mContext));
        retValue.mValueSlider.setEnabled(enabled);
        // Update manually. If value is same as last time it's not triggered.
        retValue.mValue.setText(String.valueOf(retValue.mValueSlider.getProgress()));

        return retValue;
    }

    private View versionLoad(final AbstractOption.Version option,
                             final View convertView) {
        final View retValue = inflateView(convertView, R.layout.table_cell_version);

        // Load caption and description.
        ((TextView) retValue.findViewById(R.id.table_cell_version_title)).setText(option.getCaption());
        ((TextView) retValue.findViewById(R.id.table_cell_version_subtitle)).setText(option.getValue());

        return retValue;
    }

    private View segmentLoad(final AbstractOption.Segment option,
                             final View convertView,
                             final boolean enabled) {
        final View retValue = inflateView(convertView, R.layout.table_cell_segment);

        // Load caption and description.
        ((TextView) retValue.findViewById(R.id.table_cell_segment_title)).setText(option.getCaption());
        ((TextView) retValue.findViewById(R.id.table_cell_segment_subtitle)).setText(option.getDescription());

        // Create segment list. Original definition does not have context to read resources.
        // Ideally we can take SegmentKeyCaption directly as entry.
        final List<SegmentKeyCaption> segmentItems = new ArrayList<>();
        final String keyToFind = option.getValue(mContext);
        SegmentKeyCaption currentValue = null;
        for (final Map.Entry<String, String> loopEntry : option.getOptions().entrySet()) {
            final SegmentKeyCaption toAdd = new SegmentKeyCaption(loopEntry.getKey(), loopEntry.getValue());
            segmentItems.add(toAdd);

            // Current value.
            if (loopEntry.getKey().equals(keyToFind)) {
                currentValue = toAdd;
            }
        }

        // Prepare simple default adapter.
        final ArrayAdapter<SegmentKeyCaption> spinnerArrayAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, segmentItems);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Update spinner.
        final Spinner spinner = retValue.findViewById(R.id.table_cell_segment_spinner);
        spinner.setEnabled(enabled);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent,
                                       final View view,
                                       final int position,
                                       final long id) {
                // Update values only on real change.
                final String newValue = segmentItems.get(position).getKey();
                final String originalValue = option.getValue(mContext);
                if (!newValue.equals(originalValue)) {
                    if (!option.setValue(newValue, mContext)) {
                        for (final SegmentKeyCaption loopItem : segmentItems) {
                            // Current value.
                            if (loopItem.getKey().equals(originalValue)) {
                                spinner.setSelection(segmentItems.indexOf(loopItem));
                                return;
                            }
                        }
                    } else {
                        OptionAdapter.this.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // Ignore. It's not relevant for us.
            }
        });
        spinner.setSelection(segmentItems.indexOf(currentValue));

        return retValue;
    }

    private View buttonLoad(final AbstractOption.Button option,
                            final View convertView,
                            final boolean enabled) {
        final View retValue = inflateView(convertView, R.layout.table_cell_button);

        final TextView text = retValue.findViewById(R.id.table_cell_button_text);
        text.setText(option.getCaption());
        text.setEnabled(enabled);
        text.setOnClickListener(button -> option.getAction().doAction());

        return retValue;
    }

    //endregion
}