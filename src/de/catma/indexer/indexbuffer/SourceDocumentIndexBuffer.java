package de.catma.indexer.indexbuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import de.catma.indexer.SQLWildcardMatcher;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class SourceDocumentIndexBuffer {

	private Map<String, List<TermInfo>> termInfos;
	private TreeSet<TermInfo> orderedTermInfos;
	private String sourceDocumentId;

	public SourceDocumentIndexBuffer(String sourceDocumentId, Map<String, List<TermInfo>> termInfos) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.termInfos = termInfos;
		createTermInfoArray();
	}

	private void createTermInfoArray() {
		orderedTermInfos = new TreeSet<TermInfo>(TermInfo.TOKENOFFSETCOMPARATOR);
		
		for (List<TermInfo> tiList : termInfos.values()) {
			orderedTermInfos.addAll(tiList);
		}

	}
	
	public String getSourceDocumentId() {
		return sourceDocumentId;
	}
	
	public List<TermFrequencyInfo> search(
			CompareOperator comp1, int freq1, CompareOperator comp2, int freq2) throws IOException {
		ArrayList<TermFrequencyInfo> result = new ArrayList<>(); 
		
		for (Map.Entry<String, List<TermInfo>> entry : termInfos.entrySet()) {
			if (comp1.getCondition().isTrue(entry.getValue().size(), freq1)) {
				if ((comp2 == null) || (comp2.getCondition().isTrue(entry.getValue().size(), freq2))) { 
					result.add(new TermFrequencyInfo(entry.getKey(), entry.getValue().size()));
				}
			}
		}
		return result;
	}
	
	public QueryResult search(String phrase,
			List<String> termList, int limit, boolean withWildcards) throws IOException {
		QueryResultRowArray result = new QueryResultRowArray();
		if (!withWildcards) {
			for (String term : termList) {
				for (TermInfo ti : termInfos.get(term)) {
					result.add(new QueryResultRow(sourceDocumentId, ti.getRange(), term));
					if ((limit > 0) && (result.size() == limit)) {
						return result;
					}
				}
			}
		}
		else {
			SQLWildcardMatcher sqlWildcardMatcher = new SQLWildcardMatcher();
			
			for (String wildcardTerm : termList) {
				for (Map.Entry<String, List<TermInfo>> entry : termInfos.entrySet()) {
					if (sqlWildcardMatcher.match(wildcardTerm, entry.getKey())) {
						for (TermInfo ti : entry.getValue()) {
							result.add(
								new QueryResultRow(
										sourceDocumentId, ti.getRange(), ti.getTerm()));
							if ((limit > 0) && (result.size() == limit)) {
								return result;
							}
						}
					}
				}
			}
		}
		return result;
	}
}
