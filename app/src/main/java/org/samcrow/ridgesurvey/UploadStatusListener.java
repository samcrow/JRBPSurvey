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
