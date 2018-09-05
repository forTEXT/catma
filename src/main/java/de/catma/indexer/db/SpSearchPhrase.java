package de.catma.indexer.db;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

public class SpSearchPhrase {

//	CatmaIndex.searchPhrase(" +
//			":term1, :term2, :term3, :term4, :term5, :docID, :wild, :limitresult)

	public enum ResultColumn {
		tokenOffset, 
		characterStart, 
		characterEnd,
		;
	}
	
	private static final int MAX_DIRECT_SEARCH_TERMS = 5;

	
	private int limitResult;
	private List<String> termList;
	private String documentId;
	private boolean withWildcards;
	
	
	public void setLimitResult(int limitResult) {
		this.limitResult = limitResult;
	}
	
	public void setTermList(List<String> termList) {
		this.termList = termList;
	}
	
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	
	public void setWithWildcards(boolean withWildcards) {
		this.withWildcards = withWildcards;
	}

	public Result<Record> execute(DSLContext db) {
		StringBuilder builder = new StringBuilder("call catmaindex.searchPhrase( ");
		
		int termCount=0;
		for (termCount=0; 
			termCount<Math.min(MAX_DIRECT_SEARCH_TERMS, 
			termList.size()); termCount++) {
			
			String term = termList.get(termCount);
			if (term.equals("%")) {
				builder.append(" null, ");
			}
			else {
				builder.append("'"+term.replace("\\", "\\\\").replace("'", "''") + "', ");
			}
		}
		
		for (int pIdx=termCount; pIdx<MAX_DIRECT_SEARCH_TERMS; pIdx++) {
			builder.append(" null, ");
		}

		builder.append("'");
		builder.append(documentId);
		builder.append("', ");
		builder.append(withWildcards);
		builder.append(", ");
		
		builder.append(limitResult);
		builder.append(")");
		
		return db.fetch(builder.toString());
	}
}
