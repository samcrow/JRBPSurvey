/*
 * Copyright 2016 Sam Crow
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

package org.samcrow.ridgesurvey;

import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Displays a species name check box and an optional image.
 * <p/>
 * If an image is present, clicking on it will open a window to display a larger version of
 * the image.
 */
public class SpeciesView extends LinearLayout {

    /**
     * The context
     */
    @NonNull
    private Context mContext;

    /**
     * The species being displayed
     */
    @NonNull
    private Species mSpecies;

    /**
     * The check box that the user can use to select the species
     */
    @NonNull
    private CheckBox mCheckBox;

    public SpeciesView(@NonNull Context context, @NonNull Species species) {
        super(context);
        setOrientation(HORIZONTAL);

        Objects.requireNonNull(context);
        Objects.requireNonNull(species);
        mContext = context;
        mSpecies = species;

        // Inflate the large checkbox and add it to the layout
        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.large_checkbox, this, true);
        mCheckBox = (CheckBox) findViewById(R.id.species_check_box);

        final TextView textView = new TextView(mContext);
        textView.setTextAppearance(mContext, android.R.style.TextAppearance_Large);
        textView.setText(mSpecies.getName());

        // Clicking on the species name is equivalent to clicking on the check box
        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCheckBox.performClick();
            }
        });

        final LayoutParams nameParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        nameParams.weight = 0.3f;
        nameParams.gravity = Gravity.CENTER_VERTICAL;
        addView(textView, nameParams);

        // Optional ImageButton
        @DrawableRes
        final int speciesImage = species.getImage();
        if (speciesImage != 0) {
            final ImageButton imageButton = new ImageButton(context);
            imageButton.setImageDrawable(
                    mContext.getResources().getDrawable(R.drawable.ic_image_black_18dp));

            imageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ImageView imageView = new ImageView(mContext);
                    imageView.setImageResource(mSpecies.getImage());
                    imageView.setAdjustViewBounds(true);

                    new AlertDialog.Builder(mContext)
                            .setView(imageView)
                            .show();
                }
            });

            final LayoutParams imageParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            imageParams.weight = 0.7f;
            imageParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
            addView(imageButton, imageParams);
        }
    }

    /**
     * Returns the species associated with this view
     *
     * @return the species
     */
    @NonNull
    public Species getSpecies() {
        return mSpecies;
    }

    /**
     * Returns true if the user has checked the checkbox
     *
     * @return true if the box is checked
     */
    public boolean isChecked() {
        return mCheckBox.isChecked();
    }

    public void setChecked(boolean checked) {
        mCheckBox.setChecked(checked);
    }

    /**
     * Sets the listener to be notified when the check box is checked or unchecked
     * @param listener the listener
     */
    public void setOnCheckedChangeListener(
            OnCheckedChangeListener listener) {
        mCheckBox.setOnCheckedChangeListener(listener);
    }
}
