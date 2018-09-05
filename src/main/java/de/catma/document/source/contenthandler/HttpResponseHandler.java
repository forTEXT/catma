/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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

