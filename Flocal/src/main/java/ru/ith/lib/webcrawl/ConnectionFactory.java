package ru.ith.lib.webcrawl;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.webcrawl.providers.ProviderEnum;

public class ConnectionFactory {
    private static final ConcurrentHashMap<String, Socket> keepAliveSockets = new ConcurrentHashMap<String, Socket>();

    private static Socket getConnection(String host, String url) throws IOException {
        String socketKey = host + ":" + 80;
        Socket result;
        if ((result = keepAliveSockets.remove(socketKey)) != null) {
            Log.d(FLDataLoader.FLOCAL_APP_SIGN, "reusing socket :) " + url);
            return result;
        }
        Log.d(FLDataLoader.FLOCAL_APP_SIGN, "created new socket :( " + url);
        return new Socket(host, 80);
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
            postDataB = requestString.toString().getBytes("ASCII");
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
        StringBuilder message = new StringBuilder();

        Log.d(FLDataLoader.FLOCAL_APP_SIGN, url);
        message.append(method(requestType, postData) + " " + url + " HTTP/1.0\r\n");

        if ((cookies != null) && (!cookies.isEmpty())) {
            StringBuilder cookieString = new StringBuilder("Cookie: ");
            for (Map.Entry<String, String> cookie : cookies.entrySet()) {
                cookieString.append(cookie.getKey()).append('=').append(cookie.getValue()).append("; ");
            }
            cookieString.append("\r\n");
            message.append(cookieString.toString());
        }
        message.append("Connection: keep-alive\r\n");

        if (postData != null) {
            message.append("Content-Length: " + postData.length + "\r\n");
            message.append("Content-Type: " + postContentType + "\r\n");
        }
        message.append("\r\n");

        final Socket listener = getConnection(host, url);
        OutputStream os = listener.getOutputStream();
        InputStream in = listener.getInputStream();

        os.write(message.toString().getBytes("ASCII"));
        if (postData != null) {
            os.write(postData);
        }
        os.flush();
        WebResponseReader result = WebResponseReader.make(in, requestType);
        if (result.metaData.getContentLength() != -1) {
            if (result.stream instanceof limitedStream) {
                ((limitedStream) result.stream).addFinalizer(new Runnable() {
                    @Override
                    public void run() {
                        if (!listener.isClosed())
                            keepAliveSockets.put(host + ":" + 80, listener);
                    }
                });
            }
        }
        return result;
    }
}
