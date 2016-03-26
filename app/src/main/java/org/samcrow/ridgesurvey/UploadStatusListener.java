package org.samcrow.ridgesurvey;

/**
 * An interface for objects that can respond to changes in the upload state
 */
public interface UploadStatusListener {

    /**
     * Possible upload statuses
     */
    enum UploadState {
        /**
         * All observations have been uploaded
         */
        Ok,
        /**
         * One or more observations still needs to be uploaded
         */
        NeedsUpload,
        /**
         * An upload is in progress
         */
        Uploading,
    }

    /**
     * Sets the status to display
     * @param state the status, which must not be null
     */
    void setState(UploadState state);
}
