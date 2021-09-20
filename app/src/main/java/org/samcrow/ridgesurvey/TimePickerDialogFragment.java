package org.samcrow.ridgesurvey;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class TimePickerDialogFragment extends AppCompatDialogFragment {

    public interface TimePickedListener {
        void onTimePicked(@NonNull TimePickerDialogFragment fragment, @NonNull DateTime selectedDateTime);
    }

    @NonNull
    private final String title;

    @Nullable
    private TimePickedListener listener;

    public TimePickerDialogFragment(@NonNull String title) {
        this.title = title;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setTitle(title);
        dialog.setContentView(R.layout.dialog_time_picker);

        final TimePicker picker = dialog.findViewById(R.id.time_picker);

        final Button cancelButton = dialog.findViewById(R.id.time_picker_dialog_button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        final Button saveButton = dialog.findViewById(R.id.time_picker_dialog_button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final LocalTime selectedTime = new LocalTime(picker.getCurrentHour(), picker.getCurrentMinute());
                // Apply the date and offset from the current time
                final DateTime selectedDateTime = DateTime.now().withTime(selectedTime);
                if (listener != null) {
                    listener.onTimePicked(TimePickerDialogFragment.this, selectedDateTime);
                }
                dismiss();
            }
        });

        return dialog;
    }

    public void setOnTimePickedListener(@Nullable TimePickedListener listener) {
        this.listener = listener;
    }
}
