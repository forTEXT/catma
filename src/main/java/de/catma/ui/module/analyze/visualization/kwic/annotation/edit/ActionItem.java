package de.catma.ui.module.analyze.visualization.kwic.annotation.edit;

interface ActionItem {
	public String getPropertyName();
	public String getActionDescription();
	public default String getAddValueIcon() {return null;};
	public default String getReplaceValueIcon() {return null;};
	public default String getDeleteValueIcon() {return null;};
	public default String getRemoveItemIcon() {return null;};
}
