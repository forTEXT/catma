package de.catma.indexer.wildcardparser;

import java.util.List;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.indexer.TermInfo;

class TermStateHandler extends AbstractStateHandler {
	
	public TermStateHandler(List<TermInfo> orderedTermInfos) {
		super(orderedTermInfos);
	}

	public StateHandler handle(CharTermAttribute termAttr,
			OffsetAttribute offsetAttr) {
		if (termAttr.toString().equals(WildcardParser.ESC)) {
			return new EscapeStateHandler(orderedTermInfos, termAttr, offsetAttr);
		}
		else if (WildcardParser.WILDCARDS.contains(termAttr.toString())) {
			return new WildcardStateHandler(orderedTermInfos, termAttr, offsetAttr);
		}
		else {
			orderedTermInfos.add(
				new TermInfo(termAttr.toString(),
					offsetAttr.startOffset(), offsetAttr.endOffset()));
			return this;
		}
	}
	
	public void close() {/* noop */}
}