/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009  University Of Hamburg
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

package de.catma.core.document;

import java.util.ArrayList;
import java.util.List;

/**
 * A range of text with a startPoint and an endPoint. This class is immutable.
 *
 * @author Marco Petris
 *
 */
public class Range implements Comparable<Range> {

	private int startPoint;
	private int endPoint;
	private int hashCode;
	
	/**
	 * @param startPoint the point before the first character.
	 * @param endPoint the point after the last character.
	 */
	public Range( int startPoint, int endPoint ) {
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		this.hashCode = 
			(String.valueOf( startPoint ) 
			+ "-" 
			+ String.valueOf(endPoint)).hashCode();
	}

	/**
	 * @return the start point
	 */
	public int getStartPoint() {
		return startPoint;
	}

	/**
	 * @return the end point
	 */
	public int getEndPoint() {
		return endPoint;
	}
	
	/**
     * Equality is tested with start- and endpoint.
     * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object obj ) {
		if( ( obj == null ) || !( obj instanceof Range ) ) {
			return false;
		}
		else {
			return (( this.startPoint == ((Range)obj).startPoint) 
					&& ( this.endPoint == ((Range)obj).endPoint) );
		}
	}
	
	/**
	 * @return hashcode computed with start- and endpoint
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.hashCode;
	}
	
	/**
	 * Tests whether the given point comes after (>) this start point.
	 * @param point the point to test
	 * @return true if the given point comes after (>) this start point, else false
	 */
	public boolean startsAfter( long point ) {
		return this.startPoint > point;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Range["+startPoint+","+endPoint+"]";
	}
	
	/**
	 * @return true, if startPoint == endPoint
	 */
	public boolean isSinglePoint() {
		return (this.startPoint == this.endPoint);
	}

    /**
     * Compares Ranges via startPoint and endPoint.
     * @param o tha range to compare to
     * @return the distance between the this startpoint and the given startpoint or (if there is no such
     * distance) the distance between this endpoint and the given endpoint
     */
    public int compareTo(Range o) {
        if (this.startPoint==o.startPoint) {
        	if (this.endPoint==o.endPoint) {
        		return 0;
        	}
        	else if (this.endPoint<o.endPoint) {
        		return -1;
        	}
        	else {
        		return 1;
        	}
        }
        else {
        	if (this.startPoint==o.startPoint) {
        		return 0;
        	}
        	else if (this.startPoint<o.startPoint) {
        		return -1;
        	}
        	else {
        		return 1;
        	}

        }
    }

	/**
	 * @param range
	 * @return true if the range is in between the range of this pointer (can occupy the edges).
	 */
	public boolean isInBetween( Range range ) {
		if( ( this.getStartPoint() >= range.getStartPoint() )
				&& ( this.getEndPoint() <= range.getEndPoint() ) ) {
			return  true;
		}
		return false;
	}
    
    /**
     * Tests whether the given point is in between the range of this pointer but
     * is not one of the range's edges.
     * @param point the point to test
     * @return true if point &gt; startpoint and point &lt; endpoint, else false
     */
    private boolean isInBetweenExclusiveEdge( long point ) {
        return ( (point > this.getStartPoint())
                    && (point < this.getEndPoint()) );
    }

    /**
     * Tests whether the given point is in between the range of this pointer.
     * @param point the point to test
     * @return true if point &gt;= startpoint and point &lt;= endpoint, else false
     */
    private boolean isInBetweenInclusiveEdge( long point ) {
        return ( (point >= this.getStartPoint())
                    && (point <= this.getEndPoint()) );
    }

    /**
     * Tests whether the given point is after the endpoint
     * @param point the point to test
     * @return true if the given point is after the endpoint else false
     */
    private boolean isAfter( long point ) {
        return (this.getEndPoint() < point);
    }

    /**
     * @param rangeToTest the range to test
     * @return a list of ranges which are the non overlapping parts of the range
     * to test and the range of this pointer
     */
    public List<Range> getDisjointRanges( Range rangeToTest ) {

        List<Range> result = new ArrayList<Range>();

        if( isInBetweenExclusiveEdge( rangeToTest.getStartPoint()) ) {
            result.add(
                new Range(
                    this.getStartPoint(),
                    rangeToTest.getStartPoint() ) );

            if( isInBetweenExclusiveEdge( rangeToTest.getEndPoint() ) ) {
                result.add(
                    new Range(
                        rangeToTest.getEndPoint(),
                        this.getEndPoint() ) );
            }
        }
        else if( !isAfter( rangeToTest.getEndPoint() ) ) {
            result.add(
                new Range( rangeToTest.getEndPoint(), this.getEndPoint() )  );
        }

        return result;
    }

    /**
     * @param rangeToTest the range to test
     * @return the overlapping range of the range to test and the range of this pointer
     * or null if they do not overlap
     */
    public Range getOverlappingRange(
            Range rangeToTest ) {

        if( ( rangeToTest.getStartPoint() == getEndPoint() )
                || ( getStartPoint() == rangeToTest.getEndPoint() ) ) {
            return null;
        }

        if( isInBetweenInclusiveEdge( rangeToTest.getStartPoint()) ) {
            if( isInBetweenInclusiveEdge( rangeToTest.getEndPoint() ) ) {
                return new Range(
                        rangeToTest.getStartPoint(),
                        rangeToTest.getEndPoint() );
            }
            else if( isAfter( rangeToTest.getEndPoint() ) ) {
                return new Range(
                    rangeToTest.getStartPoint(),
                    this.getEndPoint() );
            }
        }
        else if( !isAfter( rangeToTest.getStartPoint() ) ) {
            if( isInBetweenInclusiveEdge( rangeToTest.getEndPoint() ) ) {
                return new Range(
                    this.getStartPoint(),
                    rangeToTest.getEndPoint() );

            }
            else if( isAfter( rangeToTest.getEndPoint() ) ) {
                return new Range(
                    this.getStartPoint(),
                    this.getEndPoint() );
            }
        }

        // no overlap
        return null;
    }

    /**
     * @return the size of this range
     */
    public long getSize() {
        return getEndPoint()-getStartPoint();  
    }

    /**
     * @param range the range to test
     * @return true if the given range and this range overlap in any point
     */
    public boolean hasOverlappingRange(Range range) {
        return (getOverlappingRange(range) != null);
    }
}
