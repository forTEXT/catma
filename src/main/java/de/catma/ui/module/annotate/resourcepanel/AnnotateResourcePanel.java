package de.catma.ui.module.annotate.resourcepanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.project.Project;
import de.catma.project.Project.RepositoryChangeEvent;
import de.catma.project.event.ChangeType;
import de.catma.project.event.CollectionChangeEvent;
import de.catma.project.event.DocumentChangeEvent;
import de.catma.project.event.ProjectReadyEvent;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.IDGenerator;

public class AnnotateResourcePanel extends VerticalLayout {
	
	private Project project;
	private TreeGrid<DocumentTreeItem> documentTree;
	private TreeData<DocumentTreeItem> documentData;
	private Grid<TagsetDefinition> tagsetGrid;
	private ResourceSelectionListener resourceSelectionListener;
	private ActionGridComponent<TreeGrid<DocumentTreeItem>> documentActionGridComponent;
	private PropertyChangeListener projectExceptionListener;
	private ErrorHandler errorHandler;
	private PropertyChangeListener tagsetChangeListener;
	private ListDataProvider<TagsetDefinition> tagsetData;
	private ActionGridComponent<Grid<TagsetDefinition>> tagsetActionGridComponent;
	private EventBus eventBus;

	public AnnotateResourcePanel(Project project, SourceDocument currentlySelectedSourceDocument, EventBus eventBus) {
		super();
		this.project = project;
        this.errorHandler = (ErrorHandler)UI.getCurrent();
        this.eventBus = eventBus;
        eventBus.register(this);
        initProjectListeners();
		
		initComponents();
		initActions();
		initData(currentlySelectedSourceDocument, Collections.emptySet());
	}
	
	@Subscribe
	public void handleProjectReadyEvent(ProjectReadyEvent projectReadyEvent) {
		// switch off resourceSelectionListener
		ResourceSelectionListener resourceSelectionListener = this.resourceSelectionListener;
		this.resourceSelectionListener = null;
		
		Collection<TagsetDefinition> tagsets = getSelectedTagsets();

		initData(
			getSelectedDocument(), 
			getSelectedAnnotationCollectionReferences()
				.stream()
				.map(AnnotationCollectionReference::getId)
				.collect(Collectors.toSet()));
		
		tagsetData.getItems().forEach(tagset -> {
			if (tagsets.contains(tagset)) {
				tagsetGrid.select(tagset);
			}
			else {
				tagsetGrid.deselect(tagset);
			}
		});
		
		// switch on resourceSelectionListener
		this.resourceSelectionListener = resourceSelectionListener;
		this.resourceSelectionListener.resourcesChanged();
	}

