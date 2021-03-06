package ru.ith.lib.webcrawl.providers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;

import ru.ith.lib.webcrawl.WebResponseMetadata;
import ru.ith.lib.webcrawl.WebResponseReader;

public class HTMLResponse extends WebResponseReader {
	private final Document d;

	public HTMLResponse(WebResponseMetadata metaData, InputStream stream) throws IOException {
		super(metaData, stream);
		d = Jsoup.parse(stream, metaData.getEncoding(), "");
	}

	public Elements getAll(String path) throws IOException {
		return d.select(path);
	}

	@Override
	public String toString() {
		return d.toString();
	}
}
