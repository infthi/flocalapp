package ru.ith.lib.webcrawl;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import ru.ith.lib.flocal.FLDataLoader;
import ru.ith.lib.webcrawl.providers.BinaryResponse;
import ru.ith.lib.webcrawl.providers.HEADResponse;
import ru.ith.lib.webcrawl.providers.HTMLResponse;
import ru.ith.lib.webcrawl.providers.ProviderEnum;

public abstract class WebResponseReader {
	public final WebResponseMetadata metaData;
	public final InputStream stream;

	protected WebResponseReader(WebResponseMetadata metaData, InputStream stream) {
		this.metaData = metaData;
		this.stream = stream;
	}

	public static WebResponseReader make(HttpURLConnection conn, ProviderEnum provider)
			throws IOException {
		switch (conn.getResponseCode()) {
			case 200:
				break;
			default:
				throw new IOException("Unsupported server responce code: " + conn.getResponseCode());
		}

		WebResponseMetadata metaData = new WebResponseMetadata(conn);
		Log.d(FLDataLoader.FLOCAL_APP_SIGN, "Header parsed");

		InputStream stream = conn.getInputStream();

//		if (conn.getContentLength()>-1){
//			stream = new limitedStream(stream, conn.getContentLength());
//		}

		switch (provider) {
			case HTML:
				return new HTMLResponse(metaData, stream);
			case HEAD:
				return new HEADResponse(metaData, stream);
			case BINARY:
				return new BinaryResponse(metaData, stream);
			default:
				throw new RuntimeException("Unsupported reader requested");
		}
	}
}
