/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
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
package de.catma.ui.client.ui.tagger.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author marco.petris@web.de
 *
 */
public class TextRange implements Comparable<TextRange>, Serializable {
	
	private static final long serialVersionUID = 4432309957370280636L;
	
	/**
	 * Non Breakable Space.
	 */
	public static final String NBSP = "\u00A0";
	/**
	 * Zero Width Joiner.
	 */
	public static final String ZWJ  = "\u200D";
	
	private int startPos;
	private int endPos;

	public TextRange(int startPos, int endPos) {
		super();
		this.startPos = startPos;
		this.endPos = endPos;
	}

	public TextRange(TextRange toCopy) {
		this(toCopy.startPos, toCopy.endPos);
	}

	public int getStartPos() {
		return startPos;
	}

	public int getEndPos() {
		return endPos;
	}
	
	public boolean isPoint() {
		return getStartPos()==getEndPos();
	}
	
	@Override
	public String toString() {
		return "["+getStartPos()+","+getEndPos()+"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endPos;
		result = prime * result + startPos;
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
		TextRange other = (TextRange) obj;
		if (endPos != other.endPos) {
			return false;
		}
		if (startPos != other.startPos) {
			return false;
		}
		return true;
	}
	
	
    /**
     * @param range the range to test
     * @return true if the given range and this range overlap in any point
     */
    public boolean hasOverlappingRange(TextRange range) {
        return (getOverlappingRange(range) != null);
    }

    
    public TextRange getOverlappingRange(
            TextRange rangeToTest ) {

        if( ( rangeToTest.getStartPos() == getEndPos() )
                || ( getStartPos() == rangeToTest.getEndPos() ) ) {
            return null;
        }

        if( isInBetweenInclusiveEdge( rangeToTest.getStartPos()) ) {
            if( isInBetweenInclusiveEdge( rangeToTest.getEndPos() ) ) {
                return new TextRange(
                        rangeToTest.getStartPos(),
                        rangeToTest.getEndPos() );
            }
            else if( isAfter( rangeToTest.getEndPos() ) ) {
                return new TextRange(
                    rangeToTest.getStartPos(),
                    this.getEndPos() );
            }
        }
        else if( !isAfter( rangeToTest.getStartPos() ) ) {
            if( isInBetweenInclusiveEdge( rangeToTest.getEndPos() ) ) {
                return new TextRange(
                    this.getStartPos(),
                    rangeToTest.getEndPos() );

            }
            else if( isAfter( rangeToTest.getEndPos() ) ) {
                return new TextRange(
                    this.getStartPos(),
                    this.getEndPos() );
            }
        }

        // no overlap
        return null;
    }
    
    public List<TextRange> getDisjointRanges( TextRange rangeToTest ) {

        List<TextRange> result = new ArrayList<TextRange>();

        if( isInBetweenExclusiveEdge( rangeToTest.getStartPos()) ) {
            result.add(
                new TextRange(
                    this.getStartPos(),
                    rangeToTest.getStartPos() ) );

            if( isInBetweenExclusiveEdge( rangeToTest.getEndPos() ) ) {
                result.add(
                    new TextRange(
                        rangeToTest.getEndPos(),
                        this.getEndPos() ) );
            }
        }
        else if( !isAfter( rangeToTest.getEndPos() ) ) {
            result.add(
                new TextRange( rangeToTest.getEndPos(), this.getEndPos() )  );
        }

        return result;
    }
    
    /**
     * Tests whether the given point is in between the range of this pointer but
     * is not one of the range's edges.
     * @param point the point to test
     * @return true if point &gt; startpoint and point &lt; endpoint, else false
     */
    private boolean isInBetweenExclusiveEdge( long point ) {
        return ( (point > this.getStartPos())
                    && (point < this.getEndPos()) );
    }
    
    private boolean isAfter( long point ) {
        return (this.getEndPos() < point);
    }

    public boolean isInBetweenInclusiveEdge( long point ) {
        return ( (point >= this.getStartPos())
                    && (point <= this.getEndPos()) );
    }
    
    public int compareTo(TextRange o) {
        if (this.startPos==o.startPos) {
        	if (this.endPos==o.endPos) {
        		return 0;
        	}
        	else if (this.endPos<o.endPos) {
        		return -1;
        	}
        	else {
        		return 1;
        	}
        }
        else {
        	if (this.startPos==o.startPos) {
        		return 0;
        	}
        	else if (this.startPos<o.startPos) {
        		return -1;
        	}
        	else {
        		return 1;
        	}

        }
    }

	public boolean isCoveredBy(TextRange textRange) {

		TextRange overlappingRange = this.getOverlappingRange(textRange);
		if (overlappingRange != null && overlappingRange.equals(this)) {
			return true;
		}
		
		return false;
	}
    
	public boolean isCoveredBy(Collection<TextRange> textRanges) {

		for (TextRange textRange : textRanges) {
			if (isCoveredBy(textRange)) {
				return true;
			}
		}
		
		return false;
	}

	public int size() {
		return getEndPos()-getStartPos();
	}
}
