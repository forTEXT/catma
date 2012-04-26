package de.catma.indexer.elasticsearch;

import org.json.JSONException;
import org.json.JSONObject;


public interface ESDocumentFactory<T> {

    public T fromJSON(JSONObject jsonobject) throws JSONException;
	
}
