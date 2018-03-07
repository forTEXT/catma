package de.catma.repository.git.serialization.model_wrappers;

import de.catma.indexer.TermInfo;

public class GitTermInfo {
	
	private TermInfo termInfo;

	public GitTermInfo(TermInfo termInfo) {
		super();
		this.termInfo = termInfo;
	}

	public int getTokenOffset() {
		return termInfo.getTokenOffset();
	}
	
	
	public int getStartOffset() {
		return termInfo.getRange().getStartPoint();
	}

	public int getEndOffset() {
		return termInfo.getRange().getEndPoint();
	}

}
