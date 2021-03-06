package ru.ith.flocal.engine;

import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.endless.EndlessAdapter;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ru.ith.flocal.R;
import ru.ith.flocal.activities.PostListActivity;
import ru.ith.flocal.io.ImageFactory;
import ru.ith.flocal.views.PostView;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.FLException;
import ru.ith.lib.flocal.data.FLMessage;
import ru.ith.lib.flocal.data.FLMessageSet;
import ru.ith.lib.flocal.data.FLThreadHeader;
import ru.ith.lib.flocal.data.FLThreadPageSet;

/**
 * Created by infthi on 6/25/13.
 */
public class PostListAdapter extends EndlessAdapter {
	private static final AtomicInteger container = new AtomicInteger(0);
	private final FLThreadHeader thread;
	private final ArrayAdapter<FLMessageWrapper> data;
	private final PostListActivity ctxt;
	private final ListView target;
	private final LinkedList<FLMessage> newPosts = new LinkedList<FLMessage>();
	private final LinkedList<FLMessage> earlierPosts = new LinkedList<FLMessage>();
	int lastLoadedPostKnownOffset = -1;
	int firstLoadedPostKnownOffset = -1;
	private long firstKnownPost = -1l, lastKnownPost = -1l;
	private AtomicBoolean scrolledToEnd = new AtomicBoolean(false);
	private AtomicBoolean drawnUpPlaceholder = new AtomicBoolean(false);


