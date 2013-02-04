package org.pointclouds.onirec;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SliderPreference extends DialogPreference {
    private final int max;
    private int value;
    private SeekBar seekBar;
    private TextView label;
    private static final int DEFAULT = 100;

    public SliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        max = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "max", 100);

        setDialogLayoutResource(R.layout.slider_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            value = seekBar.getProgress() + 1;
            persistInt(value);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        value = restorePersistedValue ? getPersistedInt(DEFAULT) : (Integer) defaultValue;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        seekBar = (SeekBar) view.findViewById(R.id.seekbar_value);

        seekBar.setMax(max - 1); // the actual value will be seekBar.getProgress() + 1
        label = (TextView) view.findViewById(R.id.text_label);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                label.setText(Integer.toString(progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        seekBar.setProgress(value - 1);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable super_state = super.onSaveInstanceState();

        if (isPersistent()) return super_state;

        final SavedState my_state = new SavedState(super_state);
        my_state.value = value;
        return my_state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState my_state = (SavedState) state;
        super.onRestoreInstanceState(my_state.getSuperState());

        value = my_state.value;
    }

    private static class SavedState extends BaseSavedState {
        private int value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
        }

        @SuppressWarnings("UnusedDeclaration")
        public static final Parcelable.Creator<SavedState> CREATOR =
            new Parcelable.Creator<SavedState>() {

                public SavedState createFromParcel(Parcel in) {
                    return new SavedState(in);
                }

                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
    }
}
