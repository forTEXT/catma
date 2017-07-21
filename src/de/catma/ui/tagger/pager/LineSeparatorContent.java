package de.catma.ui.tagger.pager;

import de.catma.ui.client.ui.tagger.shared.TextRange;

public class LineSeparatorContent extends LineContent {
	
	public LineSeparatorContent(String content, int contentOffset) {
		super(content, contentOffset);
	}

	@Override
	public String getPresentationContent() {
		return getSolidSpace(content.length());
	}

	@Override
	public String toString() {
		return "<-" + content.length(); //$NON-NLS-1$
	}
	
	@Override
	public String getPresentationContent(TextRange rangePart) {
		TextRange overlappingRange = this.textRange.getOverlappingRange(rangePart);
		
		int start = overlappingRange.getStartPos()-this.textRange.getStartPos();
		int end = start+overlappingRange.size();
		return getSolidSpace(content.substring(start, end).length());
	}
}
