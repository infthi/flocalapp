package ru.ith.android.flocal.io;

import android.R;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import ru.ith.android.flocal.engine.SessionContainer;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.data.AvatarMetaData;

/**
 * Created by adminfthi on 28.06.13.
 */
public class ImageFactory {
	private final Activity context;
	final SQLiteDatabase avatarDB;
	final SQLiteDatabase uploadDB;
	public final float dpK;

	public ImageFactory(Activity context) {
		this.context = context;
		avatarDB = new AvatarCacheDB(context).getWritableDatabase();
		uploadDB = new UploadCacheDB(context).getWritableDatabase();
		DisplayMetrics dm = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(dm);
		dpK = dm.density;
	}

	public Drawable getDrawable(String source) {
		String whereClause =  UploadCacheDB.ROW_ID + "= ?";
		String ID = source.substring(source.lastIndexOf('/'));
		String[] whereValues = new String[]{ID};
		try {
			//first: check out cache
			Cursor c = null;
			try {
				c = uploadDB.query(UploadCacheDB.UPLOAD_TABLE, new String[]{UploadCacheDB.ROW_CACHED_FILE},whereClause, whereValues, null, null, null);
				if (c.getCount()>0) {
					c.moveToNext();
					String cachedFileName = c.getString(0);
					if (cachedFileName == null)
						return null;
					Drawable result = loadFromCache(cachedFileName);
					if (result != null){
						ContentValues updatedDate=new ContentValues();
						updatedDate.put(UploadCacheDB.ROW_LAST_CHECKED, System.currentTimeMillis());
						uploadDB.update(UploadCacheDB.UPLOAD_TABLE, updatedDate, whereClause, whereValues);
							return result;
					}
					c.close();
					c = null;
					uploadDB.delete(UploadCacheDB.UPLOAD_TABLE, whereClause, whereValues);
				}
			} finally {
				if (c!=null)
					c.close();
			}

			String cacheID = null;
			InputStream newUploadStream = null;
			newUploadStream = FLDataLoader.fetchFile(source);
			cacheID = saveToCache(newUploadStream, "file");

			ContentValues newCachedFile = new ContentValues();
			newCachedFile.put(UploadCacheDB.ROW_ID, ID);
			newCachedFile.put(UploadCacheDB.ROW_CACHED_FILE, cacheID);
			newCachedFile.put(UploadCacheDB.ROW_LAST_CHECKED, System.currentTimeMillis());
			uploadDB.insert(UploadCacheDB.UPLOAD_TABLE, null, newCachedFile);

			if (cacheID==null)
				return null;

			return loadFromCache(cacheID);
		} catch (Exception e1) {
			return null;
		}
	}

	private Map<String, Reference<Drawable>> avatarCache = new HashMap<String, Reference<Drawable>>();
	private Map<String, List<ImageView>> waiters = new TreeMap<String, List<ImageView>>();

	public synchronized void getAvatar(String user, ImageView target) {
		if (avatarCache.containsKey(user)){
			Reference<Drawable> ref = avatarCache.get(user);
			if (ref==null){
				target.setImageDrawable(null);
				return;
			} else {
				Drawable avatar = ref.get();
				if (avatar!=null){
					Log.v(FLDataLoader.FLOCAL_APP_SIGN, "got from mem-cache for "+user);
					target.setImageDrawable(avatar);
					return;
				}
			}
		}
		Log.v(FLDataLoader.FLOCAL_APP_SIGN, "forced to load for "+user);
		Drawable loading = context.getResources().getDrawable(R.drawable.spinner_background);
		target.setImageDrawable(loading);
		List<ImageView> avatarWaiters = waiters.get(user);
		if (avatarWaiters==null){
			avatarWaiters = new LinkedList<ImageView>();
			waiters.put(user, avatarWaiters);
			new avatarLoaderTask(user, this).execute();
		}
		avatarWaiters.add(target);
	}

