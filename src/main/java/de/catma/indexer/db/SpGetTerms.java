package de.catma.indexer.db;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

public class SpGetTerms {

	//call catmaindex.getTerms(:docID, :basePos, :termCount)
	
	public static enum ResultColumn {
		term, 
		characterStart, 
		characterEnd,
		;
	}
	
	private String documentId;
	private int basePos;
	private int termCount;
	
	public void setBasePos(int basePos) {
		this.basePos = basePos;
	}

	public void setTermCount(int termCount) {
		this.termCount = termCount;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	
	
	public Result<Record> execute(DSLContext db) {
		StringBuilder builder = new StringBuilder("call catmaindex.getTerms( ");
	
		builder.append("'");
		builder.append(documentId);
		builder.append("', ");
		builder.append(basePos);
		builder.append(", ");
		builder.append(termCount);
		builder.append(")");
		
		return db.fetch(builder.toString());
	}	
	
}
