package ru.ith.lib.webcrawl;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Map;
import ru.ith.lib.webcrawl.providers.ProviderEnum;

public class ConnectionFactory {
	
	private static Socket getConnection(String host) throws IOException{
		return new Socket(host, 80); //TODO: keep-alive support
	}
	public static WebResponceReader doQuery(String host, String url, Map<String, String> cookies, ProviderEnum requestType) throws IOException{
		return doQueryMain(host, url, cookies, null, null, requestType);
	}
	
	public static WebResponceReader doQuery(String host, String url, Map<String, String> cookies,
			Map<String, String> postData, String postEncoding, ProviderEnum requestType) throws IOException {
		byte[]postDataB = null;
		if (postData!=null){
			StringBuilder requestString = new StringBuilder();
			for (Map.Entry<String, String> element: postData.entrySet()){
				requestString.append(URLEncoder.encode(element.getKey(), postEncoding));
				requestString.append('=');
				requestString.append(URLEncoder.encode(element.getValue(), postEncoding));
				requestString.append('&');
			}
			postDataB = requestString.toString().getBytes("ASCII");
		}
		return doQueryMain(host, url, cookies, postDataB, "application/x-www-form-urlencoded", requestType);
	}
	
	public static WebResponceReader doQueryMain(String host, String url, Map<String, String> cookies,
			byte[] postData, String postContentType, ProviderEnum requestType) throws IOException {
		Socket listener = getConnection(host);

		OutputStream os = listener.getOutputStream();

		os.write((((postData != null) ? "POST " : "GET ") + url + " HTTP/1.0\n")
				.getBytes());

		if ((cookies!=null)&&(!cookies.isEmpty())){
			StringBuilder cookieString = new StringBuilder("Cookie: ");
			for (Map.Entry<String, String> cookie: cookies.entrySet()){
				cookieString.append(cookie.getKey()).append('=').append(cookie.getValue()).append("; ");
			}
			cookieString.append("\n");
			os.write(cookieString.toString().getBytes("ASCII"));
		}
		
		if (postData != null) {
			os.write(("Content-Length: " + postData.length + "\n").getBytes("ASCII"));
			os.write(("Content-Type: " +postContentType+"\n").getBytes("ASCII"));
		}
		os.write('\n');
		if (postData != null) {
			os.write(postData);
		}
		return  WebResponceReader.make(listener.getInputStream(), requestType);
	}
}
