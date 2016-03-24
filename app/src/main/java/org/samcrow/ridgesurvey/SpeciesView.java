package org.samcrow.ridgesurvey;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Displays a species name check box and an optional image.
 * <p/>
 * If an image is present, clicking on it will open a window to display a larger version of
 * the image.
 */
public class SpeciesView extends LinearLayout {

    /**
     * Maximum size of the small image view, in pixel-like units
     */
    private static final int MAX_IMAGE_SIZE = 160;

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
        mCheckBox = new CheckBox(context);
        mCheckBox.setText(mSpecies.getName());

        final LayoutParams checkBoxParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        checkBoxParams.weight = 0.2f;
        checkBoxParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        addView(mCheckBox, checkBoxParams);

        // Optional ImageButton
        final Drawable speciesImage = species.getImage();
        if (speciesImage != null) {
            final ImageButton imageButton = new ImageButton(context);
            imageButton.setImageDrawable(speciesImage);
            imageButton.setMaxWidth(MAX_IMAGE_SIZE);
            imageButton.setMaxHeight(MAX_IMAGE_SIZE);
            imageButton.setAdjustViewBounds(true);

            imageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ImageView imageView = new ImageView(mContext);
                    imageView.setImageDrawable(mSpecies.getImage());
                    imageView.setAdjustViewBounds(true);

                    new AlertDialog.Builder(mContext)
                            .setView(imageView)
                            .show();
                }
            });

            final LayoutParams imageParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            imageParams.weight = 0.8f;
            imageParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
            addView(imageButton, imageParams);
        }
    }

    /**
     * Returns the species associated with this view
     * @return the species
     */
    public Species getSpecies() {
        return mSpecies;
    }

    /**
     * Returns true if the user has checked the checkbox
     * @return
     */
    public boolean isChecked() {
        return mCheckBox.isChecked();
    }
}
