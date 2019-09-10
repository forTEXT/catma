package de.catma.ui.module.analyze.resourcepanel;

import com.vaadin.ui.TreeGrid;

import de.catma.document.corpus.Corpus;

public interface DocumentTreeItem {
	public String getName();
	public String getIcon();
	public String getPermissionIcon();
	public void addToCorpus(Corpus corpus);
	public default void ensureSelectedParent(TreeGrid<DocumentTreeItem> documentTree) { /* noop */ }
}
