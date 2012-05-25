package de.catma.indexer.elasticsearch;

import java.util.UUID;

import org.json.JSONException;

public interface ESDocument {
	public String toJSON() throws JSONException;
	public UUID getIndexDocumentKey();
}
