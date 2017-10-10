package de.catma.repository.git.model_wrappers;

import com.jsoniter.annotation.JsonIgnore;
import de.catma.document.source.SourceDocumentInfo;

public class GitSourceDocumentInfo {
	private SourceDocumentInfo sourceDocumentInfo;

	public GitSourceDocumentInfo() {
		this.sourceDocumentInfo = new SourceDocumentInfo();
	}

	public GitSourceDocumentInfo(SourceDocumentInfo sourceDocumentInfo) {
		this.sourceDocumentInfo = sourceDocumentInfo;
	}

	@JsonIgnore
	public SourceDocumentInfo getSourceDocumentInfo() {
		return this.sourceDocumentInfo;
	}

	public GitIndexInfoSet getGitIndexInfoSet() {
		return new GitIndexInfoSet(this.sourceDocumentInfo.getIndexInfoSet());
	}

	public void setGitIndexInfoSet(GitIndexInfoSet gitIndexInfoSet) {
		this.sourceDocumentInfo.setIndexInfoSet(gitIndexInfoSet.getIndexInfoSet());
	}

	public GitContentInfoSet getGitContentInfoSet() {
		return new GitContentInfoSet(this.sourceDocumentInfo.getContentInfoSet());
	}

	public void setGitContentInfoSet(GitContentInfoSet gitContentInfoSet) {
		this.sourceDocumentInfo.setContentInfoSet(gitContentInfoSet.getContentInfoSet());
	}

	public GitTechInfoSet getGitTechInfoSet() {
		return new GitTechInfoSet(this.sourceDocumentInfo.getTechInfoSet());
	}

	public void setGitTechInfoSet(GitTechInfoSet gitTechInfoSet) {
		this.sourceDocumentInfo.setTechInfoSet(gitTechInfoSet.getTechInfoSet());
	}
}
