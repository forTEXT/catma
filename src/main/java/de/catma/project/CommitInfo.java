package de.catma.project;

import java.util.Date;

public class CommitInfo {
	
	private final String commitId;
	private final String commitMsg;
	private final Date commitTime;
	
	public CommitInfo(String commitId, String commitMsg, Date commitTime) {
		super();
		this.commitId = commitId;
		this.commitMsg = commitMsg;
		this.commitTime = commitTime;
	}
	
	public String getCommitId() {
		return commitId;
	}
	
	public String getCommitMsg() {
		return commitMsg;
	}
	
	@Override
	public String toString() {
		return commitId + " "  + commitMsg;
	}
	
	public Date getCommitTime() {
		return commitTime;
	}
}
