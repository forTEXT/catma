package de.catma.project.conflict;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;

public class TagConflict {

	private Resolution resolution = null;

	private TagDefinition masterTagDefinition;
	private TagDefinition devTagDefinition;
	private boolean bothPossible = false;
	
	public TagConflict(
			TagDefinition masterTagDefinition, TagDefinition devTagDefinition) {
		super();
		this.masterTagDefinition = masterTagDefinition;
		this.devTagDefinition = devTagDefinition;
		this.bothPossible = 
			((this.masterTagDefinition != null) && (this.devTagDefinition != null));
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
			throw new IllegalStateException("This Tag conflict is not resolved yet!");
		}
		
		switch (this.resolution) {
		case MINE: {
			return getDevTagDefinition();
		}
		case THEIRS: {
			return getMasterTagDefinition();
		}
		default: {
			if (!isBothPossible()) {
				throw new IllegalStateException(
						"Cannot resolve Tag conflict with both versions!");
			}
			return createCombinedTagDefinition();
		}
		
		}
	}

	public TagDefinition getDismissedTagDefinition() {
		if (!isResolved()) {
			throw new IllegalStateException("This Tag conflict is not resolved yet!");
		}
		
		switch (this.resolution) {
		case MINE: {
			return getMasterTagDefinition();
		}
		case THEIRS: {
			return getDevTagDefinition();
		}
		default: {
			if (!isBothPossible()) {
				throw new IllegalStateException(
						"Cannot resolve Tag conflict with both versions!");
			}
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
		if (resolution.equals(Resolution.BOTH) && !isBothPossible()) {
			throw new IllegalStateException(
				"Cannot resolve Tag conflict with both versions!");
		}
		this.resolution = resolution;
	}
	
	public boolean isBothPossible() {
		return bothPossible;
	}
	
}
