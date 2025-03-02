package de.catma.ui.module.analyze.visualization.kwic.annotation.edit;

interface ActionItem {
	String getPropertyName();
	String getActionDescription();
	default String getAddValueIcon() {return null;};
	default String getReplaceValueIcon() {return null;};
	default String getDeleteValueIcon() {return null;};
	default String getRemoveItemIcon() {return null;};
	default PropertyAction getPropertyAction() {return null;};
}
