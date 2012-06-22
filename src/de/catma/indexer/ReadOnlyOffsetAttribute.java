package de.catma.indexer;

import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

public class ReadOnlyOffsetAttribute implements OffsetAttribute {
	
	private int startOffset;
	private int endOffset;
	
	public ReadOnlyOffsetAttribute(OffsetAttribute toCopy) {
		this.startOffset = toCopy.startOffset();
		this.endOffset = toCopy.endOffset();
	}

	public int startOffset() {
		return startOffset;
	}

	public void setOffset(int startOffset, int endOffset) {
		throw new UnsupportedOperationException("read only attribute");
	}

	public int endOffset() {
		return endOffset;
	}

	@Override
	public String toString() {
		return "["+startOffset+","+endOffset+"]";
	}
}
