package de.catma.ui.module.project;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.project.conflict.ConflictedProject;
import de.catma.project.conflict.DeletedResourceConflict;
import de.catma.project.conflict.TagConflict;
import de.catma.project.conflict.TagsetConflict;
import de.catma.repository.git.CommitMissingException;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.ExceptionUtil;

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

    public ConflictedProjectView(ConflictedProject conflictedProject, EventBus eventBus){
    	super("Resolve Project Conflicts");
    	this.conflictedProject = conflictedProject;
    	this.eventBus = eventBus;
    	initComponents();
    	initData();
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
				.filter(doc -> doc.getUuid().equals(currentCollectionConflict.getDocumentId()))
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
			if ((this.tagManager == null) // Tag/Tagset conflict resolution not applied yet
				&& ((tagsetConflictsIterator == null) || !this.tagsetConflictsIterator.hasNext()) // all Tagset conflicts resolved by the use
				&& ((tagConflictIterator == null) || !this.tagConflictIterator.hasNext())) { // all Tag conflicts resolved by the user
				// apply Tag/Tagset conflict resolutions
				conflictedProject.resolveTagsetConflicts(this.tagsetConflicts);
				
				// load Tagsets
				this.tagManager = new TagManager(new TagLibrary());

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
				try {
					Collection<DeletedResourceConflict> deletedReourceConflicts = 
							conflictedProject.resolveRootConflicts();
					
					if (!deletedReourceConflicts.isEmpty()) {
						showDeletedResourceConflicts(deletedReourceConflicts);
						return;
					}
				}
				catch (Exception e) {
					if (ExceptionUtil.stackTraceContains(CommitMissingException.class.getName(), e)) {
						Notification.show(
							"Error", 
							ExceptionUtil.getMessageFor(CommitMissingException.class.getName(), e), 
							Type.ERROR_MESSAGE);
						eventBus.post(new RouteToDashboardEvent());
						return;
					}
					else {
						// rethrow unexpected error
						throw new Exception(e);
					}
				}
				// TODO: check if there everything is pushed, remote state might have prevented a push
				eventBus.post(new RouteToProjectEvent(conflictedProject.getProjectReference(), true));
			}
		}
		catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("Error showing next conflict!", e);
		}
	}

	private void showDeletedResourceConflicts(Collection<DeletedResourceConflict> deletedReourceConflicts) {
		final Iterator<DeletedResourceConflict> deletedResourceConflictsIterator = deletedReourceConflicts.iterator();
		showNextDeletedResourceConflict(deletedReourceConflicts, deletedResourceConflictsIterator);
	}

	private void showNextDeletedResourceConflict(Collection<DeletedResourceConflict> deletedReourceConflicts,
			Iterator<DeletedResourceConflict> deletedResourceConflictsIterator) {
		if (deletedResourceConflictsIterator.hasNext()) {
			DeletedResourceConflict deletedResourceConflict = deletedResourceConflictsIterator.next();
			
			DeletedResourceConflictView deletedResourceConflictView = new DeletedResourceConflictView(deletedResourceConflict, new ResolutionListener() {
				
				@Override
				public void resolved() {
					showNextDeletedResourceConflict(deletedReourceConflicts, deletedResourceConflictsIterator);
				}
			});
			mainPanel.removeAllComponents();
			mainPanel.addComponent(deletedResourceConflictView);
		}
		else {
			try {
				conflictedProject.resolveDeletedResourceConflicts(deletedReourceConflicts);
				showNextConflict();
			} catch (Exception e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("Error resolving deleted resource conflicts!", e);
			}
		}
	}

	private void showNextTagsetConflict() {
		currentTagsetConflict = tagsetConflictsIterator.next();
		tagConflictIterator = currentTagsetConflict.getTagConflicts().iterator();
		showNextTagConflict();
	}

	private void showNextTagConflict() {
		if (tagConflictIterator.hasNext()) {
			TagConflict tagConflict = tagConflictIterator.next();
			TagConflictView tagConflictView = 
				new TagConflictView(
					tagConflict,
					currentTagsetConflict,
					()->showNextConflict());
			mainPanel.removeAllComponents();
			mainPanel.addComponent(tagConflictView);
		}
		else {
			showNextConflict();
		}
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
			((ErrorHandler)UI.getCurrent()).showAndLogError("Error loading conflicted Project!", e);
		}
	}
	
	

	private void initComponents() {
		mainPanel = new VerticalLayout();
		mainPanel.setSizeFull();
		addComponent(mainPanel);
		setExpandRatio(mainPanel, 1f);
	}
}
