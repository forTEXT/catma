package de.catma.serialization;

import de.catma.tag.TagsetDefinition;

public class TagsetDefinitionImportStatus {
	
	private TagsetDefinition tagset;
	private boolean inProjectHistory;
	private boolean current;
	private boolean doImport = true;
	
	public TagsetDefinitionImportStatus(TagsetDefinition tagset, boolean inProjectHistory, boolean current) {
		super();
		this.tagset = tagset;
		this.inProjectHistory = inProjectHistory;
		this.current = current;
	}

	public TagsetDefinition getTagset() {
		return tagset;
	}

	public boolean isInProjectHistory() {
		return inProjectHistory;
	}

	public boolean isCurrent() {
		return current;
	}
	
	public boolean isDoImport() {
		return doImport;
	}
	
	public void setDoImport(boolean doImport) {
		this.doImport = doImport;
	}
}
