package ru.ith.android.flocal.engine;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.endless.EndlessAdapter;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.activities.PostListActivity;
import ru.ith.android.flocal.io.ImageFactory;
import ru.ith.android.flocal.views.PostView;
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
    private final FLThreadHeader thread;
    private final ArrayAdapter<FLMessageWrapper> data;
    private final PostListActivity ctxt;
    private final ListView target;
    private final static Executor ImageLoader = new ThreadPoolExecutor(3, 5, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

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

                    final TextView postBodyView = ((TextView) result.findViewById(R.id.postEntryText));
                    SpannableStringBuilder htmlSpannable = null;
                    Spanned spanned = Html.fromHtml(item.message.getPostData());
                    if (spanned instanceof SpannableStringBuilder) {
                        htmlSpannable = (SpannableStringBuilder) spanned;
                    } else {
                        htmlSpannable = new SpannableStringBuilder(spanned);
                    }
                    new ImageLoadTask(htmlSpannable, postBodyView, imageGetter).executeOnExecutor(ImageLoader);

                    postBodyView.setText(htmlSpannable);
                    postBodyView.setMovementMethod(LinkMovementMethod.getInstance());

                    result.enableCutCapability();

                    ((TextView) result.findViewById(R.id.postEntryAuthor)).setText(item.message.getAuthor());//TODO: re: may be in here too
                    ((TextView) result.findViewById(R.id.postEntryDate)).setText(item.message.getDate());
                    imageGetter.getAvatar(item.message.getAuthor(), ((ImageView) result.findViewById(R.id.postEntryAvatar)));

                    cachedMessageViews.put(item.message.getID(), new WeakReference<PostView>(result));
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

    int lastLoadedPostKnownOffset = -1;
    int firstLoadedPostKnownOffset = -1;

    private final LinkedList<FLMessage> newPosts = new LinkedList<FLMessage>();
    private final LinkedList<FLMessage> earlierPosts = new LinkedList<FLMessage>();

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

    protected static View getPendingViewImpl(ViewGroup parent) {
        TextView result = new TextView(parent.getContext());
        result.setText("Loading...");
        return result;
    }

    private AtomicBoolean drawnUpPlaceholder = new AtomicBoolean(false);

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

    private static final AtomicInteger container = new AtomicInteger(0);

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

class updateHTMLPack {
    public final ImageSpan img;
    public final Drawable d;

    updateHTMLPack(ImageSpan spanToUpdate, Drawable d) {
        this.img = spanToUpdate;
        this.d = d;
    }
}

class ImageLoadTask extends AsyncTask<Void, updateHTMLPack, Void> {

    DisplayMetrics metrics = new DisplayMetrics();
    private final SpannableStringBuilder htmlSpannable;
    private ImageFactory mFactory;
    private TextView htmlTextView;

    ImageLoadTask(SpannableStringBuilder htmlSpannable, TextView htmlTextView, ImageFactory factory) {
        this.htmlSpannable = htmlSpannable;
        mFactory = factory;
        this.htmlTextView = htmlTextView;
    }

    @Override
    protected void onPreExecute() {
        // we need this to properly scale the images later
        //getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    @Override
    protected Void doInBackground(Void... params) {
        // iterate over all images found in the html
        for (final ImageSpan img : htmlSpannable.getSpans(0,
                htmlSpannable.length(), ImageSpan.class)) {
            Drawable d = getImageFile(img.getSource());
            if (d == null) {
                Log.d(FLDataLoader.FLOCAL_APP_SIGN, "Failed to load [" + img.getSource() + "]; null");
                //TODO: Load some kinf of "failed to load" image here
            } else {
                d.setBounds(0, 0, (int) (d.getIntrinsicWidth() * mFactory.dpK * 1.5), (int) (d.getIntrinsicHeight() * mFactory.dpK * 1.5));
                publishProgress(new updateHTMLPack(img, d));
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(updateHTMLPack... values) {
        updateHTMLPack pk = values[0];
        // now we create a new ImageSpan
        ImageSpan newImg = new ImageSpan(pk.d, pk.img.getSource());

        // find the position of the old ImageSpan
        int start = htmlSpannable.getSpanStart(pk.img);
        int end = htmlSpannable.getSpanEnd(pk.img);

        // remove the old ImageSpan
        htmlSpannable.removeSpan(pk.img);

        // add the new ImageSpan
        htmlSpannable.setSpan(newImg, start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // finally we have to update the TextView with our
        // updates Spannable to display the image
        htmlTextView.setText(htmlSpannable);
    }

    private TreeMap<String, Drawable> cache = new TreeMap<String, Drawable>();

    private Drawable getImageFile(String src) {
        Drawable res;
        if ((res = cache.get(src)) == null) {
            res = mFactory.getDrawable(src);
            cache.put(src, res);
        }
        return res;
    }

}
