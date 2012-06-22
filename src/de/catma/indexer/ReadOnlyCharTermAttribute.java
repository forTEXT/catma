package de.catma.indexer;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class ReadOnlyCharTermAttribute implements CharTermAttribute {
	
	private String term;
	
	public ReadOnlyCharTermAttribute(CharTermAttribute toCopy) {
		this.term = toCopy.toString();
	}

	public int length() {
		return term.length();
	}

	public char charAt(int index) {
		return term.charAt(index);
	}

	public CharSequence subSequence(int start, int end) {
		return term.subSequence(start, end);
	}

	public void copyBuffer(char[] buffer, int offset, int length) {
		int bufferIdx = 0;
		for (int i=offset; i<offset+length;i++) {
			buffer[bufferIdx++] = term.charAt(i);
		}
	}

	public char[] buffer() {
		return term.toCharArray();
	}

	public char[] resizeBuffer(int newSize) {
		return null;
	}

	public CharTermAttribute setLength(int length) {
		throw new UnsupportedOperationException("read only attribute");
	}

	public CharTermAttribute setEmpty() {
		throw new UnsupportedOperationException("read only attribute");
	}

	public CharTermAttribute append(CharSequence csq) {
		throw new UnsupportedOperationException("read only attribute");
	}

	public CharTermAttribute append(CharSequence csq, int start, int end) {
		throw new UnsupportedOperationException("read only attribute");
	}

	public CharTermAttribute append(char c) {
		throw new UnsupportedOperationException("read only attribute");
	}

	public CharTermAttribute append(String s) {
		throw new UnsupportedOperationException("read only attribute");
	}

	public CharTermAttribute append(StringBuilder sb) {
		throw new UnsupportedOperationException("read only attribute");
	}

	public CharTermAttribute append(CharTermAttribute termAtt) {
		throw new UnsupportedOperationException("read only attribute");
	}

	@Override
	public String toString() {
		return term;
	}
}
