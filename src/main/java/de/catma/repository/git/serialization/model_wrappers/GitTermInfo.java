package de.catma.repository.git.serialization.model_wrappers;

import de.catma.indexer.TermInfo;

@SuppressWarnings("unused") // fields are accessed by reflection
public class GitTermInfo {
	
	private int endOffset;
	private int startOffset;
	private int tokenOffset;

	public GitTermInfo(TermInfo termInfo) {
		this.endOffset = termInfo.getRange().getEndPoint();
		this.startOffset = termInfo.getRange().getStartPoint();
		this.tokenOffset = termInfo.getTokenOffset();
	}
}
