package de.catma.ui.repository;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.data.Item;
import com.vaadin.data.Container.Filter;

import de.catma.core.document.Corpus;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.user.UserMarkupCollectionReference;

class SourceDocumentFilter implements Filter {
	
	private Set<String> corpusContent;
	
	public SourceDocumentFilter(Corpus corpus) {
		super();
		corpusContent = new HashSet<String>();
		for (SourceDocument sd : corpus.getSourceDocuments()) {
			corpusContent.add(sd.toString());
		}

		for (UserMarkupCollectionReference ucr : corpus.getUserMarkupCollectionRefs()) {
			corpusContent.add(ucr.toString());
		}
		
		for (StructureMarkupCollectionReference scr : corpus.getStructureMarkupCollectionRefs()) {
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