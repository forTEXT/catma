package de.catma.indexer.wildcardparser;


import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

public interface StateHandler {
	public StateHandler handle(CharTermAttribute termAttr, OffsetAttribute offsetAttr);
	public void close();
}