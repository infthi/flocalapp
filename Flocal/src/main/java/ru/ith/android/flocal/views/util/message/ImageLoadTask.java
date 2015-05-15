package ru.ith.android.flocal.views.util.message;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;

import java.util.TreeMap;

import ru.ith.android.flocal.io.ImageFactory;
import ru.ith.lib.flocal.FLDataLoader;

public class ImageLoadTask extends AsyncTask<Void, updateHTMLPack, Void> {

	private final SpannableStringBuilder htmlSpannable;
	DisplayMetrics metrics = new DisplayMetrics();
	private ImageFactory mFactory;
	private TextView htmlTextView;
	private TreeMap<String, Drawable> cache = new TreeMap<String, Drawable>();

	public ImageLoadTask(SpannableStringBuilder htmlSpannable, TextView htmlTextView, ImageFactory factory) {
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

	private Drawable getImageFile(String src) {
		Drawable res;
		if ((res = cache.get(src)) == null) {
			res = mFactory.getDrawable(src);
			cache.put(src, res);
		}
		return res;
	}

}
