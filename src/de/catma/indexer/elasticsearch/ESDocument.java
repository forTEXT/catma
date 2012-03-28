package de.catma.indexer.elasticsearch;

import org.json.JSONException;

public interface ESDocument {
	public String toJSON() throws JSONException;
}
