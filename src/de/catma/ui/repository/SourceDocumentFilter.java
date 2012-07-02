package de.catma.ui.repository;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.data.Item;
import com.vaadin.data.Container.Filter;

import de.catma.document.Corpus;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

class SourceDocumentFilter implements Filter {
	
	private Set<String> corpusContent;
	
	public SourceDocumentFilter(Corpus corpus) {
		super();
		corpusContent = new HashSet<String>();
		for (SourceDocument sd : corpus.getSourceDocuments()) {
			corpusContent.add(sd.getID());
		}

		for (UserMarkupCollectionReference ucr : corpus.getUserMarkupCollectionRefs()) {
			corpusContent.add(ucr.getId());
		}
		
		for (StaticMarkupCollectionReference scr : corpus.getStaticMarkupCollectionRefs()) {
			corpusContent.add(scr.getId());
		}
	}

	public boolean appliesToProperty(Object propertyId) {
		return true;
	}
	
	public boolean passesFilter(Object itemId, Item item)
			throws UnsupportedOperationException {
		if (itemId instanceof MarkupCollectionItem) {
			return corpusContent.contains(
					((MarkupCollectionItem)itemId).getParentId());
		}
		else if (itemId instanceof SourceDocument){
			return corpusContent.contains(
					((SourceDocument)itemId).getID());
		}
		else if (itemId instanceof UserMarkupCollectionReference) {
			return corpusContent.contains(
					((UserMarkupCollectionReference)itemId).getId());
		}
		else if (itemId instanceof StaticMarkupCollectionReference) {
			return corpusContent.contains(
					((StaticMarkupCollectionReference)itemId).getId());
		}
		
		return false;
	}
	
	
}