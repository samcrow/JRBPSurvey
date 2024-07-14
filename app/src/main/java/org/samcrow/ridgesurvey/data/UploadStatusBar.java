package org.samcrow.ridgesurvey.data;

import android.content.Context;
import androidx.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.View;

import org.samcrow.ridgesurvey.Objects;
import org.samcrow.ridgesurvey.R;

/**
 * Displays the status of uploads, and allows them to be triggered
 */
public class UploadStatusBar extends View implements UploadStatusListener {

    /**
     * The color that indicates that all observations have been uploaded
     */
    @ColorInt
    private int COLOR_OK;

    public UploadStatusBar(Context context) {
        super(context);
        init();
    }

    public UploadStatusBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UploadStatusBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        COLOR_OK = getContext().getResources().getColor(R.color.status_bar_ok);
        setBackgroundColor(COLOR_OK);
    }

    @Override
    public void setState(UploadState state) {
        Objects.requireNonNull(state);
        switch (state) {
            case Ok:
                setBackgroundColor(COLOR_OK);
                break;
            case Uploading:
                setBackgroundResource(R.drawable.in_progress_background);
                break;
            case NeedsUpload:
                setBackgroundResource(R.drawable.needs_upload_background);
                break;
        }
    }
}
