package de.catma.ui.repository.entry;

import com.vaadin.data.util.BeanItem;

import de.catma.ui.repository.MarkupCollectionsNode;

public class MarkupCollectionsEntry implements TreeEntry {
	
	private BeanItem<ContentInfo> contentInfo =	
			new BeanItem<ContentInfo>(new StandardContentInfo());
	
	private MarkupCollectionsNode markupCollectionsNode;
	
	public MarkupCollectionsEntry(MarkupCollectionsNode markupCollectionsNode) {
		super();
		this.markupCollectionsNode = markupCollectionsNode;
	}

	public BeanItem<ContentInfo> getContentInfo() {
		return contentInfo;
	}

	@Override
	public String toString() {
		return this.markupCollectionsNode.toString();
	}
}
