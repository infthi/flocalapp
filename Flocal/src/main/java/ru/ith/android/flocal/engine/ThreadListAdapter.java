package ru.ith.android.flocal.engine;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.commonsware.cwac.endless.EndlessAdapter;

import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import ru.ith.android.flocal.R;
import ru.ith.lib.flocal.data.FLBoard;
import ru.ith.lib.flocal.data.FLThreadHeader;

/**
 * Created by infthi on 6/25/13.
 */
public class ThreadListAdapter extends EndlessAdapter  {
    private final FLBoard board;
    private final ArrayAdapter data;

    public ThreadListAdapter(FLBoard board, final Activity ctxt) {
        super(new ArrayAdapter<FLThreadHeader>(ctxt, R.layout.thread_entry){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;
                if(row == null)
                {
                    LayoutInflater inflater = ctxt.getLayoutInflater();
                    row = inflater.inflate(R.layout.thread_entry, parent, false);
                }
                FLThreadHeader item = getItem(position);
                ((TextView)row.findViewById(R.id.threadEntryText)).setText(item.toString());
                return row;

            }
        });
        data = (ArrayAdapter) getWrappedAdapter();
        this.board = board;
        knownThreads.clear();

    }

    Set<Long> knownThreads = new TreeSet<Long>();
    int currentPage = 0;

    private LinkedList<FLThreadHeader> threads = new LinkedList<FLThreadHeader>();
    @Override
    protected boolean cacheInBackground() throws Exception {
        synchronized (threads){
            threads = SessionContainer.getInstance().getSession().listThreads(board, currentPage++);
        }
        return true;
    }

    @Override
    protected void appendCachedData() {
        synchronized (threads){
            for (FLThreadHeader thread: threads){
                if (knownThreads.add(thread.getID())){
                    data.add(thread);
                }
            }
            threads.clear();
        }
    }

    @Override
    protected View getPendingView(ViewGroup parent) {
        TextView result = new TextView(parent.getContext());
        result.setText("Loading...");
        return result;
    }
}
