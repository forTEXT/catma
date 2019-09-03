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
package de.catma.queryengine.result;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import de.catma.document.Range;
import de.catma.queryengine.QueryId;

public class QueryResultRow {

	private QueryId queryId;
	private String sourceDocumentId;
	private Range range;
	private String phrase;

	public QueryResultRow(QueryId queryId, String sourceDocumentId, Range range, String phrase) {
		super();
		this.queryId = queryId;
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
		this.phrase = phrase;
	}
	
	public QueryResultRow(QueryId queryId, String sourceDocumentId, Range range) {
		this(queryId, sourceDocumentId, range, null);
	}

	public String getSourceDocumentId() {
		return sourceDocumentId;
	}
	
	public Range getRange() {
		return range;
	}	

	public String getPhrase() {
		return phrase;
	}

	public void setPhrase(String phrase) {
		this.phrase = phrase;
	}
	
	@Override
	public String toString() {
		return super.toString() + "SourceDoc[#"+sourceDocumentId + "]"+range
				+ ((phrase == null)?"phrase not set":"->"+phrase+"<- "); 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		result = prime
				* result
				+ ((sourceDocumentId == null) ? 0 : sourceDocumentId.hashCode());
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
		if (!(obj instanceof QueryResultRow)) {
			return false;
		}
		QueryResultRow other = (QueryResultRow) obj;
		if (range == null) {
			if (other.range != null) {
				return false;
			}
		} else if (!range.equals(other.range)) {
			return false;
		}
		if (sourceDocumentId == null) {
			if (other.sourceDocumentId != null) {
				return false;
			}
		} else if (!sourceDocumentId.equals(other.sourceDocumentId)) {
			return false;
		}
		return true;
	}

    public boolean existsIn(QueryResult queryResult, 
            Comparator<QueryResultRow> comparator) {

        for (QueryResultRow row : queryResult) {
            if(comparator.compare(this,row)==0) {
//            if((comparator.compare(this,row)==0)
            		//||(comparator.compare(row,this)==0)) {
                return true;
            }
        }

        return false;
    }
    
    public QueryId getQueryId() {
		return queryId;
	}
	
	public Set<Range> getRanges() {
		return Collections.singleton(range);
	}
}
