package ru.ith.android.flocal.views;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.activities.PostListActivity;
import ru.ith.android.flocal.io.ImageFactory;
import ru.ith.android.flocal.util.Settings;
import ru.ith.android.flocal.views.util.message.ImageLoadTask;
import ru.ith.android.flocal.views.util.message.PostPostTask;
import ru.ith.lib.flocal.data.FLMessage;

/**
 * Created by infthi on 02.08.14.
 */
public class PostView extends FrameLayout {
    enum CUT_CAPABILITY {UNAVAILABLE, COLLAPSED, EXPANDED}
    private volatile CUT_CAPABILITY postCutCapability = CUT_CAPABILITY.UNAVAILABLE;
	private int maxPossibleHeight=0;

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


        ((TextView) findViewById(R.id.postEntryAuthor)).setText(message.getAuthor());//TODO: re: may be in here too
        ((TextView) findViewById(R.id.postEntryDate)).setText(message.getDate());

        new ImageLoadTask(htmlSpannable, postBodyView, imageGetter).executeOnExecutor(ImageLoader);
        imageGetter.getAvatar(message.getAuthor(), ((ImageView) findViewById(R.id.postEntryAvatar)));

        initializeToolbar();
        enableExpandCapability();
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
				toggleExpansion();
			}
		});
    }

    private void enableExpandCapability() {
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
                Log.d(VIEW_LOG_TAG, postBodyView.getHeight()+"/"+limit);
				maxPossibleHeight = postBodyView.getHeight();
				postBodyView.setMaxHeight(maxPossibleHeight);
                if (maxPossibleHeight > limit) {
                    postCutCapability = CUT_CAPABILITY.EXPANDED;
                } else {
                    postCutCapability = CUT_CAPABILITY.UNAVAILABLE;
                }
                toggleExpansion();
            }
        };
        postBodyView.addOnLayoutChangeListener(listener);
    }

    public synchronized void toggleExpansion(){
        final TextView postBodyView = (TextView) findViewById(R.id.postEntryText);
		int newMaxHeight = -1;
        switch (postCutCapability){
            case UNAVAILABLE:
                ((ImageButton) findViewById(R.id.button_expand)).setEnabled(false);
                return;
            case COLLAPSED:
                postCutCapability = CUT_CAPABILITY.EXPANDED;
				newMaxHeight = maxPossibleHeight;
                ((ImageButton) findViewById(R.id.button_expand)).setEnabled(true);
                ((ImageButton) findViewById(R.id.button_expand)).setImageResource(android.R.drawable.ic_menu_revert);
                break;
            case EXPANDED:
                postCutCapability = CUT_CAPABILITY.COLLAPSED;
				newMaxHeight = Settings.instance.getPostCutLimit();
				((ImageButton) findViewById(R.id.button_expand)).setEnabled(true);
                ((ImageButton) findViewById(R.id.button_expand)).setImageResource(android.R.drawable.ic_menu_more);
                break;
            default:
                Log.d(VIEW_LOG_TAG, "Expand post called while post is inexpandable");
				return;
        }
		if (newMaxHeight>-1){
			final int startHeight = postBodyView.getMaxHeight();
			final int newMaxHeightF = newMaxHeight;
			Animation a = new Animation() {
				@Override
				protected void applyTransformation(float interpolatedTime, Transformation t) {
					postBodyView.setMaxHeight(startHeight+(int) (interpolatedTime*(newMaxHeightF-startHeight)));
					postBodyView.requestLayout();
				}

				@Override
				public boolean willChangeBounds() {
					return true;
				}
			};
			a.setDuration(300);
			postBodyView.startAnimation(a);
		}
    }

    public boolean isCollapsable() {
        return postCutCapability == CUT_CAPABILITY.EXPANDED;
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
}
