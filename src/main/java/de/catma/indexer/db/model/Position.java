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
package de.catma.indexer.db.model;

import de.catma.document.Range;

public class Position implements java.io.Serializable {

	private Integer positionId;
	private Term term;
	private int characterStart;
	private int characterEnd;
	private int tokenOffset;

	public Position(Integer positionId, Term term, int characterStart,
			int characterEnd, int tokenOffset) {
		this.positionId = positionId;
		this.term = term;
		this.characterStart = characterStart;
		this.characterEnd = characterEnd;
		this.tokenOffset = tokenOffset;
	}

	public Integer getPositionId() {
		return this.positionId;
	}

	public Term getTerm() {
		return term;
	}

	public int getCharacterStart() {
		return this.characterStart;
	}

	public int getCharacterEnd() {
		return this.characterEnd;
	}

	public int getTokenOffset() {
		return this.tokenOffset;
	}

	@Override
	public String toString() {
		return term.getTerm() + "@" + new Range(getCharacterStart(), getCharacterEnd());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((positionId == null) ? 0 : positionId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Position other = (Position) obj;
		if (positionId == null) {
			if (other.positionId != null) {
				return false;
			}
		} else if (!positionId.equals(other.positionId)) {
			return false;
		}
		return true;
	}


	
}
