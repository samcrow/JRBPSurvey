/*
 * Copyright (c) 2025 Sam Crow
 *
 * This file is part of JRBPSurvey.
 *
 * JRBPSurvey is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * JRBPSurvey is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar.
 * If not, see <https://www.gnu.org/licenses/>.
 */

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
