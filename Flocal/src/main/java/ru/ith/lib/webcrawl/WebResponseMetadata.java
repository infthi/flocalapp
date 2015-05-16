package ru.ith.lib.webcrawl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class WebResponseMetadata {
	public static final int HTTP_OK = 200;
	public static final int MOVED_PERMANENTLY = 301;
	public static final int FOUND = 302;
	private long lastModified = -1;
	private String encoding = "UTF8";
	private String redirect = null;
	private Map<String, String> cookies = new TreeMap<String, String>();

	WebResponseMetadata(HttpURLConnection conn) throws IOException {
		String value;
		value = conn.getHeaderField("Content-type");
		if (value != null) {
			String[] parts = value.split("; ");
			for (String part : parts) {
				if (part.startsWith("charset="))
					encoding = part.substring(8);
			}
		}

		value = conn.getHeaderField("Location");
		if (value != null) {
			redirect = value;
		}

		Map<String, List<String>> a = conn.getHeaderFields();
		List<String> rCookies = conn.getHeaderFields().get("Set-cookie");
		if (rCookies != null) for (String cookieData : rCookies) {
			cookieData = cookieData.split(";", 2)[0];
			String[] cookie = cookieData.split("=", 2);
			cookies.put(cookie[0], cookie[1]);
		}

		lastModified = conn.getHeaderFieldDate("Last-Modified", -1);
	}

	public String getEncoding() {
		return encoding;
	}

	public URI getRedirect() throws URISyntaxException {
		if (redirect == null)
			return null;
		return new URI(redirect);
	}

	public String getCookie(String name) {
		return cookies.get(name);
	}

	public long getLastModified() {
		return lastModified;
	}
}
