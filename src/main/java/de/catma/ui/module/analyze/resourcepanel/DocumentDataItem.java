package de.catma.ui.module.analyze.resourcepanel;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.corpus.Corpus;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;

public class DocumentDataItem implements DocumentTreeItem {
	
	private SourceDocumentReference documentRef;
	
	public DocumentDataItem(SourceDocumentReference docRef) {
		this.documentRef = docRef;
	}

	@Override
	public String getName() {
		return documentRef.toString();
	}

	@Override
	public String getIcon() {
		return VaadinIcons.BOOK.getHtml();
	}

	public SourceDocumentReference getDocument() {
		return documentRef;
	}
	
	@Override
	public void addToCorpus(Corpus corpus) {
		corpus.addSourceDocument(documentRef);
	}
	
	@Override
	public String getUuid() {
		return documentRef.getUuid();
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
