package ru.ith.android.flocal.views.util.message;

import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

class updateHTMLPack {
    public final ImageSpan img;
    public final Drawable d;

    updateHTMLPack(ImageSpan spanToUpdate, Drawable d) {
        this.img = spanToUpdate;
        this.d = d;
    }
}
