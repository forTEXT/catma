package de.catma.project.conflict;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;

public class TagConflict {

	private Resolution resolution = null;

	private TagDefinition masterTagDefinition;
	private TagDefinition devTagDefinition;
	
	public TagConflict(
			TagDefinition masterTagDefinition, TagDefinition devTagDefinition) {
		super();
		this.masterTagDefinition = masterTagDefinition;
		this.devTagDefinition = devTagDefinition;
	}

	public boolean isResolved() {
		return this.resolution != null;
	}

	public Resolution getResolution() {
		return resolution;
	}

	public TagDefinition getMasterTagDefinition() {
		return masterTagDefinition;
	}

	public TagDefinition getDevTagDefinition() {
		return devTagDefinition;
	}

	public TagDefinition getResolvedTagDefinition() {
		if (!isResolved()) {
			throw new IllegalStateException("this Tag Conflict is not resolved yet!");
		}
		
		switch (this.resolution) {
		case MINE: {
			return getDevTagDefinition();
		}
		case THEIRS: {
			return getMasterTagDefinition();
		}
		default: {
			return createCombinedTagDefinition();
		}
		
		}
	}

	private TagDefinition createCombinedTagDefinition() {
		
		TagDefinition tagDefinition = new TagDefinition(masterTagDefinition);
		
		for (PropertyDefinition devPropertyDef : devTagDefinition.getUserDefinedPropertyDefinitions()) {
			PropertyDefinition combinedPropertyDef  = 
				tagDefinition.getPropertyDefinitionByUuid(devPropertyDef.getUuid());
			if (combinedPropertyDef == null) {
				combinedPropertyDef = new PropertyDefinition(devPropertyDef);
				tagDefinition.addUserDefinedPropertyDefinition(combinedPropertyDef);
			}
			else {
				for (String value : devPropertyDef.getPossibleValueList()) {
					if (!combinedPropertyDef.getPossibleValueList().contains(value)) {
						combinedPropertyDef.addValue(value);
					}
				}
			}
		}
		
		return tagDefinition;
	}

	public void setResolution(Resolution resolution) {
		this.resolution = resolution;
	}
	
	
}
