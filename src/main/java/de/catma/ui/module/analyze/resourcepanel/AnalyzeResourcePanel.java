package de.catma.ui.module.analyze.resourcepanel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.data.SelectionModel;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.corpus.Corpus;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.project.event.ChangeType;
import de.catma.project.event.CollectionChangeEvent;
import de.catma.project.event.DocumentChangeEvent;
import de.catma.project.event.ProjectReadyEvent;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.module.main.ErrorHandler;

public class AnalyzeResourcePanel extends VerticalLayout {

	private Project project;
	private Corpus corpus;
	
	private TreeGrid<DocumentTreeItem> documentTree;
	private TreeData<DocumentTreeItem> documentData;
	
	private ActionGridComponent<TreeGrid<DocumentTreeItem>> documentActionGridComponent;
	private EventBus eventBus;
	private CorpusChangedListener corpusChangedListener;
	private Set<DocumentTreeItem> lastSelection = new HashSet<DocumentTreeItem>();
	
	public AnalyzeResourcePanel(EventBus eventBus, Project project, Corpus corpus, CorpusChangedListener corpusChangedListener) {
		this.eventBus = eventBus;
		eventBus.register(this);
		this.project = project;
		this.corpus = corpus;
		this.corpusChangedListener = corpusChangedListener;
		initComponents();
		initData();
		initActions();
	}

