package de.catma.ui.modules.project;

import de.catma.tag.PropertyDefinition;

public class PropertyDefValueDataItem implements PropertyDefTreeItem {
	
	private PropertyDefinition mine;
	private PropertyDefinition theirs;
	private String value;
	
	public PropertyDefValueDataItem(PropertyDefinition mine, PropertyDefinition theirs, String value) {
		super();
		this.mine = mine;
		this.theirs = theirs;
		this.value = value;
	}

	@Override
	public String getMinePropertyName() {
		return null;
	}

	@Override
	public String getMinePropertyValue() {
		if (mine != null && mine.getPossibleValueList().contains(value)) {
			return value;
		}
		return null;
	}

	@Override
	public String getTheirPropertyName() {
		return null;
	}

	@Override
	public String getTheirPropertyValue() {
		if (theirs != null && theirs.getPossibleValueList().contains(value)) {
			return value;
		}
		return null;
	}

}
