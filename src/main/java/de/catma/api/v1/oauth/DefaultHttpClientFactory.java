package de.catma.api.v1.oauth;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import de.catma.api.v1.oauth.interfaces.HttpClientFactory;

public class DefaultHttpClientFactory implements HttpClientFactory {

	@Override
	public CloseableHttpClient create() {
		return HttpClients.createDefault();
	}

}
