package de.catma.ui.analyzenew.resourcepanel;

import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.TreeGrid;

import de.catma.document.Corpus;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public class CollectionDataItem implements DocumentTreeItem {

	private UserMarkupCollectionReference collectionRef;
	private boolean hasWritePermission;

	public CollectionDataItem(UserMarkupCollectionReference collectionRef, boolean hasWritePermission) {
		super();
		this.collectionRef = collectionRef;
		this.hasWritePermission = hasWritePermission;
	}

	@Override
	public String getName() {
		return collectionRef.getName();
	}

	public UserMarkupCollectionReference getCollectionRef() {
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
		
		documentTree.select(dataProvider.getTreeData().getParent(this));
	}
}
