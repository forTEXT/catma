package de.catma.ui.repository.entry;

import com.vaadin.data.util.BeanItem;

import de.catma.core.document.source.ContentInfoSet;
import de.catma.core.document.source.SourceDocument;

public class SourceDocumentEntry implements TreeEntry {
	
	private SourceDocument sourceDocument;
	private BeanItem<ContentInfo> contentInfo;
	
	public SourceDocumentEntry(SourceDocument sourceDocument) {
		super();
		this.sourceDocument = sourceDocument;
		ContentInfoSet cis = sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet();
		this.contentInfo = new BeanItem<ContentInfo>(new StandardContentInfo(
				cis.getTitle(), cis.getAuthor(), 
				cis.getDescription(), cis.getPublisher()));
	}



	public BeanItem<ContentInfo> getContentInfo() {
		return this.contentInfo;
	}
	
	@Override
	public String toString() {
		return sourceDocument.toString();
	}

}
