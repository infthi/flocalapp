package ru.ith.lib.webcrawl;

import java.io.IOException;
import java.io.InputStream;

public class limitedStream extends InputStream {
	private final InputStream wrapped;
	private volatile long remaining;
	private long size;
	private volatile Runnable finalizer = null;

	public limitedStream(InputStream wrapped, long size) {
		this.wrapped = wrapped;
		this.remaining = this.size = size;
	}

	@Override
	public int read() throws IOException {
		if (remaining == 0) { //TODO: check concurrency,
			if (finalizer != null) {
				finalizer.run();
				finalizer = null;
			}
			return -1;
		}
		remaining--;
		return wrapped.read();
	}

	public void addFinalizer(Runnable r) {
		if (remaining == 0)
			r.run();
		else
			finalizer = r;
	}
}