	public PostListAdapter(FLThreadHeader thread, final PostListActivity ctxt, final ListView target, final ImageFactory imageGetter) {
		super(new ArrayAdapter<FLMessageWrapper>(ctxt, R.layout.thread_entry) {
			Map<Long, Reference<PostView>> cachedMessageViews = new HashMap<Long, Reference<PostView>>();

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				FLMessageWrapper item = getItem(position);
				if (item.isLoadingStub)
					return getPendingViewImpl(parent);
				Reference<PostView> resultRef = cachedMessageViews.get(item.message.getID());
				PostView result = null;
				if (resultRef != null)
					result = resultRef.get();
				if (result == null) {
					result = (PostView) ctxt.getLayoutInflater().inflate(R.layout.post_entry, null, false);
					result.setMessage(item.message);
					cachedMessageViews.put(item.message.getID(), new WeakReference<PostView>(result));
				}
				return result;
			}
		});
		PostView.setImageLoader(imageGetter);
		this.target = target;
		this.ctxt = ctxt;
		this.thread = thread;
		this.firstKnownPost = thread.getUnreadID();
		data = (ArrayAdapter) getWrappedAdapter();
	}

	protected static View getPendingViewImpl(ViewGroup parent) {
		TextView result = new TextView(parent.getContext());
		result.setText("Loading...");
		return result;
	}

	@Override
	protected boolean cacheInBackground() throws Exception {
		return cacheInBackground(false);
	}

	private FLMessageSet fetchOldPosts() throws FLException {
		if (firstLoadedPostKnownOffset <= 0)
			return new FLMessageSet(null, new LinkedList<FLMessage>(), new FLThreadPageSet(0, false));

		boolean success = false;
		FLMessageSet gotPosts = null;
		while (!success) {
			int nextPageOffset = Math.max(firstLoadedPostKnownOffset - 20 + 1, 0);
			gotPosts = FLDataLoader.listMessages(SessionContainer.getSessionInstance(), thread, nextPageOffset);
			if ((nextPageOffset == 0) || (gotPosts.getEffectiveOffset() > 0)) { //if effective offset is 0 then we requested too large offset.
				LinkedList<FLMessage> fetchedPosts = gotPosts.getPosts();

				if (fetchedPosts.getLast().getID() < firstKnownPost) {
					//this page does not contain first post we loaded: so we missed, and we must
					//load posts from biggest offset
					firstLoadedPostKnownOffset += 10;
					continue;
				}
				if (fetchedPosts.getFirst().getID() >= firstKnownPost) {
					//this page contains only newPosts before last post we knew. let's load future pages
					//to get some newer newPosts we have not read yet
					firstLoadedPostKnownOffset -= 20 - 1;
					continue;
				}

				//this page definitely contains some new newPosts; let's filter em
				firstLoadedPostKnownOffset = gotPosts.getEffectiveOffset();
				success = true;
				remover:
				while (!fetchedPosts.isEmpty()) {
					if (fetchedPosts.getLast().getID() >= firstKnownPost)
						fetchedPosts.removeLast();
					else
						break remover;
				}
				firstKnownPost = fetchedPosts.getFirst().getID();
			} else
				firstLoadedPostKnownOffset -= 10;
		}
		return gotPosts;
	}

	private FLMessageSet fetchNewPosts() throws FLException {
		boolean success = false;
		FLMessageSet gotPosts = null;
		while (!success) {
			gotPosts = FLDataLoader.listMessages(SessionContainer.getSessionInstance(), thread, lastLoadedPostKnownOffset);
			if ((lastLoadedPostKnownOffset <= 0) || (gotPosts.getEffectiveOffset() > 0)) { //if effective offset is 0 then we requested too large lastKnownOffset.
				if (lastKnownPost == -1) {
					success = true;
					firstLoadedPostKnownOffset = gotPosts.getEffectiveOffset();
					firstKnownPost = gotPosts.getPosts().getFirst().getID();
					lastLoadedPostKnownOffset = firstLoadedPostKnownOffset + gotPosts.getPosts().size() - 1;
					lastKnownPost = gotPosts.getPosts().getLast().getID();
				} else {
					LinkedList<FLMessage> fetchedPosts = gotPosts.getPosts();
					boolean foundNewPostsOnPage = false;
					boolean foundOldPostsOnPage = false;

					if (fetchedPosts.getFirst().getID() > lastKnownPost) {
						//this page does not contain last post we loaded: so some newPosts were deleted,
						//and possibly we missed some newPosts between pages. let's load some earlier newPosts
						lastLoadedPostKnownOffset -= 10;
						continue;
					}
					if (fetchedPosts.getLast().getID() <= lastKnownPost) {
						if (fetchedPosts.size() > 1) {
							//this page contains only newPosts before last post we knew. let's load future pages
							//to get some newer newPosts we have not read yet
							lastLoadedPostKnownOffset += fetchedPosts.size() - 1;
							continue;
						} else {
							//this page contains only last known post: nothing new appeared
							fetchedPosts.clear(); //let's remove it since we already cached it
							break;
						}
					}

					//this page definitely contains some new newPosts; let's filter em
					lastLoadedPostKnownOffset = gotPosts.getEffectiveOffset() + fetchedPosts.size() - 1;
					success = true;
					remover:
					while (!fetchedPosts.isEmpty()) {
						if (fetchedPosts.getFirst().getID() <= lastKnownPost)
							fetchedPosts.removeFirst();
						else
							break remover;
					}
					lastKnownPost = fetchedPosts.getLast().getID();
				}
			} else
				lastLoadedPostKnownOffset -= 10;
		}
		ctxt.dataLoaded();
		return gotPosts;
	}

	protected boolean cacheInBackground(boolean up) throws Exception {

		final FLMessageSet gotPosts;
		final LinkedList<FLMessage> target;

		if (up) {
			gotPosts = fetchOldPosts();
			target = earlierPosts;
		} else {
			gotPosts = fetchNewPosts();
			target = newPosts;
		}

		synchronized (target) {
			if (target.addAll(gotPosts.getPosts()))
				if ((!up) && scrolledToEnd.getAndSet(false))
					ctxt.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(ctxt, "New posts appeared: " + newPosts.size(), Toast.LENGTH_LONG).show();//TODO: move to resource
						}
					});
		}

		if (up || gotPosts.hasMoreData()) {
			return true;
		}

		scrolledToEnd.set(true);
		return false;
	}

	@Override
	protected void appendCachedData() {
		synchronized (newPosts) {
			for (FLMessage post : newPosts) {
				data.add(new FLMessageWrapper(post));
			}
			newPosts.clear();
		}
	}

	@Override
	public FLMessage getMessage(int position) {
		FLMessageWrapper wrap = (FLMessageWrapper) getItem(position);
		if ((wrap == null) || (wrap.isLoadingStub))
			return null;
		return wrap.message;
	}

	@Override
	protected View getPendingView(ViewGroup parent) {
		if (parent == null)
			return null;
		return getPendingViewImpl(parent);
	}

	public void upOverScroll() {
		if (firstLoadedPostKnownOffset > 0)
			if (!drawnUpPlaceholder.getAndSet(true)) {
				data.insert(FLMessageWrapper.placeholder, 0);
				cacheInBackgroundUpper();
			}
	}

	protected void cacheInBackgroundUpper() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					cacheInBackground(true);
				} catch (Exception e) {
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void vd) {
				if (drawnUpPlaceholder.getAndSet(false)) {
					data.remove(FLMessageWrapper.placeholder);
					int fetchedPostsNum = earlierPosts.size();

					synchronized (earlierPosts) {
						while (!earlierPosts.isEmpty())
							data.insert(new FLMessageWrapper(earlierPosts.removeLast()), 0);
					}

					int index = target.getFirstVisiblePosition();
					View v = target.getChildAt(1);
					int top = (v == null) ? 0 : v.getTop();

					notifyDataSetChanged();
					target.setSelectionFromTop(index + fetchedPostsNum, top);
				}
			}
		}.execute();
	}

	public void refresh() {
		ctxt.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				restartAppending();
				if (scrolledToEnd.get())
					getView(getCount() - 1, null, null); //since we restarted appending, getcount will return num of newPosts + 1 for "loading" item. we must substract this "loading" item
			}
		});
	}
}


class FLMessageWrapper {

	public static final FLMessageWrapper placeholder = new FLMessageWrapper();
	public final FLMessage message;
	public final boolean isLoadingStub;

	public FLMessageWrapper(FLMessage wrapped) {
		this.message = wrapped;
		isLoadingStub = false;
	}

	public FLMessageWrapper() {
		isLoadingStub = true;
		message = null;
	}

}
