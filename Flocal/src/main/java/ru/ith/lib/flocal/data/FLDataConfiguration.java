package ru.ith.lib.flocal.data;

/**
 * Created by infthi on 05.01.15.
 */
public class FLDataConfiguration {
	public final String POST_PROTECTION_KEY;
	public final String encoding;

	public FLDataConfiguration(String post_protection_key, String encoding) {
		POST_PROTECTION_KEY = post_protection_key;
		this.encoding = encoding;
	}
}
