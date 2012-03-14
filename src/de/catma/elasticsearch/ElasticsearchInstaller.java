package de.catma.elasticsearch;

import java.util.concurrent.Future;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gwt.dev.util.Either;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class ElasticsearchInstaller {

	public String version = "1";
	public String url = "http://clea.bsdsystems.de:9200";
	public String indexName;

	public ElasticsearchInstaller() {
		this.indexName = "catma_index_collection";
	}

	/**
	 * generates the TagInstance to elasticsearch indexer
	 * 
	 * @throws JSONException
	 * @author db
	 * @return {@link JSONObject} the actual tagInstanzIndex json object
	 */
	public String genTagInstanceIndex() throws JSONException {
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
		return new JSONObject(tagInstanceIndex).toString();
	}

	/**
	 * generates the StaticMarkupIndex to elasticsearch indexer
	 * 
	 * @throws JSONException
	 * @author db
	 * @return {@link JSONObject} the actual StaticMarkupIndex json object
	 */
	public String genStaticMarkupIndex() throws JSONException {
		String staticMarkupIndex = "{"
				+ "\"StaticMarkupIndex\" : {"
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"markupDocumentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"properties\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"characterStart\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"characterEnd\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "   }" + "}" + "}";
		return new JSONObject(staticMarkupIndex).toString();
	}

	/**
	 * generates the TermIndex to elasticsearch indexer
	 * 
	 * @throws JSONException
	 * @author db
	 * @return {@link JSONObject} the actual TermIndex json object
	 */
	public String genTermIndex() throws JSONException {
		String staticMarkupIndex = "{"
				+ "\"TermIndex\" : {"
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"markupDocumentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"frequency\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"term\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"
				+ "   }" + "}" + "}";
		return new JSONObject(staticMarkupIndex).toString();
	}

	/**
	 * generates the PositionIndex to elasticsearch indexer
	 * 
	 * @throws JSONException
	 * @author db
	 * @return {@link JSONObject} the actual PositionIndex json object
	 */
	public String genPositionIndex() throws JSONException {
		String positionIndex = "{"
				+ "\"PositionIndex\" : {"
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\" },"
				+ "       \"termId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"characterStart\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"characterEnd\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"tokenoffset\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"
				+ "   }" + "}" + "}";
		System.out.println(positionIndex);
		return new JSONObject(positionIndex).toString();
	}

	/**
	 * generates the PositionIndex to elasticsearch indexer
	 * 
	 * @throws JSONException
	 * @author db
	 * @return {@link JSONObject} the actual PositionIndex json object
	 */
	public String genWildcardIndex() throws JSONException {
		String wildcardIndex = "{"
				+ "\"PositionIndex\" : {"
				+ "   \"properties\" : {"
				+ "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"nGram\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"},"
				+ "       \"termIds\" : {\"type\" : \"binary\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"
				+ "   }" + "}" + "}";
		return new JSONObject(wildcardIndex).toString();
	}
	
	public String getUrl() {
		return this.url + "/" + this.indexName;
	}

	public static void main(String[] args) throws Exception {
		ElasticsearchInstaller ei = new ElasticsearchInstaller();
	    AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
	    Future<Response> f = asyncHttpClient.preparePut(ei.getUrl())
	    		.execute();
	    Response r = f.get();
	    System.out.println(r.getResponseBody());

	    f = asyncHttpClient.preparePut(ei.getUrl() + "/positionIndex/_mapping/")
	    		.setBody(ei.genPositionIndex())
	    		.execute();
	    r = f.get();
	    asyncHttpClient.close();
	    System.out.println(r.getResponseBody());
	}
}
