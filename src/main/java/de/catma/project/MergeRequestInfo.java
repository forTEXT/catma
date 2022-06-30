package de.catma.project;

import java.util.Date;

public class MergeRequestInfo {
	
	public final Long iid;
	public final String title;
	public final String description;
	public final Date createdAt;
	public final Long glProjectId;
	private String state;
	private String mergeStatus;
	
	public MergeRequestInfo(
			Long iid, 
			String title, String description, Date createdAt, 
			String state, String mergeStatus, Long glProjectId) {
		super();
		this.iid = iid;
		this.title = title;
		this.description = description;
		this.createdAt = createdAt;
		this.glProjectId = glProjectId;
		this.state = state;
		this.mergeStatus = mergeStatus;
	}
	public Long getIid() {
		return iid;
	}
	public String getTitle() {
		return title;
	}
	public String getDescription() {
		return description;
	}
	public Date getCreatedAt() {
		return createdAt;
	}
	
	public Long getGlProjectId() {
		return glProjectId;
	}
	
	public boolean canBeMerged() {
		return this.mergeStatus != null 
				&& this.mergeStatus.equals("can_be_merged");
	}
	
	public boolean isOpen() {
		return this.state != null && this.state.equals("opened");
	}
	
	
	public boolean isMerged() {
		return this.state != null && this.state.equals("merged");
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MergeRequest #");
		builder.append(iid);
		builder.append(" ");
		builder.append(title);
		builder.append(" Status: ");
		builder.append(state);
		builder.append(" MergeStatus: " );
		builder.append(mergeStatus);
		return builder.toString();
	}
	
	public boolean isMergeStatusCheckInProgress() {
		return this.mergeStatus != null 
			&& (this.mergeStatus.equals("checking") || this.mergeStatus.equals("unchecked"));
	}
}
