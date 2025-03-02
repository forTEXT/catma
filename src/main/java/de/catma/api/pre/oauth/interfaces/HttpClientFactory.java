package de.catma.api.pre.oauth.interfaces;

import org.apache.http.impl.client.CloseableHttpClient;

public interface HttpClientFactory {

	CloseableHttpClient create();

}
