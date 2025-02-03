package de.catma.ui.module.analyze.visualization.kwic.annotation.edit;

import com.vaadin.icons.VaadinIcons;

class PropertyActionItem implements ActionItem {

	private PropertyAction propertyAction;
	
	public PropertyActionItem(PropertyAction propertyAction) {
		super();
		this.propertyAction = propertyAction;
	}

	@Override
	public String getPropertyName() {
		return null; // intended
	}

	@Override
	public String getActionDescription() {
		switch (propertyAction.type()) {
		case ADD: return String.format("Add value \"%s\"", propertyAction.value());
		case REPLACE: return String.format("Replace value \"%s\" with value \"%s\"", propertyAction.value(), propertyAction.replaceValue());
		case REMOVE: return String.format("Remove value \"%s\"", propertyAction.value());
		default: return "";
		}
	}
	
	@Override
	public String getRemoveItemIcon() {
		return VaadinIcons.TRASH.getHtml();
	}

	public PropertyAction getPropertyAction() {
		return propertyAction;
	}
}
