package de.catma.ui.module.analyze.visualization.kwic.annotation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.project.Project;
import de.catma.project.event.ChangeType;
import de.catma.project.event.CollectionChangeEvent;
import de.catma.rbac.RBACPermission;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;
import de.catma.ui.module.project.CollectionResource;
import de.catma.ui.module.project.DocumentResource;
import de.catma.ui.module.project.Resource;

public class CollectionSelectionStep extends VerticalLayout implements WizardStep {

	private ProgressStep progressStep;
    private TreeGrid<Resource> documentGrid;
    private ActionGridComponent<TreeGrid<Resource>> documentGridComponent;
	private Project project;
	private TreeData<Resource> documentData;
	private WizardContext context;
	private TreeDataProvider<Resource> documentDataProvider;
	private StepChangeListener stepChangeListener;
	private EventBus eventBus;
    
	public CollectionSelectionStep(EventBus eventBus, Project project, WizardContext context, ProgressStepFactory progressStepFactory) {
		this.eventBus = eventBus;
		eventBus.register(this);
		this.project = project;
		this.context = context;
		this.progressStep = progressStepFactory.create(3, "Select Collections");
		initComponents();
		initActions();
		try {
			initData();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Subscribe
	public void handleCollectionChanged(CollectionChangeEvent collectionChangeEvent) {
		if (collectionChangeEvent.getChangeType().equals(ChangeType.CREATED)) {
    		AnnotationCollectionReference collectionReference = 
    				collectionChangeEvent.getCollectionReference();
			addCollection(collectionReference);
		}
	}	

	private void addCollection(AnnotationCollectionReference userMarkupCollectionReference) {
		documentData.getRootItems()
			.stream()
			.filter(resource -> 
				((DocumentResource)resource).getDocument().getUuid().equals(
						userMarkupCollectionReference.getSourceDocumentId()))
			.findAny()
			.ifPresent(sourceDocResource -> 
				documentData.addItem(
						sourceDocResource, 
						new CollectionResource(userMarkupCollectionReference, project.getProjectId(), true)));
		documentDataProvider.refreshAll();
	}

	private void initData() throws Exception {
        documentData = new TreeData<>();
        
        @SuppressWarnings("unchecked")
		Set<String> documentIds = (Set<String>) context.get(AnnotationWizardContextKey.DOCUMENTIDS);
        
        for(String documentId : documentIds) {
        	SourceDocument srcDoc = project.getSourceDocument(documentId);
            DocumentResource docResource = 
            		new DocumentResource(
            			srcDoc, 
            			project.getProjectId(), 
            			project.hasPermission(project.getRoleForDocument(srcDoc.getUuid()), RBACPermission.DOCUMENT_WRITE));
            
            if(project.hasPermission(project.getRoleForDocument(srcDoc.getUuid()), RBACPermission.DOCUMENT_READ)) {
                documentData.addItem(null,docResource);
                
                List<AnnotationCollectionReference> collections = 
                		srcDoc.getUserMarkupCollectionRefs();
                
            	List<Resource> readableCollectionResources = collections
        		.stream()
        		.map(collectionRef -> 
        			(Resource)new CollectionResource(
        				collectionRef, 
        				project.getProjectId(),
        				project.hasPermission(project.getRoleForCollection(collectionRef.getId()), RBACPermission.COLLECTION_WRITE))
        		)
        		.filter(colRes -> project.hasPermission(
        			project.getRoleForCollection(colRes.getResourceId()), RBACPermission.COLLECTION_READ))
        		.collect(Collectors.toList());
        		
                
                if(!collections.isEmpty()){
                	
                    documentData.addItems(
                    	docResource,
                    	readableCollectionResources
                    );
                }
            }
        }
        
        documentDataProvider = new TreeDataProvider<Resource>(documentData);
        documentGrid.setDataProvider(documentDataProvider);
	}

	private void initActions() {
		documentGridComponent.setSearchFilterProvider(searchInput -> createSearchFilter(searchInput));
		documentGridComponent.getActionGridBar().addBtnAddClickListener(clickEvent -> handleAddCollectionRequest());
        ContextMenu documentsGridMoreOptionsContextMenu = 
            	documentGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

        documentsGridMoreOptionsContextMenu.addItem(
        	"Select filtered Documents", mi-> handleSelectFilteredDocuments());
        documentsGridMoreOptionsContextMenu.addItem(
        	"Select filtered Collections", mi-> handleSelectFilteredCollections());
        
        documentGrid.addSelectionListener(event -> {
        	if (stepChangeListener != null) {
        		stepChangeListener.stepChanged(this);
        	}
        });
	}

	private SerializablePredicate<Object> createSearchFilter(String searchInput) {
		return new SerializablePredicate<Object>() {
			@Override
			public boolean test(Object r) {
				if (r instanceof CollectionResource) {
					return r.toString().startsWith(searchInput);
				}
				else {
					if (r.toString().startsWith(searchInput)) {
						return true;
					}
					else {
						return documentData.getChildren((Resource)r)
								.stream()
								.filter(child -> child.toString().startsWith(searchInput))
								.findAny()
								.isPresent();
					}
				}
			}
		};
	}

	private void handleSelectFilteredDocuments() {
		documentDataProvider.fetch(
				new HierarchicalQuery<>(documentDataProvider.getFilter(), null))
		.forEach(resource -> documentGrid.select(resource));
	}

	private void handleSelectFilteredCollections() {
		documentDataProvider.fetch(
				new HierarchicalQuery<>(documentDataProvider.getFilter(), null))
		.forEach(resource -> {
			documentDataProvider.fetch(new HierarchicalQuery<>(documentDataProvider.getFilter(), resource))
			.forEach(child -> documentGrid.select(child));
		});
	}
	
	private void handleAddCollectionRequest() {
		try {
			if (!project.hasPermission(project.getRoleOnProject(), RBACPermission.COLLECTION_CREATE)) {
				Notification.show(
						"Info", 
						"You do not have the permission to create Collections, please contact a Project maintainer!", 
						Type.HUMANIZED_MESSAGE);
				return;
			}
			
			
			@SuppressWarnings("unchecked")
			TreeDataProvider<Resource> resourceDataProvider = 
					(TreeDataProvider<Resource>) documentGrid.getDataProvider();
			
	    	Set<Resource> selectedResources = documentGrid.getSelectedItems();
	    	
	    	Set<SourceDocument> selectedDocuments = new HashSet<>();
	    	
	    	for (Resource resource : selectedResources) {
	    		Resource root = 
	        			resourceDataProvider.getTreeData().getParent(resource);
	
	    		if (root == null) {
	    			root = resource;
	    		}
	    		
	    		DocumentResource documentResource = (DocumentResource)root;
	    		selectedDocuments.add(documentResource.getDocument());
	    	}
	    	
	    	if (!selectedDocuments.isEmpty()) {
		    	SingleTextInputDialog collectionNameDlg = 
		    		new SingleTextInputDialog("Add Annotation Collection(s)", "Please enter the Collection name:",
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
	    	else {
	    		Notification.show("Info", "Please select one or more Documents first!", Type.HUMANIZED_MESSAGE);
	    	}
		}
		catch (Exception e) {
			//TODO:
			e.printStackTrace();
		}
	}

	private void initComponents() {
		setSizeFull();
    	documentGrid = new TreeGrid<>();
    	documentGrid.setSizeFull();
    	
        documentGrid.addStyleNames(
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");

        documentGrid.setRowHeight(45);
        documentGrid.setSelectionMode(SelectionMode.MULTI);
        
		documentGrid
			.addColumn(resource -> resource.getIcon(), new HtmlRenderer())
			.setWidth(100);
        
		Function<Resource,String> buildNameFunction = (resource) -> {
			StringBuilder sb = new StringBuilder()
			  .append("<div class='documentsgrid__doc'> ")
		      .append("<div class='documentsgrid__doc__title'> ")
		      .append(resource.getName())
		      .append("</div>");
			if(resource.hasDetail()){
		        sb
		        .append("<span class='documentsgrid__doc__author'> ")
		        .append(resource.getDetail())
		        .append("</span>");
			}
			sb.append("</div>");
				        
		    return sb.toString();
		};
      
        documentGrid
        	.addColumn(resource -> buildNameFunction.apply(resource), new HtmlRenderer())  	
        	.setCaption("Name")
        	.setWidth(300);
        
        documentGrid
    	.addColumn(res -> res.getPermissionIcon() , new HtmlRenderer())
    	.setCaption("Permission")
    	.setExpandRatio(1);      

        Label documentsAnnotations = new Label("Select one Collection per Document");

        documentGridComponent = new ActionGridComponent<TreeGrid<Resource>>(
                documentsAnnotations,
                documentGrid
        );
        documentGridComponent.setSizeFull();
        
        addComponent(documentGridComponent);
	}

	@Override
	public ProgressStep getProgressStep() {
		return progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		return null; // no next step
	}

	@Override
	public boolean isValid() {
        @SuppressWarnings("unchecked")
		Set<String> documentIds = (Set<String>) context.get(AnnotationWizardContextKey.DOCUMENTIDS);

        Map<String, AnnotationCollectionReference> collectionsByDocumentId = 
        		getCollectionRefsByDocumentId();
		
		return collectionsByDocumentId.keySet().containsAll(documentIds);
	}

	private Map<String, AnnotationCollectionReference> getCollectionRefsByDocumentId() {
		Set<Resource> selectedItems = documentGrid.getSelectedItems();
		
		Map<String, AnnotationCollectionReference> collectionsByDocumentId = 
				new HashMap<>();
		for (Resource resource : selectedItems) {
			if (resource instanceof CollectionResource) {
				collectionsByDocumentId.put(
					((CollectionResource) resource).getCollectionReference().getSourceDocumentId(), 
					((CollectionResource) resource).getCollectionReference());
			}
		}
		return collectionsByDocumentId;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		this.stepChangeListener = stepChangeListener;
	}

	@Override
	public void setFinished() {
        Map<String, AnnotationCollectionReference> collectionsByDocumentId = 
        		getCollectionRefsByDocumentId();
        
        context.put(AnnotationWizardContextKey.COLLECTIONREFS_BY_DOCID, collectionsByDocumentId);
        
		eventBus.unregister(this);
	}
	
	@Override
	public void setCurrent() {
		progressStep.setCurrent();
	}

}
