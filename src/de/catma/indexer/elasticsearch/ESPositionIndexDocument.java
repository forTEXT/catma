package de.catma.indexer.elasticsearch;

import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ning.http.client.Response;

import de.catma.core.document.Range;
import de.catma.core.util.IDGenerator;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.QueryResultRow;

public class ESPositionIndexDocument implements ESDocument {

	private UUID positionId;
	private String documentId;
	private UUID termId;
	private Range range;
	private int tokenOffset;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((documentId == null) ? 0 : documentId.hashCode());
		result = prime * result
				+ ((positionId == null) ? 0 : positionId.hashCode());
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
		ESPositionIndexDocument other = (ESPositionIndexDocument) obj;
		if (documentId == null) {
			if (other.documentId != null)
				return false;
		} else if (!documentId.equals(other.documentId))
			return false;
		if (positionId == null) {
			if (other.positionId != null)
				return false;
		} else if (!positionId.equals(other.positionId))
			return false;
		if (termId == null) {
			if (other.termId != null)
				return false;
		} else if (!termId.equals(other.termId))
			return false;
		return true;
	}

	public ESPositionIndexDocument(String documentId, UUID termId,
			TermInfo termInfo) {
		this.documentId = documentId;
		this.termId = termId;
		this.range = termInfo.getRange();
		this.tokenOffset = termInfo.getTokenOffset();
		this.positionId = getIndexDocumentKey();
	}

	public UUID getIndexDocumentKey() {
		return UUID.nameUUIDFromBytes((documentId + termId
				+ range.getStartPoint() + range.getEndPoint()).getBytes());
	}

	public String toJSON() throws JSONException {
		JSONObject j_root = new JSONObject();
		j_root.put("documentId", documentId);
		j_root.put("characterStart", range.getStartPoint());
		j_root.put("characterEnd", range.getEndPoint());
		j_root.put("termId_l", termId.getLeastSignificantBits());
		j_root.put("termId_m", termId.getMostSignificantBits());
		j_root.put("tokenoffset", tokenOffset);
		return j_root.toString();
	}


	public UUID getPositionId() {
		return positionId;
	}

	public String getDocumentId() {
		return documentId;
	}

	public UUID getTermId() {
		return termId;
	}

	public Range getRange() {
		return range;
	}

	public int getTokenOffset() {
		return tokenOffset;
	}

	
}
