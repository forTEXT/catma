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

import java.util.Comparator;

import de.catma.document.Range;

public class TermInfo {
	
	static class TokenOffsetComparator implements Comparator<TermInfo> {
		public int compare(TermInfo o1, TermInfo o2) {
			return o1.tokenOffset-o2.tokenOffset;
		}
	}
	
	public static final TokenOffsetComparator TOKENOFFSETCOMPARATOR = 
			new TokenOffsetComparator();

	private String term;
	private Range range;
	private int tokenOffset;
	
	public TermInfo(String term, int start, int end) {
		this(term, start, end, 0);
	}

	public TermInfo(String term, int start, int end, int tokenOffset) {
		this.term = term;
		this.range = new Range(start,end);
		this.tokenOffset = tokenOffset;
	}
	
	public Range getRange() {
		return range;
	}
	
	public String getTerm() {
		return term;
	}

	public int getTokenOffset() {
		return tokenOffset;
	}

	@Override
	public String toString() {
		return term + "@" + tokenOffset + "@" + range ;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		result = prime * result + ((term == null) ? 0 : term.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TermInfo other = (TermInfo) obj;
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
			return false;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		return true;
	}
	
	
}
