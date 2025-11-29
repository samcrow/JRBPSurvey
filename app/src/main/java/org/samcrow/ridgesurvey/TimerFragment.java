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


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A fragment that displays a timer
 */
public class TimerFragment extends Fragment {

    private static final int NOTIFICATION_HALF_PERIOD = 3;
    private static final int NOTIFICATION_FULL_PERIOD = 4;
    private static final String CHANNEL_HALF = "timer_half";
    private static final String CHANNEL_FULL = "timer_full";

    /**
     * The duration to count up to
     */
    private static final Duration COUNT_UP_PERIOD = Duration.standardMinutes(5);

    /**
     * Half of the count up period
     */
    private static final Duration HALF_PERIOD = COUNT_UP_PERIOD.dividedBy(2);

    private static final String TAG = TimerFragment.class.getSimpleName();

    @DrawableRes
    private static final int ICON_START = R.drawable.ic_play_arrow_black_36dp;

    @DrawableRes
    private static final int ICON_STOP = R.drawable.ic_stop_black_36dp;

    /**
     * The timer
     */
    private Timer mTimer;

    /**
     * If the timer is running
     */
    private volatile boolean mRunning;

    /**
     * The current displayed timer duration
     */
    private volatile Duration mCurrentDuration;

    /**
     * The formatter used to format periods
     */
    private final PeriodFormatter mFormatter;

    private NotificationManagerCompat mNotificationManager;
    private ActivityResultLauncher<String> mPermissionLauncher;

    TextView mTimeView;

    ImageButton mStartStopButton;

    public TimerFragment() {
        mRunning = false;
        mFormatter = new PeriodFormatterBuilder()
                .printZeroAlways()
                .minimumPrintedDigits(1)
                .appendMinutes()
                .appendSeparator(":")
                .minimumPrintedDigits(2)
                .appendSeconds()
                .toFormatter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNotificationManager = NotificationManagerCompat.from(requireContext());
        final AudioAttributes audioAttributes =
                new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build();
        final NotificationChannelCompat halfChannel = new NotificationChannelCompat.Builder(
                CHANNEL_HALF,
                NotificationManagerCompat.IMPORTANCE_HIGH).setName(requireContext().getString(R.string.app_name))
                .setSound(getSingleSound(), audioAttributes)
                .build();
        mNotificationManager.createNotificationChannel(halfChannel);
        final NotificationChannelCompat fullChannel = new NotificationChannelCompat.Builder(
                CHANNEL_FULL,
                NotificationManagerCompat.IMPORTANCE_HIGH).setName(requireContext().getString(R.string.app_name))
                .setSound(getDoubleSound(), audioAttributes)
                .build();
        mNotificationManager.createNotificationChannel(fullChannel);

        mPermissionLauncher = registerForActivityResult(new RequestPermission(), granted -> {
            if (granted != Boolean.TRUE) {
                new AlertDialog.Builder(requireContext()).setTitle(R.string.notification_permission_denied_title)
                        .setMessage(R.string.notification_permission_denied_message)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mNotificationManager.cancel(NOTIFICATION_HALF_PERIOD);
        mNotificationManager.cancel(NOTIFICATION_FULL_PERIOD);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View root = inflater.inflate(R.layout.fragment_timer, container, false);

        mTimeView = (TextView) root.findViewById(R.id.time_view);

        mStartStopButton = (ImageButton) root.findViewById(R.id.start_stop_button);
        mStartStopButton.setImageResource(ICON_START);
        mStartStopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRunning) {
                    stopTimer();
                } else {
                    startTimer();
                }
            }
        });

        return root;
    }

    private void startTimer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            mPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (mRunning) {
            return;
        }
        showTime(Duration.ZERO);
        mStartStopButton.setImageResource(ICON_STOP);
        mRunning = true;
        mTimer = new Timer();
        mNotificationManager.cancel(NOTIFICATION_HALF_PERIOD);
        mNotificationManager.cancel(NOTIFICATION_FULL_PERIOD);

        mCurrentDuration = Duration.ZERO;
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final Activity activity = getActivity();
                if (activity == null) {
                    Log.w(TAG, "Activity gone, canceling timer");
                    mTimer.cancel();
                    return;
                }
                final Duration newDuration = mCurrentDuration.plus(Duration.standardSeconds(1));
                mCurrentDuration = newDuration;

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showTime(newDuration);
                    }
                });

                if (newDuration.isEqual(HALF_PERIOD)) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyHalfPeriod();
                        }
                    });
                }

                if (newDuration.isEqual(COUNT_UP_PERIOD) ||
                        newDuration.isLongerThan(COUNT_UP_PERIOD)) {
                    mTimer.cancel();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyStopped();
                            stopTimer();
                        }
                    });
                }
            }
        }, 1000, 1000);
    }

    private void stopTimer() {
        if (mRunning) {
            mStartStopButton.setImageResource(ICON_START);
            mRunning = false;
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void showTime(ReadableDuration period) {
        mTimeView.setText(mFormatter.print(period.toPeriod()));
    }

    private void notifyHalfPeriod() {
        final Notification notification = new Builder(requireContext(),
                CHANNEL_HALF).setContentTitle(getString(R.string.title_half_elapsed,
                        mFormatter.print(HALF_PERIOD.toPeriod())))
                .setContentText(getString(R.string.content_half_elapsed))
                .setSmallIcon(R.drawable.ic_timer_white_18dp)
                .setAutoCancel(true)
                .setSound(getSingleSound())
                .build();
        try {
            mNotificationManager.notify(NOTIFICATION_HALF_PERIOD, notification);
        } catch (SecurityException e) {
            Log.w(TAG, "Missing permission to send notification", e);
        }
    }

    private void notifyStopped() {
        final Notification notification = new Builder(requireContext(),
                CHANNEL_FULL).setContentTitle(getString(R.string.title_full_elapsed,
                        mFormatter.print(COUNT_UP_PERIOD.toPeriod())))
                .setContentText(getString(R.string.content_full_elapsed))
                .setSmallIcon(R.drawable.ic_timer_white_18dp)
                .setAutoCancel(true)
                .setSound(getDoubleSound())
                .build();
        mNotificationManager.cancel(NOTIFICATION_HALF_PERIOD);
        try {
            mNotificationManager.notify(NOTIFICATION_FULL_PERIOD, notification);
        } catch (SecurityException e) {
            Log.w(TAG, "Missing permission to send notification", e);
        }
    }

    private @NonNull Uri getSingleSound() {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                requireContext().getPackageName() + "/raw/sound_notification_single");
    }

    private @NonNull Uri getDoubleSound() {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                requireContext().getPackageName() + "/raw/sound_notification_double");
    }
}
