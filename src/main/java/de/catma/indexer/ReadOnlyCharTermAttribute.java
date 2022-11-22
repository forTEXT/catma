/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
		throw new UnsupportedOperationException("Read-only attribute");
	}

	public CharTermAttribute setEmpty() {
		throw new UnsupportedOperationException("Read-only attribute");
	}

	public CharTermAttribute append(CharSequence csq) {
		throw new UnsupportedOperationException("Read-only attribute");
	}

	public CharTermAttribute append(CharSequence csq, int start, int end) {
		throw new UnsupportedOperationException("Read-only attribute");
	}

	public CharTermAttribute append(char c) {
		throw new UnsupportedOperationException("Read-only attribute");
	}

	public CharTermAttribute append(String s) {
		throw new UnsupportedOperationException("Read-only attribute");
	}

	public CharTermAttribute append(StringBuilder sb) {
		throw new UnsupportedOperationException("Read-only attribute");
	}

	public CharTermAttribute append(CharTermAttribute termAtt) {
		throw new UnsupportedOperationException("Read-only attribute");
	}

	@Override
	public String toString() {
		return term;
	}
}
