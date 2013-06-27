package ru.ith.android.flocal.engine;

import android.app.Activity;
import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.endless.EndlessAdapter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ArrayAdapter<FLMessageWrapper> data;
    private final Activity ctxt;
	private final ListView target;

    public PostListAdapter(FLThreadHeader thread, final Activity ctxt, ListView target) {
        super(new ArrayAdapter<FLMessageWrapper>(ctxt, R.layout.thread_entry){
			Map<Long, View> cachedMessageViews = new WeakHashMap<Long, View>();
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
				FLMessageWrapper item = getItem(position);
				if (item.isLoadingStub)
					return getPendingViewImpl(parent);
				View result = cachedMessageViews.get(item.message.getID());
				if (result==null){
					LayoutInflater inflater = ctxt.getLayoutInflater();
					result = inflater.inflate(R.layout.post_entry, parent, false);
					((TextView)result.findViewById(R.id.postEntryText)).setText(Html.fromHtml(item.message.getPostData()));
					cachedMessageViews.put(item.message.getID(), result);
				}
                return result;
            }
        });
		this.target = target;
        this.ctxt = ctxt;
        this.thread = thread;
        this.firstKnownPost = thread.getUnreadID();
        data = (ArrayAdapter) getWrappedAdapter();
    }

    private long firstKnownPost = -1l, lastKnownPost = -1l;
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
                if (post.getID()>lastKnownPost){
                    data.add(new FLMessageWrapper(post));
                    lastKnownPost = post.getID();
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
            data.insert(FLMessageWrapper.placeholder, 0);
			cacheInBackgroundUpper();
        }
    }

	protected boolean cacheInBackgroundUpper() {
		if (!running.get())
			return true;
		new AsyncTask<Void, Void, FLMessageSet>(){

			@Override
			protected FLMessageSet doInBackground(Void... params) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				return null;
			}

			@Override
			protected void onPostExecute(FLMessageSet flMessageSet) {
				if (drawnUpPlaceholder.getAndSet(false)){
					data.remove(FLMessageWrapper.placeholder);
					for (int i = 0; i< 10; i++){
						data.insert(new FLMessageWrapper(new FLMessage("user", "upper message "+i, "cap", "ololo", 0, container.incrementAndGet())), 0);
					}
					FLMessageWrapper test = data.getItem(0);
					int index = target.getFirstVisiblePosition();
					View v = target.getChildAt(1);
					int top = (v == null) ? 0 : v.getTop();

					notifyDataSetChanged();
					target.setSelectionFromTop(index+10, top);
				}
			}
		}.execute();
		return false;
	}
	private  static final AtomicInteger container = new AtomicInteger(0);
}


class FLMessageWrapper{

	public static final FLMessageWrapper placeholder = new FLMessageWrapper();
	public final FLMessage message;
	public final boolean isLoadingStub;

	public FLMessageWrapper(FLMessage wrapped){
		this.message = wrapped;
		isLoadingStub = false;
	}

	public  FLMessageWrapper(){
		isLoadingStub = true;
		message = null;
	}

}
