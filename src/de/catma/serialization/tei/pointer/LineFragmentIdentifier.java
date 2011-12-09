package de.catma.serialization.tei.pointer;

import java.util.Scanner;

import de.catma.core.document.Range;

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
