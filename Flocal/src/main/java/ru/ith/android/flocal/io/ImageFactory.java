package ru.ith.android.flocal.io;

import android.R;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Html;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import ru.ith.android.flocal.engine.SessionContainer;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.FLException;

/**
 * Created by adminfthi on 28.06.13.
 */
public class ImageFactory implements Html.ImageGetter {
	private final Activity context;

	public ImageFactory(Activity context) {
		this.context = context;
	}

	@Override
	public Drawable getDrawable(String source) {
		Drawable result = context.getResources().getDrawable(android.R.drawable.ic_delete);
		result.setBounds(0, 0, result.getIntrinsicWidth(), result
				.getIntrinsicHeight());
		return result;
	}

	private Map<String, Drawable> avatarCache = new HashMap<String, Drawable>();
	private Map<String, List<ImageView>> waiters = new TreeMap<String, List<ImageView>>();

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void getAvatar(String user, ImageView target) {
		if (avatarCache.containsKey(user)){
			target.setImageDrawable(avatarCache.get(user));
			return;
		}
		Drawable loading = context.getResources().getDrawable(R.drawable.stat_sys_download);
		target.setImageDrawable(loading);
		List<ImageView> thiswaiters = waiters.get(user);
		if (thiswaiters==null){
			thiswaiters = new LinkedList<ImageView>();
			waiters.put(user, thiswaiters);
			new avatarLoaderTask(user, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		thiswaiters.add(target);
	}

	public void avatarLoaded(String user, final Drawable drawable) {
		avatarCache.put(user, drawable);
		final List<ImageView> victims = waiters.get(user);
		if (victims==null)
			return;
		if (drawable!=null)
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable
				.getIntrinsicHeight());
		for (final ImageView view:victims){
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					view.setImageDrawable(drawable);
				}
			});
		}
	}
}

class avatarLoaderTask extends AsyncTask<String, Void, Drawable> {

	private String mUser;
	private ImageFactory mImageFactory;

	public avatarLoaderTask(String user, ImageFactory imageFactory) {
		mUser = user;
		mImageFactory = imageFactory;
	}

	@Override
	protected Drawable doInBackground(String... params) {
		try {
			String avatarURL = FLDataLoader.getAvatarURL(SessionContainer.getSessionInstance(), mUser);
			if (avatarURL==null)
				return null;
			Bitmap x;

			HttpURLConnection connection = (HttpURLConnection) new URL(avatarURL).openConnection();
			connection.connect();
			InputStream input = connection.getInputStream();

			x = BitmapFactory.decodeStream(input);
			x.setDensity(Bitmap.DENSITY_NONE);
			return new BitmapDrawable(x);
		} catch (Exception e1) {
			return null;
		}
	}

	@Override
	protected void onPostExecute(Drawable drawable) {
		mImageFactory.avatarLoaded(mUser, drawable);
	}
}