package de.catma.indexer.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;

import de.catma.indexer.TermInfo;

public class ESCommunication {

	public String version = ESOptions.getString("ESInstaller.version");
	public String url = ESOptions.getString("ESInstaller.uri");
	public String indexName = ESOptions.getString("ESInstaller.name");
	public AsyncHttpClient httpTransport;
	private Logger logger = LoggerFactory.getLogger(ESCommunication.class);

	public ESCommunication() {
		AsyncHttpClientConfig config = 
				new AsyncHttpClientConfig.Builder()
				.setRequestTimeoutInMs(900000)
				.setMaximumConnectionsPerHost(-1)
				.setMaximumConnectionsTotal(-1)
				.setConnectionTimeoutInMs(900000)
				.build();
		this.httpTransport = new AsyncHttpClient(config);
	}

	public boolean addToIndex(String docId, Map<String, List<TermInfo>> terms)
			throws IllegalArgumentException, IOException, JSONException {
		List<Future<Response>> httpRequests = new ArrayList<Future<Response>>();

		for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
			ESTermIndexDocument termDoc = new ESTermIndexDocument(docId,entry.getKey(),entry.getValue().size());
			logger.info("termdocTermId: " + termDoc.getTermId());
			Future<Response> f = this.httpTransport
					.preparePut(this.termIndexUrl()+"/"+termDoc.getTermId().toString()).setBody(termDoc.toJSON())
					.execute();
			httpRequests.add(f);
		}

		/*
		 * This is to join results
		 */
		for (Future<Response> response : httpRequests) {
			try {
				Response r = response.get();
				logger.info(r.getResponseBody());
				if (r.getStatusCode() != 200) {
					logger.error("response was " + r.getStatusCode());
//					return false;
				}
			} catch (InterruptedException e) {
				logger.info("http request got interrupted: "
						+ response.toString());
			} catch (ExecutionException e) {
				logger.info("Couldn't execute http request: "
						+ response.toString());
			}
		}
		return true;
	}

	/**
	 * generates the full uri to the elastic search index
	 * 
	 * @return String the URI in form of http://<host>:<port>/indexname
	 */
	private String baseIndexUrl() {
		return this.url + "/" + this.indexName;
	}

	private String termIndexUrl() {
		return this.baseIndexUrl() + "/" + ESOptions.getString("ESInstaller.termindex");
	}
}
