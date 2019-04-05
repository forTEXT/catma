package de.catma.ui.modules.project;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.project.conflict.ConflictedProject;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.ui.component.hugecard.HugeCard;

public class ConflictedProjectView extends HugeCard {
	
	private VerticalLayout mainPanel;
	private ConflictedProject conflictedProject;
	private Iterator<CollectionConflict> collectionConflictIterator;
	private Iterator<AnnotationConflict> annotationConflictIterator;
	private TagManager tagManager;
	private Collection<SourceDocument> documents;

	@Inject
    public ConflictedProjectView(ConflictedProject conflictedProject, EventBus eventBus){
    	super("Resolve Project Conflicts");
    	this.conflictedProject = conflictedProject;
    	initComponents();
    	initActions();
    	initData();
	}

	private void initActions() {
		// TODO Auto-generated method stub
		
	}
	
	private void showNextCollectionConflict() throws IOException {
		CollectionConflict collectionConflict = collectionConflictIterator.next();
		annotationConflictIterator = collectionConflict.getAnnotationConflicts().iterator();
		showNextAnnotationConflict(collectionConflict);
	}

	private void showNextAnnotationConflict(CollectionConflict collectionConflict) throws IOException {
		SourceDocument document = 
				documents
				.stream()
				.filter(doc -> doc.getID().equals(collectionConflict.getDocumentId()))
				.findFirst()
				.get();
		KwicProvider kwicProvider = new KwicProvider(document);
		
		AnnotationConflict annotationConflict  = annotationConflictIterator.next();
		AnnotationConflictView annotationConflictView = 
				new AnnotationConflictView(annotationConflict, collectionConflict, tagManager, kwicProvider);
		mainPanel.removeAllComponents();
		mainPanel.addComponent(annotationConflictView);
	}

	private void initData() {
		try {
			this.tagManager = new TagManager(new TagLibrary(null, "conflicted-project"));
			this.documents = conflictedProject.getDocuments();
			conflictedProject.getTagsets().stream().forEach(tagset -> tagManager.addTagsetDefinition(tagset));
			collectionConflictIterator = this.conflictedProject.getCollectionConflicts().iterator();
			showNextCollectionConflict();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	private void initComponents() {
		mainPanel = new VerticalLayout();
		mainPanel.setWidthUndefined();
		addComponent(mainPanel);
		
		
	}
}
