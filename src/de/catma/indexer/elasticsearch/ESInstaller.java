package de.catma.indexer.elasticsearch;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

/**
 * Installs the required catma indices on the elastic search server.
 * @author db <db@bsdsystems.de>
 *
 */
public class ESInstaller {

	public String version = ESOptions.getString("ESInstaller.version");
	public String url = ESOptions.getString("ESInstaller.uri");
	public String indexName = ESOptions.getString("ESInstaller.name");
	public AsyncHttpClient httpTransport;
	private Logger logger = LoggerFactory.getLogger(ESInstaller.class);

	public ESInstaller() {
		this.httpTransport = new AsyncHttpClient();
	}

	public void setup() {
		try {
			taskCreateIndex();
			taskCreateTagRefernceIndex();
			taskCreateStaticMarkupIndex();
			taskCreatePositionIndex();
			taskCreateTermIndex();
			taskCreateWildcardIndex();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Creates the pure index with default options from elastic search.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void taskCreateIndex() throws IOException, InterruptedException,
			ExecutionException {
		Future<Response> f = this.httpTransport.preparePut(this.baseIndexUrl())
				.execute();
		Response r = f.get();
		String result = r.getResponseBody();
		if (!validateResponse(result)) {
			throw new IOException("Couldn't create index error: " + result);
		}
		logger.info("Index successfully created");
	}

	/**
	 * Creates the TagInstaceIndex on elastic search server
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void taskCreateTagRefernceIndex() throws IOException,
			InterruptedException, ExecutionException {
		String tagReferenceIndex = "{"
				+ "\"TagReferenceIndex\" : {"
				+ "       \"_id\" : {\"store\" : \"yes\", \"index\" : \"not_analyzed\"},"	
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\" },"
				+ "       \"userMarkupCollectionId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"tagId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"tagId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"tagPath\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"tagInstanceId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"tagInstanceId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"properties\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"characterStart\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"characterEnd\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"
				+ "   }" + "}" + "}";
	
		Future<Response> f = this.httpTransport
				.preparePut(
						baseIndexUrl()
								+ ESOptions
										.getString("ESInstaller.tagreferenceindex")
								+ "/_mapping/").setBody(tagReferenceIndex)
				.execute();
		Response r = f.get();
		String result = r.getResponseBody();
		if (!validateResponse(result)) {
			throw new IOException("Couldn't create TagReferenceIndex: " + result);
		}
		logger.info("TagReferenceIndex successfully created");
	}

	/**
	 * Creates the StaticMarkupIndex on elastic search server
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void taskCreateStaticMarkupIndex() throws IOException,
			InterruptedException, ExecutionException {
		String staticMarkupIndex = "{"
				+ "\"StaticMarkupIndex\" : {"
				+ "       \"_id\" : {\"store\" : \"yes\", \"index\" : \"not_analyzed\"},"	
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"markupDocumentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"properties\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"characterStart\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"characterEnd\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"
				+ "   }" + "}" + "}";
		Future<Response> f = this.httpTransport
				.preparePut(
						baseIndexUrl()
								+ ESOptions
										.getString("ESInstaller.staticmarkupindex")
								+ "/_mapping/").setBody(staticMarkupIndex)
				.execute();
		Response r = f.get();
		String result = r.getResponseBody();
		if (!validateResponse(result)) {
			throw new IOException("Couldn't create StaticMarkupIndex: "
					+ result);
		}
		logger.info("StaticMarkupIndex successfully created");
	}

	/**
	 * Creates the TermIndex on elastic search server
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void taskCreateTermIndex() throws IOException,
			InterruptedException, ExecutionException {
		String termindex = "{"
				+ "\"TermIndex\" : {"
				+ "       \"_id\" : {\"store\" : \"yes\", \"index\" : \"not_analyzed\"},"	
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"frequency\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"term\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"
				+ "   }" + "}" + "}";
		Future<Response> f = this.httpTransport
				.prepareDelete(
						baseIndexUrl()
								+ ESOptions.getString("ESInstaller.termindex")).execute();
		f.get();
		f = this.httpTransport
				.preparePut(
						baseIndexUrl()
								+ ESOptions.getString("ESInstaller.termindex")
								+ "/_mapping/").setBody(termindex).execute();
		Response r = f.get();
		String result = r.getResponseBody();
		if (!validateResponse(result)) {
			throw new IOException("Couldn't create TermIndex: " + result);
		}
		logger.info("TermIndex successfully created");
	}

	/**
	 * Creates the PositionIndex on elastic search server
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void taskCreatePositionIndex() throws IOException,
			InterruptedException, ExecutionException {
		String positionindex = "{"
				+ "\"PositionIndex\" : {"
				+ "       \"_id\" : {\"store\" : \"yes\", \"index\" : \"not_analyzed\"},"	
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\" },"
				+ "       \"termId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"characterStart\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"characterEnd\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"tokenoffset\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"
				+ "   }" + "}" + "}";
		Future<Response> f = this.httpTransport
				.preparePut(
						baseIndexUrl()
								+ ESOptions
										.getString("ESInstaller.positionindex")
								+ "/_mapping/").setBody(positionindex)
				.execute();
		Response r = f.get();
		String result = r.getResponseBody();
		if (!validateResponse(result)) {
			throw new IOException("Couldn't create PositionIndex: " + result);
		}
		logger.info("PositionIndex successfully created");
	}

	/**
	 * Creates the WildcardIndex on elastic search server
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void taskCreateWildcardIndex() throws IOException,
			InterruptedException, ExecutionException {
		String wildcardindex = "{"
				+ "\"WildcardIndex\" : {"
				+ "       \"_id\" : {\"store\" : \"yes\", \"index\" : \"not_analyzed\"},"	
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"nGram\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termIds\" : {\"type\" : \"binary\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"
				+ "   }" + "}" + "}";

		Future<Response> f = this.httpTransport
				.preparePut(
						baseIndexUrl()
								+ ESOptions
										.getString("ESInstaller.wildcardindex")
								+ "/_mapping/").setBody(wildcardindex)
				.execute();
		Response r = f.get();
		String result = r.getResponseBody();
		if (!validateResponse(result)) {
			throw new IOException("Couldn't create WildcardIndex: " + result);
		}
		logger.info("WildcardIndex successfully created");

	}

	/**
	 * Validates the elasticsearch response codes.
	 * It is true if one of the following conditions is true:
	 * <ul>
	 * 	<li>response 200 and content ok : true 
	 *  <li>response 200 and content acknowledged: true
	 *  <li>response 400 and content error "IndexAlreadyExistsException"
	 * </ul>
	 * @param response the responsestring in JSONsyntax
	 * @return boolean result
	 */
	private boolean validateResponse(String response) {
		JSONObject result;
		try {
			result = new JSONObject(response);
		} catch (JSONException e) {
			return false;
		}
		try {
			if (result.has("ok") && result.getBoolean("ok") == true) {
				return true;
			}
			if (result.has("ok") && result.getBoolean("ok") == true) {
				return true;
			}
			if (result.has("error")) {
				String error = result.getString("error");
				if (error.contains("IndexAlreadyExistsException")) {
					return true;
				}
			}
		} catch (JSONException e) {
			return false;
		}
		return false;
	}

	/**
	 * generates the full uri to the elastic search index
	 * 
	 * @return String the URI in form of http://<host>:<port>/indexname
	 */
	private String baseIndexUrl() {
		return this.url + "/" + this.indexName;
	}

	/**
	 * Closes the async http transport client
	 */
	public void close() {
		this.httpTransport.close();
	}

	/**
	 * When you forgot to close. I'll close on GC
	 */
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	public static void main(String[] args) {
		ESInstaller installer = new ESInstaller();
		installer.setup();
		installer.close();
	}
}
