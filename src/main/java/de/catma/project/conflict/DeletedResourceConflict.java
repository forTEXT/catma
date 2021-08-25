package de.catma.project.conflict;

import de.catma.document.source.ContentInfoSet;

public class DeletedResourceConflict {
	
	public static enum ResourceType {
		ANNOTATION_COLLECTION,
		TAGSET,
		SOURCE_DOCUMENT
	}
	
	private ResourceType resourceType;
	private String ourCommitName;
	private String ourLastCommitMsg;
	private String theirCommitName;
	private String theirLastCommitMsg;
	private String theirCommiterName;
	private Resolution resolution;
	private String projectId;
	private String relativeModulePath;
	private String resourceId;
	private ContentInfoSet contentInfoSet;
	private boolean deletedByThem;

	public DeletedResourceConflict(
			String projectId, String relativeModulePath, 
			String ourCommitName, String ourLastCommitMsg, 
			String theirCommitName, String theirLastCommitMsg,
			String theirCommiterName,
			boolean deletedByThem) {
		this.projectId = projectId;
		this.relativeModulePath = relativeModulePath;
		this.ourCommitName = ourCommitName;
		this.ourLastCommitMsg = ourLastCommitMsg;
		this.theirCommitName = theirCommitName;
		this.theirLastCommitMsg = theirLastCommitMsg;
		this.theirCommiterName = theirCommiterName;
		this.deletedByThem = deletedByThem;
	}

	public boolean isResolved() {
		return this.resolution != null;
	}

	public Resolution getResolution() {
		return resolution;
	}

	public String getOurCommitName() {
		return ourCommitName;
	}

	public String getOurLastCommitMsg() {
		return ourLastCommitMsg;
	}

	public String getTheirCommitName() {
		return theirCommitName;
	}

	public String getTheirLastCommitMsg() {
		return theirLastCommitMsg;
	}

	public String getTheirCommiterName() {
		return theirCommiterName;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getRelativeModulePath() {
		return relativeModulePath;
	}
	
	public void setResolution(Resolution resolution) {
		this.resolution = resolution;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getResourceId() {
		return resourceId;
	}
	
	public void setContentInfoSet(ContentInfoSet contentInfoSet) {
		this.contentInfoSet = contentInfoSet;
	}
	
	public ContentInfoSet getContentInfoSet() {
		return contentInfoSet;
	}
	
	public boolean isDeletedByThem() {
		return deletedByThem;
	}
	
	public ResourceType getResourceType() {
		return resourceType;
	}
	
	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}
}
