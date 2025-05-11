package de.catma.api.v1.serialization.model_wrappers;

public class ProjectExportAnnotatedPhrase {
	private final int startOffset;
	private final int endOffset;
	private final String phrase;
	public ProjectExportAnnotatedPhrase(int startOffset, int endOffset, String phrase) {
		super();
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.phrase = phrase;
	}
	public int getStartOffset() {
		return startOffset;
	}
	public int getEndOffset() {
		return endOffset;
	}
	public String getPhrase() {
		return phrase;
	}
}
