/*
 * Copyright 2017 Sam Crow
 *
 * This file is part of JRBP Survey.
 *
 * JRBP Survey is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JRBP Survey is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRBP Survey.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey.data;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.samcrow.ridgesurvey.ObservationEditActivity;
import org.samcrow.ridgesurvey.R;

import java.util.Locale;

/**
 * Displays an observation in a list
 */
public class ObservationItemView extends LinearLayout {

    private final int PADDING = 30;

    /**
     * The observation to display
     */
    @Nullable
    private IdentifiedObservation mObservation;

    /**
     * The primary text view
     */
    private TextView mPrimaryText;

    /**
     * The secondary text view
     */
    private TextView mSecondaryText;

    /**
     * The uploaded icon
     */
    private ImageView mIcon;

    public ObservationItemView(Context context) {
        super(context);
        init();
    }

    public ObservationItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ObservationItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);

        mPrimaryText = new TextView(getContext());
        mPrimaryText.setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
        final LayoutParams primaryParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        primaryParams.setMargins(PADDING, PADDING, PADDING, PADDING);
        primaryParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;

        mSecondaryText = new TextView(getContext());
        mSecondaryText.setTextAppearance(getContext(), android.R.style.TextAppearance_Medium);
        final LayoutParams secondaryParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        secondaryParams.setMargins(PADDING, PADDING, PADDING, PADDING);
        secondaryParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;

        mIcon = new ImageView(getContext());
        mIcon.setImageResource(R.drawable.ic_cloud_done_black_24dp);
        final LayoutParams iconParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        iconParams.setMargins(PADDING, PADDING, PADDING, PADDING);
        iconParams.weight = 0.2f;
        iconParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;

        addView(mPrimaryText, primaryParams);
        addView(mSecondaryText, secondaryParams);
        addView(mIcon, iconParams);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditActivity();
            }
        });
    }

    public void setObservation(@Nullable IdentifiedObservation observation) {
        mObservation = observation;
        if (mObservation != null) {
            mPrimaryText.setText(String.format(Locale.getDefault(), "Site %d", mObservation.getSiteId()));

            final Period timeSinceUpload = new Period(mObservation.getTime(), DateTime.now());

            mSecondaryText.setText(formatAgo(timeSinceUpload));

            if (mObservation.isUploaded()) {
                mIcon.setVisibility(VISIBLE);
            } else {
                mIcon.setVisibility(GONE);
            }
        }
    }

    /**
     * Formats a duration with a human-friendly form of [] minutes/hours... ago
     * @param duration the duration to format
     * @return a representation of the duration
     */
    private static String formatAgo(Period duration) {
        if (duration.getDays() > 1) {
            return String.format(Locale.getDefault(), "%d days ago", duration.getDays());
        }
        if (duration.getHours() > 1) {
            return String.format(Locale.getDefault(), "%d hours ago", duration.getHours());
        }
        if (duration.getMinutes() > 1) {
            return String.format(Locale.getDefault(), "%d minutes ago", duration.getMinutes());
        } else {
            return "Just now";
        }
    }

    /**
     * Opens an activity to edit the observation
     */
    private void openEditActivity() {
        final Intent intent = new Intent(getContext(), ObservationEditActivity.class);
        intent.putExtra(ObservationEditActivity.EXTRA_OBSERVATION, mObservation);
        getContext().startActivity(intent);
    }
}
