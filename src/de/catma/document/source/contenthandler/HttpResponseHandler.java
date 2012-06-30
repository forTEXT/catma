package de.catma.document.source.contenthandler;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

public class HttpResponseHandler implements ResponseHandler<byte[]> {

	private String encoding = null;
	private String contentType;
	
	public byte[] handleResponse(HttpResponse response)
			throws ClientProtocolException, IOException {
		Header[] headers = response.getHeaders("Content-Type");
		if (headers.length > 0) {
			contentType = headers[0].getValue();
		}
		// TODO: Content-Encoding, hanle compressed content
		
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        if (statusLine.getStatusCode() >= 300) {
        	try {
        		EntityUtils.toByteArray(entity);
        	}
        	catch(IOException notOfInterest) {}
            throw new HttpResponseException(statusLine.getStatusCode(),
                    statusLine.getReasonPhrase());
        }
        return entity == null ? null : EntityUtils.toByteArray(entity);
	}
	
	public String getEncoding() {
		return encoding;
	}
	
	public String getContentType() {
		return contentType;
	}
}

