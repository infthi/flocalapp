package ru.ith.lib.webcrawl.providers;

import java.io.IOException;
import java.io.InputStream;

import ru.ith.lib.webcrawl.WebResponseMetadata;
import ru.ith.lib.webcrawl.WebResponseReader;

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

class limitedStream extends InputStream {
	private final InputStream wrapped;
	private long remaining;
	private long size;

	limitedStream(InputStream wrapped, long size) {
		this.wrapped = wrapped;
		this.remaining = this.size = size;
	}

	@Override
	public int read() throws IOException {
		if (remaining==0)
			return -1;
		remaining--;
		return wrapped.read();
	}
}
