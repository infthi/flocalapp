package ru.ith.android.flocal.views;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.activities.PostListActivity;
import ru.ith.android.flocal.engine.SessionContainer;
import ru.ith.android.flocal.io.ImageFactory;
import ru.ith.android.flocal.util.Settings;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.data.FLMessage;

/**
 * Created by infthi on 02.08.14.
 */
public class PostView extends FrameLayout {
    private final static Executor ImageLoader = new ThreadPoolExecutor(3, 5, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private static volatile ImageFactory imageGetter;

    private FLMessage message;

    public static void setImageLoader(ImageFactory imageGetter) {
        PostView.imageGetter = imageGetter;
    }

    public void setMessage(FLMessage message) {
        this.message = message;

        final TextView postBodyView = (TextView) findViewById(R.id.postEntryText);
        SpannableStringBuilder htmlSpannable = null;
        Spanned spanned = Html.fromHtml(message.getPostData());
        if (spanned instanceof SpannableStringBuilder) {
            htmlSpannable = (SpannableStringBuilder) spanned;
        } else {
            htmlSpannable = new SpannableStringBuilder(spanned);
        }

        postBodyView.setText(htmlSpannable);
        postBodyView.setMovementMethod(LinkMovementMethod.getInstance());

        enableCutCapability();

        ((TextView) findViewById(R.id.postEntryAuthor)).setText(message.getAuthor());//TODO: re: may be in here too
        ((TextView) findViewById(R.id.postEntryDate)).setText(message.getDate());

        initializeToolbar();

        new ImageLoadTask(htmlSpannable, postBodyView, imageGetter).executeOnExecutor(ImageLoader);
        imageGetter.getAvatar(message.getAuthor(), ((ImageView) findViewById(R.id.postEntryAvatar)));
    }

    private void initializeToolbar() {
        ((ImageButton)findViewById(R.id.button_upvote)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "Upvote not implemented yet!", Toast.LENGTH_LONG).show();
                ((ImageButton) findViewById(R.id.button_upvote)).setImageResource(android.R.drawable.btn_star_big_on);
            }
        });
        ((ImageButton)findViewById(R.id.button_reply)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showReplyWindow();
            }
        });
        ((ImageButton)findViewById(R.id.button_expand)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleMinimization();
            }
        });
    }

    private void enableCutCapability() {
        final TextView postBodyView = (TextView) findViewById(R.id.postEntryText);
        /**
         * This is one-time listener. It is run after view is initially formatted,
         * and checks it's size. Depending on it's size, it enables or disables
         * 'expand/minimize' capability for the message text.
         */
        final View.OnLayoutChangeListener listener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                postBodyView.removeOnLayoutChangeListener(this);
                int limit = Settings.instance.getPostCutLimit();
                if (postBodyView.getLineCount() > limit) {
                    postCutCapability = CUT_CAPABILITY.MAXIMIZED;
                } else {
                    postCutCapability = CUT_CAPABILITY.UNAVAILABLE;
                }
                toggleMinimization();
            }
        };
        postBodyView.addOnLayoutChangeListener(listener);
    }

    public synchronized void toggleMinimization(){
        final TextView postBodyView = (TextView) findViewById(R.id.postEntryText);
        switch (postCutCapability){
            case UNAVAILABLE:
                ((ImageButton) findViewById(R.id.button_expand)).setEnabled(false);
                return;
            case MINIMIZED:
                postCutCapability = CUT_CAPABILITY.MAXIMIZED;
                postBodyView.setMaxLines(Integer.MAX_VALUE);
                ((ImageButton) findViewById(R.id.button_expand)).setImageResource(android.R.drawable.ic_menu_revert);
                break;
            case MAXIMIZED:
                postCutCapability = CUT_CAPABILITY.MINIMIZED;
                postBodyView.setMaxLines(Settings.instance.getPostCutLimit());
                ((ImageButton) findViewById(R.id.button_expand)).setImageResource(android.R.drawable.ic_menu_more);
                break;
            default:
                Log.d(VIEW_LOG_TAG, "Expand post called while post is inexpandable");
        }
    }

    public String getMessageText() {
        if (message == null)
            return "";
        return ((TextView) findViewById(R.id.postEntryText)).getText().toString();
    }

    public String getMessageUrl() {
        if (message == null)
            return "";
        return message.getURL();
    }

    enum CUT_CAPABILITY {UNAVAILABLE, MINIMIZED, MAXIMIZED}

    private volatile CUT_CAPABILITY postCutCapability = CUT_CAPABILITY.UNAVAILABLE;

    public PostView(Context context) {
        super(context);
    }

    public PostView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PostView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void showReplyWindow() {
        final Dialog reply = new Dialog(getContext());
        reply.setContentView(R.layout.dialog_reply);
        reply.setTitle(R.string.dialog_reply_title);
        final EditText edit = ((EditText) reply.findViewById(R.id.replyText));
        ((Button) reply.findViewById(R.id.cancelButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reply.hide();
            }
        });
        ((Button) reply.findViewById(R.id.dialogReplyButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = edit.getText().toString();
                ((PostListActivity) PostView.this.getContext()).notify(text);
                final ProgressDialog progress = new ProgressDialog(getContext());
                progress.setMessage("Sending...");
                progress.setCancelable(false);
                progress.show();
                new PostPostTask(progress, reply, PostView.this.message).execute(text);
            }
        });
        reply.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        reply.show();
        edit.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
    }

    public boolean isMinimizable() {
        return postCutCapability == CUT_CAPABILITY.MAXIMIZED;
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

class updateHTMLPack {
    public final ImageSpan img;
    public final Drawable d;

    updateHTMLPack(ImageSpan spanToUpdate, Drawable d) {
        this.img = spanToUpdate;
        this.d = d;
    }
}

class PostPostTask extends AsyncTask<String, Void, Boolean> {
    private final ProgressDialog progress;
    private final Dialog reply;
    private final FLMessage parent;
    private volatile String problem = "Unknown problem";

    public PostPostTask(ProgressDialog progress, Dialog reply, FLMessage parent) {
        this.progress = progress;
        this.reply = reply;
        this.parent = parent;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            FLDataLoader.sendMessage(SessionContainer.getSessionInstance(), parent, params[0]);
            return true;
        } catch (Exception e) {
            problem = e.getMessage();
            return false;
        }
    }


    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        progress.hide();
        if (aBoolean)
            reply.hide();
        else
            Toast.makeText(reply.getContext(), problem, Toast.LENGTH_SHORT).show();
    }
}