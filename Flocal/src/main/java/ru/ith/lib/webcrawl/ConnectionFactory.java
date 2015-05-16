package ru.ith.lib.webcrawl;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.webcrawl.providers.ProviderEnum;

public class ConnectionFactory {
	private static boolean useHTTPS = true;

	private static HttpURLConnection getConnection(String host, String request) throws IOException {
		//TODO: test if HTTPS connection succeeds and revert to HTTP is required
		URL url = new URL("https", host, request);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setReadTimeout(10000);
		connection.setConnectTimeout(15000);
		connection.setDoInput(true);

		return connection;
	}

	public static WebResponseReader doQuery(String host, String url, Map<String, String> cookies, ProviderEnum requestType) throws IOException {
		return doQueryMain(host, url, cookies, null, null, requestType);
	}

	public static WebResponseReader doQuery(String host, String url, Map<String, String> cookies,
											Map<String, String> postData, String postEncoding, ProviderEnum requestType) throws IOException {
		byte[] postDataB = null;
		if (postData != null) {
			StringBuilder requestString = new StringBuilder();
			for (Map.Entry<String, String> element : postData.entrySet()) {
				requestString.append(URLEncoder.encode(element.getKey(), postEncoding));
				requestString.append('=');
				requestString.append(URLEncoder.encode(element.getValue(), postEncoding));
				requestString.append('&');
			}
			postDataB = requestString.toString().getBytes("ASCII"); //TODO: check if forum accepts UTF
		}
		return doQueryMain(host, url, cookies, postDataB, "application/x-www-form-urlencoded", requestType);
	}

	private static String method(ProviderEnum request, byte[] postData) {
		if (request == ProviderEnum.HEAD)
			return "HEAD";
		if (postData == null)
			return "GET";
		return "POST";
	}

	public static WebResponseReader doQueryMain(final String host, String url, Map<String, String> cookies,
												byte[] postData, String postContentType, ProviderEnum requestType) throws IOException {
		Log.d(FLDataLoader.FLOCAL_APP_SIGN, url);

		HttpURLConnection conn = getConnection(host, url);
		conn.setRequestMethod(method(requestType, postData));

		if ((cookies != null) && (!cookies.isEmpty())) {
			StringBuilder cookieString = new StringBuilder();
			for (Map.Entry<String, String> cookie : cookies.entrySet()) {
				cookieString.append(cookie.getKey()).append('=').append(cookie.getValue()).append("; ");
			}
			conn.setRequestProperty("Cookie", cookieString.toString());
		}

		if (postData != null) {
			conn.setDoOutput(true);
			conn.setFixedLengthStreamingMode(postData.length);
			conn.setRequestProperty("Content-Type", String.valueOf(postContentType));

			OutputStream os = conn.getOutputStream();
//			BufferedWriter writer = new BufferedWriter(
//					new OutputStreamWriter(os, "UTF-8"));

			os.write(postData);
			os.flush();
		}

		WebResponseReader result = WebResponseReader.make(conn, requestType);
		return result;
	}
}
