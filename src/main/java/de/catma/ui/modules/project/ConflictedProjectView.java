package de.catma.ui.modules.project;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.project.ProjectReference;
import de.catma.project.TagsetConflict;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.project.conflict.ConflictedProject;
import de.catma.project.conflict.TagConflict;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.layout.VerticalLayout;

public class ConflictedProjectView extends HugeCard {
	
	private VerticalLayout mainPanel;
	private ConflictedProject conflictedProject;
	private Iterator<CollectionConflict> collectionConflictIterator;
	private Iterator<AnnotationConflict> annotationConflictIterator;
	private TagManager tagManager;
	private Collection<SourceDocument> documents;
	private CollectionConflict currentCollectionConflict;
	private List<CollectionConflict> collectionConflicts;
	private EventBus eventBus;
	private List<TagsetConflict> tagsetConflicts;
	private Iterator<TagsetConflict> tagsetConflictsIterator;
	private TagsetConflict currentTagsetConflict;
	private Iterator<TagConflict> tagConflictIterator;

	@Inject
    public ConflictedProjectView(ConflictedProject conflictedProject, EventBus eventBus){
    	super("Resolve Project Conflicts");
    	this.conflictedProject = conflictedProject;
    	this.eventBus = eventBus;
    	initComponents();
    	initActions();
    	initData();
	}

	private void initActions() {
		// TODO Auto-generated method stub
		
	}
	
	private void showNextCollectionConflict() throws IOException {
		currentCollectionConflict = collectionConflictIterator.next();
		annotationConflictIterator = currentCollectionConflict.getAnnotationConflicts().iterator();
		showNextAnnotationConflict();
	}

	private void showNextAnnotationConflict() throws IOException {
		SourceDocument document = 
				documents
				.stream()
				.filter(doc -> doc.getID().equals(currentCollectionConflict.getDocumentId()))
				.findFirst()
				.get();
		KwicProvider kwicProvider = new KwicProvider(document);
		
		AnnotationConflict annotationConflict  = annotationConflictIterator.next();
		AnnotationConflictView annotationConflictView = 
				new AnnotationConflictView(
					annotationConflict, 
					currentCollectionConflict, 
					tagManager, 
					kwicProvider,
					() -> showNextConflict());
		mainPanel.removeAllComponents();
		mainPanel.addComponent(annotationConflictView);
	}

	private void showNextConflict() {
		try {
			if ((this.tagManager == null)
				&& ((tagsetConflictsIterator == null) || !this.tagsetConflictsIterator.hasNext())
				&& ((tagConflictIterator == null) || !this.tagConflictIterator.hasNext())) {
				conflictedProject.resolveTagsetConflicts(this.tagsetConflicts);
				this.tagManager = new TagManager(new TagLibrary(null, "conflicted-project"));

				conflictedProject.getTagsets().stream().forEach(
						tagset -> tagManager.addTagsetDefinition(tagset));
			}

			if ((tagConflictIterator != null) && tagConflictIterator.hasNext()) {
				showNextTagConflict();
			}
			else if ((tagsetConflictsIterator != null) && tagsetConflictsIterator.hasNext()) {
				showNextTagsetConflict();
			}
			else if (annotationConflictIterator != null && annotationConflictIterator.hasNext()) {
				showNextAnnotationConflict();
			}
			else if (collectionConflictIterator != null && collectionConflictIterator.hasNext()) {
				showNextCollectionConflict();
				
			}
			else {
				conflictedProject.resolveCollectionConflict(
					this.collectionConflicts, this.tagManager.getTagLibrary());
				
				conflictedProject.resolveRootConflicts();
				
				eventBus.post(new RouteToProjectEvent(conflictedProject.getProjectReference()));
			}
		}
		catch (Exception e) {
			e.printStackTrace(); //TODO:
		}
	}

	private void showNextTagsetConflict() {
		currentTagsetConflict = tagsetConflictsIterator.next();
		tagConflictIterator = currentTagsetConflict.getTagConflicts().iterator();
		showNextTagConflict();
	}

	private void showNextTagConflict() {
		// TODO Auto-generated method stub
		
	}

	private void initData() {
		try {
			this.documents = conflictedProject.getDocuments();
			this.tagsetConflicts = conflictedProject.getTagsetConflicts();
			this.tagsetConflictsIterator = this.tagsetConflicts.iterator();
			this.collectionConflicts = this.conflictedProject.getCollectionConflicts();
			this.collectionConflictIterator = collectionConflicts.iterator();
			showNextConflict();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	private void initComponents() {
		mainPanel = new VerticalLayout();
		addComponent(mainPanel);
		
		
	}
}
