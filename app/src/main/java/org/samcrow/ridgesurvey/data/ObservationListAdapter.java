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
