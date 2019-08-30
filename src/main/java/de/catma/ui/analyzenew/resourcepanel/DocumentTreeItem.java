package de.catma.ui.analyzenew.resourcepanel;

import com.vaadin.ui.TreeGrid;

import de.catma.document.Corpus;

public interface DocumentTreeItem {
	public String getName();
	public String getIcon();
	public String getPermissionIcon();
	public void addToCorpus(Corpus corpus);
	public default void ensureSelectedParent(TreeGrid<DocumentTreeItem> documentTree) { /* noop */ }
}
