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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tracks a selected site
 */
public class SelectionManager {

    /**
     * The current selected site, or null if none is selected
     */
    @Nullable
    private Site mSelectedSite;

    /**
     * The route that contains the selected site, or null if no site is selected
     */
    @Nullable
    private Route mSelectedSiteRoute;

    /**
     * The listeners that will be notified when the selected site changes
     */
    @NonNull
    private final Set<SelectionListener> mListeners;

    public interface SelectionListener {
        /**
         * Called when the selected site is changed
         * @param newSelection the new selected site, which may be null
         * @param siteRoute the route that contains the new selected site, or null if newSelection
         *                  is null
         */
        void selectionChanged(@Nullable Site newSelection, @Nullable Route siteRoute);
    }

    /**
     * Creates a new SelectionManager with no site selected
     */
    public SelectionManager() {
        mSelectedSite = null;
        mSelectedSiteRoute = null;
        mListeners = new LinkedHashSet<>();
    }

    /**
     * Returns the currently selected site
     * @return
     */
    @Nullable
    public Site getSelectedSite() {
        return mSelectedSite;
    }

    /**
     * Returns the route that contains the currently selected site
     * @return
     */
    @Nullable
    public Route getSelectedSiteRoute() {
        return mSelectedSiteRoute;
    }

    /**
     * Sets the selected site
     * @param selectedSite the site to set
     * @param siteRoute the route that contains the site. Must be null if and only if selectedSite
     *                  is null.
     */
    public void setSelectedSite(@Nullable Site selectedSite, @Nullable Route siteRoute) {
        if ((selectedSite == null) ^ (siteRoute == null)) {
            throw new IllegalArgumentException("selectedSite and siteRoute must be both null or " +
                    "both non-null");
        }
        final boolean changed = selectedSite != mSelectedSite || siteRoute != mSelectedSiteRoute;
        mSelectedSite = selectedSite;
        mSelectedSiteRoute = siteRoute;
        if (changed) {
            for (SelectionListener listener : mListeners) {
                listener.selectionChanged(mSelectedSite, mSelectedSiteRoute);
            }
        }
    }

    /**
     * Adds a selection listener to be notified when the selection changes
     * @param listener
     */
    public void addSelectionListener(SelectionListener listener) {
        mListeners.add(listener);
        // Initialize the selection
        listener.selectionChanged(mSelectedSite, mSelectedSiteRoute);
    }
}
