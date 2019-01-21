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

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.icons.VaadinIcons;
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
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class ResourcePanel extends VerticalLayout {
	
	private Repository project;
	private TreeGrid<DocumentTreeItem> documentTree;
	private TreeData<DocumentTreeItem> documentsData;
	private Grid<TagsetDefinition> tagsetGrid;
	private ResourceSelectionListener resourceSelectionListener;
	private ActionGridComponent<TreeGrid<DocumentTreeItem>> documentActionGridComponent;
	private PropertyChangeListener collectionChangeListener;
	private PropertyChangeListener projectExceptionListener;
	private ErrorHandler errorHandler;
	private PropertyChangeListener tagsetChangeListener;
	private ListDataProvider<TagsetDefinition> tagsetData;
	private ActionGridComponent<Grid<TagsetDefinition>> tagsetActionGridComponent;

	public ResourcePanel(Repository project, SourceDocument currentlySelectedSourceDocument) {
		super();
		this.project = project;
        this.errorHandler = (ErrorHandler)UI.getCurrent();
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
		
        this.collectionChangeListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				handleCollectionChange(evt);
			}
		};
		
		project.addPropertyChangeListener(
			RepositoryChangeEvent.userMarkupCollectionChanged, collectionChangeListener);
		
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
			TagsetDefinition tagset = (TagsetDefinition)newValue;
			tagsetData.getItems().add(tagset);
			tagsetData.refreshAll();
		}
		else if (newValue == null) { // removal
			TagsetDefinition tagset = (TagsetDefinition)oldValue;
			tagsetData.getItems().remove(tagset);
			tagsetData.refreshAll();
		}
		else { // metadata update
			TagsetDefinition tagset = (TagsetDefinition)newValue;
			tagsetData.refreshItem(tagset);
		}
	}    
	@SuppressWarnings("unchecked")
	private void handleCollectionChange(PropertyChangeEvent evt) {
    	
    	Object oldValue = evt.getOldValue();
    	Object newValue = evt.getNewValue();
    	
    	if (oldValue == null) { // creation
			Pair<UserMarkupCollectionReference, SourceDocument> creationResult = 
    				(Pair<UserMarkupCollectionReference, SourceDocument>) newValue;
    		
    		SourceDocument document = creationResult.getSecond();
    		UserMarkupCollectionReference collectionReference = creationResult.getFirst();
    		
			CollectionDataItem collectionDataItem = new CollectionDataItem(collectionReference);
			DocumentDataItem documentDataItem = new DocumentDataItem(document, true);
			
			documentsData.addItem(
    				documentDataItem, collectionDataItem);
			documentTree.getDataProvider().refreshAll();
			
			Notification.show(
				"Info", 
				String.format("Collection %1$s has been created!", collectionReference.toString()),  
				Type.TRAY_NOTIFICATION);
    	}
    	else if (newValue == null) { // removal
    		//TODO:
    	}
    	else { // metadata update
    		//TODO:
    	}
    	
	}

	private void initActions() {
		documentActionGridComponent.getActionGridBar().addBtnAddClickListener(
				clickEvent -> handleAddCollectionRequest());
		tagsetGrid.addSelectionListener(
				selectionEvent -> handleTagsetSelectionEvent(selectionEvent));
        tagsetActionGridComponent.getActionGridBar().addBtnAddClickListener(
            	click -> handleAddTagsetRequest());		
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
			documentsData.addRootItems(
				documents
				.stream()
				.map(document -> new DocumentDataItem(document, document.equals(currentlySelectedSourceDocument))));
			
			for (DocumentTreeItem documentDataItem : documentsData.getRootItems()) {
				for (UserMarkupCollectionReference umcRef : 
					((DocumentDataItem)documentDataItem).getDocument().getUserMarkupCollectionRefs()) {
					documentsData.addItem(documentDataItem, new CollectionDataItem(umcRef));
				}
			}
			
			documentTree.setDataProvider(new TreeDataProvider<>(documentsData));
			
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
		documentTree.addStyleNames("annotate-resource-grid", "flat-undecorated-icon-buttonrenderer");
		
		ButtonRenderer<DocumentTreeItem> documentSelectionRenderer = 
				new ButtonRenderer<DocumentTreeItem>(
					documentSelectionClick -> handleVisibilityClickEvent(documentSelectionClick));
		documentSelectionRenderer.setHtmlContentAllowed(true);
		Column<DocumentTreeItem, String> selectionColumn = 
			documentTree.addColumn(
				documentTreeItem -> documentTreeItem.getSelectionIcon(),
				documentSelectionRenderer);
		
		documentTree.setHierarchyColumn(selectionColumn);
		
		documentTree
			.addColumn(documentTreeItem -> documentTreeItem.getName())
			.setCaption("Name")
			.setExpandRatio(3);
		
		//TODO: shouldn't be fixed size
		documentTree.setWidth("400px");
		documentTree.setHeight("250px");

		
		documentTree
			.addColumn(documentTreeItem -> documentTreeItem.getIcon(), new HtmlRenderer());

		documentActionGridComponent = 
				new ActionGridComponent<TreeGrid<DocumentTreeItem>>(documentTreeLabel, documentTree);
		
		addComponent(documentActionGridComponent);
		
		Label tagsetLabel = new Label("Tagsets");
		
		tagsetGrid = new Grid<>();
		tagsetGrid.addStyleNames("annotate-resource-grid", "flat-undecorated-icon-buttonrenderer");
		tagsetGrid.setSelectionMode(SelectionMode.MULTI);
		//TODO: shouldn't be fixed size
		tagsetGrid.setWidth("400px");
		tagsetGrid.setHeight("230px");
		tagsetGrid
			.addColumn(tagset -> tagset.getName())
			.setCaption("Name")
			.setExpandRatio(2);
		
		tagsetGrid
			.addColumn(tagset -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer());
		
		tagsetActionGridComponent = 
				new ActionGridComponent<Grid<TagsetDefinition>>(tagsetLabel, tagsetGrid);
		
		addComponent(tagsetActionGridComponent);
	}

	private void handleVisibilityClickEvent(RendererClickEvent<DocumentTreeItem> documentSelectionClick) {
		DocumentTreeItem selectedItem = documentSelectionClick.getItem();
		selectedItem.setSelected(!selectedItem.isSelected());
		
		if (selectedItem.isSingleSelection()) {
			for (DocumentTreeItem item : documentsData.getRootItems()) {
				if (!item.equals(selectedItem)) {
					item.setSelected(false);
				}
			}
		}		
		documentTree.getDataProvider().refreshAll();
		
		selectedItem.fireSelectedEvent(this.resourceSelectionListener);
	}

	public void setSelectionListener(
			ResourceSelectionListener resourceSelectionListener) {
		this.resourceSelectionListener = resourceSelectionListener;
	}
	
	public void close() {
		if (project != null) {
			project.removePropertyChangeListener(
				RepositoryChangeEvent.exceptionOccurred, projectExceptionListener);
		
			project.removePropertyChangeListener(
				RepositoryChangeEvent.userMarkupCollectionChanged, collectionChangeListener);
			
	        project.getTagManager().removePropertyChangeListener(
        		TagManagerEvent.tagsetDefinitionChanged,
        		tagsetChangeListener);
		}
	}
}
