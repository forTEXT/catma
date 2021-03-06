/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2014  University Of Hamburg
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

/**
 * A matcher that can match a second term against a first term accorgin to 
 * some matching rules.
 * 
 * @author marco.petris@web.de
 *
 */
public interface TermMatcher {
	/**
	 * @param term1 first term
	 * @param term2 second term
	 * @return <code>true</code> if the second term matches the first term
	 *  according to the matching rules of the implementing class.
	 */
	public boolean match(String term1, String term2);
}
