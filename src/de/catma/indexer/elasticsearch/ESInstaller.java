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
			taskCreateTaginstanceIndex();
			taskCreateStaticMarkupIndex();
			taskCreatePositionIndex();
			taskCreateTermIndex();
			taskCreateWildcardIndex();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Creates the pure index with default options from elastic search.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws JSONException
	 */
	private void taskCreateIndex() throws IOException, InterruptedException,
			ExecutionException, JSONException {
		Future<Response> f = this.httpTransport.preparePut(this.baseIndexUrl())
				.execute();
		Response r = f.get();
		JSONObject result = new JSONObject(r.getResponseBody());
		System.out.println(result);
		// TODO: validate result and log with logger, throw exceptions!!
	}

	/**
	 * Creates the TagInstaceIndex on elastic search server
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws JSONException
	 */
	private void taskCreateTaginstanceIndex() throws IOException,
			InterruptedException, ExecutionException, JSONException {
		String tagInstanceIndex = "{"
				+ "\"TagInstanceIndex\" : {"
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\" },"
				+ "       \"markupDocumentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"tagId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"tagId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
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
										.getString("ESInstaller.taginstanceindex")
								+ "/_mapping/").setBody(tagInstanceIndex)
				.execute();
		Response r = f.get();
		JSONObject result = new JSONObject(r.getResponseBody());
		System.out.println(result);

		// TODO: validate result and log with logger, throw exceptions!!
	}

	/**
	 * Creates the StaticMarkupIndex on elastic search server
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws JSONException
	 */
	private void taskCreateStaticMarkupIndex() throws IOException,
			InterruptedException, ExecutionException, JSONException {
		String staticMarkupIndex = "{"
				+ "\"StaticMarkupIndex\" : {"
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
		JSONObject result = new JSONObject(r.getResponseBody());
		System.out.println(result);

		// TODO: validate result and log with logger, throw exceptions!!
	}

	/**
	 * Creates the TermIndex on elastic search server
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws JSONException
	 */
	private void taskCreateTermIndex() throws IOException,
			InterruptedException, ExecutionException, JSONException {
		String termindex = "{"
				+ "\"TermIndex\" : {"
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"markupDocumentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"frequency\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"term\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"
				+ "   }" + "}" + "}";
		Future<Response> f = this.httpTransport
				.preparePut(
						baseIndexUrl()
								+ ESOptions
										.getString("ESInstaller.termindex")
								+ "/_mapping/").setBody(termindex)
				.execute();
		Response r = f.get();
		JSONObject result = new JSONObject(r.getResponseBody());
		System.out.println(result);

		// TODO: validate result and log with logger, throw exceptions!!
	}


	/**
	 * Creates the PositionIndex on elastic search server
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws JSONException
	 */
	private void taskCreatePositionIndex() throws IOException,
			InterruptedException, ExecutionException, JSONException {
		String positionindex = "{"
				+ "\"PositionIndex\" : {"
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
		JSONObject result = new JSONObject(r.getResponseBody());
		System.out.println(result);

		// TODO: validate result and log with logger, throw exceptions!!
	}

	/**
	 * Creates the WildcardIndex on elastic search server
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws JSONException
	 */
	private void taskCreateWildcardIndex() throws IOException,
			InterruptedException, ExecutionException, JSONException {
		String wildcardindex = "{"
				+ "\"PositionIndex\" : {"
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
		JSONObject result = new JSONObject(r.getResponseBody());
		System.out.println(result);

		// TODO: validate result and log with logger, throw exceptions!!
	}
	
	}
	/**
	 * generates the full uri to the elastic search index
	 * @return String the URI in form of http://<host>:<port>/indexname
	 */
	private String baseIndexUrl() {
		return this.url + "/" + this.indexName;
	}
	
	/**
	 * Closes the async http transport client
	 */
	public void close(){
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
