package org.samcrow.ridgesurvey;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.util.AttributeSet;
import android.view.View;

/**
 * Displays the status of uploads, and allows them to be triggered
 */
public class UploadStatusBar extends View implements UploadStatusListener {

    /**
     * The color that indicates that all observations have been uploaded
     */
    @ColorInt
    private int COLOR_OK;

    /**
     * The color that indicates that an observation needs to be uploaded
     */
    @ColorInt
    private int COLOR_NEEDS_UPLOAD;
    /**
     * The color that indicates that an upload is in progress
     */
    @ColorInt
    private int COLOR_IN_PROGRESS;

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
        COLOR_NEEDS_UPLOAD = getContext().getResources().getColor(R.color.status_bar_need_upload);
        COLOR_IN_PROGRESS = getContext().getResources().getColor(R.color.status_bar_in_progress);
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
                setBackgroundColor(COLOR_IN_PROGRESS);
                break;
            case NeedsUpload:
                setBackgroundColor(COLOR_NEEDS_UPLOAD);
                break;
        }
    }
}
