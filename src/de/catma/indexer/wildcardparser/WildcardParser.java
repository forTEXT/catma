package de.catma.indexer.wildcardparser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.indexer.TermInfo;




public class WildcardParser {

	static final String ESC= "\\";
	public final static HashSet<String> WILDCARDS = new HashSet<String>();
	static {
		WILDCARDS.add("%");
		WILDCARDS.add("_");
	}
	
	private StateHandler curState;
	private List<TermInfo> orderedTermInfos;
	
	public WildcardParser() {
		orderedTermInfos = new ArrayList<TermInfo>();
		curState = new TermStateHandler(orderedTermInfos);
	}
	
	public void handle(CharTermAttribute termAttr, OffsetAttribute offsetAttr) {
		curState = curState.handle(termAttr, offsetAttr);
	}
	
	public void finish() {
		curState.close();
	}
	
	public List<TermInfo> getOrderedTermInfos() {
		return orderedTermInfos;
	}
}
