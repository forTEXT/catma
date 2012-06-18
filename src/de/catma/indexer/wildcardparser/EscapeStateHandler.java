package de.catma.indexer.wildcardparser;

import java.util.List;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.indexer.TermInfo;

class EscapeStateHandler extends AbstractStateHandler {
	
	private TermInfo escTermInfo;
	
	public EscapeStateHandler(List<TermInfo> orderedTermInfos,
			CharTermAttribute termAttr, OffsetAttribute offsetAttr) {
		super(orderedTermInfos);
		this.escTermInfo = 
				new TermInfo(
					termAttr.toString(),
					offsetAttr.startOffset(), 
					offsetAttr.endOffset());
	}

	public StateHandler handle(CharTermAttribute termAttr,
			OffsetAttribute offsetAttr) {
		if (WildcardParser.WILDCARDS.contains(termAttr.toString())) {
			return new WildcardStateHandler(orderedTermInfos, new TermInfo(
				escTermInfo.getTerm()+termAttr.toString(),
				escTermInfo.getRange().getStartPoint(),
				offsetAttr.endOffset()));
		}
		else {
			orderedTermInfos.add(escTermInfo);
			return new TermStateHandler(orderedTermInfos).handle(termAttr, offsetAttr);
		}
	}
	
	public void close() {
		if (escTermInfo != null) {
			orderedTermInfos.add(escTermInfo);
		}
	}
}