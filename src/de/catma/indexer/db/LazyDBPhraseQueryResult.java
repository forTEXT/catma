package de.catma.indexer.db;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;

import de.catma.indexer.db.model.DBTerm;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.PhraseResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class LazyDBPhraseQueryResult implements GroupedQueryResult {
	
	private SessionFactory sessionFactory;
	private Map<String, DBTerm> termsByDocument;
	private String term;
	private QueryResultRowArray queryResultRowArray;

	public LazyDBPhraseQueryResult(
			SessionFactory sessionFactory, String term) {
		this.sessionFactory = sessionFactory;
		this.term = term;
		termsByDocument = new HashMap<String, DBTerm>();
	}

	public Iterator<QueryResultRow> iterator() {
		if (queryResultRowArray == null) {
			loadQueryResultRows();
		}
		return queryResultRowArray.iterator();
	}

	private void loadQueryResultRows() {
		queryResultRowArray = new QueryResultRowArray();
		PhraseSearcher phraseSearcher = new PhraseSearcher(sessionFactory);
		for (String sourceDocumentID : getSourceDocumentIDs()) {
			queryResultRowArray.addAll(
				phraseSearcher.getPositionsForTerm(
					term, 
					sourceDocumentID, 
					0)); // no limit
		}
	}

	public Object getGroup() {
		return term;
	}

	public int getTotalFrequency() {
		int sum = 0;
		for (DBTerm t : termsByDocument.values()) {
			sum += t.getFrequency();
		}
		return sum;
	}

	public int getFrequency(String sourceDocumentID) {
		if (termsByDocument.containsKey(sourceDocumentID)) {
			return termsByDocument.get(sourceDocumentID).getFrequency();
		}
		else {
			return 0;
		}
	}

	public Set<String> getSourceDocumentIDs() {
		return termsByDocument.keySet();
	}

	void addTerm(DBTerm t) {
		termsByDocument.put(t.getDocumentId(), t);
	}

	public GroupedQueryResult getSubResult(String... sourceDocumentID) {
		Set<String> filterSourceDocumentIds = new HashSet<String>(); 
		filterSourceDocumentIds.addAll(Arrays.asList(sourceDocumentID));
		
		PhraseResult subResult = new PhraseResult(term);
		for (QueryResultRow row : this) {
			if (filterSourceDocumentIds.contains(row.getSourceDocumentId())) {
				subResult.addQueryResultRow(row);
			}
		}
		return subResult;
	}
}
