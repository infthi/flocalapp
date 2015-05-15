package ru.ith.lib.webcrawl.providers;

import java.io.InputStream;

import ru.ith.lib.webcrawl.WebResponseMetadata;
import ru.ith.lib.webcrawl.WebResponseReader;
import ru.ith.lib.webcrawl.limitedStream;

/**
 * Created by adminfthi on 02.07.13.
 */
public class BinaryResponse extends WebResponseReader {
	private final InputStream mStream;

	public BinaryResponse(WebResponseMetadata metaData, InputStream stream) {
		super(metaData, stream);
		mStream = new limitedStream(stream, metaData.getContentLength());
	}

	public InputStream getStream() {
		return mStream;
	}
}


