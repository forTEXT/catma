package de.catma.ui.module.analyze.resourcepanel;

import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.TreeGrid;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.corpus.Corpus;

public class CollectionDataItem implements DocumentTreeItem {

	private AnnotationCollectionReference collectionRef;
	private boolean hasWritePermission;

	public CollectionDataItem(AnnotationCollectionReference collectionRef, boolean hasWritePermission) {
		super();
		this.collectionRef = collectionRef;
		this.hasWritePermission = hasWritePermission;
	}

	@Override
	public String getName() {
		return collectionRef.getName();
	}

	public AnnotationCollectionReference getCollectionRef() {
		return collectionRef;
	}
	
	@Override
	public String getIcon() {
		return VaadinIcons.NOTEBOOK.getHtml();
	}
	
	@Override
	public String getPermissionIcon() {
		return hasWritePermission?VaadinIcons.UNLOCK.getHtml():VaadinIcons.LOCK.getHtml();
	}

	@Override
	public void addToCorpus(Corpus corpus) {
		corpus.addUserMarkupCollectionReference(collectionRef);
	}
	
	@Override
	public void ensureSelectedParent(TreeGrid<DocumentTreeItem> documentTree) {
		@SuppressWarnings("unchecked")
		TreeDataProvider<DocumentTreeItem> dataProvider = 
			(TreeDataProvider<DocumentTreeItem>) documentTree.getDataProvider();
		if (dataProvider.getTreeData().contains(this)) {
			documentTree.select(dataProvider.getTreeData().getParent(this));
		}
	}
}
