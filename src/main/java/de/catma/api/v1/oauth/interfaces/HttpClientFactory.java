package de.catma.api.v1.oauth.interfaces;

import org.apache.http.impl.client.CloseableHttpClient;

public interface HttpClientFactory {

	CloseableHttpClient create();

}
