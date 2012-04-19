package de.catma.indexer.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import com.ning.http.client.extra.ThrottleRequestFilter;

import de.catma.indexer.TermInfo;

public class ESCommunication {

	public String version = ESOptions.getString("ESInstaller.version");
	public String url = ESOptions.getString("ESInstaller.uri");
	public String indexName = ESOptions.getString("ESInstaller.name");
	public AsyncHttpClient httpTransport;
	private static Logger logger = LoggerFactory
			.getLogger(ESCommunication.class);

	public ESCommunication() {
		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
				.setRequestTimeoutInMs(900000).setMaximumConnectionsPerHost(-1)
				.setConnectionTimeoutInMs(900000).setMaxRequestRetry(5)
				.addRequestFilter(new ThrottleRequestFilter(50, 900000))
				.setMaximumConnectionsTotal(-1).build();

		this.httpTransport = new AsyncHttpClient(config);
	}

	public boolean indexTerms(String docId, Map<String, List<TermInfo>> terms)
			throws IllegalArgumentException, IOException, JSONException {
		List<Future<Response>> httpRequests = new ArrayList<Future<Response>>();

		for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
			ESTermIndexDocument termDoc = new ESTermIndexDocument(docId,
					entry.getKey(), entry.getValue().size());
			logger.info("termdocTermId: " + termDoc.getTermId());
			Future<Response> f = this.httpTransport
					.preparePut(
							this.termIndexUrl() + "/"
									+ termDoc.getTermId().toString())
					.setBody(termDoc.toJSON()).execute();
			httpRequests.add(f);
			for (TermInfo terminfo : entry.getValue()) {
				ESPositionIndexDocument positionDoc = new ESPositionIndexDocument(
						docId, termDoc.getTermId(), terminfo);
				logger.info("termdocPositionId: " + positionDoc.getPositionId());
				Future<Response> fp = this.httpTransport
						.preparePut(
								this.positionIndexUrl()
										+ "/"
										+ positionDoc.getPositionId()
												.toString())
						.setBody(positionDoc.toJSON()).execute();
				httpRequests.add(fp);
			}
		}
		
		waitForRequests(httpRequests);

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

	public String termIndexUrl() {
		return this.baseIndexUrl() + "/"
				+ ESOptions.getString("ESInstaller.termindex");
	}

	public String positionIndexUrl() {
		return this.baseIndexUrl() + "/"
				+ ESOptions.getString("ESInstaller.positionindex");
	}

	public String tagReferenceIndexUrl() {
		return this.baseIndexUrl() + "/"
				+ ESOptions.getString("ESInstaller.tagreferenceindex");
	}

	public static boolean waitForRequests(
			Collection<Future<Response>> collection) {
		/*
		 * This is to join results
		 */
		boolean result = true;
		
		for (Future<Response> response : collection) {
			try {
				Response r = response.get();
				if (r.getStatusCode() >= 400) {
					logger.error("response was " + r.getStatusCode());
					result = false;
				}
			} catch (InterruptedException e) {
				logger.info("http request got interrupted: "
						+ response.toString());
			} catch (ExecutionException e) {
				logger.info("Couldn't execute http request: "
						+ response.toString());
			}
		}
		return result;
	}

	public boolean indexTagReferences(
			List<ESTagReferenceDocument> esTagReferences) throws IllegalArgumentException, IOException,
			JSONException {
		List<Future<Response>> httpRequests = new ArrayList<Future<Response>>();

		for (ESTagReferenceDocument esTagReference : esTagReferences) {
			logger.info("indexing tagReference: " + esTagReference);
			Future<Response> f = this.httpTransport
					.preparePut(
							this.tagReferenceIndexUrl()
									+ "/"
									+ esTagReference.getTagReferenceId()
											.toString())
					.setBody(esTagReference.toJSON()).execute();
			httpRequests.add(f);
		}

		waitForRequests(httpRequests);

		return true;

	}
	
	/**
	 * Closes the async http transport client
	 */
	public void close() {
		this.httpTransport.close();
	}
}
