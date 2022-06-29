package de.catma.project;

public class CommitInfo {
	
	private final String commitId;
	private final String commitMsg;
	
	public CommitInfo(String commitId, String commitMsg) {
		super();
		this.commitId = commitId;
		this.commitMsg = commitMsg;
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
}
