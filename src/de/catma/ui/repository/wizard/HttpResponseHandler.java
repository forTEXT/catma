package de.catma.ui.repository.wizard;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.BasicResponseHandler;

public class HttpResponseHandler extends BasicResponseHandler {

	private String encoding;

	@Override
	public String handleResponse(HttpResponse response)
			throws HttpResponseException, IOException {
		System.out.println(response.getEntity());
		System.out.println(response.getEntity().getContentEncoding());
		Header[] headers = response.getHeaders("Content-Type");
		System.out.println(headers[0].getValue());
		String responseBody = super.handleResponse(response);
		return responseBody;
	}
	
	public String getEncoding() {
		return encoding;
	}
}

