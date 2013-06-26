package ru.ith.android.flocal.engine;

import android.app.Activity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.endless.EndlessAdapter;

import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.ith.android.flocal.R;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.data.FLMessage;
import ru.ith.lib.flocal.data.FLMessageSet;
import ru.ith.lib.flocal.data.FLThreadHeader;

/**
 * Created by infthi on 6/25/13.
 */
public class PostListAdapter extends EndlessAdapter  {
    private final FLThreadHeader thread;
    private final ArrayAdapter<FLMessage> data;
    private final Activity ctxt;

    public PostListAdapter(FLThreadHeader thread, final Activity ctxt) {
        super(new ArrayAdapter<FLMessage>(ctxt, R.layout.thread_entry){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;
                if(row == null)
                {
                    FLMessage item = getItem(position);
                    if (item==null)
                        return getPendingViewImpl(parent);
                    LayoutInflater inflater = ctxt.getLayoutInflater();
                    row = inflater.inflate(R.layout.post_entry, parent, false);
                    ((TextView)row.findViewById(R.id.postEntryText)).setText(Html.fromHtml(item.getPostData()));
                }

                return row;

            }
        });
        this.ctxt = ctxt;
        this.thread = thread;
        data = (ArrayAdapter) getWrappedAdapter();
        knownPosts.clear();

    }

    Set<Long> knownPosts = new TreeSet<Long>();
    private AtomicBoolean scrolledToEnd = new AtomicBoolean(false);
    public Thread checkerThread = null;

    private FLMessage lastLoadedPost = null;
    int lastLoadedPostKnownOffset = -1;

    private LinkedList<FLMessage> posts = new LinkedList<FLMessage>();
    @Override
    protected boolean cacheInBackground() throws Exception {
        if (!running.get())
            return true;

        boolean success = false;

        FLMessageSet gotPosts = null;
        while (!success){
            gotPosts = FLDataLoader.listMessages(SessionContainer.getSessionInstance(), thread, lastLoadedPostKnownOffset);
            if ((lastLoadedPostKnownOffset<=0)||(gotPosts.getEffectiveOffset()>0)){ //if effective offset is 0 then we requested too large lastKnownOffset.
                if (lastLoadedPost == null) {
                    success = true;
                } else {
                    LinkedList<FLMessage> posts = gotPosts.getPosts();
                    remover:
                    while (!posts.isEmpty())
                        if (posts.removeFirst().getID() == lastLoadedPost.getID()) {
                            success = true;
                            break remover;
                        } else {
                            lastLoadedPostKnownOffset++;
                        }
                }
                if (!success)
                    lastLoadedPostKnownOffset = gotPosts.getEffectiveOffset()-10;
            } else
                lastLoadedPostKnownOffset-=10;

        }

        synchronized (posts){
            if (posts.addAll(gotPosts.getPosts()))
                if (scrolledToEnd.getAndSet(false))
                    ctxt.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ctxt, "New posts appeared: "+posts.size(), Toast.LENGTH_LONG).show();//TODO: move to resource
                        }
                    });
        }

        if (!gotPosts.getPosts().isEmpty()){
            lastLoadedPost = gotPosts.getPosts().getLast();
            lastLoadedPostKnownOffset = lastLoadedPostKnownOffset+gotPosts.getPosts().size();
        }

        if (gotPosts.hasMoreData()){
            return true;
        }

        scrolledToEnd.set(true);

        checkerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    return;
                }
                ctxt.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        restartAppending();
                    }
                });
            }
        });
        checkerThread.start();
        return false;
    }

    @Override
    protected void appendCachedData() {
        synchronized (posts){
            for (FLMessage post: posts){
                if (knownPosts.add(post.getID())){
                    data.add(post);
                }
            }
            posts.clear();
        }
    }

    @Override
    protected View getPendingView(ViewGroup parent) {
        return  getPendingViewImpl(parent);
    }
    protected static View getPendingViewImpl(ViewGroup parent) {
        TextView result = new TextView(parent.getContext());
        result.setText("Loading...");
        return result;
    }

    private AtomicBoolean running = new AtomicBoolean(true);

    public void setRunning(final boolean isRunning) {
        running.set(isRunning);
    }

    private AtomicBoolean drawnUpPlaceholder = new AtomicBoolean(false);

    public void upOverScroll() {
        if (!drawnUpPlaceholder.getAndSet(true)){
            data.insert(null, 0);
        }
    }
}
