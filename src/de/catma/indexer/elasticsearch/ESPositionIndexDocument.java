package de.catma.indexer.elasticsearch;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import de.catma.core.document.Range;
import de.catma.indexer.TermInfo;

public class ESPositionIndexDocument implements ESDocument {

	private UUID positionId;
	private String documentId;
	private UUID termId;
	private Range range;
	private int tokenOffset;

	public ESPositionIndexDocument(String documentId, UUID termId,
			TermInfo termInfo) {
		this.documentId = documentId;
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
