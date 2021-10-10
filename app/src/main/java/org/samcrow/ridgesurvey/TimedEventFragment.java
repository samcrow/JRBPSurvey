package org.samcrow.ridgesurvey;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.samcrow.ridgesurvey.data.Database;
import org.samcrow.ridgesurvey.data.SimpleTimedEvent;
import org.samcrow.ridgesurvey.data.SimpleTimedEventDao;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

/**
 * A fragment representing a list of Items.
 */
public class TimedEventFragment extends DialogFragment {

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TimedEventFragment() {
    }

    @SuppressWarnings("unused")
    public static TimedEventFragment newInstance() {
        return new TimedEventFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.setTitle("Events");

        dialog.setContentView(R.layout.fragment_item_list);
        final RecyclerView recyclerView = dialog.findViewById(R.id.list);
        Context context = recyclerView.getContext();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // Load all timed events from the database
        final Database db = Room.databaseBuilder(context, Database.class, "events")
                .allowMainThreadQueries()
                .build();
        try {
            final SimpleTimedEventDao dao = db.simpleTimedEventDao();
            final List<SimpleTimedEvent> events = dao.getAll();

            recyclerView.setAdapter(new MyTimedEventRecyclerViewAdapter(events));
        } finally {
            db.close();
        }

        return dialog;
    }
}