package ru.ith.android.flocal.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

import ru.ith.android.flocal.R;
import ru.ith.android.flocal.util.Settings;
import ru.ith.lib.flocal.data.FLMessage;

/**
 * Created by infthi on 02.08.14.
 */
public class PostView extends FrameLayout {
    private FLMessage message;

    public void setMessage(FLMessage message) {
        this.message = message;
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

    private final AtomicBoolean isTextCut = new AtomicBoolean(false);

    public PostView(Context context) {
        super(context);
    }

    public PostView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PostView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void enableCutCapability() {
        final TextView postBodyView = (TextView) findViewById(R.id.postEntryText);
        final View showMore = findViewById(R.id.postEntryShowMore);
        showMore.setVisibility(View.INVISIBLE);
        postBodyView.setMaxLines(Settings.instance.getPostCutLimit());
        final View.OnLayoutChangeListener listener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                postBodyView.removeOnLayoutChangeListener(this);
                int limit = Settings.instance.getPostCutLimit();
                if (postBodyView.getLineCount() > limit) {
                    postCutCapability = CUT_CAPABILITY.MINIMIZED;
                    showMore.setVisibility(View.VISIBLE);
                } else {
                    postCutCapability = CUT_CAPABILITY.UNAVAILABLE;
                    showMore.setVisibility(View.GONE);
                }
                showMore.postInvalidate();
            }
        };

        showMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postCutCapability = CUT_CAPABILITY.MAXIMIZED;
                postBodyView.setMaxLines(Integer.MAX_VALUE);
                showMore.setVisibility(View.GONE);
            }
        });
        postBodyView.addOnLayoutChangeListener(listener);
    }

    public boolean isMinimizable() {
        return postCutCapability == CUT_CAPABILITY.MAXIMIZED;
    }
}
