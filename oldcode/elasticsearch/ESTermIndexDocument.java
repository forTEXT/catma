package de.catma.indexer.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

public class ESTermIndexDocument implements ESDocument {

	private String documentId;
	private UUID termId;
	private int frequency;
	private String term;
	private List<ESPositionIndexDocument> positions;

	public ESTermIndexDocument(String documentId, String term, int frequency) throws NullPointerException {
		this.documentId = documentId;
		this.frequency = frequency;
		this.term = term;
		this.termId = getIndexDocumentKey();
		this.positions = new ArrayList<ESPositionIndexDocument>();
	}

	public String getDocumentId() {
		return documentId;
	}

	public int getFrequency() {
		return frequency;
	}

	public String getTerm() {
		return term;
	}

	public UUID getTermId() {
		return termId;
	}

	public String toJSON() throws JSONException {
		JSONObject j_root = new JSONObject();
		j_root.put("documentId", documentId);
		j_root.put("frequency", frequency);
		j_root.put("term", term);
		return j_root.toString();
	}

	public UUID getIndexDocumentKey() throws NullPointerException {
		if(documentId == null)
			throw new NullPointerException("documentId is empty!");
		if(term == null || term.equals(""))
			throw new NullPointerException("term is empty!");
		return UUID.nameUUIDFromBytes((documentId + term).getBytes());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((termId == null) ? 0 : termId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ESTermIndexDocument other = (ESTermIndexDocument) obj;
		if (termId == null) {
			if (other.termId != null)
				return false;
		} else if (!termId.equals(other.termId))
			return false;
		return true;
	}
	
	public void add(ESPositionIndexDocument position){
		this.positions.add(position);
	}
	
	public List<ESPositionIndexDocument> getPositions(){
		return  Collections.unmodifiableList(this.positions);
	}
	
	public boolean hasPositions(){
		return ! this.positions.isEmpty();
	}
}
