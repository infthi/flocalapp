package ru.ith.lib.flocal.data;

/**
 * Created by adminfthi on 30.06.13.
 */
public class AvatarMetaData {
	public final String URL;
	public final long lastModified;

	public AvatarMetaData(String url, long lastModified) {
		URL = url;
		this.lastModified = lastModified;
	}
}
