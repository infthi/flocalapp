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
                    LayoutInflater inflater = ctxt.getLayoutInflater();
                    row = inflater.inflate(R.layout.post_entry, parent, false);
                }
                FLMessage item = getItem(position);
                ((TextView)row.findViewById(R.id.postEntryText)).setText(Html.fromHtml(item.getPostData()));
                return row;

            }
        });
        this.ctxt = ctxt;
        data = (ArrayAdapter) getWrappedAdapter();
        this.thread = thread;
        knownPosts.clear();

    }

    Set<Long> knownPosts = new TreeSet<Long>();
    int currentPost = 0;
    private AtomicBoolean scrolledToEnd = new AtomicBoolean(false);

    private LinkedList<FLMessage> posts = new LinkedList<FLMessage>();
    @Override
    protected boolean cacheInBackground() throws Exception {
        FLMessageSet gotPosts = SessionContainer.getInstance().getSession().listMessages(thread, currentPost);
        synchronized (posts){
            posts = gotPosts.getPosts();
            if (!posts.isEmpty())
                if (scrolledToEnd.getAndSet(false))
                    ctxt.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ctxt, "New posts appeared", Toast.LENGTH_LONG).show();//TODO: move to resource
                        }
                    });
        }

        currentPost=gotPosts.getOffset()+gotPosts.getPosts().size();

        if (gotPosts.hasMoreData()){
            return true;
        }

        scrolledToEnd.set(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
                ctxt.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        restartAppending();
                        buildTask().execute();
                    }
                });
            }
        }).start();
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
        TextView result = new TextView(parent.getContext());
        result.setText("Loading...");
        return result;
    }
}