    private void initProjectListeners() {
        this.projectExceptionListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Exception e = (Exception) evt.getNewValue();
				errorHandler.showAndLogError("Error handling Project!", e);
				
			}
		};
		project.addPropertyChangeListener(
				RepositoryChangeEvent.exceptionOccurred, projectExceptionListener);
		
		this.tagsetChangeListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				handleTagsetChange(evt);
			}
		};		
		
        project.getTagManager().addPropertyChangeListener(
        		TagManagerEvent.tagsetDefinitionChanged,
        		tagsetChangeListener);
    }
    
	private void handleTagsetChange(PropertyChangeEvent evt) {
		Object oldValue = evt.getOldValue();
		Object newValue = evt.getNewValue();
		
		if (oldValue == null) { // creation
			tagsetData.refreshAll();
		}
		else if (newValue == null) { // removal
			tagsetGrid.deselect((TagsetDefinition) oldValue);
			tagsetData.refreshAll();
		}
		else { // metadata update
			TagsetDefinition tagset = (TagsetDefinition)newValue;
			tagsetData.refreshItem(tagset);
		}
		
    	if (resourceSelectionListener != null) {
    		resourceSelectionListener.tagsetsSelected(getSelectedTagsets());
    	}
	} 
	
	@Subscribe
	public void handleCollectionChanged(CollectionChangeEvent collectionChangeEvent) {
		if (collectionChangeEvent.getChangeType().equals(ChangeType.CREATED)) {
			
    		SourceDocument document = collectionChangeEvent.getDocument();
    		AnnotationCollectionReference collectionReference = 
    				collectionChangeEvent.getCollectionReference();

    		
			CollectionDataItem collectionDataItem = 
				new CollectionDataItem(
					collectionReference, 
					collectionReference.isResponable(project.getUser().getIdentifier()));
			documentData.getRootItems()
			.stream()
			.filter(item -> ((DocumentDataItem)item).getDocument().equals(document))
			.findAny().ifPresent(documentDataItem -> {
				documentData.addItem(
	    				documentDataItem, collectionDataItem);
				documentTree.getDataProvider().refreshAll();
			});
			
			if (isAttached()) {
				Notification.show(
					"Info", 
					String.format("Collection %1$s has been created!", collectionReference.toString()),  
					Type.TRAY_NOTIFICATION);
			}
			
			if (getSelectedDocument() != null && getSelectedDocument().equals(document)) {
				collectionDataItem.fireSelectedEvent(this.resourceSelectionListener);
			}
    	}
		else if (collectionChangeEvent.getChangeType().equals(ChangeType.DELETED)) {
			Optional<DocumentTreeItem> optionalDocResource = documentData.getRootItems()
			.stream()
			.filter(item -> ((DocumentDataItem)item).getDocument().equals(collectionChangeEvent.getDocument()))
			.findAny();
			
			if (optionalDocResource.isPresent()) {
				documentData.getChildren(optionalDocResource.get()).stream()
				.filter(item -> ((CollectionDataItem)item).getCollectionRef().equals(collectionChangeEvent.getCollectionReference()))
				.findAny()
				.ifPresent(item -> documentData.removeItem(item));
			}
		}
    	else {
    		documentTree.getDataProvider().refreshAll();
    	}
	}
	
	

	private void initActions() {
		documentActionGridComponent.getActionGridBar().addBtnAddClickListener(
				clickEvent -> handleAddCollectionRequest());
		tagsetGrid.addSelectionListener(
				selectionEvent -> handleTagsetSelectionEvent(selectionEvent));
        tagsetActionGridComponent.getActionGridBar().addBtnAddClickListener(
            	click -> handleAddTagsetRequest());		
        tagsetActionGridComponent.setSearchFilterProvider(new SearchFilterProvider<TagsetDefinition>() {
        	@Override
        	public SerializablePredicate<TagsetDefinition> createSearchFilter(final String searchInput) {
        		return new SerializablePredicate<TagsetDefinition>() {
        			@Override
        			public boolean test(TagsetDefinition t) {
        				if (t != null) {
        					String name = t.getName();
        					if (name != null) {
        						return name.toLowerCase().contains(searchInput.toLowerCase());
        					}
        				}
        				return false;
        			}
				};
        	}
		});
	}

	private void handleAddTagsetRequest() {
    	
    	SingleTextInputDialog tagsetNameDlg = 
    		new SingleTextInputDialog("Add Tagset", "Please enter the Tagset name:",
    				new SaveCancelListener<String>() {
						
						@Override
						public void savePressed(String result) {
							IDGenerator idGenerator = new IDGenerator();
							project.getTagManager().addTagsetDefinition(
								new TagsetDefinition(
									idGenerator.generateTagsetId(), result));
						}
					});
        	
        tagsetNameDlg.show();
	}
	
    private void handleTagsetSelectionEvent(SelectionEvent<TagsetDefinition> selectionEvent) {
    	if (resourceSelectionListener != null) {
    		resourceSelectionListener.tagsetsSelected(selectionEvent.getAllSelectedItems());
    	}
    
    }

	private void handleAddCollectionRequest() {
    	Set<DocumentTreeItem> selectedItems = documentTree.getSelectedItems();
    	
    	Set<SourceDocument> selectedDocuments = new HashSet<>();
    	
    	for (DocumentTreeItem resource : selectedItems) {
    		DocumentTreeItem root = documentData.getParent(resource);

    		if (root == null) {
    			root = resource;
    		}
    		
    		DocumentDataItem documentDataItem = (DocumentDataItem)root;
    		selectedDocuments.add(documentDataItem.getDocument());
    	}
    	
    	if (selectedDocuments.isEmpty()) {
    		SourceDocument document = getSelectedDocument();
    		if (document != null) {
    			selectedDocuments.add(document);
    		}
    	}
    	if (selectedDocuments.isEmpty()) {
    		Notification.show("Info", "Please select at least one Document first!", Type.HUMANIZED_MESSAGE);
    	}
    	else {
	    	SingleTextInputDialog collectionNameDlg = 
	    		new SingleTextInputDialog("Add Annotation Collection", "Please enter the Collection name:",
	    				new SaveCancelListener<String>() {
							
							@Override
							public void savePressed(String result) {
								for (SourceDocument document : selectedDocuments) {
									project.createUserMarkupCollection(result, document);
								}
							}
						});
	    	
	    	collectionNameDlg.show();
    	}
    }
    
	private void initData(SourceDocument currentlySelectedSourceDocument, Set<String> currentlysSelectedColletionIds) {
		try {
			documentData = new TreeData<>();
			
			Collection<SourceDocument> documents = project.getSourceDocuments(); 
			
			final SourceDocument preselection = currentlySelectedSourceDocument;
			
			documentData.addRootItems(
				documents
				.stream()
				.map(document -> new DocumentDataItem(
						document, 
						preselection != null && document.equals(preselection))));
			
			DocumentTreeItem preselectedItem = null;
			
			for (DocumentTreeItem documentDataItem : documentData.getRootItems()) {
				if (documentDataItem.isSelected()) {
					preselectedItem = documentDataItem;
				}
				for (AnnotationCollectionReference umcRef : 
					((DocumentDataItem)documentDataItem).getDocument().getUserMarkupCollectionRefs()) {
					documentData.addItem(
						documentDataItem, 
						new CollectionDataItem(
							umcRef,
							umcRef.isResponable(project.getUser().getIdentifier()),
							(currentlysSelectedColletionIds.isEmpty() || currentlysSelectedColletionIds.contains(umcRef.getId()))
						)
					);
				}
			}
			
			documentTree.setDataProvider(new TreeDataProvider<>(documentData));
			if (preselectedItem != null) {
				documentTree.expand(preselectedItem);
			}
			
			tagsetData = new ListDataProvider<TagsetDefinition>(project.getTagsets());
			tagsetGrid.setDataProvider(tagsetData);
			tagsetData.getItems().forEach(tagsetGrid::select);
			
			documentData
				.getRootItems()
				.stream()
				.filter(documentItem -> documentItem.isSelected())
				.findAny()
				.ifPresent(documentItem -> documentTree.expand(documentItem));
			
		} catch (Exception e) {
			errorHandler.showAndLogError("Error loading data!", e);
		}
	}
	
	public List<AnnotationCollectionReference> getSelectedAnnotationCollectionReferences() {
		
		Optional<DocumentTreeItem> optionalDocumentTreeItem = 
				documentData.getRootItems()
				.stream()
				.filter(documentTreeItem->documentTreeItem.isSelected())
				.findFirst();
		
		if (optionalDocumentTreeItem.isPresent()) {
			return documentData.getChildren(optionalDocumentTreeItem.get())
				.stream()
				.filter(documentTreeItem -> documentTreeItem.isSelected())
				.map(CollectionDataItem.class::cast)
				.map(collectionDataItem -> collectionDataItem.getCollectionRef())
				.collect(Collectors.toList());
		}
		
		return Collections.emptyList();
	}
	
	public Collection<TagsetDefinition> getSelectedTagsets() {
		return tagsetGrid.getSelectedItems();
	}

	private void initComponents() {
		addStyleName("annotate-resource-panel");
		Label documentTreeLabel = new Label("Documents & Annotations");
		documentTree = TreeGridFactory.createDefaultTreeGrid();
		documentTree.addStyleNames(
				"resource-grid", 
				"flat-undecorated-icon-buttonrenderer");
		
		ButtonRenderer<DocumentTreeItem> documentSelectionRenderer = 
				new ButtonRenderer<DocumentTreeItem>(
					documentSelectionClick -> handleVisibilityClickEvent(documentSelectionClick));
		documentSelectionRenderer.setHtmlContentAllowed(true);
		Column<DocumentTreeItem, String> selectionColumn = 
			documentTree.addColumn(
				documentTreeItem -> documentTreeItem.getSelectionIcon(),
				documentSelectionRenderer)
			.setWidth(100);
		
		documentTree.setHierarchyColumn(selectionColumn);
		
		documentTree
			.addColumn(documentTreeItem -> documentTreeItem.getName())
			.setCaption("Name")
			.setWidth(150);
		
		documentTree.setHeight("250px");

		documentTree.addColumn(
				documentTreeItem -> documentTreeItem.getPermissionIcon(), new HtmlRenderer())
		.setWidth(50);
			
		documentTree.addColumn(
				documentTreeItem -> documentTreeItem.getIcon(), new HtmlRenderer())
		.setExpandRatio(1);

		documentActionGridComponent = 
				new ActionGridComponent<TreeGrid<DocumentTreeItem>>(documentTreeLabel, documentTree);
		documentActionGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
		
		addComponent(documentActionGridComponent);
		
		Label tagsetLabel = new Label("Tagsets");
		
		tagsetGrid = new Grid<>();
		tagsetGrid.addStyleNames(
				"resource-grid", 				
				"flat-undecorated-icon-buttonrenderer",
				"no-focused-before-border");

		tagsetGrid.setHeight("250px");
		tagsetGrid
			.addColumn(tagset -> tagset.getName())
			.setCaption("Name")
			.setWidth(150);
		
		tagsetGrid.addColumn(
				tagset -> tagset.isResponable(project.getUser().getIdentifier())?VaadinIcons.UNLOCK.getHtml():VaadinIcons.LOCK.getHtml(),
				new HtmlRenderer())
		.setWidth(50);
		
		tagsetGrid
			.addColumn(tagset -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer())
			.setExpandRatio(1);
		
		tagsetActionGridComponent = 
				new ActionGridComponent<Grid<TagsetDefinition>>(tagsetLabel, tagsetGrid);
		tagsetActionGridComponent.setSelectionModeFixed(SelectionMode.MULTI);
		tagsetActionGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
		tagsetActionGridComponent.getActionGridBar().setMargin(new MarginInfo(false, false, false, true));
		
		addComponent(tagsetActionGridComponent);
	}

	private void handleVisibilityClickEvent(RendererClickEvent<DocumentTreeItem> documentSelectionClick) {
		DocumentTreeItem selectedItem = documentSelectionClick.getItem();
		handleVisibilityClickItem(selectedItem);
	}
	
	private void handleVisibilityClickItem(DocumentTreeItem selectedItem) {
		if (!selectedItem.isSelected() || !selectedItem.isSingleSelection()) {
			selectedItem.setSelected(!selectedItem.isSelected());
			
			if (selectedItem.isSingleSelection()) {
				for (DocumentTreeItem item : documentData.getRootItems()) {
					if (!item.equals(selectedItem)) {
						item.setSelected(false);
						documentTree.collapse(item);
					}
					else {
						documentTree.expand(item);
					}
				}
			}		
			documentTree.getDataProvider().refreshAll();
			
			selectedItem.fireSelectedEvent(this.resourceSelectionListener);
		}
	}
	
	public void selectCollectionVisible(String collectionId) {
		documentData.getRootItems()
		.stream()
		.filter(documentTreeItem->documentTreeItem.isSelected())
		.findFirst()
		.ifPresent(
			documentItem -> selectCollectionVisible(documentItem, collectionId));
		
	}

	private void selectCollectionVisible(DocumentTreeItem documentItem, String collectionId) {
		documentData.getChildren(documentItem)
		.stream()
		.filter(item -> ((CollectionDataItem)item).getCollectionRef().getId().equals(collectionId))
		.findFirst()
		.ifPresent(collectionItem -> handleVisibilityClickItem(collectionItem));
	}

	public void setSelectionListener(
			ResourceSelectionListener resourceSelectionListener) {
		this.resourceSelectionListener = resourceSelectionListener;
	}
	
    @Subscribe
    public void handleDocumentChanged(DocumentChangeEvent documentChangeEvent) {
    	SourceDocument currentlySelectedDocument = getSelectedDocument();
    	SourceDocument nextSelectedDocument = null;
    	if ((currentlySelectedDocument != null)
    			&& !(documentChangeEvent.getChangeType().equals(ChangeType.DELETED)
    					&& documentChangeEvent.getDocument().equals(currentlySelectedDocument))) {
    		nextSelectedDocument = currentlySelectedDocument;
    	}
    	
    	initData(nextSelectedDocument, Collections.emptySet());
    }
    
    private SourceDocument getSelectedDocument() {
    	for (DocumentTreeItem documentTreeItem : documentData.getRootItems()) {
    		if ((documentTreeItem instanceof DocumentDataItem) && documentTreeItem.isSelected()) {
    			return ((DocumentDataItem)documentTreeItem).getDocument();
    		}
    	}
    	
    	return null;
    }
    
    public void setSelectedDocument(SourceDocument sourceDocument) {
    	SourceDocument selected = getSelectedDocument();
    	if ((selected == null) || !selected.equals(sourceDocument)) {
    		for (DocumentTreeItem documentTreeItem : documentData.getRootItems()) {
    			if (documentTreeItem instanceof DocumentDataItem) {
    				DocumentDataItem documentDataItem = (DocumentDataItem)documentTreeItem;
    				if (documentDataItem.getDocument().equals(sourceDocument)) {
    					documentDataItem.setSelected(true);
    					documentTree.getDataProvider().refreshItem(documentDataItem);
    					documentTree.expand(documentDataItem);
    				}
    			}
    		}
    	}
    }
	
	public void close() {
		if (project != null) {
			project.removePropertyChangeListener(
				RepositoryChangeEvent.exceptionOccurred, projectExceptionListener);

	        project.getTagManager().removePropertyChangeListener(
        		TagManagerEvent.tagsetDefinitionChanged,
        		tagsetChangeListener);
		}
		
		eventBus.unregister(this);
	}
}
