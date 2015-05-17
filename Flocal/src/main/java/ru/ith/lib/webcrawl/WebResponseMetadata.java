package ru.ith.lib.webcrawl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class WebResponseMetadata {
	private long lastModified = -1;
	private String encoding = "UTF8";
	private Map<String, String> cookies = new TreeMap<String, String>();

	WebResponseMetadata(HttpURLConnection conn) throws IOException {
		String contentType = conn.getContentType();
		if (contentType != null) {
			String[] parts = contentType.split("; ");
			for (String part : parts) {
				if (part.startsWith("charset="))
					encoding = part.substring(8);
			}
		}

		List<String> rCookies = conn.getHeaderFields().get("Set-cookie");
		if (rCookies != null) for (String cookie : rCookies) {
			cookie = cookie.split(";", 2)[0];
			String[] cookieData = cookie.split("=", 2);
			cookies.put(cookieData[0], cookieData[1]);
		}

		lastModified = conn.getHeaderFieldDate("Last-Modified", -1);
	}

	public String getEncoding() {
		return encoding;
	}

	public String getCookie(String name) {
		return cookies.get(name);
	}

	public long getLastModified() {
		return lastModified;
	}
}
