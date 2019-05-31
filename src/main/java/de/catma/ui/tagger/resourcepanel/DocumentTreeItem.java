package de.catma.ui.tagger.resourcepanel;

public interface DocumentTreeItem {
	public String getSelectionIcon();
	public String getName();
	public String getIcon();
	public String getPermissionIcon();
	public void setSelected(boolean value);
	public boolean isSingleSelection();
	public boolean isSelected();
	public void fireSelectedEvent(ResourceSelectionListener resourceSelectionListener);
}
