package de.catma.ui.tagger.pager;

import de.catma.ui.client.ui.tagger.shared.TextRange;

public class CharacterContent extends LineContent {

	public CharacterContent(String content, int contentOffset) {
		super(content, contentOffset);
	}

	@Override
	public String getPresentationContent() {
		return content;
	}
	
	@Override
	public String getPresentationContent(TextRange rangePart) {
		TextRange overlappingRange = this.textRange.getOverlappingRange(rangePart);
		
		int start = overlappingRange.getStartPos()-this.textRange.getStartPos();
		int end = start+overlappingRange.size();
		
		return content.substring(start, end);
	}
}
