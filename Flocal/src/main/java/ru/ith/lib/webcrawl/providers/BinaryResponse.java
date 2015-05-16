package ru.ith.lib.webcrawl.providers;

import java.io.InputStream;

import ru.ith.lib.webcrawl.WebResponseMetadata;
import ru.ith.lib.webcrawl.WebResponseReader;

/**
 * Created by adminfthi on 02.07.13.
 */
public class BinaryResponse extends WebResponseReader {

	public BinaryResponse(WebResponseMetadata metaData, InputStream stream) {
		super(metaData, stream);
	}

	public InputStream getStream() {
		return stream;
	}
}


