package de.catma.indexer.wildcardparser;

import java.util.List;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.indexer.TermInfo;

class WildcardStateHandler extends AbstractStateHandler {

	private TermInfo wildcardTermInfo;

	public WildcardStateHandler(List<TermInfo> orderedTermInfos,
			CharTermAttribute termAttr, OffsetAttribute offsetAttr) {
		super(orderedTermInfos);
		this.wildcardTermInfo = new TermInfo(termAttr.toString(),
				offsetAttr.startOffset(), offsetAttr.endOffset());
		checkLastListEntry();
	}

	public WildcardStateHandler(List<TermInfo> orderedTermInfos, TermInfo ti) {
		super(orderedTermInfos);
		this.wildcardTermInfo = ti;
		checkLastListEntry();
	}
	
	private void checkLastListEntry() {
		
		if (!orderedTermInfos.isEmpty()) {
			TermInfo lastTermInfo = orderedTermInfos.get(orderedTermInfos.size()-1);
			if (lastTermInfo.getRange().getEndPoint() 
					== this.wildcardTermInfo.getRange().getStartPoint()) {
				this.wildcardTermInfo = 
						new TermInfo(lastTermInfo.getTerm() + wildcardTermInfo.getTerm(),
								lastTermInfo.getRange().getStartPoint(),
								wildcardTermInfo.getRange().getEndPoint());
				orderedTermInfos.remove(orderedTermInfos.size()-1);
			}
		}
	}

	public StateHandler handle(CharTermAttribute termAttr,
			OffsetAttribute offsetAttr) {
		if (offsetAttr.startOffset() == wildcardTermInfo.getRange().getEndPoint()) {
			
			if (termAttr.toString().equals(WildcardParser.ESC)) {
				return new EscapeStateHandler(orderedTermInfos, termAttr, offsetAttr);
			}
			else if (WildcardParser.WILDCARDS.contains(termAttr.toString())) {
				TermInfo ti = new
						TermInfo(wildcardTermInfo.getTerm()+termAttr.toString(),
								wildcardTermInfo.getRange().getStartPoint(),
								offsetAttr.endOffset());
				
				return new WildcardStateHandler(orderedTermInfos, ti);
			}
			else {
				this.orderedTermInfos.add(new
						TermInfo(wildcardTermInfo.getTerm()+termAttr.toString(),
								wildcardTermInfo.getRange().getStartPoint(),
								offsetAttr.endOffset()));

				return new TermStateHandler(orderedTermInfos);
			}
		}
		else {
			this.orderedTermInfos.add(wildcardTermInfo);
			
			return new TermStateHandler(orderedTermInfos).handle(
					termAttr, offsetAttr);
		}
	}
	
	public void close() {
		if (wildcardTermInfo != null) {
			orderedTermInfos.add(wildcardTermInfo);
		}
	}
}