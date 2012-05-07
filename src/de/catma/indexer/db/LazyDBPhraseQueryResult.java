package de.catma.indexer.db;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;

public class LazyDBPhraseQueryResult implements GroupedQueryResult {
	
	private SessionFactory sessionFactory;
	private Map<String, Term> termsByDocument;
	private String phrase;

	public LazyDBPhraseQueryResult(
			SessionFactory sessionFactory, String phrase) {
		this.sessionFactory = sessionFactory;
		this.phrase = phrase;
		termsByDocument = new HashMap<String, Term>();
	}

	public Iterator<QueryResultRow> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getGroup() {
		return phrase;
	}

	public int getTotalFrequency() {
		int sum = 0;
		for (Term t : termsByDocument.values()) {
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

	void addTerm(Term t) {
		termsByDocument.put(t.getDocumentId(), t);
	}

}
