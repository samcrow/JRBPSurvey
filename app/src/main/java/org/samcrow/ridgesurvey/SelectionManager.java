package org.samcrow.ridgesurvey;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
     * The listeners that will be notified when the selected site changes
     */
    @NonNull
    private final Set<SelectionListener> mListeners;

    public interface SelectionListener {
        /**
         * Called when the selected site is changed
         * @param newSelection the new selected site, which may be null
         */
        void selectionChanged(@Nullable Site newSelection);
    }

    /**
     * Creates a new SelectionManager with no site selected
     */
    public SelectionManager() {
        mSelectedSite = null;
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
     * Sets the selected site
     * @param selectedSite the site to set
     */
    public void setSelectedSite(@Nullable Site selectedSite) {
        final boolean changed = selectedSite != mSelectedSite;
        mSelectedSite = selectedSite;
        if (changed) {
            for (SelectionListener listener : mListeners) {
                listener.selectionChanged(mSelectedSite);
            }
        }
    }

    /**
     * Adds a selection listener to be notified when the selection changes
     * @param listener
     */
    public void addSelectionListener(SelectionListener listener) {
        mListeners.add(listener);
    }
}
