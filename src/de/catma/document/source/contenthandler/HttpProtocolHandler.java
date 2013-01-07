package de.catma.document.source.contenthandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocumentHandler;
import de.catma.util.CloseSafe;

public class HttpProtocolHandler implements ProtocolHandler {
	
	private byte[] byteContent;
	private String encoding;
	private String mimeType;
	
	public HttpProtocolHandler(URI sourceDocURI, String sourceDocumentFileUri) throws IOException {
		handle(sourceDocURI, sourceDocumentFileUri);
	}

	private void handle(URI sourceDocURI, String sourceDocumentFileUri) throws IOException {
		SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler();
		
		HttpClient httpclient = new DefaultHttpClient();
		try {
			HttpGet httpGet = new HttpGet(sourceDocURI);
			
			HttpResponseHandler responseHandler = new HttpResponseHandler();
			
			this.byteContent = httpclient.execute(httpGet, responseHandler);

			this.encoding = sourceDocumentHandler.getEncoding(
	        		responseHandler.getEncoding(), responseHandler.getContentType(), 
	        		byteContent,
	        		Charset.defaultCharset().name());
	        this.mimeType = sourceDocumentHandler.getMimeType(
	        		sourceDocURI.getPath(), 
	        		responseHandler.getContentType(), 
	        		FileType.TEXT.getMimeType());
	        try {
		        File tempFile = new File(new URI(sourceDocumentFileUri));
		        if (tempFile.exists()) {
		        	tempFile.delete();
		        }
		        
		        tempFile.createNewFile();
		        
		        FileOutputStream fos = new FileOutputStream(tempFile);
		        try {
		        	IOUtils.copy(new ByteArrayInputStream(byteContent), fos);
		        }
		        finally {
		        	CloseSafe.close(fos);
		        }
	        }
	        catch (URISyntaxException se) {
	        	throw new IOException(se);
	        }
		}
		finally {
			httpclient.getConnectionManager().shutdown();
		}
        
	}

	public byte[] getByteContent() {
		return byteContent;
	}

	public String getEncoding() {
		return encoding;
	}

	public String getMimeType() {
		return mimeType;
	}

}
