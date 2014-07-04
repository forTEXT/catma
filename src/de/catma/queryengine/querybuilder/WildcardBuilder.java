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
package de.catma.queryengine.querybuilder;

public class WildcardBuilder {

	public String getWildcardFor(
			String exactWord, int position) {
		StringBuilder builder = new StringBuilder();
		
		for (int i=1; i<position; i++) {
			builder.append(" % ");
		}
		builder.append(escape(exactWord));
		
		return builder.toString(); 
	}

	public String getWildcardFor(
			String startsWith, String contains, String endsWith, int position) {
		
		if (startsWith == null) {
			startsWith = "";
		}
		if (contains == null) {
			contains = "";
		}
		if (endsWith == null) {
			endsWith = "";
		}
		StringBuilder builder = new StringBuilder();
		
		for (int i=1; i<position; i++) {
			builder.append(" % ");
		}
		if (!startsWith.isEmpty()) {
			builder.append(escape(startsWith));
			builder.append("%");
		}
		if (!contains.isEmpty()) {
			if (startsWith.isEmpty()) {
				builder.append("%");
			}
			builder.append(escape(contains));
			builder.append("%");
		}
		if (!endsWith.isEmpty()) {
			if (contains.isEmpty() && startsWith.isEmpty()) {
				builder.append("%");
			}
			builder.append(escape(endsWith));
		}
		return builder.toString();
	}
	
	private String escape(String input) {
		return input.replace("_", "\\_").replace("%", "\\%");
	}
}
