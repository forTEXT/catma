package de.catma.ui.tagger.pager;

import de.catma.ui.client.ui.tagger.shared.TextRange;

public abstract class LineContent {

	protected String content;
	protected int contentOffset;

	protected TextRange textRange;

	public LineContent(String content, int contentOffset) {
		super();
		this.content = content;
		this.contentOffset = contentOffset;
	}

	public String getContent() {
		return content;
	}
	
	@Override
	public String toString() {
		return content;
	}

	protected String getSolidSpace(int count) {
    	StringBuilder builder = new StringBuilder();
    	for (int i=0; i<count;i++) {
    		builder.append(Page.SOLIDSPACE);
    	}
    	return builder.toString();
    }
	
	public int getContentOffset() {
		return contentOffset;
	}
	

	public void setLineOffset(int startPos) {
		this.textRange = new TextRange(startPos+contentOffset, startPos+contentOffset+content.length());
	}

	public boolean hasOverlappingRange(TextRange textRange) {
		return this.textRange.hasOverlappingRange(textRange);
	}

	public abstract String getPresentationContent();
	public abstract String getPresentationContent(TextRange rangePart);
}
