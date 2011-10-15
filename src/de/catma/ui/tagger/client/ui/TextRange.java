package de.catma.ui.tagger.client.ui;

public class TextRange {

	private int startPos;
	private int endPos;

	public TextRange(int startPos, int endPos) {
		super();
		this.startPos = startPos;
		this.endPos = endPos;
	}

	public int getStartPos() {
		return startPos;
	}

	public int getEndPos() {
		return endPos;
	}
	
	@Override
	public String toString() {
		return "["+getStartPos()+","+getEndPos()+"]";
	}
}
