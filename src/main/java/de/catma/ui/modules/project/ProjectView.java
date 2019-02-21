package de.catma.ui.modules.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.CatmaApplication;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.main.CanReloadAll;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.modules.main.HeaderContextChangeEvent;
import de.catma.ui.repository.Messages;
import de.catma.ui.repository.wizard.AddSourceDocWizardFactory;
import de.catma.ui.repository.wizard.AddSourceDocWizardResult;
import de.catma.ui.repository.wizard.SourceDocumentResult;
import de.catma.user.User;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

/**
 *
 * Renders one project with all resources
 *
 * @author db
 */
public class ProjectView extends HugeCard implements CanReloadAll {

	private ProjectManager projectManager;
    private ProjectReference projectReference;
    private Repository project;

    private final ErrorHandler errorHandler;
    private final EventBus eventBus;

    private TreeGrid<Resource> resourceGrid;
    private Grid<TagsetDefinition> tagsetGrid;
	private Grid<User> teamGrid;
	private ActionGridComponent<TreeGrid<Resource>> sourceDocumentsGridComponent;
	private PropertyChangeListener collectionChangeListener;
	private PropertyChangeListener projectExceptionListener;
	private PropertyChangeListener documentChangeListener;
	private ActionGridComponent<Grid<TagsetDefinition>> tagsetsGridComponent;
	private PropertyChangeListener tagsetChangeListener;
	private ListDataProvider<TagsetDefinition> tagsetData;

    public ProjectView(ProjectManager projectManager, EventBus eventBus) {
    	super("Project");
    	this.projectManager = projectManager;
        this.eventBus = eventBus;
        this.errorHandler = (ErrorHandler)UI.getCurrent();
        initProjectListeners();

        initComponents();
        initActions();
        
        eventBus.register(this);
    }

