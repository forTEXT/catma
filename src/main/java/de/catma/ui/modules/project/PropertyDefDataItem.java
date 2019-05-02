package de.catma.ui.modules.project;

import de.catma.tag.PropertyDefinition;

public class PropertyDefDataItem implements PropertyDefTreeItem {
	
	private PropertyDefinition mine;
	private PropertyDefinition theirs;

	public PropertyDefDataItem(PropertyDefinition mine, PropertyDefinition theirs) {
		super();
		this.mine = mine;
		this.theirs = theirs;
	}

	@Override
	public String getMinePropertyName() {
		return mine != null?mine.getName():null;
	}

	@Override
	public String getMinePropertyValue() {
//		if ((mine != null) && (!mine.getPossibleValueList().isEmpty())) {
//			return VaadinIcons.CARET_DOWN.getHtml();
//		}
		return null;
	}

	@Override
	public String getTheirPropertyName() {
		return theirs != null?theirs.getName():null;
	}

	@Override
	public String getTheirPropertyValue() {
//		if ((theirs != null) && (!theirs.getPossibleValueList().isEmpty())) {
//			return VaadinIcons.CARET_DOWN.getHtml();
//		}
		return null;
	}
}
