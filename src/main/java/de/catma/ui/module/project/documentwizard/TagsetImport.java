package de.catma.ui.module.project.documentwizard;

import de.catma.tag.TagsetDefinition;

public class TagsetImport {
	
	private String namespace;
	private TagsetDefinition extractedTagset;
	private TagsetDefinition targetTagset;
	private TagsetImportState importState;

	public TagsetImport(
			String namespace, 
			TagsetDefinition extractedTagset, 
			TagsetDefinition targetTagset, TagsetImportState importState) {
		super();
		this.namespace = namespace;
		this.extractedTagset = extractedTagset;
		this.targetTagset = targetTagset;
		this.importState = importState;
	}
	
	public String getNamespace() {
		return namespace;
	}
	public TagsetDefinition getExtractedTagset() {
		return extractedTagset;
	}
	
	public String getTargetName() {
		return targetTagset==null?"":targetTagset.getName();
	}
	
	public TagsetImportState getImportState() {
		return importState;
	}
	
	public void setImportState(TagsetImportState importState) {
		this.importState = importState;
	}
	
	public void setTargetTagset(TagsetDefinition targetTagset) {
		this.targetTagset = targetTagset;
	}
	
	public TagsetDefinition getTargetTagset() {
		return targetTagset;
	}
}