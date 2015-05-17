package ru.ith.android.flocal.io;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.io.InputStream;

import ru.ith.android.flocal.engine.SessionContainer;
import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.flocal.data.AvatarMetaData;

/**
 * This class implements task, which tries to fetch avatar from L2(disk) cache.
 * If no avatar if found on disk, it fetches data from the network.
 */
class AvatarLoaderTask extends AsyncTask<Void, Void, Drawable> {

	private String mUser;
	private ImageFactory mImageFactory;

	public AvatarLoaderTask(String user, ImageFactory imageFactory) {
		mUser = user;
		mImageFactory = imageFactory;
	}

	@Override
	protected Drawable doInBackground(Void... params) {
		boolean useCachedVersion = false;
		AvatarMetaData meta = null;
		String whereClause = AvatarCacheDB.ROW_USER + "= ?";
		String[] whereValues = new String[]{mUser};
		try {
			//first: check out cache
			Cursor c = null;
			try {
				c = mImageFactory.avatarDB.query(AvatarCacheDB.AVATAR_TABLE, new String[]{AvatarCacheDB.ROW_CACHED_FILE, AvatarCacheDB.ROW_LAST_CHECKED}, whereClause, whereValues, null, null, null);
				if (c.getCount() > 0) {
					c.moveToNext();
					String cachedFileName = c.getString(0);
					Long cachedAt = c.getLong(1);
					if (cachedAt >= System.currentTimeMillis() - 2 * 7 * 86400 * 1000) { //two-week invalidation
						useCachedVersion = true;
					} else {
						meta = FLDataLoader.getAvatarMetadata(SessionContainer.getSessionInstance(), mUser, false);
						if (meta.URL == null) {
							if (cachedFileName == null)
								useCachedVersion = true;
						} else if (meta.lastModified < cachedAt) {
							useCachedVersion = true;
						}
					}
					if (useCachedVersion) {
						if (cachedFileName == null)
							return null;
						Drawable result = mImageFactory.loadFromCache(cachedFileName);
						if (result != null) {
							ContentValues updatedDate = new ContentValues();
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
				if (c != null)
					c.close();
			}

			if (meta == null)
				meta = FLDataLoader.getAvatarMetadata(SessionContainer.getSessionInstance(), mUser, true);

			String cacheID = null;
			if (meta.URL != null) {
				InputStream newAvatarStream = null;
				newAvatarStream = FLDataLoader.fetchAvatar(meta);
				cacheID = mImageFactory.saveToCache(newAvatarStream, "avatar");
			}

			ContentValues newCachedAvatar = new ContentValues();
			newCachedAvatar.put(AvatarCacheDB.ROW_USER, mUser);
			newCachedAvatar.put(AvatarCacheDB.ROW_CACHED_FILE, cacheID);
			newCachedAvatar.put(AvatarCacheDB.ROW_LAST_CHECKED, System.currentTimeMillis());
			mImageFactory.avatarDB.insert(AvatarCacheDB.AVATAR_TABLE, null, newCachedAvatar);

			if (cacheID == null)
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
