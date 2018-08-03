package de.catma.indexer.indexbuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.catma.indexer.SQLWildcardMatcher;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class SourceDocumentIndexBuffer {

	private Map<String, List<TermInfo>> termInfos;
	private TreeSet<TermInfo> orderedTermInfos;
	private String sourceDocumentId;
	private int size;

	public SourceDocumentIndexBuffer(String sourceDocumentId, int size, Map<String, List<TermInfo>> termInfos) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.size = size;
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
	
	public QueryResult search(Set<QueryResultRow> baseResult,
			Set<QueryResultRow> collocationConditionResult, int spanContextSize,
			SpanDirection direction) {
		QueryResultRowArray result = new QueryResultRowArray();
		
		for (QueryResultRow baseRow : baseResult) {
			
			SpanContext spanContext = getSpanContext(baseRow, spanContextSize, direction);
			
			if (spanContextContainsAny(spanContext, collocationConditionResult)) {
				result.add(baseRow);
			}
		}
		
		return result;
	}

	private boolean spanContextContainsAny(SpanContext spanContext,
			Set<QueryResultRow> collocationConditionResult) {
		for (QueryResultRow row : collocationConditionResult) {
			List<TermInfo> termInfos = getTermInfos(row);
			if (spanContext.contains(termInfos)) {
				return true;
			}
		}
		return false;
	}

	private List<TermInfo> getTermInfos(QueryResultRow row) {
		ArrayList<TermInfo> result = new ArrayList<>();
		boolean withinRange = false;
		
		for (TermInfo termInfo : orderedTermInfos) {
			if (termInfo.getRange().hasOverlappingRange(row.getRange())) {
				result.add(termInfo);
				withinRange = true;
			}
			else if (withinRange) {
				return result;
			}
		}
		
		return result;
	}

	private SpanContext getSpanContext(QueryResultRow row, int spanContextSize,
			SpanDirection direction) {
		
		SpanContext spanContext = new SpanContext(row.getSourceDocumentId());
		ArrayList<TermInfo> backwardContext = new ArrayList<>(spanContextSize);
		ArrayList<TermInfo> forwardContext = new ArrayList<>(spanContextSize);
		
		boolean withinRange = false;	
		boolean outOfRange = false;
		
		for (TermInfo termInfo : orderedTermInfos) {
			
			if (termInfo.getRange().hasOverlappingRange(row.getRange())) {
				withinRange = true;
			}
			else if (withinRange) {
				outOfRange = true;
			}
			
			if (!withinRange) {
				if (backwardContext.size() == spanContextSize) {
					backwardContext.remove(0);
				}
				backwardContext.add(termInfo);
			}
			
			if (outOfRange) {
				forwardContext.add(termInfo);
				
				if (forwardContext.size()==spanContextSize) {
					break;
				}
			}
			
		}
	
		if (direction.equals(SpanDirection.BOTH) || direction.equals(SpanDirection.FORWARD)) {
			spanContext.addForwardTokens(forwardContext);
		}

		if (direction.equals(SpanDirection.BOTH) || direction.equals(SpanDirection.BACKWARD)) {
			Collections.reverse(backwardContext);
			spanContext.addBackwardTokens(backwardContext);
		}
		
		return spanContext;
	}
	
	public int getSize() {
		return size;
	}
}
