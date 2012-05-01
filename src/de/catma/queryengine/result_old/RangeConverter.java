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

package de.catma.queryengine.result;

import de.catma.core.document.Range;

/**
 * Implementors can convert Ranges to and from {@link org.catma.document.source.SourceDocument}-Ranges.
 * What ranges are converted and how they are converted is up to the implementor.
 *
 * @author Marco Petris
 */
public interface RangeConverter {
    /**
     * Converts the given source Range to a Range relative to a reference system marked by the
     * second range argument.
     * @param sourceRange the range to convert
     * @param range the reference system
     * @return the converted range or <code>null</code> if the source range does not have an equivalent
     * in the given reference system
     */
    public Range convertFromSourceDocumentRangeRelativeToRange(Range sourceRange, Range range);

    /**
     * Converts the given Range to a Range within the {@link org.catma.document.source.SourceDocument}.
     * @param range the range to convert
     * @return the converted range or <code>null</code> if the given range does not have an equivalent
     * Range in the {@link org.catma.document.source.SourceDocument}
     */
    public Range convertToSourceDocumentRange(Range range);
}
