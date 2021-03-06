package de.catma.project.conflict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TagsetConflict {
	
	private Resolution resolution;
	
	private List<TagConflict> tagConflicts;
	private String projectId;
	private String uuid;
	private String name;
	
	private Set<String> devDeletedDefinitions;
	private Set<String> masterDeletedDefinitions;
	
	private boolean headerConflict;
	
	public TagsetConflict(String projectId, String uuid, String name,
			Set<String> devDeletedDefinitions) {
		super();
		this.tagConflicts = new ArrayList<>();
		this.projectId = projectId;
		this.uuid = uuid;
		this.name = name;
		this.devDeletedDefinitions = devDeletedDefinitions;
		this.resolution = Resolution.MINE;
	}

	public TagsetConflict(String projectId, String uuid, String name, Set<String> devDeletedDefinitions,
			Set<String> masterDeletedDefinitions) {
		super();
		this.projectId = projectId;
		this.uuid = uuid;
		this.name = name;
		this.devDeletedDefinitions = devDeletedDefinitions;
		this.masterDeletedDefinitions = masterDeletedDefinitions;
		this.tagConflicts = new ArrayList<>();
	}

	public boolean isResolved() {
		return this.resolution != null;
	}

	public Resolution getResolution() {
		return resolution;
	}

	public List<TagConflict> getTagConflicts() {
		return Collections.unmodifiableList(tagConflicts);
	}

	public String getProjectId() {
		return projectId;
	}

	public String getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public Set<String> getDevDeletedDefinitions() {
		return devDeletedDefinitions;
	}

	public Set<String> getMasterDeletedDefinitions() {
		return masterDeletedDefinitions;
	}

	public void addTagConflict(TagConflict tagConflict) {
		this.tagConflicts.add(tagConflict);
	}
	
	public void setHeaderConflict(boolean headerConflict) {
		this.headerConflict = headerConflict;
	}
	
	public boolean isHeaderConflict() {
		return headerConflict;
	}
}
