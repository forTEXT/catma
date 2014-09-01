package de.catma.indexer;

import de.catma.document.Range;
import de.catma.document.source.KeywordInContext;

public class KeywordInSpanContext extends KeywordInContext {
	
	private SpanContext spanContext;

	public KeywordInSpanContext(String keyword, String kwic,
			Range kwicSourceRange, int relativeKeywordStartPos, boolean rtl, SpanContext spanContext) {
		super(keyword, kwic, kwicSourceRange, relativeKeywordStartPos, rtl);
		this.spanContext = spanContext;
	}
	
	public SpanContext getSpanContext() {
		return spanContext;
	}
}
