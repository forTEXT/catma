package de.catma.elasticsearch;

import java.util.concurrent.Future;

import org.json.JSONException;
import org.json.JSONObject;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class ElasticsearchInstaller {

	public String version = "1";
	public String url = "http://clea.bsdsystems.de:9200";
	public String indexName;
	
	public ElasticsearchInstaller(){
		this.indexName="catmaIndexCollection"+ "-" + this.version;
	}
	
	/**
	 * generates the TagInstance to elasticsearch indexer
	 * @throws JSONException
	 * @author db
	 * @return {@link JSONObject} the actual tagInstanzIndex json object
	 */
	public String genTagInstanceIndex() throws JSONException{
		String tagInstanceIndex = "{" +
     "\"TagInstanceIndex\" : {"+
     "   \"properties\" : {"+
     "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\" }"+
     "       \"markupDocumentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"+
     "       \"tagId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"tagId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"tagInstanceId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"tagInstanceId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"properties\" : {\"type\" : \"string\", \"store\" : \"yes\"} , \"index\" : \"not_analyzed\""+
     "       \"characterStart\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"characterEnd\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "   }"+
     "}" +
	"}";
		return new JSONObject(tagInstanceIndex).toString();
	}
	
	/**
	 * generates the StaticMarkupIndex to elasticsearch indexer
	 * @throws JSONException
	 * @author db
	 * @return {@link JSONObject} the actual StaticMarkupIndex json object
	 */
	public String genStaticMarkupIndex() throws JSONException{
		String staticMarkupIndex = "{" +
     "\"StaticMarkupIndex\" : {"+
     "   \"properties\" : {"+
     "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"+
     "       \"markupDocumentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"+
     "       \"properties\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"+
     "       \"characterStart\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"characterEnd\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "   }"+
     "}" +
	"}";
		return new JSONObject(staticMarkupIndex).toString();
	}

	/**
	 * generates the TermIndex to elasticsearch indexer
	 * @throws JSONException
	 * @author db
	 * @return {@link JSONObject} the actual TermIndex json object
	 */
	public String genTermIndex() throws JSONException{
		String staticMarkupIndex = "{" +
     "\"TermIndex\" : {"+
     "   \"properties\" : {"+
     "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"+
     "       \"markupDocumentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"+
     "       \"termId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"termId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"frequency\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"term\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "   }"+
     "}" +
	"}";
		return new JSONObject(staticMarkupIndex).toString();
	}
	
	/**
	 * generates the PositionIndex to elasticsearch indexer
	 * @throws JSONException
	 * @author db
	 * @return {@link JSONObject} the actual PositionIndex json object
	 */
	public String genPositionIndex() throws JSONException{
		String positionIndex = "{" +
     "\"PositionIndex\" : {"+
     "   \"properties\" : {"+
     "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"+
     "       \"termId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"termId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"characterStart\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"characterEnd\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"tokenoffset\" : {\"type\" : \"integer\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "   }"+
     "}" +
	"}";
		return new JSONObject(positionIndex).toString();
	}
	
	/**
	 * generates the PositionIndex to elasticsearch indexer
	 * @throws JSONException
	 * @author db
	 * @return {@link JSONObject} the actual PositionIndex json object
	 */
	public String genWildcardIndex() throws JSONException{
		String wildcardIndex = "{" +
     "\"PositionIndex\" : {"+
     "   \"properties\" : {"+
     "       \"documentId\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"not_analyzed\"}"+
     "       \"termId_l\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"termId_m\" : {\"type\" : \"long\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"nGram\" : {\"type\" : \"string\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "       \"termIds\" : {\"type\" : \"binary\", \"store\" : \"yes\", \"index\" : \"yes\"}"+
     "   }"+
     "}" +
	"}";
		return new JSONObject(wildcardIndex).toString();
	}
	
	public String getUrl(){
		return this.url + "/" + this.indexName;
	}
	
	public static void main(String[] args) {
		ElasticsearchInstaller ei = new ElasticsearchInstaller();
		AsyncHttpClient a = new AsyncHttpClient();
	  //  Future<Response> f = a.preparePost(ei.getUrl() + "/" + "TermIndex" + "/" );
	  //  Response r = f.get();
	}
}
