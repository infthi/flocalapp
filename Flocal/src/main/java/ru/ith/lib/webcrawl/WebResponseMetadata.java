package ru.ith.lib.webcrawl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class WebResponseMetadata {
	public static final int HTTP_OK = 200;
	public static final int MOVED_PERMANENTLY = 301;
    public static final int FOUND = 302;

	private int headerCount = 0;
	private long lastModified = -1;
	private String method;
	private int HTTPCode;
	private String encoding = "UTF8";
	private String redirect = null;
	private Map<String, String> cookies = new TreeMap<String, String>();
	private static final SimpleDateFormat LAST_MODIFIED_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    private long mContentLength = -1;

    public void processHeader(String header) throws IOException{
		if (headerCount++==0){
			processHTTPHeader(header);
			return;
		}
		String[]data = header.split(": ", 2);
		if (data.length!=2)
			throw new IOException("Malformed server responce");//TODO: details
		if (data[0].equalsIgnoreCase("Content-type")){
			String[]parts = data[1].split("; ");
			for (String part: parts){
				if (part.startsWith("charset="))
					encoding = part.substring(8);
			}
		} else if (data[0].equalsIgnoreCase("Location"))
			redirect = data[1];
		else if (data[0].equalsIgnoreCase("Set-Cookie")) {
			String cookieData = data[1].split(";", 2)[0];
			String[] cookie = cookieData.split("=", 2);
			cookies.put(cookie[0], cookie[1]);
		} else if (data[0].equalsIgnoreCase("Last-Modified")){
			synchronized (LAST_MODIFIED_FORMAT) {
				try {
					Date d = LAST_MODIFIED_FORMAT.parse(data[1]);
					lastModified = d.getTime();
				} catch (ParseException e) {
				}
			}
		} else if (data[0].equalsIgnoreCase("Content-Length")){
			mContentLength = Long.valueOf(data[1]);
		}
	}

	private void processHTTPHeader(String header) throws IOException {
		String[] data = header.split(" ",3);
		if (data.length!=3)
			throw new IOException("Malformed server responce");
		method = data[0];
		try{
			HTTPCode = Integer.valueOf(data[1]);
		} catch (NumberFormatException e){
			throw new IOException("Malformed server responce");
		}
	}

	public String getEncoding() {
		return encoding;
	}
	
	public URI getRedirect() throws URISyntaxException{
		if (redirect==null)
			return null;
		return new URI(redirect);
	}

	public int getCode() {
		return HTTPCode;
	}

	public String getCookie(String name) {
		return cookies.get(name);
	}

	public long getLastModified(){
		return lastModified;
	}

	public long getContentLength() {
		return mContentLength;
	}
}
