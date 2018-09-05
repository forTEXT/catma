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
package de.catma.serialization.tei.pointer;

import java.util.Scanner;

import de.catma.document.Range;

public class LineFragmentIdentifier extends TextFragmentIdentifier {
	
	public final static String SCHEME_NAME = "line";
	
	public LineFragmentIdentifier(Range range) {
		super();
		setRange(range);
	}
	
	public int getCharacterStartPos(String primarySource) {
		return getCharacterPos(primarySource, getRange().getStartPoint());
	}
	
	public int getCharacterEndPos(String primarySource) {
 		return getCharacterPos(primarySource, getRange().getEndPoint());
	}
	
	private int getCharacterPos(String primarySource, int line) {
		Scanner scanner = new Scanner(primarySource);
		int linePosition=0;
		int characterPos=0;
		while(scanner.hasNextLine()) {
			String curLine = scanner.nextLine();
			if (linePosition == line) {
				return characterPos;
			}
			characterPos+=curLine.length()
					+ (scanner.match().group(1)==null ? 0 : scanner.match().group(1).length());
			
			linePosition++;
		}
				
		return primarySource.length();
	}

	@Override
	public String getTextSchemeName() {
		return SCHEME_NAME + "=";
	}
}
