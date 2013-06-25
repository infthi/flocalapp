package ru.ith.lib.webcrawl.providers;

import java.io.IOException;
import java.io.InputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import ru.ith.lib.webcrawl.WebResponceReader;
import ru.ith.lib.webcrawl.WebResponceMetadata;

public class HTMLResponce extends WebResponceReader {
	private final Document d;

	public HTMLResponce(WebResponceMetadata metaData, InputStream stream) throws IOException {
		super(metaData, stream);
		d = Jsoup.parse(stream, metaData.getEncoding(), "");
	}

	public Elements getAll(String path) throws IOException{
		return d.select(path);
	}

	@Override
	public String toString() {
		return d.toString();
	}
}
