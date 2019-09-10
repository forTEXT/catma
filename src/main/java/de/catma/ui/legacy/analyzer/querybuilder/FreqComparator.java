/*
 *   CATMA Computer Aided Text Markup and Analysis
 *
 *   Copyright (C) 2008-2010  University Of Hamburg
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

package de.catma.ui.legacy.analyzer.querybuilder;

/**
 * A selectable item that represents the comparator of frequency queries.
 *
 * @author Marco Petris
 *
 */
public class FreqComparator {
    private String label;
    private String comparator;
    private boolean isRange;

    /**
     * @param label the label of the item
     * @param comparator the comparator string: =, &gt;, &lt; ...
     */
    public FreqComparator(String label, String comparator) {
        this(label, comparator, false);
    }

    /**
     * @param label the label of the item
     * @param comparator the comparator string: =, &gt;, &lt; ...
     * @param range true -> this is the item representing the 'between' comparator
     */
    public FreqComparator(String label, String comparator, boolean range) {
        this.label = label;
        this.comparator = comparator;
        isRange = range;
    }

    /**
     * @return the comparator character sequence
     */
    public String getComparator() {
        return comparator;
    }

    /**
     * @return true if this is the 'between'-comparator, else false.
     */
    public boolean isRange() {
        return isRange;
    }

    @Override
    public String toString() {
        return label;
    }
}
