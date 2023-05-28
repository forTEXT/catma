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
		throw new UnsupportedOperationException("Read-only attribute");
	}

	public int endOffset() {
		return endOffset;
	}

	@Override
	public String toString() {
		return "["+startOffset+","+endOffset+"]";
	}
}
