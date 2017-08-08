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

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * A model for an observation list
 */
public class ObservationListAdapter extends BaseAdapter {

    /**
     * The observations
     */
    private final List<IdentifiedObservation> mObservations;

    public ObservationListAdapter(
            List<IdentifiedObservation> observations) {
        mObservations = observations;
    }

    @Override
    public int getCount() {
        return mObservations.size();
    }

    @Override
    public Object getItem(int position) {
        return mObservations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mObservations.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final IdentifiedObservation observation = mObservations.get(position);
        if (observation == null) {
            return null;
        }
        ObservationItemView view;
        if (convertView != null) {
            view = (ObservationItemView) convertView;
        } else {
            view = new ObservationItemView(parent.getContext());
        }
        view.setObservation(observation);

        return view;
    }
}
