package de.catma.api.pre.oauth;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import de.catma.api.pre.oauth.interfaces.HttpClientFactory;

public class DefaultHttpClientFactory implements HttpClientFactory {

	@Override
	public CloseableHttpClient create() {
		return HttpClients.createDefault();
	}

}