	private void initData() {
		documentData = new TreeData<>();
		try {
			Collection<SourceDocumentReference> documents = project.getSourceDocumentReferences(); 
			
			documentData.addRootItems(
				documents
				.stream()
				.map(document -> new DocumentDataItem(document)));
						
			for (DocumentTreeItem documentDataItem : documentData.getRootItems()) {
				for (AnnotationCollectionReference umcRef : 
					((DocumentDataItem)documentDataItem).getDocument().getUserMarkupCollectionRefs()) {
					documentData.addItem(
						documentDataItem, new CollectionDataItem(umcRef));
				}
			}
			
			documentTree.setDataProvider(new TreeDataProvider<>(documentData));			
			
			Collection<SourceDocumentReference> selectedDocuments = corpus.getSourceDocuments();
			Collection<AnnotationCollectionReference> selectedCollections = 
					corpus.getUserMarkupCollectionRefs();
			
			documentData.getRootItems().stream()
					.filter(documentItem -> 
						selectedDocuments.contains(((DocumentDataItem) documentItem).getDocument()))
					.forEach(documentTree::select);
	
			for (DocumentTreeItem documentDataItem : documentData.getRootItems()) {
				List<DocumentTreeItem> collectionItems = documentData.getChildren(documentDataItem);
				for (DocumentTreeItem oneCollection : collectionItems) {
					if (selectedCollections.contains(((CollectionDataItem) oneCollection).getCollectionRef())) {
						documentTree.getSelectionModel().select(oneCollection);
					}
				}
			}
			
			documentTree.expand(documentData.getRootItems());
		}
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error loading project data", e);
		}
	}
	
	@Subscribe
	public void handleProjectReadyEvent(ProjectReadyEvent projectReadyEvent) {
		Corpus corpus = getCorpus();
		List<String> documentIds = corpus.getDocumentIds();
		List<String> collectionIds = corpus.getCollectionIds();
		
		initData();
		
		for (DocumentTreeItem documentDataItem : documentData.getRootItems()) {
			List<DocumentTreeItem> collectionItems = documentData.getChildren(documentDataItem);
			if (documentIds.contains(documentDataItem.getUuid())) {
				documentTree.getSelectionModel().select(documentDataItem);
				for (DocumentTreeItem oneCollection : collectionItems) {
					if (collectionIds.contains(oneCollection.getUuid())) {
						documentTree.getSelectionModel().select(oneCollection);
					}
				}
			}
		}
		
		corpusChangedListener.corpusChanged();
		
	}
	
    @SuppressWarnings("unchecked")
	@Subscribe
    public void handleDocumentChanged(DocumentChangeEvent documentChangeEvent) {
    	if (documentChangeEvent.getChangeType().equals(ChangeType.CREATED)) {
    		SourceDocumentReference documentRef = documentChangeEvent.getDocument();
			documentData.addItem(null, new DocumentDataItem(documentRef));    		
    	}
    	else if (documentChangeEvent.getChangeType().equals(ChangeType.DELETED)) {
    		Optional<DocumentTreeItem> optionalDocItem = 
    			documentData.getRootItems()
    			.stream()
    			.filter(item -> ((DocumentDataItem)item).getDocument().equals(documentChangeEvent.getDocument()))
    			.findAny();
    		if (optionalDocItem.isPresent()) {
    			
    			DocumentTreeItem docItem = optionalDocItem.get();
    			
    			List<DocumentTreeItem> children = documentData.getChildren(docItem);
    			
    			documentData.removeItem(docItem);
    			Set<DocumentTreeItem> updated = new HashSet<>(children);
    			updated.add(docItem);
				
    			// selections needs manual update...
				((SelectionModel.Multi<DocumentTreeItem>)documentTree.getSelectionModel()).updateSelection(
					Collections.emptySet(), updated);

    			corpusChangedListener.corpusChanged();
    		}
    	}
    	else {
    		documentData.getRootItems()
    		.stream()
    		.filter(item -> ((DocumentDataItem)item).getDocument().equals(documentChangeEvent.getDocument()))
    		.findAny()
    		.ifPresent(item -> documentTree.getDataProvider().refreshItem(item));
    		corpusChangedListener.corpusChanged();
    	}
    	
    }
	
	@SuppressWarnings("unchecked")
	@Subscribe
	public void handleCollectionChanged(CollectionChangeEvent collectionChangeEvent) {
		if (collectionChangeEvent.getChangeType().equals(ChangeType.CREATED)) {
			
    		SourceDocumentReference documentRef = collectionChangeEvent.getDocument();
    		AnnotationCollectionReference collectionReference = 
    				collectionChangeEvent.getCollectionReference();

    		
			CollectionDataItem collectionDataItem = 
				new CollectionDataItem(collectionReference); 

			documentData.getRootItems()
			.stream()
			.filter(item -> ((DocumentDataItem)item).getDocument().equals(documentRef))
			.findAny().ifPresent(documentDataItem -> {
				documentData.addItem(
	    				documentDataItem, collectionDataItem);
				documentTree.getDataProvider().refreshAll();
			});
			
			if (isAttached()) {
				documentTree.expand(documentData.getParent(collectionDataItem));
				Notification.show(
					"Info", 
					String.format("Collection \"%s\" has been created", collectionReference.toString()),
					Type.TRAY_NOTIFICATION);
			}
    		
    	}
		else if (collectionChangeEvent.getChangeType().equals(ChangeType.DELETED)) {
			Optional<DocumentTreeItem> optionalDocResource = documentData.getRootItems()
			.stream()
			.filter(item -> ((DocumentDataItem)item).getDocument().equals(collectionChangeEvent.getDocument()))
			.findAny();
			
			if (optionalDocResource.isPresent()) {
				Optional<DocumentTreeItem> optionalCollectionResource = 
						documentData.getChildren(optionalDocResource.get()).stream()
						.filter(item -> 
							((CollectionDataItem)item).getCollectionRef().equals(
									collectionChangeEvent.getCollectionReference()))
						.findAny();
				if (optionalCollectionResource.isPresent()) {
					DocumentTreeItem collectionItem = optionalCollectionResource.get();
					documentData.removeItem(collectionItem);
					documentTree.getDataProvider().refreshAll();
					// selections needs manual update...
					((SelectionModel.Multi<DocumentTreeItem>)documentTree.getSelectionModel()).updateSelection(
						Collections.emptySet(), Collections.singleton(collectionItem));
				}
				
				corpusChangedListener.corpusChanged();
			}
		}
    	else {
    		documentTree.getDataProvider().refreshAll();
    		corpusChangedListener.corpusChanged();
    	}	
		
	}	

	private void initComponents() {
		Label documentTreeLabel = new Label("Documents & Annotations");

		documentTree = TreeGridFactory.createDefaultTreeGrid();
		documentTree.addStyleNames("resource-grid", "flat-undecorated-icon-buttonrenderer");

		documentTree
			.addColumn(documentTreeItem -> documentTreeItem.getName())
			.setCaption("Name")
			.setWidth(300);
	

		documentTree
			.addColumn(
				documentTreeItem -> documentTreeItem.getIcon(), new HtmlRenderer())
			.setExpandRatio(1);

		documentTree.setSizeFull();

		documentActionGridComponent = new ActionGridComponent<TreeGrid<DocumentTreeItem>>(documentTreeLabel,
				documentTree);

		documentActionGridComponent.setSelectionModeFixed(Grid.SelectionMode.MULTI);
		documentActionGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
		documentActionGridComponent.getActionGridBar().setMargin(new MarginInfo(false, false, false, true));

		addComponent(documentActionGridComponent);

	}

	private void initActions() {
		
		documentTree.addSelectionListener(new SelectionListener<DocumentTreeItem>() {
			@Override
			public void selectionChange(SelectionEvent<DocumentTreeItem> event) {
				if (event.isUserOriginated()) {
					Set<DocumentTreeItem> selectedItems = new HashSet<>(event.getAllSelectedItems());
					
					Set<DocumentTreeItem> deselectedRootItems = 
						lastSelection
						.stream()
						.filter(item -> !event.getAllSelectedItems().contains(item) && item instanceof DocumentDataItem)
						.collect(Collectors.toSet());
					
					for (DocumentTreeItem item : deselectedRootItems) {
						if (!event.getAllSelectedItems().contains(item)) {
							documentData.getChildren(item).forEach(child -> {
								documentTree.deselect(child);
								selectedItems.remove(child);
							});
						}
					}
					
					selectedItems.forEach(item -> item.ensureSelectedParent(documentTree));
					lastSelection.clear();
					lastSelection.addAll(documentTree.getSelectedItems());
					
					corpusChangedListener.corpusChanged();
				}
			}
		});
	}

	public Corpus getCorpus() {
		Corpus corpus = new Corpus();
		
		documentTree.getSelectedItems().forEach(item -> item.addToCorpus(corpus));
		
		return corpus;
	}
	
	public void close() {
		eventBus.unregister(this);
		eventBus = null;
		project = null;
	}
}
