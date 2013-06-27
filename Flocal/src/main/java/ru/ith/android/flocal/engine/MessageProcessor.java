package ru.ith.android.flocal.engine;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Html;

import org.xml.sax.XMLReader;

import ru.ith.android.flocal.R;

/**
 * Created by infthi on 6/27/13.
 */
public class MessageProcessor implements Html.TagHandler, Html.ImageGetter {
    public static final MessageProcessor instance = new MessageProcessor();

    public void setContext(Context context) {
        this.context = context;
    }

    private Context context;

    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {

    }

    @Override
    public Drawable getDrawable(String source) {
        return context.getResources().getDrawable(R.drawable.ic_launcher);
    }
}