	public synchronized void avatarLoaded(String user, final Drawable avatar) {
		Log.v(FLDataLoader.FLOCAL_APP_SIGN, "loaded for "+user);
		if (avatar!=null)
			avatarCache.put(user, new WeakReference<Drawable>(avatar));
		else
			avatarCache.put(user, null);
		final List<ImageView> victims = waiters.get(user);
		if (victims==null)
			return;
		if (avatar!=null)
			avatar.setBounds(0, 0, avatar.getIntrinsicWidth(), avatar
				.getIntrinsicHeight());
		for (final ImageView view:victims){
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					view.setImageDrawable(avatar);
				}
			});
		}
		waiters.put(user, null);
	}

	public Drawable loadFromCache(String cachedFileName) {
		File cachedFile = new File(context.getCacheDir(), cachedFileName);
		Drawable result = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(cachedFile);
			BitmapFactory.Options o = new BitmapFactory.Options();
            Bitmap largeAvatar = BitmapFactory.decodeStream(fis, null, o);

            if (largeAvatar!=null){
                int max = Math.max(o.outHeight, o.outWidth);
                if (max>80){
                    double scale = 80.0/max;
                    largeAvatar = Bitmap.createScaledBitmap(largeAvatar, (int)(o.outWidth*scale), (int)(o.outHeight*scale), true);
                }
                return new BitmapDrawable(null, largeAvatar);
            }
            return null;
		} catch (FileNotFoundException e) {
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {
				}
		}
		//TODO: some marker that avatar failed to load
		return result;
	}

	public String saveToCache(InputStream newAvatarStream, String prefix) throws IOException {
		if (newAvatarStream==null)
			return null;
		String fileName = prefix+"-"+UUID.randomUUID().toString();
		File result = new File(context.getCacheDir(), fileName);
		FileOutputStream cacheStream = new FileOutputStream(result);
		byte[]buf = new byte[2048];
		int len;
		while ((len = newAvatarStream.read(buf))>=0){
			cacheStream.write(buf, 0, len);
		}
		cacheStream.close();
		return fileName;
	}
}

class avatarLoaderTask extends AsyncTask<Void, Void, Drawable> {

	private String mUser;
	private ImageFactory mImageFactory;

	public avatarLoaderTask(String user, ImageFactory imageFactory) {
		mUser = user;
		mImageFactory = imageFactory;
	}

	@Override
	protected Drawable doInBackground(Void... params) {
		boolean useCachedVersion = false;
		AvatarMetaData meta = null;
		String whereClause =  AvatarCacheDB.ROW_USER + "= ?";
		String[] whereValues = new String[]{mUser};
		try {
			//first: check out cache
			Cursor c = null;
			try {
				c = mImageFactory.avatarDB.query(AvatarCacheDB.AVATAR_TABLE, new String[]{AvatarCacheDB.ROW_CACHED_FILE, AvatarCacheDB.ROW_LAST_CHECKED},whereClause, whereValues, null, null, null);
				if (c.getCount()>0) {
					c.moveToNext();
					String cachedFileName = c.getString(0);
					Long cachedAt = c.getLong(1);
					if (cachedAt >= System.currentTimeMillis() - 2 * 7 * 86400 * 1000) { //two-week invalidation
						useCachedVersion = true;
					} else {
						meta = FLDataLoader.getAvatarMetadata(SessionContainer.getSessionInstance(), mUser, false);
						if (meta.URL==null){
							if (cachedFileName==null)
								useCachedVersion = true;
						} else if (meta.lastModified<cachedAt){
							useCachedVersion = true;
						}
					}
					if (useCachedVersion) {
						if (cachedFileName == null)
							return null;
						Drawable result = mImageFactory.loadFromCache(cachedFileName);
						if (result != null){
							ContentValues updatedDate=new ContentValues();
							updatedDate.put(AvatarCacheDB.ROW_LAST_CHECKED, System.currentTimeMillis());
							mImageFactory.avatarDB.update(AvatarCacheDB.AVATAR_TABLE, updatedDate, whereClause, whereValues);
							return result;
						}
					}
					c.close();
					c = null;
					mImageFactory.avatarDB.delete(AvatarCacheDB.AVATAR_TABLE, whereClause, whereValues);
				}
			} finally {
				if (c!=null)
					c.close();
			}

			if (meta==null)
				meta = FLDataLoader.getAvatarMetadata(SessionContainer.getSessionInstance(), mUser, true);

			String cacheID = null;
			if (meta.URL!=null){
				InputStream newAvatarStream = null;
				newAvatarStream = FLDataLoader.fetchAvatar(meta);
				cacheID = mImageFactory.saveToCache(newAvatarStream, "avatar");
			}

			ContentValues newCachedAvatar = new ContentValues();
			newCachedAvatar.put(AvatarCacheDB.ROW_USER, mUser);
			newCachedAvatar.put(AvatarCacheDB.ROW_CACHED_FILE, cacheID);
			newCachedAvatar.put(AvatarCacheDB.ROW_LAST_CHECKED, System.currentTimeMillis());
			mImageFactory.avatarDB.insert(AvatarCacheDB.AVATAR_TABLE, null, newCachedAvatar);

			if (cacheID==null)
				return null;

			return mImageFactory.loadFromCache(cacheID);
		} catch (Exception e1) {
			return null;
		}
	}

	@Override
	protected void onPostExecute(Drawable drawable) {
		mImageFactory.avatarLoaded(mUser, drawable);
	}
}