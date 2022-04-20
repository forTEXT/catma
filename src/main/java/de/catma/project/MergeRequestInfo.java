package de.catma.project;

import java.util.Date;

public class MergeRequestInfo {
	
	public final int iid;
	public final String title;
	public final String description;
	public final Date createdAt;
	
	public MergeRequestInfo(int iid, String title, String description, Date createdAt) {
		super();
		this.iid = iid;
		this.title = title;
		this.description = description;
		this.createdAt = createdAt;
	}
	public int getIid() {
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
}