    private void initProjectListeners() {
        this.projectExceptionListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Exception e = (Exception) evt.getNewValue();
				errorHandler.showAndLogError("Error handling Project!", e);
				
			}
		};
        this.collectionChangeListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				handleCollectionChange(evt);
			}
		};
		
		this.documentChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				handleDocumentChange(evt);
			}
		};
		
		this.tagsetChangeListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				handleTagsetChange(evt);
			}
		};
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
	}

	@SuppressWarnings("unchecked")
	private void handleDocumentChange(PropertyChangeEvent evt) {
		Object oldValue = evt.getOldValue();
		Object newValue = evt.getNewValue();
		try {
			if (oldValue == null) { // creation
				String sourceDocumentId = (String)newValue;
				SourceDocument document = project.getSourceDocument(sourceDocumentId);
			
				TreeDataProvider<Resource> resourceDataProvider = 
	    				(TreeDataProvider<Resource>) resourceGrid.getDataProvider();

				DocumentResource documentResource = new DocumentResource(document);
				
				resourceDataProvider.getTreeData().addItem(null, documentResource);
				resourceDataProvider.refreshAll();
				
				Notification.show(
					"Info", 
					String.format("Document %1$s has been added!", document.toString()),  
					Type.TRAY_NOTIFICATION);

			}
			else if (newValue == null) { // removal
				//TODO
			}
			else { // metadata update
				//TODO
			}
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Error handling Document", e);
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
    		
			TreeDataProvider<Resource> resourceDataProvider = 
    				(TreeDataProvider<Resource>) resourceGrid.getDataProvider();

			CollectionResource collectionResource = new CollectionResource(collectionReference);
			DocumentResource documentResource = new DocumentResource(document);
			
			resourceDataProvider.getTreeData().addItem(
    				documentResource, collectionResource);
			resourceDataProvider.refreshAll();
			
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
    	resourceGrid.addItemClickListener(itemClickEvent -> handleResourceItemClick(itemClickEvent));
    	
        ContextMenu addContextMenu = 
        	sourceDocumentsGridComponent.getActionGridBar().getBtnAddContextMenu();
        addContextMenu.addItem("Add Document", clickEvent -> handleAddDocumentRequest());
        addContextMenu.addItem("Add Annotation Collection", e -> handleAddCollectionRequest());
        
        tagsetsGridComponent.getActionGridBar().addBtnAddClickListener(
        	click -> handleAddTagsetRequest());
        
        ContextMenu moreOptionsMenu = 
        	tagsetsGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
        moreOptionsMenu.addItem("Edit Tagset", clickEvent -> handleEditTagsetRequest());
        moreOptionsMenu.addItem("Delete Tagset", clickEvent -> handleDeleteTagsetRequest());
	}

    private void handleDeleteTagsetRequest() {
		final Set<TagsetDefinition> tagsets = tagsetGrid.getSelectedItems();
		if (!tagsets.isEmpty()) {
			ConfirmDialog.show(
					UI.getCurrent(), 
					"Warnung", 
					"Are you sure you want to delete the selected Tagsets with all their contents?", 
					"Delete",
					"Cancel", 
					dlg -> {
						for (TagsetDefinition tagset : tagsets) {
							project.getTagManager().removeTagsetDefinition(tagset);
						}
					}
			);
		}
		else {
			Notification.show(
				"Info", "Please select one or more Tagsets first!", 
				Type.HUMANIZED_MESSAGE);
		}
		
	}

	private void handleEditTagsetRequest() {
		final Set<TagsetDefinition> tagsets = tagsetGrid.getSelectedItems();
		if (!tagsets.isEmpty()) {
			final TagsetDefinition tagset = tagsets.iterator().next();
	    	SingleTextInputDialog collectionNameDlg = 
	        		new SingleTextInputDialog("Edit Tagset", "Please enter the new Tagset name:",
	        				new SaveCancelListener<String>() {
	    						@Override
	    						public void savePressed(String result) {
	    							project.getTagManager().setTagsetDefinitionName(tagset, result);
	    						}
	    					});
	            	
	            collectionNameDlg.show();			
		}
		else {
			Notification.show(
				"Info", "Please select a Tagset first!", 
				Type.HUMANIZED_MESSAGE);
		}		
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

	private void handleAddCollectionRequest() {
		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> resourceDataProvider = 
				(TreeDataProvider<Resource>) resourceGrid.getDataProvider();
		
    	Set<Resource> selectedResources = resourceGrid.getSelectedItems();
    	
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
    	else {
    		Notification.show("Info", "Please select one or more Documents first!", Type.HUMANIZED_MESSAGE);
    	}
    }

	private void handleAddDocumentRequest() {
		
		final AddSourceDocWizardResult wizardResult = 
				new AddSourceDocWizardResult();
		
		AddSourceDocWizardFactory factory = 
			new AddSourceDocWizardFactory(
					new WizardProgressListener() {
				
				public void wizardCompleted(WizardCompletedEvent event) {
					event.getWizard().removeListener(this);
					//TODO:
//					final boolean generateStarterKit = repository.getSourceDocuments().isEmpty();
					try {
						for(SourceDocumentResult sdr : wizardResult.getSourceDocumentResults()){
							final SourceDocument sourceDocument = sdr.getSourceDocument();
							
							project.addPropertyChangeListener(
								RepositoryChangeEvent.sourceDocumentChanged,
								new PropertyChangeListener() {

									@Override
									public void propertyChange(PropertyChangeEvent evt) {
										
										if ((evt.getNewValue() == null)	|| (evt.getOldValue() != null)) {
											return; // no insert
										}
										
										String newSdId = (String) evt.getNewValue();
										if (!sourceDocument.getID().equals(newSdId)) {
											return;
										}
										
											
										project.removePropertyChangeListener(
											RepositoryChangeEvent.sourceDocumentChanged, 
											this);
										
										//TODO:
//										if (currentCorpus != null) {
//											try {
//												repository.update(currentCorpus, sourceDocument);
//												setSourceDocumentsFilter(currentCorpus);
//												
//											} catch (IOException e) {
//												((CatmaApplication)UI.getCurrent()).showAndLogError(
//													Messages.getString("SourceDocumentPanel.errorAddingSourceDocToCorpus"), e); //$NON-NLS-1$
//											}
//											
//										}
										
										//TODO:
//										if (sourceDocument
//												.getSourceContentHandler()
//												.hasIntrinsicMarkupCollection()) {
//											try {
//												handleIntrinsicMarkupCollection(sourceDocument);
//											} catch (IOException e) {
//												((CatmaApplication)UI.getCurrent()).showAndLogError(
//													Messages.getString("SourceDocumentPanel.errorExtratingIntrinsicAnnotations"), e); //$NON-NLS-1$
//											}
//										}
										
//										if (generateStarterKit) {
//											generateStarterKit(sourceDocument);
//										}
									}
								});

							project.insert(sourceDocument);
						}
						
					} catch (Exception e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(
							Messages.getString("SourceDocumentPanel.errorAddingSourceDoc"), e); //$NON-NLS-1$
					}
				}

				public void wizardCancelled(WizardCancelledEvent event) {
					event.getWizard().removeListener(this);
				}
				
				public void stepSetChanged(WizardStepSetChangedEvent event) {/*not needed*/}
				
				public void activeStepChanged(WizardStepActivationEvent event) {/*not needed*/}
			}, 
			wizardResult,
			project);
		
		Window sourceDocCreationWizardWindow = 
				factory.createWizardWindow(
						Messages.getString("SourceDocumentPanel.addNewSourceDoc"), "85%",  "98%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		UI.getCurrent().addWindow(
				sourceDocCreationWizardWindow);
		
		sourceDocCreationWizardWindow.center();
	}
	
	private void handleResourceItemClick(ItemClick<Resource> itemClickEvent) {
    	if (itemClickEvent.getMouseEventDetails().isDoubleClick()) {
    		Resource resource = itemClickEvent.getItem();
    		
    		@SuppressWarnings("unchecked")
			TreeDataProvider<Resource> resourceDataProvider = 
    				(TreeDataProvider<Resource>) resourceGrid.getDataProvider();
    		
    		Resource root = 
    			resourceDataProvider.getTreeData().getParent(resource);
    		Resource child = null;
    		
    		if (root == null) {
    			root = resource;
    		}
    		else {
    			child = resource;
    		}
    		
    		SourceDocument document = ((DocumentResource)root).getDocument();
    		UserMarkupCollectionReference collectionReference = 
    			(child==null?null:((CollectionResource)child).getCollectionReference());
    		
    		eventBus.post(new RouteToAnnotateEvent(project, document, collectionReference));
    	}
    }
    

	/* build the GUI */

	private void initComponents() {
		HorizontalLayout mainPanel = new HorizontalLayout();
    	mainPanel.setFlexWrap(FlexWrap.WRAP);
    	
    	VerticalLayout resourcePanel = new VerticalLayout();
    	
        resourcePanel.setSizeUndefined(); // don't set width 100%
        resourcePanel.addComponent(new Label("Resources"));

        mainPanel.addComponent(resourcePanel);

        VerticalLayout teamPanel = new VerticalLayout();
        teamPanel.setSizeUndefined(); // don't set width 100%
        teamPanel.addComponent(new Label("Team"));

        mainPanel.addComponent(teamPanel);

        addComponent(mainPanel);
        
        ContextMenu hugeCardMoreOptions = getBtnMoreOptionsContextMenu();
        hugeCardMoreOptions.addItem("Share Ressources", e -> Notification.show("Sharing"));// TODO: 29.10.18 actually share something
        hugeCardMoreOptions.addItem("Delete Ressources", e -> Notification.show("Deleting")); // TODO: 29.10.18 actually delete something

        resourcePanel.addComponent(initResourceContent());
        teamPanel.addComponent(initTeamContent());

    }

    /**
     * initialize the resource part
     * @return
     */
    private Component initResourceContent() {
    	HorizontalLayout resourceContent = new HorizontalLayout();
    	resourceGrid = new TreeGrid<>();
        resourceGrid.addStyleName("project-view-document-grid");
        resourceGrid.setHeaderVisible(false);
        resourceGrid.setRowHeight(45);

		resourceGrid
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
      
        resourceGrid
        	.addColumn(resource -> buildNameFunction.apply(resource), new HtmlRenderer())  	
        	.setCaption("Name")
        	.setWidth(300);
        //TODO: see MD for when it is appropriate to offer row options
//        ButtonRenderer<Resource> resourceOptionsRenderer = new ButtonRenderer<>(
//				resourceOptionClickedEvent -> handleResourceOptionClicked(resourceOptionClickedEvent));
//        resourceOptionsRenderer.setHtmlContentAllowed(true);
        
//		resourceGrid.addColumn(
//			(nan) -> VaadinIcons.ELLIPSIS_DOTS_V.getHtml(), 
//			resourceOptionsRenderer);
        
        
        
        Label documentsAnnotations = new Label("Documents & Annotations");

        sourceDocumentsGridComponent = new ActionGridComponent<TreeGrid<Resource>>(
                documentsAnnotations,
                resourceGrid
        );
        sourceDocumentsGridComponent.addStyleName("project-view-action-grid");

        ContextMenu BtnMoreOptionsContextMenu = sourceDocumentsGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
        BtnMoreOptionsContextMenu.addItem("Delete documents / collections",(menuItem) -> handleDeleteResources(menuItem, resourceGrid));
        BtnMoreOptionsContextMenu.addItem("Share documents / collections", (menuItem) -> handleShareResources(menuItem, resourceGrid));


        resourceContent.addComponent(sourceDocumentsGridComponent);

        tagsetGrid = new Grid<>();
        tagsetGrid.setHeaderVisible(false);
        tagsetGrid.setWidth("400px");

        tagsetGrid.addColumn(tagset -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer()).setWidth(100);
		tagsetGrid
			.addColumn(tagset -> tagset.getName())
			.setCaption("Name")
			.setWidth(300);
	

        Label tagsetsAnnotations = new Label("Tagsets");
        tagsetsGridComponent = new ActionGridComponent<Grid<TagsetDefinition>> (
                tagsetsAnnotations,
                tagsetGrid
        );

        tagsetsGridComponent.addStyleName("project-view-action-grid");
        
        resourceContent.addComponent(tagsetsGridComponent);
        return resourceContent;
    }

	private Component initTeamContent() {
		HorizontalLayout teamContent = new HorizontalLayout();
        teamGrid = new Grid<>();
        teamGrid.setHeaderVisible(false);
        teamGrid.setWidth("402px");
        teamGrid.addColumn((user) -> VaadinIcons.USER.getHtml(), new HtmlRenderer());
        teamGrid.addColumn(User::getName).setExpandRatio(1);

        Label membersAnnotations = new Label("Members");
        ActionGridComponent<Grid<User>> membersGridComponent = new ActionGridComponent<>(
                membersAnnotations,
                teamGrid
        );
        membersGridComponent.addStyleName("project-view-action-grid");
        teamContent.addComponent(membersGridComponent);
        return teamContent;
    }




    /**
     * @param projectReference
     */
    private void initProject(ProjectReference projectReference) {
        projectManager.openProject(projectReference, new OpenProjectListener() {

            @Override
            public void progress(String msg, Object... params) {
            }

            @Override
            public void ready(Repository repository) {
                ProjectView.this.project = repository;
                ProjectView.this.project.addPropertyChangeListener(
                		RepositoryChangeEvent.exceptionOccurred, 
                		projectExceptionListener);
                ProjectView.this.project.addPropertyChangeListener(
                		RepositoryChangeEvent.userMarkupCollectionChanged, 
                		collectionChangeListener);
                ProjectView.this.project.addPropertyChangeListener(
                		RepositoryChangeEvent.sourceDocumentChanged, 
                		documentChangeListener);
                
                ProjectView.this.project.getTagManager().addPropertyChangeListener(
                		TagManagerEvent.tagsetDefinitionChanged,
                		tagsetChangeListener);
                
				initData();
            }

            @Override
            public void failure(Throwable t) {
                errorHandler.showAndLogError("error opening project", t);
            }
        });
    }

    private void initData() {
        try {
        	TreeDataProvider<Resource> resourceDataProvider = buildResourceDataProvider(); 
        	resourceGrid.setDataProvider(resourceDataProvider);
        	
        	resourceGrid.expand(resourceDataProvider.getTreeData().getRootItems());
        	
        	tagsetData = new ListDataProvider<>(project.getTagsets());
        	tagsetGrid.setDataProvider(tagsetData);
        	
        	ListDataProvider<User> memberData = new ListDataProvider<>(project.getProjectMembers());
        	teamGrid.setDataProvider(memberData);
		} catch (Exception e) {
			errorHandler.showAndLogError("error initializing data", e);
		}
	}

    private TreeDataProvider<Resource> buildResourceDataProvider() throws Exception {
        if(project != null){
            TreeData<Resource> treeData = new TreeData<>();
            Collection<SourceDocument> srcDocs = project.getSourceDocuments();
            for(SourceDocument srcDoc : srcDocs){
                DocumentResource srcDocResource = new DocumentResource(srcDoc);
                treeData.addItem(null,srcDocResource);
                List<UserMarkupCollectionReference> collections = srcDoc.getUserMarkupCollectionRefs();
                if(!collections.isEmpty()){
                    treeData.addItems(srcDocResource,collections.stream().map(CollectionResource::new));
                }
            }
            return new TreeDataProvider<>(treeData);
        }
        return new TreeDataProvider<>(new TreeData<>());
    }

    /* Event handler */

    /**
     * reloads all data in this view
     */
    @Override
    public void reloadAll() {
        initProject(projectReference);
    }

    /**
     * handler for project selection
     */
    public void setProjectReference(ProjectReference projectReference) {
        this.projectReference = projectReference;
        eventBus.post(new HeaderContextChangeEvent(new Label(projectReference.getName())));
        reloadAll();
    }

    /**
     * called when {@link ResourcesChangedEvent} is fired e.g. when source documents have been removed or added
     * @param resourcesChangedEvent
     */
    @Subscribe
    public void handleResourceChanged(ResourcesChangedEvent<TreeGrid<Resource>> resourcesChangedEvent){
    	initData();
    }

    /**
     * deletes selected resources
     *
     * @param clickEvent
     * @param resourceGrid
     */
    private void handleDeleteResources(MenuBar.MenuItem menuItem, TreeGrid<Resource> resourceGrid) {
    	
    	ConfirmDialog.show(
    		UI.getCurrent(), 
    		"Info", 
    		"Are you sure you want to delete the selected resources: "
    		+ resourceGrid.getSelectedItems()
    			.stream()
    			.map(resource -> resource.getName())
    			.collect(Collectors.joining(","))
    		+ "?", 
    		"Yes", 
    		"Cancel", dlg -> {
	            for (Resource resource: resourceGrid.getSelectedItems()) {
	            	try {
	            		resource.deleteFrom(project);
	                } catch (Exception e) {
	                    errorHandler.showAndLogError("Error deleting resource "+resource, e);
	                }
	            }
	            eventBus.post(new ResourcesChangedEvent<>(resourceGrid));
    		});

    }

    /**
     * TODO: 29.10.18 actually share resources
     *
     * @param clickEvent
     * @param resourceGrid
     */
    private void handleShareResources(MenuBar.MenuItem menuItem, TreeGrid<Resource> resourceGrid) {
    	ConfirmDialog dialog = new ConfirmDialog();
        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setWidth("100%");
        dialogContent.addComponent(new Label("The following resources will be shared"));
        dialog.getCancelButton().addClickListener((evt)-> dialog.close());
        dialog.setContent(dialogContent);
        dialog.show(UI.getCurrent(),(evt) -> {
            dialog.close();
        },true);
    }

	public void close() {
		try {
			if (project != null) {
				if (collectionChangeListener != null) {
					project.removePropertyChangeListener(
						RepositoryChangeEvent.userMarkupCollectionChanged, 
						collectionChangeListener);
				}
				if (documentChangeListener != null) {
					project.removePropertyChangeListener(
						RepositoryChangeEvent.sourceDocumentChanged, 
						documentChangeListener);
				}

				if (projectExceptionListener != null) {
					project.removePropertyChangeListener(
						RepositoryChangeEvent.exceptionOccurred, 
						projectExceptionListener);
				}
				
				if (tagsetChangeListener != null) {
	                ProjectView.this.project.getTagManager().removePropertyChangeListener(
	                		TagManagerEvent.tagsetDefinitionChanged,
	                		tagsetChangeListener);
				}				
			}			
			
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Error closing ProjectView", e);
		}
	}
 }
