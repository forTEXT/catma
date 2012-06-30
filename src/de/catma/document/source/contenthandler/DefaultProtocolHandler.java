package de.catma.document.source.contenthandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocumentHandler;

public class DefaultProtocolHandler implements ProtocolHandler {
	private String mimeType;
	private String encoding;
	private byte[] byteContent;
	
	public DefaultProtocolHandler(URI sourceDocURI, String mimeType) 
			throws IOException {
		this.mimeType = mimeType;
		handle(sourceDocURI);
	}
	
	private void handle(URI sourceDocURI) throws IOException {
		
		final String sourceDocURL = 
				sourceDocURI.toURL().toString();
		final String sourceURIPath = 
				sourceDocURI.getPath();
		
		SourceDocumentHandler sourceDocumentHandler = 
				new SourceDocumentHandler();
		
		URLConnection urlConnection = 
				new URL(sourceDocURL).openConnection();

		InputStream is = urlConnection.getInputStream();
		try {
			this.byteContent = IOUtils.toByteArray(is);
			if (this.mimeType == null) {
				this.mimeType = 
						sourceDocumentHandler.getMimeType(
								sourceURIPath, urlConnection, 
								FileType.TEXT.getMimeType());
			}
			
			this.encoding = Charset.defaultCharset().name();
			
			if (this.mimeType.equals(FileType.TEXT.getMimeType())
					||(this.mimeType.equals(FileType.HTML.getMimeType()))) {
				this.encoding = 
						sourceDocumentHandler.getEncoding(
								urlConnection, 
								byteContent, 
								Charset.defaultCharset().name());	
			}
		}
		finally {
			is.close();
		}
	
	}

	public byte[] getByteContent() {
		return this.byteContent;
	}

	public String getEncoding() {
		return this.encoding;
	}

	public String getMimeType() {
		return this.mimeType;
	}

}
