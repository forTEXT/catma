package de.catma.ui.module.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.Collator;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.corpus.Corpus;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.IndexedProject;
import de.catma.project.OpenProjectListener;
import de.catma.project.Project;
import de.catma.project.Project.RepositoryChangeEvent;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.project.conflict.ConflictedProject;
import de.catma.project.event.ChangeType;
import de.catma.project.event.CollectionChangeEvent;
import de.catma.project.event.DocumentChangeEvent;
import de.catma.project.event.ProjectReadyEvent;
import de.catma.rbac.RBACConstraint;
import de.catma.rbac.RBACConstraintEnforcer;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.CatmaApplication;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.events.HeaderContextChangeEvent;
import de.catma.ui.events.MembersChangedEvent;
import de.catma.ui.events.ProjectChangedEvent;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToConflictedProjectEvent;
import de.catma.ui.events.routing.RouteToTagsEvent;
import de.catma.ui.layout.FlexLayout.FlexWrap;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.main.CanReloadAll;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.project.document.AddSourceDocWizardFactory;
import de.catma.ui.module.project.document.AddSourceDocWizardResult;
import de.catma.ui.module.project.document.SourceDocumentResult;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.IDGenerator;

/**
 *
 * Renders one project with all resources
 *
 * @author db
 */
public class ProjectView extends HugeCard implements CanReloadAll {
	
	private enum DocumentGridColumn {
		NAME,
		;
	}
	
	private enum TagsetGridColumn {
		NAME,
		;
	}

	private final TagManager tagManager;

	private ProjectManager projectManager;
    private ProjectReference projectReference;
    private Project project;

    private final ErrorHandler errorHandler;
	private final EventBus eventBus;
	private final RBACConstraintEnforcer<RBACRole> rbacEnforcer = new RBACConstraintEnforcer<>();
	
    private TreeGrid<Resource> documentGrid;
    private ActionGridComponent<TreeGrid<Resource>> documentGridComponent;

    private Grid<TagsetDefinition> tagsetGrid;
    private ActionGridComponent<Grid<TagsetDefinition>> tagsetGridComponent;

    private Grid<Member> teamGrid;
    private VerticalFlexLayout teamPanel;
    private ListDataProvider<TagsetDefinition> tagsetData;
    private PropertyChangeListener tagsetChangeListener;

    private PropertyChangeListener projectExceptionListener;
	private MenuItem miInvite;
	private ProgressBar progressBar;

