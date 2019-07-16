package de.catma.ui.modules.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.repository.event.ChangeType;
import de.catma.document.repository.event.CollectionChangeEvent;
import de.catma.document.repository.event.DocumentChangeEvent;
import de.catma.document.repository.event.ProjectReadyEvent;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.project.conflict.ConflictedProject;
import de.catma.rbac.RBACConstraint;
import de.catma.rbac.RBACConstraintEnforcer;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.CatmaApplication;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.di.UIFactory;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.dialog.UploadDialog;
import de.catma.ui.events.HeaderContextChangeEvent;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnalyzeOldEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToConflictedProjectEvent;
import de.catma.ui.layout.FlexLayout.FlexWrap;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.modules.main.CanReloadAll;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.repository.Messages;
import de.catma.ui.repository.wizard.AddSourceDocWizardFactory;
import de.catma.ui.repository.wizard.AddSourceDocWizardResult;
import de.catma.ui.repository.wizard.SourceDocumentResult;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;

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
	private final RBACConstraintEnforcer<RBACRole> rbacEnforcer = new RBACConstraintEnforcer<>();
	private final UIFactory uiFactory;
	
    private TreeGrid<Resource> resourceGrid;
    private Grid<TagsetDefinition> tagsetGrid;
	private Grid<Member> teamGrid;
	private ActionGridComponent<TreeGrid<Resource>> documentsGridComponent;
	private PropertyChangeListener projectExceptionListener;
	private ActionGridComponent<Grid<TagsetDefinition>> tagsetsGridComponent;
	private PropertyChangeListener tagsetChangeListener;
	private ListDataProvider<TagsetDefinition> tagsetData;
	private Multimap<Resource, Resource> docResourceToReadableCollectionResourceMap = HashMultimap.create();
	private MenuItem miInvite;
	private VerticalFlexLayout teamPanel;

	@Inject
    public ProjectView(
    		UIFactory uiFactory, ProjectManager projectManager, 
    		EventBus eventBus) {
    	super("Project");
    	this.projectManager = projectManager;
        this.eventBus = eventBus;
    	this.errorHandler = (ErrorHandler)UI.getCurrent();
    	this.uiFactory = uiFactory;
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
    		UserMarkupCollectionReference collectionReference = 
    				collectionChangeEvent.getCollectionReference();
    		
			@SuppressWarnings("unchecked")
			TreeDataProvider<Resource> resourceDataProvider = 
    				(TreeDataProvider<Resource>) resourceGrid.getDataProvider();

			CollectionResource collectionResource = 
				new CollectionResource(
					collectionReference, 
					project.getProjectId(), 
					project.hasPermission(project.getRoleForCollection(collectionReference.getId()), RBACPermission.COLLECTION_WRITE));
			
			DocumentResource documentResource = 
				new DocumentResource(
					document, 
					project.getProjectId(), 
					project.hasPermission(project.getRoleForDocument(document.getID()), RBACPermission.DOCUMENT_WRITE));
			
			resourceDataProvider.getTreeData().addItem(
    				documentResource, collectionResource);
			resourceDataProvider.refreshAll();
			
			resourceGrid.expand(documentResource);
			
			Notification.show(
				"Info", 
				String.format("Collection %1$s has been created!", collectionReference.toString()),  
				Type.TRAY_NOTIFICATION);			
		}
		else {
			initData();
		}
	}

	private void initActions() {
    	resourceGrid.addItemClickListener(itemClickEvent -> handleResourceItemClick(itemClickEvent));
    	
        ContextMenu addContextMenu = 
        	documentsGridComponent.getActionGridBar().getBtnAddContextMenu();
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
        	documentsGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
        
        MenuItem editDocBtn = documentsGridMoreOptionsContextMenu.addItem(
            	"Edit documents / collections",(menuItem) -> handleEditResources());
        editDocBtn.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.COLLECTION_DELETE_OR_EDIT) || 
        				project.hasPermission(role, RBACPermission.DOCUMENT_DELETE_OR_EDIT)),
        		() -> editDocBtn.setEnabled(true))
        		);
        MenuItem deleteDocsBtn = documentsGridMoreOptionsContextMenu.addItem(
        	"Delete documents / collections",(menuItem) -> handleDeleteResources(menuItem, resourceGrid));
        deleteDocsBtn.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.COLLECTION_DELETE_OR_EDIT) || 
        				project.hasPermission(role, RBACPermission.DOCUMENT_DELETE_OR_EDIT)),
        		() -> deleteDocsBtn.setEnabled(true))
        		);
        
        documentsGridMoreOptionsContextMenu.addItem(
            	"Analyze documents / collections",(menuItem) -> handleAnalyzeResources(menuItem, resourceGrid));


        MenuItem editResBtn = documentsGridMoreOptionsContextMenu.addItem("Edit resource permissions", (click) -> {
		        new ResourcePermissionView(
		        		docResourceToReadableCollectionResourceMap,
		        		this.project).show();
		        }
        );
        editResBtn.setEnabled(false);
        
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.PROJECT_MEMBERS_EDIT)),
        		() -> editResBtn.setEnabled(true))
        		);
        
        
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.TAGSET_CREATE_OR_UPLOAD)),
        		() ->  tagsetsGridComponent.getActionGridBar().addBtnAddClickListener(
        	        	click -> handleAddTagsetRequest()))
        		);
   
        ContextMenu moreOptionsMenu = 
        	tagsetsGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
        MenuItem editTagset = moreOptionsMenu.addItem("Edit Tagset", clickEvent -> handleEditTagsetRequest());
        editTagset.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.TAGSET_DELETE_OR_EDIT)),
        		() -> editTagset.setEnabled(true))
        		);

        MenuItem deleteTagSetBtn = moreOptionsMenu.addItem("Delete Tagset", clickEvent -> handleDeleteTagsetRequest());
        deleteTagSetBtn.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.TAGSET_DELETE_OR_EDIT)),
        		() -> deleteTagSetBtn.setEnabled(true))
        		);
        
        MenuItem importTagSetBtn = moreOptionsMenu.addItem("Import Tagsets", clickEvent -> handleImportTagsetsRequest());
        importTagSetBtn.setEnabled(false);
        rbacEnforcer.register(RBACConstraint.ifAuthorized(
        		role -> (project.hasPermission(role, RBACPermission.TAGSET_CREATE_OR_UPLOAD)),
        		() -> importTagSetBtn.setEnabled(true))
        		);
        
        ContextMenu hugeCardMoreOptions = getMoreOptionsContextMenu();
        hugeCardMoreOptions.addItem("Commit all changes", e -> handleCommitRequest());
        hugeCardMoreOptions.addItem("Synchronize with the team", e -> handleSynchronizeRequest());
        hugeCardMoreOptions.addItem("Print status", e -> project.printStatus());
        
        tagsetsGridComponent.setSearchFilterProvider(new SearchFilterProvider<TagsetDefinition>() {
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

	private void handleImportTagsetsRequest() {
		UploadDialog uploadDialog =
				new UploadDialog("Upload Tagsets",
						new SaveCancelListener<byte[]>() {
			
			public void cancelPressed() {}
			
			public void savePressed(byte[] result) {
				InputStream is = new ByteArrayInputStream(result);
				try {
					if (BOMFilterInputStream.hasBOM(result)) {
						is = new BOMFilterInputStream(
								is, Charset.forName("UTF-8")); //$NON-NLS-1$
					}
					
					project.importTagLibrary(is);
					
					
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
						"Error importing Tagsets", e);
				}
				finally {
					CloseSafe.close(is);
				}
			}
			
		});
		uploadDialog.show();	
	}

	private void handleSynchronizeRequest() {
    	try {
	    	if (project.hasUncommittedChanges()) {
	    		SingleTextInputDialog dlg = new SingleTextInputDialog(
	    			"Commit all changes", 
	    			"You have uncommited changes, please enter a short description for this commit:", 
	    			commitMsg -> {
	    				try {
		    				project.commitChanges(commitMsg);
		    				project.synchronizeWithRemote(new OpenProjectListener() {

		    		            @Override
		    		            public void progress(String msg, Object... params) {
		    		            }

		    		            @Override
		    		            public void ready(Repository project) {
		    						initData();
				    				Notification.show(
					    					"Info", 
					    					"Your Project has been synchronized!", 
					    					Type.HUMANIZED_MESSAGE);		    						
		    		            }
		    		            
		    		            @Override
		    		            public void conflictResolutionNeeded(ConflictedProject conflictedProject) {
		    						eventBus.post(new RouteToConflictedProjectEvent(conflictedProject));
		    		            }

		    		            @Override
		    		            public void failure(Throwable t) {
		    		                errorHandler.showAndLogError("error opening project", t);
		    		            }
		    		        });

	    				}
	    				catch (Exception e) {
	    					e.printStackTrace(); //TODO
	    				}
	    			});
	    		dlg.show();
	    	}
	    	else {
	    		project.synchronizeWithRemote(new OpenProjectListener() {

	                @Override
	                public void progress(String msg, Object... params) {
	                }

	                @Override
	                public void ready(Repository project) {
	    				initData();
	    				//TODO: post event to other views needed
	    				Notification.show(
		    					"Info", 
		    					"Your Project has been synchronized!", 
		    					Type.HUMANIZED_MESSAGE);	    				
	                }
	                
	                @Override
	                public void conflictResolutionNeeded(ConflictedProject conflictedProject) {
	    				eventBus.post(new RouteToConflictedProjectEvent(conflictedProject));
	                }

	                @Override
	                public void failure(Throwable t) {
	                    errorHandler.showAndLogError("error opening project", t);
	                }
	            });
				Notification.show(
					"Info", 
					"Your Project has been synchronized!", 
					Type.HUMANIZED_MESSAGE);	    		
	    	}
    	}
    	catch (Exception e) {
            errorHandler.showAndLogError("error accessing project", e);
    	}	
    }

	private void handleEditResources() {
		final Set<Resource> selectedResources = resourceGrid.getSelectedItems();
		if ((selectedResources.size() != 1) 
				&& !selectedResources.iterator().next().isCollection()) {
			Notification.show("Info", "Please select a single entry first!", Type.HUMANIZED_MESSAGE);
		}	
		else {
			final Resource resource = selectedResources.iterator().next();
			
			// TODO: add proper edit metadata dialog including document level annotations!
			
			if (resource.isCollection()) {
				final UserMarkupCollectionReference collectionRef = 
						((CollectionResource)selectedResources.iterator().next()).getCollectionReference();
		    	SingleTextInputDialog collectionNameDlg = 
	        		new SingleTextInputDialog("Edit Collection", "Please enter the new Collection name:",
        				new SaveCancelListener<String>() {
    						@Override
    						public void savePressed(String result) {
    							collectionRef.getContentInfoSet().setTitle(result);
    							try {
									project.update(collectionRef, collectionRef.getContentInfoSet());
									resourceGrid.getDataProvider().refreshItem(resource);
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
							
//							project.addPropertyChangeListener(
//								RepositoryChangeEvent.sourceDocumentChanged,
//								new PropertyChangeListener() {
//
//									@Override
//									public void propertyChange(PropertyChangeEvent evt) {
//										
//										if ((evt.getNewValue() == null)	|| (evt.getOldValue() != null)) {
//											return; // no insert
//										}
//										
//										String newSdId = (String) evt.getNewValue();
//										if (!sourceDocument.getID().equals(newSdId)) {
//											return;
//										}
//										
//											
//										project.removePropertyChangeListener(
//											RepositoryChangeEvent.sourceDocumentChanged, 
//											this);
										
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
//									}
//								});

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
 
        miInvite = getMoreOptionsContextMenu().addItem(
        	"Invite someone to the Project", click -> 
        		uiFactory.getProjectInvitationDialog(
        			project, 
        			docResourceToReadableCollectionResourceMap.keySet()).show());
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
    	resourceGrid = new TreeGrid<>();
        resourceGrid.addStyleNames(
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");

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
        
        resourceGrid
    	.addColumn(res -> res.getPermissionIcon() , new HtmlRenderer())
    	.setCaption("Permission")
    	.setExpandRatio(1);      
        
        //TODO: see MD for when it is appropriate to offer row options
//        ButtonRenderer<Resource> resourceOptionsRenderer = new ButtonRenderer<>(
//				resourceOptionClickedEvent -> handleResourceOptionClicked(resourceOptionClickedEvent));
//        resourceOptionsRenderer.setHtmlContentAllowed(true);
        
//		resourceGrid.addColumn(
//			(nan) -> VaadinIcons.ELLIPSIS_DOTS_V.getHtml(), 
//			resourceOptionsRenderer);
        
        
        
        Label documentsAnnotations = new Label("Documents & Annotations");

        documentsGridComponent = new ActionGridComponent<TreeGrid<Resource>>(
                documentsAnnotations,
                resourceGrid
        );
        documentsGridComponent.addStyleName("project-view-action-grid");

        resourceContent.addComponent(documentsGridComponent);

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
		HorizontalFlexLayout teamContent = new HorizontalFlexLayout();
        teamGrid = new Grid<>();
        teamGrid.setHeaderVisible(false);
        teamGrid.setWidth("402px");
        teamGrid.addColumn((user) -> VaadinIcons.USER.getHtml(), new HtmlRenderer());
        teamGrid.addColumn(User::getName).setWidth(200).setDescriptionGenerator(User::preciseName);
        teamGrid.addColumn(Member::getRole).setExpandRatio(1);
        
        Label membersAnnotations = new Label("Members");
        ActionGridComponent<Grid<Member>> membersGridComponent = new ActionGridComponent<>(
                membersAnnotations,
                teamGrid
        );
        membersGridComponent.addStyleName("project-view-action-grid");
        ContextMenu addContextMenu = membersGridComponent.getActionGridBar().getBtnAddContextMenu();

        addContextMenu.addItem("Add Member", (click) -> 
        	new CreateMemberDialog(
        		project::assignOnProject,
        		(query) -> project.findUser(query.getFilter().isPresent() ? query.getFilter().get() : "", query.getOffset(), query.getLimit()),
        		(evt) -> eventBus.post(new ResourcesChangedEvent<Component>(this))
        		).show());
        
        ContextMenu moreOptionsContextMenu = membersGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

        moreOptionsContextMenu.addItem("Edit Members", (click) -> new EditMemberDialog(
        		project::assignOnProject,
        		teamGrid.getSelectedItems(),
        		(evt) -> eventBus.post(new ResourcesChangedEvent<Component>(this))
        		).show());
        moreOptionsContextMenu.addItem("Remove Members", (click) -> new RemoveMemberDialog(
        		project::unassignFromProject,
        		teamGrid.getSelectedItems(),
        		(evt) -> eventBus.post(new ResourcesChangedEvent<Component>(this))
        		).show());
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
            public void ready(Repository project) {
                ProjectView.this.project = project;
                ProjectView.this.project.addPropertyChangeListener(
                		RepositoryChangeEvent.exceptionOccurred, 
                		projectExceptionListener);
                
                ProjectView.this.project.getTagManager().addPropertyChangeListener(
                		TagManagerEvent.tagsetDefinitionChanged,
                		tagsetChangeListener);

                boolean membersEditAllowed = 
                		projectManager.isAuthorizedOnProject(RBACPermission.PROJECT_MEMBERS_EDIT, projectReference.getProjectId());
		        miInvite.setVisible(membersEditAllowed);
		        teamPanel.setVisible(membersEditAllowed);
                
				initData();
				eventBus.post(new ProjectReadyEvent(project));
            }
            
            @Override
            public void conflictResolutionNeeded(ConflictedProject conflictedProject) {
				eventBus.post(new RouteToConflictedProjectEvent(conflictedProject));
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
        	
        	ListDataProvider<Member> memberData = new ListDataProvider<>(project.getProjectMembers());
        	teamGrid.setDataProvider(memberData);
		} catch (Exception e) {
			errorHandler.showAndLogError("error initializing data", e);
		}
	}

    private TreeDataProvider<Resource> buildResourceDataProvider() throws Exception {
        if(project != null){
            docResourceToReadableCollectionResourceMap.clear();

            TreeData<Resource> treeData = new TreeData<>();
            Collection<SourceDocument> srcDocs = project.getSourceDocuments();
            
            for(SourceDocument srcDoc : srcDocs) {
            	
                DocumentResource docResource = 
                		new DocumentResource(
                			srcDoc, 
                			project.getProjectId(), 
                			project.hasPermission(project.getRoleForDocument(srcDoc.getID()), RBACPermission.DOCUMENT_WRITE));
                
                if(project.hasPermission(project.getRoleForDocument(srcDoc.getID()), RBACPermission.DOCUMENT_READ)) {
	                treeData.addItem(null,docResource);
	                
	                List<UserMarkupCollectionReference> collections = 
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
	                	
	                    treeData.addItems(
	                    	docResource,
	                    	readableCollectionResources
	                    );
	                }
	                
	                docResourceToReadableCollectionResourceMap.putAll(
	                		docResource, readableCollectionResources);

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
        reloadAll();
    }

    /**
     * called when {@link ResourcesChangedEvent} is fired e.g. when source documents have been removed or added
     * @param resourcesChangedEvent
     */
    @Subscribe
    public void handleResourceChanged(ResourcesChangedEvent<TreeGrid<Resource>> resourcesChangedEvent){
    	reloadAll();
    }
    
    @Subscribe
    public void handleDocumentChanged(DocumentChangeEvent documentChangeEvent) {
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
    		});

    }
    
    private void handleAnalyzeResources(MenuBar.MenuItem menuItem, TreeGrid<Resource> resourceGrid) {
    	if (resourceGrid.getSelectedItems().isEmpty()) {
    		Notification.show("Info", "Please select something first!", Type.HUMANIZED_MESSAGE);
    	}
    	else {
			Corpus corpus = new Corpus("to analyze");
			
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
    
           eventBus.post( new RouteToAnalyzeOldEvent((IndexedRepository)project, corpus));
           eventBus.post( new RouteToAnalyzeEvent((IndexedRepository)project, corpus));
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
 }
