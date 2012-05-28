package de.catma.ui.repository;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.data.Item;
import com.vaadin.data.Container.Filter;

import de.catma.document.Corpus;
import de.catma.document.source.ISourceDocument;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

class SourceDocumentFilter implements Filter {
	
	private Set<String> corpusContent;
	
	public SourceDocumentFilter(Corpus corpus) {
		super();
		corpusContent = new HashSet<String>();
		for (ISourceDocument sd : corpus.getSourceDocuments()) {
			corpusContent.add(sd.toString());
		}

		for (UserMarkupCollectionReference ucr : corpus.getUserMarkupCollectionRefs()) {
			corpusContent.add(ucr.toString());
		}
		
		for (StaticMarkupCollectionReference scr : corpus.getStaticMarkupCollectionRefs()) {
			corpusContent.add(scr.toString());
		}
	}

	public boolean appliesToProperty(Object propertyId) {
		return true;
	}
	
	public boolean passesFilter(Object itemId, Item item)
			throws UnsupportedOperationException {
		
		return corpusContent.contains(itemId.toString());
	}
	
	
}