    public ProjectView(
    		ProjectManager projectManager, 
    		EventBus eventBus) {
    	super("Project");
    	this.projectManager = projectManager;
        this.eventBus = eventBus;
    	this.errorHandler = (ErrorHandler)UI.getCurrent();
		TagLibrary tagLibrary = new TagLibrary();
		this.tagManager = new TagManager(tagLibrary);


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
	
	@Subscribe
	public void handleCollectionChanged(CollectionChangeEvent collectionChangeEvent) {
		if (collectionChangeEvent.getChangeType().equals(ChangeType.CREATED)) {
    		SourceDocument document = collectionChangeEvent.getDocument();
    		AnnotationCollectionReference collectionReference = 
    				collectionChangeEvent.getCollectionReference();
    		
			@SuppressWarnings("unchecked")
			TreeDataProvider<Resource> resourceDataProvider = 
    				(TreeDataProvider<Resource>) documentGrid.getDataProvider();

			CollectionResource collectionResource = 
				new CollectionResource(
					collectionReference, 
					project.getProjectId(), 
					project.hasPermission(project.getRoleForCollection(collectionReference.getId()), RBACPermission.COLLECTION_WRITE));
			
			DocumentResource documentResource = 
				new DocumentResource(
					document, 
					project.getProjectId(), 
					project.hasPermission(project.getRoleForDocument(document.getUuid()), RBACPermission.DOCUMENT_WRITE));
			
			resourceDataProvider.getTreeData().addItem(
    				documentResource, collectionResource);
			resourceDataProvider.refreshAll();

			if (isAttached()) {
				documentGrid.expand(documentResource);
				
				Notification.show(
						"Info", 
						String.format("Collection %1$s has been created!", collectionReference.toString()),  
						Type.TRAY_NOTIFICATION);			
			}
		}
		else {
			initData();
		}
	}

	private void initActions() {
		documentGridComponent.setSearchFilterProvider(searchInput -> createSearchFilter(searchInput));

    	documentGrid.addItemClickListener(itemClickEvent -> handleResourceItemClick(itemClickEvent));
    	
        ContextMenu addContextMenu = 
        	documentGridComponent.getActionGridBar().getBtnAddContextMenu();
        MenuItem addDocumentBtn = addContextMenu.addItem("Add Document", clickEvent -> handleAddDocumentRequest());
        addDocumentBtn.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.DOCUMENT_CREATE_OR_UPLOAD)),
        		() -> addDocumentBtn.setEnabled(true))
        		);

        MenuItem addCollectionBtn = addContextMenu.addItem("Add Annotation Collection", e -> handleAddCollectionRequest());
        addCollectionBtn.setEnabled(false);
        
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.COLLECTION_CREATE)),
        		() -> addCollectionBtn.setEnabled(true))
        		);
        
        ContextMenu documentsGridMoreOptionsContextMenu = 
        	documentGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
        
        MenuItem editDocBtn = documentsGridMoreOptionsContextMenu.addItem(
            	"Edit Documents / Collections",(menuItem) -> handleEditResources());
        editDocBtn.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.COLLECTION_DELETE_OR_EDIT) || 
        				project.hasPermission(role, RBACPermission.DOCUMENT_DELETE_OR_EDIT)),
        		() -> editDocBtn.setEnabled(true))
        		);
        MenuItem deleteDocsBtn = documentsGridMoreOptionsContextMenu.addItem(
        	"Delete Documents / Collections",(menuItem) -> handleDeleteResources(menuItem, documentGrid));
        deleteDocsBtn.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.COLLECTION_DELETE_OR_EDIT) || 
        				project.hasPermission(role, RBACPermission.DOCUMENT_DELETE_OR_EDIT)),
        		() -> deleteDocsBtn.setEnabled(true))
        		);
        
        documentsGridMoreOptionsContextMenu.addItem(
            	"Analyze Documents / Collections",(menuItem) -> handleAnalyzeResources(menuItem, documentGrid));

        documentsGridMoreOptionsContextMenu.addItem(
        		"Import Collections", 
        		mi -> Notification.show("Info", "Will be implemented soon!", Type.HUMANIZED_MESSAGE));
        documentsGridMoreOptionsContextMenu.addItem(
        		"Export Collections", 
        		mi -> Notification.show("Info", "Will be implemented soon!", Type.HUMANIZED_MESSAGE));

        documentsGridMoreOptionsContextMenu.addItem("Select filtered entries", mi-> handleSelectFilteredDocuments());
        
        
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.TAGSET_CREATE_OR_UPLOAD)),
        		() ->  tagsetGridComponent.getActionGridBar().addBtnAddClickListener(
        	        	click -> handleAddTagsetRequest()))
        		);
   
        ContextMenu moreOptionsMenu = 
        	tagsetGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
        MenuItem editTagset = moreOptionsMenu.addItem("Edit Tagset", mi -> handleEditTagsetRequest());
        editTagset.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.TAGSET_DELETE_OR_EDIT)),
        		() -> editTagset.setEnabled(true))
        		);

        MenuItem deleteTagSetBtn = moreOptionsMenu.addItem("Delete Tagset", mi -> handleDeleteTagsetRequest());
        deleteTagSetBtn.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.TAGSET_DELETE_OR_EDIT)),
        		() -> deleteTagSetBtn.setEnabled(true))
        		);
        
        MenuItem importTagSetBtn = moreOptionsMenu.addItem("Import Tagsets", mi -> handleImportTagsetsRequest());
        importTagSetBtn.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.TAGSET_CREATE_OR_UPLOAD)),
        		() -> importTagSetBtn.setEnabled(true))
        		);
        
        moreOptionsMenu.addItem("Export Tagsets", mi -> Notification.show("Info", "Will be implemented soon!", Type.HUMANIZED_MESSAGE));
        
        ContextMenu hugeCardMoreOptions = getMoreOptionsContextMenu();
        hugeCardMoreOptions.addItem("Commit all changes", mi -> handleCommitRequest());
        hugeCardMoreOptions.addItem("Synchronize with the team", mi -> handleSynchronizeRequest());
        //TODO:
//        hugeCardMoreOptions.addItem("Print status", e -> project.printStatus());
        
        tagsetGridComponent.setSearchFilterProvider(new SearchFilterProvider<TagsetDefinition>() {
        	@Override
        	public SerializablePredicate<TagsetDefinition> createSearchFilter(final String searchInput) {
        		
        		return new SerializablePredicate<TagsetDefinition>() {
        			@Override
        			public boolean test(TagsetDefinition t) {
        				if (t != null) {
	        				String name = t.getName();
	        				if (name != null) {
	        					return name.toLowerCase().startsWith(searchInput.toLowerCase());
	        				}
        				}
        				return false;
        			}
				};
        	}
		});
        
        tagsetGrid.addItemClickListener(clickEvent -> handleTagsetClick(clickEvent));
        
        
	}

	private void handleTagsetClick(ItemClick<TagsetDefinition> itemClickEvent) {
    	if (itemClickEvent.getMouseEventDetails().isDoubleClick()) {
    		TagsetDefinition tagset = itemClickEvent.getItem();
    		
    		eventBus.post(new RouteToTagsEvent(project, tagset));
    	}		
	}

	private void handleSelectFilteredDocuments() {
		documentGridComponent.setSelectionMode(SelectionMode.MULTI);
		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> dataProvider = (TreeDataProvider<Resource>) documentGrid.getDataProvider();
		dataProvider.fetch(
				new HierarchicalQuery<>(dataProvider.getFilter(), null))
		.forEach(resource -> {
			documentGrid.select(resource);
			dataProvider.fetch(new HierarchicalQuery<>(dataProvider.getFilter(), resource))
			.forEach(child -> documentGrid.select(child));
		});
	}

	private void handleImportTagsetsRequest() {
		Notification.show("Info", "Will be implemented soon!", Type.HUMANIZED_MESSAGE);
//		UploadDialog uploadDialog =
//				new UploadDialog("Upload Tagsets",
//						new SaveCancelListener<byte[]>() {
//			
//			public void cancelPressed() {}
//			
//			public void savePressed(byte[] result) {
//				InputStream is = new ByteArrayInputStream(result);
//				try {
//					if (BOMFilterInputStream.hasBOM(result)) {
//						is = new BOMFilterInputStream(
//								is, Charset.forName("UTF-8")); //$NON-NLS-1$
//					}
//					
//					project.importTagLibrary(is);
//					
//					
//				} catch (IOException e) {
//					((CatmaApplication)UI.getCurrent()).showAndLogError(
//						"Error importing Tagsets", e);
//				}
//				finally {
//					CloseSafe.close(is);
//				}
//			}
//			
//		});
//		uploadDialog.show();	
	}

	private void handleSynchronizeRequest() {
    	try {
    		setEnabled(false);
    		
	    	if (project.hasUncommittedChanges()) {
	    		SingleTextInputDialog dlg = new SingleTextInputDialog(
	    			"Commit all changes", 
	    			"You have uncommited changes, please enter a short description for this commit:", 
	    			commitMsg -> {
	    				try {
		    				project.commitChanges(commitMsg);
		    				synchronizeProject();
	    				}
	    				catch (Exception e) {
	    					setEnabled(true);
	    					((ErrorHandler)UI.getCurrent()).showAndLogError("error committing changes", e);
	    				}
	    			});
	    		dlg.show();
	    	}
	    	else {
	    		synchronizeProject();
	    	}
    	}
    	catch (Exception e) {
            errorHandler.showAndLogError("error accessing project", e);
    	}	
    }

	private void synchronizeProject() throws Exception {
    	setProgressBarVisible(true);
    	
    	final UI ui = UI.getCurrent();
    	
		project.synchronizeWithRemote(new OpenProjectListener() {

            @Override
            public void progress(String msg, Object... params) {
            	ui.access(() -> {
	            	if (params != null) {
	            		progressBar.setCaption(String.format(msg, params));
	            	}
	            	else {
	            		progressBar.setCaption(msg);
	            	}
	            	ui.push();
            	});
            }

            @Override
            public void ready(Project project) {
            	setProgressBarVisible(false);
            	reloadAll();
            	setEnabled(true);
				Notification.show(
    					"Info", 
    					"Your Project has been synchronized!", 
    					Type.HUMANIZED_MESSAGE);		    						
            }
            
            @Override
            public void conflictResolutionNeeded(ConflictedProject conflictedProject) {
            	setProgressBarVisible(false);
            	setEnabled(true);
				eventBus.post(new RouteToConflictedProjectEvent(conflictedProject));
            }

            @Override
            public void failure(Throwable t) {
            	setProgressBarVisible(false);
            	setEnabled(true);
                errorHandler.showAndLogError("error opening project", t);
            }
        });

	}
	
	private void setProgressBarVisible(boolean visible) {
    	progressBar.setIndeterminate(visible);
    	progressBar.setVisible(visible);
		if (!visible) {
			progressBar.setCaption("");
		}
	}

	private void handleEditResources() {
		final Set<Resource> selectedResources = documentGrid.getSelectedItems();
		if (selectedResources.isEmpty()) {
			Notification.show("Info", "Please select a resource first!", Type.HUMANIZED_MESSAGE); 
		}	
		else {
			final Resource resource = selectedResources.iterator().next();
			if (selectedResources.size() > 1) {
				documentGridComponent.setSelectionMode(SelectionMode.SINGLE);
			}
			// TODO: add proper edit metadata dialog including document level annotations!
			
			if (resource.isCollection()) {
				final AnnotationCollectionReference collectionRef = 
						((CollectionResource)selectedResources.iterator().next()).getCollectionReference();
		    	SingleTextInputDialog collectionNameDlg = 
	        		new SingleTextInputDialog("Edit Collection", "Please enter the new Collection name:", 
        				new SaveCancelListener<String>() {
    						@Override
    						public void savePressed(String result) {
    							collectionRef.getContentInfoSet().setTitle(result);
    							try {
									project.update(collectionRef, collectionRef.getContentInfoSet());
									documentGrid.getDataProvider().refreshItem(resource);
								} catch (Exception e) {
									errorHandler.showAndLogError("error updating Collection", e);
								}
    						}
    					});
	            	
	            collectionNameDlg.show();						
			}
			else {
				final SourceDocument document = 
						((DocumentResource)selectedResources.iterator().next()).getDocument();
		    	SingleTextInputDialog collectionNameDlg = 
	        		new SingleTextInputDialog("Edit Document", "Please enter the new Document name:", 
        				new SaveCancelListener<String>() {
    						@Override
    						public void savePressed(String result) {
    							document.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().setTitle(result);
    							project.update(
    								document, 
    								document.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet());
    						}
    					});
	            	
	            collectionNameDlg.show();								
			}
		}
		
	}

	private void handleDeleteTagsetRequest() {
		final Set<TagsetDefinition> tagsets = tagsetGrid.getSelectedItems();
		if (!tagsets.isEmpty()) {
			ConfirmDialog.show(
					UI.getCurrent(), 
					"Warning", 
					"Are you sure you want to delete the selected Tagsets with all their contents?", 
					"Delete",
					"Cancel", 
					dlg -> {
						if (dlg.isConfirmed()) {
							for (TagsetDefinition tagset : tagsets) {
								project.getTagManager().removeTagsetDefinition(tagset);
							}
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
	    	SingleTextInputDialog tagsetNameDlg = 
	        		new SingleTextInputDialog("Edit Tagset", "Please enter the new Tagset name:", tagset.getName(), 
	        				new SaveCancelListener<String>() {
	    						@Override
	    						public void savePressed(String result) {
	    							project.getTagManager().setTagsetDefinitionName(tagset, result);
	    						}
	    					});
	            	
	            tagsetNameDlg.show();			
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
									idGenerator.generateTagsetId(), result, new Version()));
						}
					});
        	
        collectionNameDlg.show();
    	
	}

	private void handleAddCollectionRequest() {
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

	private void handleAddDocumentRequest() {
		
		final AddSourceDocWizardResult wizardResult = 
				new AddSourceDocWizardResult();
		
		AddSourceDocWizardFactory factory = 
			new AddSourceDocWizardFactory(
					new WizardProgressListener() {
				
				public void wizardCompleted(WizardCompletedEvent event) {
					event.getWizard().removeListener(this);

					try {
						for(SourceDocumentResult sdr : wizardResult.getSourceDocumentResults()){
							final SourceDocument sourceDocument = sdr.getSourceDocument();
							project.insert(sourceDocument);
						}
						
					} catch (Exception e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(
							"Error adding the Source Document!", e);
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
						"Add new Source Document", "85%",  "98%");  //$NON-NLS-2$ //$NON-NLS-3$
		
		UI.getCurrent().addWindow(
				sourceDocCreationWizardWindow);
		
		sourceDocCreationWizardWindow.center();
	}
	
	private void handleResourceItemClick(ItemClick<Resource> itemClickEvent) {
    	if (itemClickEvent.getMouseEventDetails().isDoubleClick()) {
    		Resource resource = itemClickEvent.getItem();
    		
    		@SuppressWarnings("unchecked")
			TreeDataProvider<Resource> resourceDataProvider = 
    				(TreeDataProvider<Resource>) documentGrid.getDataProvider();
    		
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
    		AnnotationCollectionReference collectionReference = 
    			(child==null?null:((CollectionResource)child).getCollectionReference());
    		
    		eventBus.post(new RouteToAnnotateEvent(project, document, collectionReference));
    	}
    }
    

	/* build the GUI */

	private void initComponents() {
		progressBar = new ProgressBar();
		progressBar.setIndeterminate(false);
		progressBar.setVisible(false);
		addComponent(progressBar);
		setComponentAlignment(progressBar, Alignment.TOP_CENTER);
		HorizontalFlexLayout mainPanel = new HorizontalFlexLayout();
    	mainPanel.setFlexWrap(FlexWrap.WRAP);
    	mainPanel.addStyleName("project-view-main-panel");
    	VerticalFlexLayout resourcePanel = new VerticalFlexLayout();
    	
        resourcePanel.setSizeUndefined(); // don't set width 100%
        resourcePanel.addComponent(new Label("Resources"));

        mainPanel.addComponent(resourcePanel);


        addComponent(mainPanel);
        setExpandRatio(mainPanel, 1.f);
        
        resourcePanel.addComponent(initResourceContent());
        
        teamPanel = new VerticalFlexLayout();
        teamPanel.setSizeUndefined(); // don't set width 100%
        teamPanel.setVisible(false);
        teamPanel.addComponent(new Label("Team"));
        
        mainPanel.addComponent(teamPanel);
        teamPanel.addComponent(initTeamContent());
    }

    private void handleProjectInvitationRequest() {
		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> resourceDataProvider = 
				(TreeDataProvider<Resource>) documentGrid.getDataProvider();
		
		List<DocumentResource> documentsWithWriteAccess = 
			resourceDataProvider.getTreeData().getRootItems()
			.stream()
			.filter(resource -> resource instanceof DocumentResource)
			.map(resource -> (DocumentResource)resource)
			.filter(resource -> resource.hasWritePermission())
			.collect(Collectors.toList());
		
		new ProjectInvitationDialog(
        			project, 
        			documentsWithWriteAccess,
        			eventBus, 
        			((CatmaApplication)UI.getCurrent()).getHazelCastService()).show();
	}

	private void handleCommitRequest() {
    	try {
	    	if (project.hasUncommittedChanges()) {
	    		SingleTextInputDialog dlg = new SingleTextInputDialog(
	    			"Commit all changes", 
	    			"Please enter a short description for this commit:", 
	    			commitMsg -> {
	    				project.commitChanges(commitMsg);
	    				Notification.show(
	    					"Info", 
	    					"Your changes have been committed!", 
	    					Type.HUMANIZED_MESSAGE);
	    			});
	    		dlg.show();
	    	}
	    	else {
	    		Notification.show("Info", "There are no uncommitted changes!", Type.HUMANIZED_MESSAGE); 
	    	}
    	}
    	catch (Exception e) {
            errorHandler.showAndLogError("error accessing project", e);
    	}
	}

	/**
     * initialize the resource part
     * @return
     */
    private Component initResourceContent() {
    	HorizontalFlexLayout resourceContent = new HorizontalFlexLayout();
    	documentGrid = TreeGridFactory.createDefaultTreeGrid();
        documentGrid.addStyleNames(
				"flat-undecorated-icon-buttonrenderer"); //$NON-NLS-1$ 

        documentGrid.setHeaderVisible(false);
        documentGrid.setRowHeight(45);

		documentGrid
			.addColumn(resource -> resource.getIcon(), new HtmlRenderer())
			.setWidth(100);
        
		Function<Resource,String> buildNameFunction = (resource) -> {
			StringBuilder sb = new StringBuilder()
			  .append("<div class='documentsgrid__doc'> ") //$NON-NLS-1$
		      .append("<div class='documentsgrid__doc__title'> ") //$NON-NLS-1$
		      .append(resource.getName())
		      .append("</div>"); //$NON-NLS-1$
			if(resource.hasDetail()){
		        sb
		        .append("<span class='documentsgrid__doc__author'> ") //$NON-NLS-1$
		        .append(resource.getDetail())
		        .append("</span>"); //$NON-NLS-1$
			}
			sb.append("</div>"); //$NON-NLS-1$
				        
		    return sb.toString();
		};
		
        documentGrid
        	.addColumn(resource -> buildNameFunction.apply(resource), new HtmlRenderer())  	
        	.setCaption("Name")
        	.setId(DocumentGridColumn.NAME.name())
        	.setWidth(300);
        
        documentGrid
    	.addColumn(res -> res.getPermissionIcon() , new HtmlRenderer())
    	.setCaption("Permission")
    	.setExpandRatio(1);      
        
        Label documentsAnnotations = new Label("Documents & Annotations");

        documentGridComponent = new ActionGridComponent<TreeGrid<Resource>>(
                documentsAnnotations,
                documentGrid
        );
        documentGridComponent.addStyleName("project-view-action-grid"); //$NON-NLS-1$

        resourceContent.addComponent(documentGridComponent);

        tagsetGrid = new Grid<>();
        tagsetGrid.setHeaderVisible(false);
        tagsetGrid.setWidth("400px"); //$NON-NLS-1$

        tagsetGrid.addColumn(tagset -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer()).setWidth(100);
		tagsetGrid
			.addColumn(tagset -> tagset.getName())
			.setId(TagsetGridColumn.NAME.name())
			.setCaption("Name")
			.setWidth(300);
	

        Label tagsetsAnnotations = new Label("Tagsets");
        tagsetGridComponent = new ActionGridComponent<Grid<TagsetDefinition>> (
                tagsetsAnnotations,
                tagsetGrid
        );

        tagsetGridComponent.addStyleName("project-view-action-grid"); //$NON-NLS-1$
        
        resourceContent.addComponent(tagsetGridComponent);
        return resourceContent;
    }

	private Component initTeamContent() {
		HorizontalFlexLayout teamContent = new HorizontalFlexLayout();
        teamGrid = new Grid<>();
        teamGrid.setHeaderVisible(false);
        teamGrid.setWidth("402px"); //$NON-NLS-1$
        teamGrid.addColumn((user) -> VaadinIcons.USER.getHtml(), new HtmlRenderer());
        teamGrid.addColumn(User::getName)
        	.setWidth(200)
        	.setComparator((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getName(), r2.getName()))
        	.setDescriptionGenerator(User::preciseName);
        teamGrid.addColumn(Member::getRole).setExpandRatio(1);
        
        Label membersAnnotations = new Label("Members");
        ActionGridComponent<Grid<Member>> membersGridComponent = new ActionGridComponent<>(
                membersAnnotations,
                teamGrid
        );
        membersGridComponent.addStyleName("project-view-action-grid"); //$NON-NLS-1$
        ContextMenu addContextMenu = membersGridComponent.getActionGridBar().getBtnAddContextMenu();

        addContextMenu.addItem("Add Member", (click) -> 
        	new CreateMemberDialog(
        		project::assignOnProject,
        		(query) -> project.findUser(query.getFilter().isPresent() ? query.getFilter().get() : "", query.getOffset(), query.getLimit()), //$NON-NLS-1$
        		(evt) -> eventBus.post(new MembersChangedEvent())
        		).show());
        
        ContextMenu moreOptionsContextMenu = membersGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

        moreOptionsContextMenu.addItem("Edit Members", (click) -> handleEditMembers());
        moreOptionsContextMenu.addItem("Remove Members", (click) -> handleRemoveMembers());
        
        miInvite = moreOptionsContextMenu.addItem(
        	"Invite someone to the Project", click -> handleProjectInvitationRequest());
        
        MenuItem editResBtn = moreOptionsContextMenu.addItem(
        		"Resource permissions", 
        		click -> {
        			@SuppressWarnings("unchecked")
					TreeDataProvider<Resource> resourceDataProvider = 
        					(TreeDataProvider<Resource>) documentGrid.getDataProvider();
        			TreeData<Resource> resourceData = resourceDataProvider.getTreeData();
        			if (!resourceData.getRootItems().isEmpty()) {
	        			new ResourcePermissionView(
	        				resourceData,
			        		tagsetData.getItems(),
			        		this.project)
	        			.show();
        			}
        			else {
        				Notification.show(
        					"Info", 
        					"You do not have any Documents yet, please add a Document first!", 
        					Type.HUMANIZED_MESSAGE);
        			}
		        }
        );
        editResBtn.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.PROJECT_MEMBERS_EDIT)),
        		() -> editResBtn.setEnabled(true))
        		);
        
        teamContent.addComponent(membersGridComponent);
        return teamContent;
    }




    private void handleRemoveMembers() {
    	if (teamGrid.getSelectedItems().isEmpty()) {
    		Notification.show("Info", "Please select one or more members first!", Type.HUMANIZED_MESSAGE);
    	}
    	else {
    		if (teamGrid.getSelectedItems()
    				.stream()
    				.map(User::getUserId)
    				.filter(id -> id.equals(project.getUser().getUserId()))
    				.findFirst()
    				.isPresent()) {
    			HTMLNotification.show(
    				"Info", 
    				"You cannot remove yourself from the Project. "
    				+ "Please use the 'Leave Project' button on the Project card on the Dashboard instead!"
					+ "<br><br> If your are the owner of the Project, "
					+ "please contact the Administrator to request an transfer of ownership.",
    				Type.ERROR_MESSAGE);
    		}
    		Set<Member> members = teamGrid.getSelectedItems()
    				.stream()
    				.filter(member -> !member.getUserId().equals(project.getUser().getUserId()))
    				.collect(Collectors.toSet());
    		
    		members.remove(project.getUser());
    		
    		if (!members.isEmpty()) {
	    		new RemoveMemberDialog(
	            		project::unassignFromProject,
	            		teamGrid.getSelectedItems(),
	            		evt -> eventBus.post(new MembersChangedEvent())
	            ).show();
    		}
    	}
	}

	private void handleEditMembers() {
    	if (teamGrid.getSelectedItems().isEmpty()) {
    		Notification.show("Info", "Select at least one Member first!", Type.HUMANIZED_MESSAGE);
    	}
    	else {
    		new EditMemberDialog(
            		project::assignOnProject,
            		teamGrid.getSelectedItems(),
            		(evt) -> eventBus.post(new MembersChangedEvent())
            		).show();
    	}
    }

	/**
     * @param projectReference
     */
    private void initProject(ProjectReference projectReference) {
    	setEnabled(false);
    	setProgressBarVisible(true);
    	
    	final UI ui = UI.getCurrent();
        projectManager.openProject(tagManager, projectReference, new OpenProjectListener() {

            @Override
            public void progress(String msg, Object... params) {
            	ui.access(() -> {
	            	if (params != null) {
	            		progressBar.setCaption(String.format(msg, params));
	            	}
	            	else {
	            		progressBar.setCaption(msg);
	            	}
	            	ui.push();
            	});
            }

            @Override
            public void ready(Project project) {
            	setProgressBarVisible(false);
                ProjectView.this.project = project;
                ProjectView.this.project.addPropertyChangeListener(
                		RepositoryChangeEvent.exceptionOccurred, 
                		projectExceptionListener);
                
                ProjectView.this.project.getTagManager().addPropertyChangeListener(
                		TagManagerEvent.tagsetDefinitionChanged,
                		tagsetChangeListener);
                setEnabled(true);
                reloadAll();
            }
            
            @Override
            public void conflictResolutionNeeded(ConflictedProject conflictedProject) {
            	setProgressBarVisible(false);
            	setEnabled(true);
				eventBus.post(new RouteToConflictedProjectEvent(conflictedProject));
            }

            @Override
            public void failure(Throwable t) {
            	setProgressBarVisible(false);
            	setEnabled(true);
                errorHandler.showAndLogError("error opening project", t);
            }
        });
    }

    private void initData() {
        try {
        	TreeDataProvider<Resource> resourceDataProvider = buildResourceDataProvider(); 
        	documentGrid.setDataProvider(resourceDataProvider);
        	documentGrid.sort(DocumentGridColumn.NAME.name());
        	documentGrid.expand(resourceDataProvider.getTreeData().getRootItems());
        	
        	tagsetData = new ListDataProvider<>(project.getTagsets());
        	tagsetGrid.setDataProvider(tagsetData);
        	ListDataProvider<Member> memberData = new ListDataProvider<>(project.getProjectMembers());
        	teamGrid.setDataProvider(memberData);
        	tagsetGrid.sort(TagsetGridColumn.NAME.name());
		} catch (Exception e) {
			errorHandler.showAndLogError("error initializing data", e);
		}
	}

    private TreeDataProvider<Resource> buildResourceDataProvider() throws Exception {
        if(project != null){

            TreeData<Resource> treeData = new TreeData<>();
            Collection<SourceDocument> srcDocs = project.getSourceDocuments();
            Locale locale = Locale.getDefault();
            for(SourceDocument srcDoc : srcDocs) {
            	locale = 
            		srcDoc.getSourceContentHandler().getSourceDocumentInfo().getIndexInfoSet().getLocale();
            	
                DocumentResource docResource = 
                		new DocumentResource(
                			srcDoc, 
                			project.getProjectId(), 
                			project.hasPermission(
                					project.getRoleForDocument(srcDoc.getUuid()), 
                					RBACPermission.DOCUMENT_WRITE));
                
                if(project.hasPermission(
                		project.getRoleForDocument(srcDoc.getUuid()), 
                		RBACPermission.DOCUMENT_READ)) {
                	
	                treeData.addItem(null, docResource);
	                
	                List<AnnotationCollectionReference> collections = 
	                		srcDoc.getUserMarkupCollectionRefs();
	                
	            	List<Resource> readableCollectionResources = collections
            		.stream()
            		.filter(collectionRef -> project.hasPermission(
            				project.getRoleForCollection(
            						collectionRef.getId()), RBACPermission.COLLECTION_READ))
            		.map(collectionRef -> 
            			new CollectionResource(
            				collectionRef, 
            				project.getProjectId(),
            				project.hasPermission(
            						project.getRoleForCollection(collectionRef.getId()), 
            						RBACPermission.COLLECTION_WRITE))
            		)
            		.collect(Collectors.toList());
            		
                    
	                if(!collections.isEmpty()){
	                	
	                    treeData.addItems(
	                    	docResource,
	                    	readableCollectionResources
	                    );
	                }
	                
                }
            }
            Collator collator = Collator.getInstance(locale);
            collator.setStrength(Collator.PRIMARY);
            documentGrid
            	.getColumn(DocumentGridColumn.NAME.name())
            	.setComparator((r1, r2) -> collator.compare(r1.getName(), r2.getName()));
            tagsetGrid.getColumn(TagsetGridColumn.NAME.name())
            .setComparator((t1, t2) -> collator.compare(t1.getName(), t2.getName()));
            
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

        boolean membersEditAllowed = 
        		projectManager.isAuthorizedOnProject(
        				RBACPermission.PROJECT_MEMBERS_EDIT, projectReference.getProjectId());
        miInvite.setVisible(membersEditAllowed);
        teamPanel.setVisible(membersEditAllowed);
        
		initData();
		eventBus.post(new ProjectReadyEvent(project));
    	try {
			rbacEnforcer.enforceConstraints(project.getRoleOnProject());
		} catch (IOException e) {
			errorHandler.showAndLogError("Error trying to fetch role", e);
		}        
    }

    /**
     * handler for project selection
     */
    public void setProjectReference(ProjectReference projectReference) {
        this.projectReference = projectReference;
        eventBus.post(new HeaderContextChangeEvent(projectReference.getName()));
        initProject(projectReference);
    }

    /**
     * called when {@link ProjectChangedEvent} is fired e.g. when source documents have been removed or added
     * @param resourcesChangedEvent
     */
    @Subscribe
    public void handleResourceChanged(ProjectChangedEvent resourcesChangedEvent){
    	reloadAll();
    }
    
    @Subscribe
    public void handleDocumentChanged(DocumentChangeEvent documentChangeEvent) {
    	initData();
    }

    @Subscribe
    public void handleMembersChanged(MembersChangedEvent membersChangedEvent) {
    	try {
	    	ListDataProvider<Member> memberData = new ListDataProvider<>(project.getProjectMembers());
	    	teamGrid.setDataProvider(memberData);
    	}
    	catch (Exception e) {
    		((ErrorHandler)UI.getCurrent()).showAndLogError("error loading Members", e);
    	}
    }
    
    /**
     * deletes selected resources
     *
     * @param clickEvent
     * @param resourceGrid
     */
    private void handleDeleteResources(MenuBar.MenuItem menuItem, TreeGrid<Resource> resourceGrid) {
    	if (!resourceGrid.getSelectedItems().isEmpty()) {
	    	ConfirmDialog.show( 
	    		UI.getCurrent(), 
	    		"Info", 
	    		"Are you sure you want to delete the selected resources: "
	    		+ resourceGrid.getSelectedItems()
	    			.stream()
	    			.map(resource -> resource.getName())
	    			.collect(Collectors.joining(",")) //$NON-NLS-1$
	    		+ "?", 
	    		"Yes", 
	    		"Cancel", dlg -> {
	    			if (dlg.isConfirmed()) {
			            for (Resource resource: resourceGrid.getSelectedItems()) {
			            	try {
			            		resource.deleteFrom(project);
			                } catch (Exception e) {
			                    errorHandler.showAndLogError("Error deleting resource "+resource, e);
			                }
			            }
	    			}
	    		});
    	}
    	else {
    		Notification.show("Info", "Please select a resource first!", Type.HUMANIZED_MESSAGE);
    	}
    }
    
    private void handleAnalyzeResources(MenuBar.MenuItem menuItem, TreeGrid<Resource> resourceGrid) {
    	if (resourceGrid.getSelectedItems().isEmpty()) {
    		Notification.show("Info", "Please select something first!", Type.HUMANIZED_MESSAGE); 
    	}
    	else {
			Corpus corpus = new Corpus();
			
	         for (Resource resource: resourceGrid.getSelectedItems()) {
            	try {
            		if(resource.getClass().equals(DocumentResource.class)) {
            		DocumentResource docResource = (DocumentResource) resource;
            		corpus.addSourceDocument(docResource.getDocument());
            		}else {
            			CollectionResource collResource = (CollectionResource) resource;
	            		corpus.addUserMarkupCollectionReference(collResource.getCollectionReference());
	            		DocumentResource docParent =(DocumentResource) resourceGrid.getTreeData().getParent(collResource);
	            		if(!corpus.getSourceDocuments().contains(docParent.getDocument())) {
	            			corpus.addSourceDocument(docParent.getDocument());
	            		}
            			
            		}
                } catch (Exception e) {
                    errorHandler.showAndLogError("Error adding resource to analyzer module "+resource, e);
                }
            }
    
           eventBus.post( new RouteToAnalyzeEvent((IndexedProject)project, corpus));
    	}

    }

	public void close() {
		try {
			this.eventBus.unregister(this);
			if (project != null) {
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
			if (project != null) {
				project.close();
				project = null;
			}
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Error closing ProjectView", e);
		}
	}
	
	private SerializablePredicate<Object> createSearchFilter(String searchInput) {
		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> documentDataProvider = 
			(TreeDataProvider<Resource>) documentGrid.getDataProvider();
		TreeData<Resource> documentData = documentDataProvider.getTreeData();
		
		return new SerializablePredicate<Object>() {
			@Override
			public boolean test(Object r) {
				if (r instanceof CollectionResource) {
					return r.toString().toLowerCase().startsWith(searchInput.toLowerCase());
				}
				else {
					if (r.toString().toLowerCase().startsWith(searchInput.toLowerCase())) {
						return true;
					}
					else {
						return documentData.getChildren((Resource)r)
								.stream()
								.filter(child -> 
									child.toString().toLowerCase().startsWith(searchInput.toLowerCase()))
								.findAny()
								.isPresent();
					}
				}
			}
		};
	}

 }
