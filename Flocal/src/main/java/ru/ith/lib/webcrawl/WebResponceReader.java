package ru.ith.lib.webcrawl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import ru.ith.lib.webcrawl.providers.ProviderEnum;
import ru.ith.lib.webcrawl.providers.HTMLResponce;

public abstract class WebResponceReader {
	public final WebResponceMetadata metaData;

	protected WebResponceReader(WebResponceMetadata metaData, InputStream stream) {
		this.metaData = metaData;
	}
	
	public static WebResponceReader make(InputStream stream, ProviderEnum provider)
			throws IOException {
		int bufSize = 20;
		byte[] headerBuf = new byte[bufSize];
		boolean isHeaderBlock = true;
		int b;
		WebResponceMetadata metaData = new WebResponceMetadata();
		while (isHeaderBlock) {
			int index = 0;
			while ((b = stream.read()) != -1) {
				if (b == '\r')
					continue;
				if (b == '\n')
					break;
				headerBuf[index++] = (byte) b;
				if (index == bufSize) {
					byte[] newBuf = new byte[bufSize *= 2];
					System.arraycopy(headerBuf, 0, newBuf, 0, index);
					headerBuf = newBuf;
				}
			}
			if (index == 0)
				isHeaderBlock = false;
			else
				metaData.processHeader(new String(headerBuf, 0, index, "ASCII"));
		}
		switch (metaData.getCode()){
		case WebResponceMetadata.HTTP_OK:
			break;
		case WebResponceMetadata.MOVED_PERMANENTLY:
//			try {
//				URI location = metaData.getRedirect();
//			} catch (URISyntaxException e) {
//				throw new IOException("Server provided wrong redirection");
//			}
		default:
			throw new IOException("Unsupported server responce code: "+metaData.getCode());
		}
		
		switch (provider) {
		case HTML:
			return new HTMLResponce(metaData, stream);
		default:
			throw new RuntimeException("Unsupported reader requested");
		}
	}
}
