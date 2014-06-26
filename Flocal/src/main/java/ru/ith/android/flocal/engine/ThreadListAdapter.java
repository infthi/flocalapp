package ru.ith.android.flocal.engine;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.commonsware.cwac.endless.EndlessAdapter;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import ru.ith.android.flocal.R;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.data.FLBoard;
import ru.ith.lib.flocal.data.FLThreadHeader;

/**
 * Created by infthi on 6/25/13.
 */
public class ThreadListAdapter extends EndlessAdapter  {
    private final FLBoard board;
    private final ArrayAdapter<FLThreadHeader> data;
    private Activity ctxt;

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
                CharSequence threadText = item.getName();
				if (item.getUnreadID()>=0){
                    SpannableString spanString = new SpannableString(threadText);
                    spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
                    threadText = spanString;
                }
                ((TextView)row.findViewById(R.id.threadEntryText)).setText(threadText);
                return row;

            }
        });
        this.ctxt = ctxt;
        data = (ArrayAdapter) getWrappedAdapter();
        this.board = board;
        knownThreads.clear();
    }

    Set<FLThreadHeader> knownThreads = new HashSet<FLThreadHeader>();
    int currentPage = 0;

    private LinkedList<FLThreadHeader> threads = new LinkedList<FLThreadHeader>();
    @Override
    protected boolean cacheInBackground() throws Exception {
        LinkedList<FLThreadHeader> nextPage = FLDataLoader.listThreads(SessionContainer.getSessionInstance(), board, currentPage++);
        synchronized (threads){
            threads.addAll(nextPage);
        }
        return !nextPage.isEmpty();
    }

    @Override
    protected void appendCachedData() {
        synchronized (threads){
            for (FLThreadHeader thread: threads){
                if (knownThreads.add(thread)){
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

    public void applyUpdate(final LinkedList<FLThreadHeader> firstPage) {
        this.ctxt.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int counter = 0;
                for (FLThreadHeader thread: firstPage){
                    if (knownThreads.remove(thread))
                        data.remove(thread);
                    knownThreads.add(thread);
                    data.insert(thread, counter++);
                }
                notifyDataSetChanged();
            }
        });
    }
}


class ThreadListUpdater extends TimerTask{

    private final FLBoard board;
    private ThreadListAdapter target;

    ThreadListUpdater(FLBoard board, ThreadListAdapter target) {
        this.board = board;
        this.target = target;
    }

    @Override
    public void run() {
        try {
            LinkedList<FLThreadHeader> firstPage = FLDataLoader.listThreads(SessionContainer.getSessionInstance(), board, 0);
            target.applyUpdate(firstPage);
        } catch (FLException e) {
        }
    }
}