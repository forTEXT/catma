package de.catma.queryengine.result;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author marco.petris@web.de
 *
 */
public class TagQueryResult implements GroupedQueryResult {
	
	private Set<QueryResultRow> rows;
	private Map<String, Integer> frequencyBySourceDocument;
	private String group;
	
	public TagQueryResult(String group) {
		this.group = group;
		this.rows = new HashSet<QueryResultRow>();
		this.frequencyBySourceDocument = new HashMap<String, Integer>();
	}
	
	public Iterator<QueryResultRow> iterator() {
		return rows.iterator();
	}

	public Object getGroup() {
		return group;
	}
	
	public void addTagQueryResultRow(TagQueryResultRow tagQueryResultRow) {
		rows.add(tagQueryResultRow);
		if (!frequencyBySourceDocument.containsKey(tagQueryResultRow.getSourceDocumentId())) {
			frequencyBySourceDocument.put(tagQueryResultRow.getSourceDocumentId(), 1);
		}
		else {
			frequencyBySourceDocument.put(tagQueryResultRow.getSourceDocumentId(),
				frequencyBySourceDocument.get(tagQueryResultRow.getSourceDocumentId())+1);
		}
	}

	public int getTotalFrequency() {
		return rows.size();
	}

	public int getFrequency(String sourceDocumentID) {
		return frequencyBySourceDocument.get(sourceDocumentID);
	}

	public Set<String> getSourceDocumentIDs() {
		return Collections.unmodifiableSet(frequencyBySourceDocument.keySet());
	}

}
