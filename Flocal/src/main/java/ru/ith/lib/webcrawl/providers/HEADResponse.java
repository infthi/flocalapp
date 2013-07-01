package ru.ith.lib.webcrawl.providers;

import java.io.IOException;
import java.io.InputStream;

import ru.ith.lib.webcrawl.WebResponseMetadata;
import ru.ith.lib.webcrawl.WebResponseReader;

/**
 * Created by adminfthi on 02.07.13.
 */
public class HEADResponse extends WebResponseReader {
	public HEADResponse(WebResponseMetadata metaData, InputStream stream) {
		super(metaData, stream);
		try {
			stream.close();
		} catch (IOException e) {
		}
	}
}
