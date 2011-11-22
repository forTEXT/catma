package de.catma.ui.client.ui.tagger;

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
	
	public boolean isPoint() {
		return getStartPos()==getEndPos();
	}
	
	@Override
	public String toString() {
		return "["+getStartPos()+","+getEndPos()+"]";
	}
}
