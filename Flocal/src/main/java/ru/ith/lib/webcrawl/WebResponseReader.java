package ru.ith.lib.webcrawl;

import java.io.IOException;
import java.io.InputStream;

import ru.ith.lib.webcrawl.providers.BinaryResponse;
import ru.ith.lib.webcrawl.providers.HEADResponse;
import ru.ith.lib.webcrawl.providers.ProviderEnum;
import ru.ith.lib.webcrawl.providers.HTMLResponce;

public abstract class WebResponseReader {
	public final WebResponseMetadata metaData;

	protected WebResponseReader(WebResponseMetadata metaData, InputStream stream) {
		this.metaData = metaData;
	}
	
	public static WebResponseReader make(InputStream stream, ProviderEnum provider)
			throws IOException {
		int bufSize = 20;
		byte[] headerBuf = new byte[bufSize];
		boolean isHeaderBlock = true;
		int b;
		WebResponseMetadata metaData = new WebResponseMetadata();
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
		case WebResponseMetadata.HTTP_OK:
			break;
		case WebResponseMetadata.MOVED_PERMANENTLY:
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
		case HEAD:
			return new HEADResponse(metaData, stream);
		case BINARY:
			return new BinaryResponse(metaData, stream);
		default:
			throw new RuntimeException("Unsupported reader requested");
		}
	}
}
