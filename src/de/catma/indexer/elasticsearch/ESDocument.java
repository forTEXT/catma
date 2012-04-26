package de.catma.indexer.elasticsearch;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

public interface ESDocument<T> {
	public String toJSON() throws JSONException;
	public UUID getIndexDocumentKey();
}
