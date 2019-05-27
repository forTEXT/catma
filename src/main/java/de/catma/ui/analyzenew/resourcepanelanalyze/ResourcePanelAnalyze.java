package de.catma.ui.analyzenew.resourcepanelanalyze;

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

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.analyzenew.resourcepanelanalyze.AnalyzeResourceSelectionListener;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

	public class ResourcePanelAnalyze extends VerticalLayout {
		
		private Repository project;
		private Corpus corpus;
		private TreeGrid<DocumentTreeItem> documentTree;
		private TreeData<DocumentTreeItem> documentsData;
		private Grid<TagsetDefinition> tagsetGrid;
		private AnalyzeResourceSelectionListener analyzeResourceSelectionListener;
		private ActionGridComponent<TreeGrid<DocumentTreeItem>> documentActionGridComponent;
		private PropertyChangeListener collectionChangeListener;
		private PropertyChangeListener projectExceptionListener;
		private ErrorHandler errorHandler;
		private PropertyChangeListener tagsetChangeListener;

		

		public ResourcePanelAnalyze(Repository project, Corpus corpus) {
			super();
			this.project = project;
			this.corpus=corpus;
	        this.errorHandler = (ErrorHandler)UI.getCurrent();
	       //initProjectListeners();
			
			initComponents();
			initData();
		}


/*	    private void initProjectListeners() {
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
			
	
			
	        project.getTagManager().addPropertyChangeListener(
	        		TagManagerEvent.tagsetDefinitionChanged,
	        		tagsetChangeListener);
	    }*/
	    
   
/*		@SuppressWarnings("unchecked")
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
	    	
		}*/

	    
		private void initData() {
			try {
				documentsData = new TreeData<>();
				
				Collection<SourceDocument> documents = project.getSourceDocuments(); 
				
				Collection<SourceDocument> selectedDocuments = corpus.getSourceDocuments(); 
				Collection<UserMarkupCollectionReference> selectedCollections = corpus.getUserMarkupCollectionRefs();
				
				documentsData.addRootItems(
					documents
					.stream()
					.map(document -> new DocumentDataItem(document, selectedDocuments.contains(document))));
				
				for (DocumentTreeItem documentDataItem : documentsData.getRootItems()) {
					for (UserMarkupCollectionReference umcRef : ((DocumentDataItem)documentDataItem).getDocument().getUserMarkupCollectionRefs()) {
						// List<UserMarkupCollectionReference> selectedUMCReferences=corpus.getUserMarkupCollectionRefs();
			
							 documentsData.addItem(documentDataItem, new CollectionDataItem(umcRef,selectedCollections.contains(umcRef)));
		
					}
				}
				
				documentTree.setDataProvider(new TreeDataProvider<>(documentsData));
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
		


		private void initComponents() {
			addStyleName("annotate-resource-panel");
			Label documentTreeLabel = new Label("Documents & Annotations");
			documentTree = new TreeGrid<>();
			documentTree.addStyleNames("annotate-resource-grid", "flat-undecorated-icon-buttonrenderer");
			
			ButtonRenderer<DocumentTreeItem> documentSelectionRenderer = 
					new ButtonRenderer<DocumentTreeItem>(
						documentSelectionClick -> handleSelectClickEvent(documentSelectionClick));
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
			documentTree.setHeight("450px");
				
			documentTree
				.addColumn(documentTreeItem -> documentTreeItem.getIcon(), new HtmlRenderer());
			
			//documentTree.setSelectionMode(SelectionMode.NONE);
			
	

			documentActionGridComponent = 
					new ActionGridComponent<TreeGrid<DocumentTreeItem>>(documentTreeLabel, documentTree);
	
			
			
		
			
			addComponent(documentActionGridComponent);
			

		}
		private void handleSelectClickEvent(RendererClickEvent<DocumentTreeItem> documentSelectionClick) {
			DocumentTreeItem selectedItem = documentSelectionClick.getItem();
			handleSelectClicItem(selectedItem);
		}
		
		private void handleSelectClicItem(DocumentTreeItem selectedItem) {
			selectedItem.setSelected(!selectedItem.isSelected());
			
			if (selectedItem.getClass()==CollectionDataItem.class) {
			DocumentTreeItem docItem = documentsData.getParent(selectedItem);
			docItem.setSelected(true);

			}		
			documentTree.getDataProvider().refreshAll();
			
			selectedItem.fireSelectedEvent(this.analyzeResourceSelectionListener,selectedItem.isSelected());		
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
			.ifPresent(collectionItem -> handleSelectClicItem(collectionItem));
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


		public void setSelectionListener(AnalyzeResourceSelectionListener analyzeResourceSelectionListener) {
			this.analyzeResourceSelectionListener = analyzeResourceSelectionListener;
			// TODO Auto-generated method stub
			
		}
	}



