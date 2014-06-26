package ru.ith.android.flocal.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * When we have a long line in one-line text view (like, y'now, thread name),
 * we don't want to split it in multiple lines.
 *
 * This solution is inspired by
 * http://stackoverflow.com/questions/1827751/is-there-a-way-to-make-ellipsize-marquee-always-scroll
 *
 * Not sure if it's a really good solution :3
 * Created by infthi on 25.06.14.
 */
public class AutoScrollableTextView extends TextView {

    public AutoScrollableTextView(Context context) {
        super(context);
    }

    public AutoScrollableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if(focused)
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        if(focused)
            super.onWindowFocusChanged(focused);
    }


    @Override
    public boolean isFocused() {
        return true;
    }
}
