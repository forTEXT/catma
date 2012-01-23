package de.catma.ui.repository.entry;

import com.vaadin.data.util.BeanItem;

public class MarkupCollectionEntry implements TreeEntry {
	
	private Object markupColl;
	private BeanItem<ContentInfo> contentInfo;
	
	public MarkupCollectionEntry(Object markupColl) {
		super();
		this.markupColl = markupColl;
		this.contentInfo = new BeanItem<ContentInfo>(new StandardContentInfo());
	}



	public BeanItem<ContentInfo> getContentInfo() {
		return this.contentInfo;
	}
	
	@Override
	public String toString() {
		return markupColl.toString();
	}

}
