package de.catma.ui.tagger.resourcepanel;

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

import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.repository.event.ChangeType;
import de.catma.document.repository.event.CollectionChangeEvent;
import de.catma.document.repository.event.DocumentChangeEvent;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.rbac.RBACPermission;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.util.IDGenerator;

public class ResourcePanel extends VerticalLayout {
	
	private Repository project;
	private TreeGrid<DocumentTreeItem> documentTree;
	private TreeData<DocumentTreeItem> documentsData;
	private Grid<TagsetDefinition> tagsetGrid;
	private ResourceSelectionListener resourceSelectionListener;
	private ActionGridComponent<TreeGrid<DocumentTreeItem>> documentActionGridComponent;
	private PropertyChangeListener projectExceptionListener;
	private ErrorHandler errorHandler;
	private PropertyChangeListener tagsetChangeListener;
	private ListDataProvider<TagsetDefinition> tagsetData;
	private ActionGridComponent<Grid<TagsetDefinition>> tagsetActionGridComponent;
	private EventBus eventBus;

	public ResourcePanel(Repository project, SourceDocument currentlySelectedSourceDocument, EventBus eventBus) {
		super();
		this.project = project;
        this.errorHandler = (ErrorHandler)UI.getCurrent();
        this.eventBus = eventBus;
        eventBus.register(this);
        initProjectListeners();
		
		initComponents();
		initActions();
		initData(currentlySelectedSourceDocument);
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
    		UserMarkupCollectionReference collectionReference = 
    				collectionChangeEvent.getCollectionReference();

    		
			CollectionDataItem collectionDataItem = 
				new CollectionDataItem(
					collectionReference, 
					project.hasPermission(
						project.getRoleForCollection(
							collectionReference.getId()), 
							RBACPermission.COLLECTION_WRITE));
			documentsData.getRootItems()
			.stream()
			.filter(item -> ((DocumentDataItem)item).getDocument().equals(document))
			.findAny().ifPresent(documentDataItem -> {
				documentsData.addItem(
	    				documentDataItem, collectionDataItem);
				documentTree.getDataProvider().refreshAll();
			});
			
			Notification.show(
				"Info", 
				String.format("Collection %1$s has been created!", collectionReference.toString()),  
				Type.TRAY_NOTIFICATION);
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
        						return name.startsWith(searchInput);
        					}
        				}
        				return false;
        			}
				};
        	}
		});
	}

	private void handleAddTagsetRequest() {
    	
    	SingleTextInputDialog collectionNameDlg = 
    		new SingleTextInputDialog("Add Tagset", "Please enter the Tagset name:",
    				new SaveCancelListener<String>() {
						
						@Override
						public void savePressed(String result) {
							IDGenerator idGenerator = new IDGenerator();
							project.getTagManager().addTagsetDefinition(
								new TagsetDefinition(
									null, 
									idGenerator.generate(), result, new Version()));
						}
					});
        	
        collectionNameDlg.show();
    	
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
    		DocumentTreeItem root = documentsData.getParent(resource);

    		if (root == null) {
    			root = resource;
    		}
    		
    		DocumentDataItem documentDataItem = (DocumentDataItem)root;
    		selectedDocuments.add(documentDataItem.getDocument());
    	}
    	
    	
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
    
	private void initData(SourceDocument currentlySelectedSourceDocument) {
		try {
			documentsData = new TreeData<>();
			
			Collection<SourceDocument> documents = project.getSourceDocuments(); 
			
			final SourceDocument preselection = currentlySelectedSourceDocument;
			
			documentsData.addRootItems(
				documents
				.stream()
				.map(document -> new DocumentDataItem(
						document, 
						preselection != null && document.equals(preselection))));
			
			DocumentTreeItem preselectedItem = null;
			
			for (DocumentTreeItem documentDataItem : documentsData.getRootItems()) {
				for (UserMarkupCollectionReference umcRef : 
					((DocumentDataItem)documentDataItem).getDocument().getUserMarkupCollectionRefs()) {
					documentsData.addItem(
						documentDataItem, 
						new CollectionDataItem(
							umcRef,
							project.hasPermission(
									project.getRoleForCollection(umcRef.getId()),
									RBACPermission.COLLECTION_WRITE)));
					if (documentDataItem.isSelected()) {
						preselectedItem = documentDataItem;
					}
				}
			}
			
			documentTree.setDataProvider(new TreeDataProvider<>(documentsData));
			if (preselectedItem != null) {
				documentTree.expand(preselectedItem);
			}
			
			tagsetData = new ListDataProvider<TagsetDefinition>(project.getTagsets());
			tagsetGrid.setDataProvider(tagsetData);
			tagsetData.getItems().forEach(tagsetGrid::select);
			
			documentsData
				.getRootItems()
				.stream()
				.filter(documentItem -> documentItem.isSelected())
				.findAny()
				.ifPresent(documentItem -> documentTree.expand(documentItem));
			
		} catch (Exception e) {
			errorHandler.showAndLogError("Error loading data!", e);
		}
	}
	
	public List<UserMarkupCollectionReference> getSelectedUserMarkupCollectionReferences() {
		
		Optional<DocumentTreeItem> optionalDocumentTreeItem = 
				documentsData.getRootItems()
				.stream()
				.filter(documentTreeItem->documentTreeItem.isSelected())
				.findFirst();
		
		if (optionalDocumentTreeItem.isPresent()) {
			return documentsData.getChildren(optionalDocumentTreeItem.get())
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
		documentTree = new TreeGrid<>();
		documentTree.addStyleNames(
				"annotate-resource-grid", 
				"flat-undecorated-icon-buttonrenderer", 
				"no-focused-before-border");
		
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
		
		//TODO: shouldn't be fixed size
		documentTree.setWidth("400px");
		documentTree.setHeight("250px");

		documentTree.addColumn(
				documentTreeItem -> documentTreeItem.getPermissionIcon(), new HtmlRenderer())
		.setWidth(50);
			
		documentTree.addColumn(
				documentTreeItem -> documentTreeItem.getIcon(), new HtmlRenderer())
		.setExpandRatio(1);

		documentActionGridComponent = 
				new ActionGridComponent<TreeGrid<DocumentTreeItem>>(documentTreeLabel, documentTree);
		
		addComponent(documentActionGridComponent);
		
		Label tagsetLabel = new Label("Tagsets");
		
		tagsetGrid = new Grid<>();
		tagsetGrid.addStyleNames(
				"annotate-resource-grid", 
				"flat-undecorated-icon-buttonrenderer",
				"no-focused-before-border");
		tagsetGrid.setSelectionMode(SelectionMode.MULTI);
		//TODO: shouldn't be fixed size
		tagsetGrid.setWidth("400px");
		tagsetGrid.setHeight("230px");
		tagsetGrid
			.addColumn(tagset -> tagset.getName())
			.setCaption("Name")
			.setWidth(150);
		
		tagsetGrid.addColumn(
				tagset -> project.hasPermission(
					project.getRoleForTagset(tagset.getUuid()),
					RBACPermission.TAGSET_WRITE)?VaadinIcons.UNLOCK.getHtml():VaadinIcons.LOCK.getHtml(),
				new HtmlRenderer())
		.setWidth(50);
		
		tagsetGrid
			.addColumn(tagset -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer())
			.setExpandRatio(1);
		
		tagsetActionGridComponent = 
				new ActionGridComponent<Grid<TagsetDefinition>>(tagsetLabel, tagsetGrid);
		
		addComponent(tagsetActionGridComponent);
	}

	private void handleVisibilityClickEvent(RendererClickEvent<DocumentTreeItem> documentSelectionClick) {
		DocumentTreeItem selectedItem = documentSelectionClick.getItem();
		handleVisibilityClickItem(selectedItem);
	}
	
	private void handleVisibilityClickItem(DocumentTreeItem selectedItem) {
		if (!selectedItem.isSelected()) {
			selectedItem.setSelected(!selectedItem.isSelected());
			
			if (selectedItem.isSingleSelection()) {
				for (DocumentTreeItem item : documentsData.getRootItems()) {
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
		documentsData.getRootItems()
		.stream()
		.filter(documentTreeItem->documentTreeItem.isSelected())
		.findFirst()
		.ifPresent(
			documentItem -> selectCollectionVisible(documentItem, collectionId));
		
	}

	private void selectCollectionVisible(DocumentTreeItem documentItem, String collectionId) {
		documentsData.getChildren(documentItem)
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
    	
    	initData(nextSelectedDocument);
    }
    
    public SourceDocument getSelectedDocument() {
    	for (DocumentTreeItem documentTreeItem : documentsData.getRootItems()) {
    		if ((documentTreeItem instanceof DocumentDataItem) && documentTreeItem.isSelected()) {
    			return ((DocumentDataItem)documentTreeItem).getDocument();
    		}
    	}
    	
    	return null;
    }
    
    public void setSelectedDocument(SourceDocument sourceDocument) {
    	SourceDocument selected = getSelectedDocument();
    	if ((selected == null) || !selected.equals(sourceDocument)) {
    		for (DocumentTreeItem documentTreeItem : documentsData.getRootItems()) {
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
