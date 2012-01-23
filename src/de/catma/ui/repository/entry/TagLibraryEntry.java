package de.catma.ui.repository.entry;

import com.vaadin.data.util.BeanItem;

import de.catma.core.tag.TagLibraryReference;

public class TagLibraryEntry implements TreeEntry {
	
	private BeanItem<ContentInfo> contentInfo;
	private TagLibraryReference tagLibraryReference;
	
	public TagLibraryEntry(TagLibraryReference tagLibraryReference) {
		super();
		this.tagLibraryReference = tagLibraryReference;
		this.contentInfo = new BeanItem<ContentInfo>(new StandardContentInfo());
	}

	public BeanItem<ContentInfo> getContentInfo() {
		return contentInfo;
	}

	@Override
	public String toString() {
		return tagLibraryReference.toString();
	}
}
