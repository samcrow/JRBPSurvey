package org.samcrow.ridgesurvey;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.samcrow.ridgesurvey.data.SimpleTimedEvent;
import org.samcrow.ridgesurvey.databinding.FragmentItemBinding;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link org.samcrow.ridgesurvey.data.SimpleTimedEvent}.
 */
public class MyTimedEventRecyclerViewAdapter extends RecyclerView.Adapter<MyTimedEventRecyclerViewAdapter.ViewHolder> {

    private final List<SimpleTimedEvent> mEvents;

    public MyTimedEventRecyclerViewAdapter(List<SimpleTimedEvent> items) {
        mEvents = items;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final SimpleTimedEvent event = mEvents.get(position);
        holder.mItem = event;
        holder.mIdView.setText(Integer.toString(mEvents.get(position).getId()));

        final String timeString = DateTimeFormat.shortTime().print(event.getTime());
        holder.mContentView.setText(String.format("%s at %s", event.getName(), timeString));
    }

    @Override
    public int getItemCount() {
        return mEvents.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public SimpleTimedEvent mItem;

        public ViewHolder(FragmentItemBinding binding) {
            super(binding.getRoot());
            mIdView = binding.itemNumber;
            mContentView = binding.content;
        }

        @Override
        @NonNull
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}