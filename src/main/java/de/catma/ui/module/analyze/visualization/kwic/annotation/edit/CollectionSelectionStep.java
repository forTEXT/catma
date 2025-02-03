package de.catma.ui.module.analyze.visualization.kwic.annotation.edit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.tag.TagDefinition;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.project.CollectionResource;
import de.catma.ui.module.project.DocumentResource;
import de.catma.ui.module.project.Resource;
import de.catma.user.Member;

class CollectionSelectionStep extends VerticalLayout implements WizardStep {
	private enum DocumentGridColumn {
		NAME,
		RESPONSIBLE,
	}

	private ProgressStep progressStep;
    private TreeGrid<Resource> documentGrid;
    private ActionGridComponent<TreeGrid<Resource>> documentGridComponent;
	private Project project;
	private TreeData<Resource> documentData;
	private WizardContext context;
	private TreeDataProvider<Resource> documentDataProvider;
	private StepChangeListener stepChangeListener;
	private MenuItem miToggleResponsibiltityFilter;
	private WizardStep nextStep;
    
	public CollectionSelectionStep(Project project, WizardContext context, ProgressStepFactory progressStepFactory) {
		this.project = project;
		this.context = context;
		this.progressStep = progressStepFactory.create(1, "Select collections");
		this.nextStep = new PropertyActionSelectionStep(project, context, progressStepFactory);
		initComponents();
		initActions();
		try {
			initData();
		} catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error loading available collections", e);
		}
	}

	private void initData() throws Exception {
    	Map<String, Member> membersByIdentfier = project.getProjectMembers().stream()
			.collect(Collectors.toMap(
					Member::getIdentifier, 
					Function.identity()));

        documentData = new TreeData<>();
        
        for(SourceDocumentReference srcDoc : project.getSourceDocumentReferences()) {

        	if (!srcDoc.getUserMarkupCollectionRefs().isEmpty()) {
	            DocumentResource docResource = 
	            		new DocumentResource(
	            			srcDoc, 
	            			project.getId(),
	            			srcDoc.getResponsibleUser()!= null?membersByIdentfier.get(srcDoc.getResponsibleUser()):null);
	            
	            documentData.addItem(null,docResource);
	            
	            List<AnnotationCollectionReference> collections = 
	            		srcDoc.getUserMarkupCollectionRefs();
	            
	        	List<Resource> collectionResources = collections
	    		.stream()
	    		.filter(collectionRef -> 
	    			!miToggleResponsibiltityFilter.isChecked() 
	    			|| collectionRef.isResponsible(project.getCurrentUser().getIdentifier()))
	    		.map(collectionRef -> 
	    			(Resource)new CollectionResource(
	    				collectionRef, 
	    				project.getId(),
	    				collectionRef.getResponsibleUser()!= null?membersByIdentfier.get(collectionRef.getResponsibleUser()):null)
	    		)
	    		.collect(Collectors.toList());
	    		
	            
	            if(!collections.isEmpty()){
	                documentData.addItems(
	                	docResource,
	                	collectionResources
	                );
	            }
	            else {
	            	Notification.show(
	            		"Info", 
	            		String.format("Collections for document \"%s\" are not your responsability and have been filtered out, please toggle the filter to include them!", srcDoc),
	            		Type.HUMANIZED_MESSAGE);
	            }
        	}
        }
        
        documentDataProvider = new TreeDataProvider<Resource>(documentData);
        documentGrid.setDataProvider(documentDataProvider);
	}

	private void initActions() {
		documentGridComponent.setSearchFilterProvider(searchInput -> createSearchFilter(searchInput));

        ContextMenu documentsGridMoreOptionsContextMenu = 
            	documentGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

        documentsGridMoreOptionsContextMenu.addItem(
        	"Select Filtered Documents", mi-> handleSelectFilteredDocuments());
        documentsGridMoreOptionsContextMenu.addItem(
        	"Select Filtered Collections", mi-> handleSelectFilteredCollections());
        
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
					return r.toString().toLowerCase().contains(searchInput.toLowerCase());
				}
				else {
					if (r.toString().toLowerCase().contains(searchInput.toLowerCase())) {
						return true;
					}
					else {
						return documentData.getChildren((Resource)r)
								.stream()
								.filter(child -> child.toString().toLowerCase().startsWith(searchInput.toLowerCase()))
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

	private void initComponents() {
		setSizeFull();
    	documentGrid = TreeGridFactory.createDefaultTreeGrid();
    	documentGrid.setSizeFull();
    	
        documentGrid.addStyleNames(
				"flat-undecorated-icon-buttonrenderer");

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
		
		Function<Resource,String> buildResponsibleFunction = (resource) -> {
			
			if (resource.getResponsibleUser() == null) {
				return "";
			}
			
			StringBuilder sb = new StringBuilder()
			  .append("<div class='documentsgrid__doc'> ") //$NON-NLS-1$
		      .append(resource.getResponsibleUser())
		      .append("</div>"); //$NON-NLS-1$
			sb.append("</div>"); //$NON-NLS-1$
				        
		    return sb.toString();
		};
		
        documentGrid
        	.addColumn(resource -> buildNameFunction.apply(resource), new HtmlRenderer())  	
        	.setCaption("Name")
        	.setId(DocumentGridColumn.NAME.name());
        	
        documentGrid
		  	.addColumn(res -> buildResponsibleFunction.apply(res), new HtmlRenderer())
		  	.setCaption("Responsible")
		  	.setId(DocumentGridColumn.RESPONSIBLE.name())
		  	.setExpandRatio(1)
		  	.setHidden(true);

        Label documentsAnnotations = new Label("Select at least one collections you want to modify");

        documentGridComponent = new ActionGridComponent<TreeGrid<Resource>>(
                documentsAnnotations,
                documentGrid
        );
        documentGridComponent.setSizeFull();
        miToggleResponsibiltityFilter = 
        	documentGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu().addItem(
        			"Hide others' responsibilities", mi -> toggleResponsibilityFilter());
        
        miToggleResponsibiltityFilter.setCheckable(true);
        miToggleResponsibiltityFilter.setChecked(true);

        
        addComponent(documentGridComponent);
	}

	private void toggleResponsibilityFilter() {
		if (!miToggleResponsibiltityFilter.isChecked()) {
			Notification.show(
					"Warning",
					"Selecting collections that are beyond your responsibility "
					+ "might result in conflicts with operations of other project members!",
					Type.WARNING_MESSAGE
			);
		}

		documentGrid.getColumn(DocumentGridColumn.RESPONSIBLE.name()).setHidden(
				miToggleResponsibiltityFilter.isChecked()
		);

		try {
			initData();
		}
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError(
					"Error loading available collections", e
			);
		}
	}

	@Override
	public ProgressStep getProgressStep() {
		return progressStep;
	}

	@Override
	public WizardStep getNextStep() {		
		return nextStep;
	}

	@Override
	public boolean isValid() {
		return !getSelectedCollections().isEmpty();
	}

	private Set<AnnotationCollectionReference> getSelectedCollections() {
		Set<Resource> selectedItems = documentGrid.getSelectedItems();
		
		Set<AnnotationCollectionReference> collections = 
				new HashSet<>();
		for (Resource resource : selectedItems) {
			if (resource instanceof CollectionResource) {
				collections.add(
					((CollectionResource) resource).getCollectionReference());
			}
		}
		return collections;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		this.stepChangeListener = stepChangeListener;
	}
	
	@Override
	public void exit(boolean back) {
		Set<AnnotationCollectionReference> collections = getSelectedCollections();
		
		if (back) {
			context.put(EditAnnotationWizardContextKey.COLLECTIONS, Collections.emptySet());
		}
		else {
			context.put(EditAnnotationWizardContextKey.COLLECTIONS, collections);
		}
	}

}
