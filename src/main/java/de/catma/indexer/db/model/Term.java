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

public class Term {

	private Integer termId;
	private String documentId;
	private int frequency;
	private String term;
	
	public Term(Integer termId, String documentId, int frequency, String term) {
		this.termId = termId;
		this.documentId = documentId;
		this.frequency = frequency;
		this.term = term;
	}

	public Integer getTermId() {
		return this.termId;
	}

	public String getDocumentId() {
		return this.documentId;
	}

	public int getFrequency() {
		return this.frequency;
	}

	public String getTerm() {
		return this.term;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((termId == null) ? 0 : termId.hashCode());
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
		Term other = (Term) obj;
		if (termId == null) {
			if (other.termId != null) {
				return false;
			}
		} else if (!termId.equals(other.termId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Term [termId=" + termId + ", documentId=" + documentId
				+ ", frequency=" + frequency + ", term=" + term + "]";
	}
}
