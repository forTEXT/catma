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
 * Matches a first term that can contain the SQL-wildcards % (zero or more) and _ (excactly one) 
 * against a second term.  
 * 
 * @author marco.petris@web.de
 *
 */
public class SQLWildcardMatcher implements TermMatcher {
	
	/* (non-Javadoc)
	 * @see de.catma.indexer.TermMatcher#match(java.lang.String, java.lang.String)
	 */
	public boolean match(String wildcardTerm, String term) {
		return term.matches(SQLWildcard2RegexConverter.convert(wildcardTerm));
	}
}
