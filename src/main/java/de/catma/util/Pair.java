/*
 * CATMA Computer Aided Text Markup and Analysis
 *
 *    Copyright (C) 2008-2010  University Of Hamburg
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.catma.util;

/**
 * A simple implementation of a pair: two elements grouped together.
 *
 * @author Marco Petris
 *
 */
public class Pair<V,T> {
    private V first;
    private T second;

    /**
     * Empty pair.
     */
    public Pair() {
    }

    /**
     * Constructor
     * @param first first value
     * @param second second value
     */
    public Pair(V first, T second) {
        this.first = first;
        this.second = second;
    }

    /**
     * @return first value
     */
    public V getFirst() {
        return first;
    }

    /**
     * @return second value
     */
    public T getSecond() {
        return second;
    }

    /**
     * @param first first value
     */
    public void setFirst(V first) {
        this.first = first;
    }

    /**
     * @param second second value
     */
    public void setSecond(T second) {
        this.second = second;
    }
    
    @Override
    public String toString() {
    	return "[" + getFirst() + "," + getSecond() + "]";
    }
}
