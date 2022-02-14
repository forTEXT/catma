package de.catma.serialization;

import java.util.Set;

import de.catma.tag.TagsetDefinition;

public class TagsetDefinitionImportStatus {
	
	private TagsetDefinition tagset;
	private boolean current;
	private boolean doImport = true;
	private Set<String> tagDefinitionIds;
	
	/**
	 * @param tagset
	 * @param current <code>true</code> if currently present in the Project
	 */
	public TagsetDefinitionImportStatus(TagsetDefinition tagset, boolean current) {
		super();
		this.tagset = tagset;
		this.current = current;
	}

	public TagsetDefinition getTagset() {
		return tagset;
	}

	/**
	 * @return <code>true</code> if currently present in the Project, else <code>false</code>
	 */
	public boolean isCurrent() {
		return current;
	}
	
	public boolean isDoImport() {
		return doImport;
	}
	
	public void setDoImport(boolean doImport) {
		this.doImport = doImport;
	}

	public void setUpdateFilter(Set<String> tagDefinitionIds) {
		this.tagDefinitionIds = tagDefinitionIds;
	}

	public boolean passesUpdateFilter(String tagDefinitionId) {
		
		if (this.tagDefinitionIds != null) {
			return this.tagDefinitionIds.contains(tagDefinitionId);
		}
		
		return true;
	}
	
	
}
