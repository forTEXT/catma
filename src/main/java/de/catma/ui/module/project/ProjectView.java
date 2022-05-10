package de.catma.ui.module.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
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
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.corpus.Corpus;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.SourceDocumentReference;
import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.document.source.contenthandler.TikaContentHandler;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.indexer.IndexedProject;
import de.catma.project.CommitInfo;
import de.catma.project.OpenProjectListener;
import de.catma.project.Project;
import de.catma.project.Project.RepositoryChangeEvent;
import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.project.event.ChangeType;
import de.catma.project.event.CollectionChangeEvent;
import de.catma.project.event.DocumentChangeEvent;
import de.catma.project.event.ProjectReadyEvent;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACConstraintEnforcer;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.serialization.TagsetDefinitionImportStatus;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.TagsetMetadata;
import de.catma.ui.CatmaApplication;
import de.catma.ui.Parameter;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.dialog.BeyondResponsibilityConfirmDialog;
import de.catma.ui.dialog.BeyondResponsibilityConfirmDialog.Action;
import de.catma.ui.dialog.GenericUploadDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.events.HeaderContextChangeEvent;
import de.catma.ui.events.MembersChangedEvent;
import de.catma.ui.events.ProjectChangedEvent;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToTagsEvent;
import de.catma.ui.layout.FlexLayout.FlexWrap;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.main.CanReloadAll;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.project.corpusimport.CorpusImportDialog;
import de.catma.ui.module.project.corpusimport.CorpusImportDocumentMetadata;
import de.catma.ui.module.project.corpusimport.CorpusImporter;
import de.catma.ui.module.project.documentwizard.DocumentWizard;
import de.catma.ui.module.project.documentwizard.TagsetImport;
import de.catma.ui.module.project.documentwizard.TagsetImportState;
import de.catma.ui.module.project.documentwizard.UploadFile;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

/**
 *
 * Renders one project with all resources
 *
 * @author db
 */
public class ProjectView extends HugeCard implements CanReloadAll {
	
	private enum DocumentGridColumn {
		NAME,
		RESPONSIBLE,
		;
	}
	
	private enum TagsetGridColumn {
		NAME, 
		RESPONSIBLE,
		;
	}

	private Logger logger = Logger.getLogger(ProjectView.class.getName());
	private final TagManager tagManager;

	private ProjectsManager projectManager;
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
    private PropertyChangeListener tagReferencesChangedListener;

    private MenuItem miInvite;
	private ProgressBar progressBar;

	private Button btSynchBell;
	private final ProgressListener progressListener;
	private MenuItem miToggleResponsibiltityFilter;
	private Map<String, Member> membersByIdentfier;

    public ProjectView(
    		ProjectsManager projectManager, 
    		EventBus eventBus) {
    	super("Project");
    	this.projectManager = projectManager;
        this.eventBus = eventBus;
    	this.errorHandler = (ErrorHandler)UI.getCurrent();
		TagLibrary tagLibrary = new TagLibrary();
		this.tagManager = new TagManager(tagLibrary);

		
		final UI ui = UI.getCurrent();
		this.progressListener = new ProgressListener() {
			
			@Override
			public void setProgress(String value, Object... args) {
				ui.accessSynchronously(() -> {
	            	if (args != null) {
	            		progressBar.setCaption(String.format(value, args));
	            	}
	            	else {
	            		progressBar.setCaption(value);
	            	}
	            	ui.push();
				});
			}
		};
		

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
		
		this.tagReferencesChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				checkForUnsynchronizedCommits();
			};
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
			tagsetGrid.deselect((TagsetDefinition) oldValue);
		}
		else { // metadata update
			TagsetDefinition tagset = (TagsetDefinition)newValue;
			tagsetData.refreshItem(tagset);
		}
		
		btSynchBell.setVisible(true);
	}
	
	@Subscribe
	public void handleCollectionChanged(CollectionChangeEvent collectionChangeEvent) {
		if (collectionChangeEvent.getChangeType().equals(ChangeType.CREATED)) {
    		SourceDocumentReference document = collectionChangeEvent.getDocument();
    		AnnotationCollectionReference collectionReference = 
    				collectionChangeEvent.getCollectionReference();
    		
			@SuppressWarnings("unchecked")
			TreeDataProvider<Resource> resourceDataProvider = 
    				(TreeDataProvider<Resource>) documentGrid.getDataProvider();

			CollectionResource collectionResource = 
				new CollectionResource(
					collectionReference, 
					project.getProjectId(), 
					project.getUser());
			
			DocumentResource documentResource = 
				new DocumentResource(
					document, 
					project.getProjectId());
			
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
		
		btSynchBell.setVisible(true);
	}

	private void initActions() {
		documentGridComponent.setSearchFilterProvider(searchInput -> createSearchFilter(searchInput));

    	documentGrid.addItemClickListener(itemClickEvent -> handleResourceItemClick(itemClickEvent));
    	
        ContextMenu addContextMenu = 
        	documentGridComponent.getActionGridBar().getBtnAddContextMenu();
        addContextMenu.addItem("Add Document", clickEvent -> handleAddDocumentRequest());
        addContextMenu.addItem("Add Annotation Collection", e -> handleAddCollectionRequest());
        
        ContextMenu documentsGridMoreOptionsContextMenu = 
        	documentGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
        
        documentsGridMoreOptionsContextMenu.addItem(
            	"Edit Documents / Collections",(menuItem) -> handleEditResources());

        documentsGridMoreOptionsContextMenu.addItem(
        	"Delete Documents / Collections",(menuItem) -> handleDeleteResources(menuItem, documentGrid));
        
        documentsGridMoreOptionsContextMenu.addItem(
            	"Analyze Documents / Collections",(menuItem) -> handleAnalyzeResources(menuItem, documentGrid));

        documentsGridMoreOptionsContextMenu.addItem(
        		"Import a Collection", 
        		mi -> handleImportCollectionRequest());
        MenuItem miExportCollections = documentsGridMoreOptionsContextMenu.addItem(
        		"Export Documents & Collections");
        
		StreamResource collectionXmlExportResource = new StreamResource(
				new CollectionXMLExportStreamSource(
					()-> getSelectedDocuments(),
					() -> documentGrid.getSelectedItems().stream()
						.filter(resource -> resource.isCollection())
						.map(resource -> ((CollectionResource)resource).getCollectionReference())
						.collect(Collectors.toList()),
					() -> project),
				"CATMA-Corpus_Export-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ".tar.gz");
		collectionXmlExportResource	.setCacheTime(0);
		collectionXmlExportResource.setMIMEType("application/gzip");
	
		FileDownloader collectionXmlExportFileDownloader = 
					new FileDownloader(collectionXmlExportResource);
		
		collectionXmlExportFileDownloader.extend(miExportCollections);

		
        documentsGridMoreOptionsContextMenu.addItem("Select filtered entries", mi-> handleSelectFilteredDocuments());
        
        miToggleResponsibiltityFilter = 
        	documentsGridMoreOptionsContextMenu.addItem(
        			"Hide other's responsibilities", mi -> toggleResponsibilityFilter());
        
        miToggleResponsibiltityFilter.setCheckable(true);
        miToggleResponsibiltityFilter.setChecked(false);
        
        tagsetGridComponent.getActionGridBar().addBtnAddClickListener(click -> handleAddTagsetRequest());
   
        ContextMenu moreOptionsMenu = 
        	tagsetGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
        moreOptionsMenu.addItem("Edit Tagset", mi -> handleEditTagsetRequest());

        moreOptionsMenu.addItem("Delete Tagset", mi -> handleDeleteTagsetRequest());
        
        moreOptionsMenu.addItem("Import Tagsets", mi -> handleImportTagsetsRequest());
        
        MenuItem miExportTagsets = moreOptionsMenu.addItem("Export Tagsets");
        MenuItem miExportTagsetsAsXML = miExportTagsets.addItem("as XML");
        
		StreamResource tagsetXmlExportResource = new StreamResource(
			new TagsetXMLExportStreamSource(
				() -> tagsetGrid.getSelectedItems(), 
				() -> project),
			"CATMA-Tag-Library_Export-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ".xml");
		tagsetXmlExportResource	.setCacheTime(0);
		tagsetXmlExportResource.setMIMEType("text/xml");
	
		FileDownloader tagsetXmlExportFileDownloader = 
				new FileDownloader(tagsetXmlExportResource);
	
		tagsetXmlExportFileDownloader.extend(miExportTagsetsAsXML);
		
        MenuItem miExportTagsetsAsCSV = miExportTagsets.addItem("as CSV");
        
		StreamResource tagsetCsvExportResource = new StreamResource(
			new TagsetCSVExportStreamSource(
				() -> tagsetGrid.getSelectedItems(), 
				() -> project),
			"CATMA-Tag-Library_Export-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ".csv");
		tagsetCsvExportResource	.setCacheTime(0);
		tagsetCsvExportResource.setMIMEType("text/comma-separated-values");
	
		FileDownloader tagsetCsvExportFileDownloader = 
				new FileDownloader(tagsetCsvExportResource);
	
		tagsetCsvExportFileDownloader.extend(miExportTagsetsAsCSV);
		
		
        ContextMenu hugeCardMoreOptions = getMoreOptionsContextMenu();
        hugeCardMoreOptions.addItem("Commit all changes", mi -> handleCommitRequest());
        hugeCardMoreOptions.addItem("Synchronize with the team", mi -> handleSynchronizeRequest());
        MenuItem miImportCorpus = hugeCardMoreOptions.addItem("Import CATMA 5 Corpus", mi -> handleCorpusImport());
        miImportCorpus.setVisible(CATMAPropertyKey.EXPERT.getValue(false) 
        		|| Boolean.valueOf(((CatmaApplication)UI.getCurrent()).getParameter(Parameter.EXPERT, Boolean.FALSE.toString())));
        

        btSynchBell.addClickListener(event -> handleBtSynchBellClick(event));
        
        tagsetGridComponent.setSearchFilterProvider(new SearchFilterProvider<TagsetDefinition>() {
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
        
        tagsetGrid.addItemClickListener(clickEvent -> handleTagsetClick(clickEvent));
        
        
	}
	
	private void toggleResponsibilityFilter() {

		documentGrid.getColumn(DocumentGridColumn.RESPONSIBLE.name()).setHidden(
				miToggleResponsibiltityFilter.isChecked());
		documentGrid.getColumn(DocumentGridColumn.NAME.name()).setWidth(
				miToggleResponsibiltityFilter.isChecked()?300:200);

		initData();
	}

	private void handleBtSynchBellClick(ClickEvent event) {
		try {
			List<CommitInfo> unsynchronizedChanges = project.getUnsynchronizedCommits();
			if (unsynchronizedChanges.isEmpty() && project.hasUncommittedChanges()) {
				ConfirmDialog.show(UI.getCurrent(), "You have uncommited changes, do you want to commit now?", dlg -> {
					if (dlg.isConfirmed()) {
						handleCommitRequest();
					}
				});
			}
			else {
				UnsychronizedCommitsDialog dlg = 
					new UnsychronizedCommitsDialog(
							unsynchronizedChanges, ()->handleSynchronizeRequest());
				dlg.show();
			}
		} catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("Checking for unsynchronized changes failed!", e);
		}
	}
	private void handleCorpusImport() {
		try {
	    	if (project.hasUncommittedChanges()) {
	    		SingleTextInputDialog dlg = new SingleTextInputDialog(
	    			"Commit all changes", 
	    			"You have changes, that need to be committed first, please enter a short description for this commit:", 
	    			commitMsg -> {
	    				try {
		    				project.commitChanges(commitMsg);
		    				importCollection();
	    				}
	    				catch (Exception e) {
	    					setEnabled(true);
	    					((ErrorHandler)UI.getCurrent()).showAndLogError("error committing changes", e);
	    				}
	    			});
	    		dlg.show();
	    	}
	    	else {
	    		CorpusImportDialog corpusImportDialog = new CorpusImportDialog(
	    			new SaveCancelListener<Pair<File,List<CorpusImportDocumentMetadata>>>() {
	    				@Override
	    				public void savePressed(Pair<File, List<CorpusImportDocumentMetadata>> result) {
	    					importCorpus(result.getFirst(), result.getSecond());
	    				}
				});
	    		
	    		corpusImportDialog.show();
	    	}
    	}
    	catch (Exception e) {
            errorHandler.showAndLogError("Error accessing Project!", e);
    	}		
	}
	
	private void importCorpus(final File corpusFile, final List<CorpusImportDocumentMetadata> documentMetadataList) {
    	setEnabled(false);
    	setProgressBarVisible(true);
		try {
			final String tempDir = 
					((CatmaApplication)UI.getCurrent()).accquirePersonalTempFolder();
	    	final UI ui = UI.getCurrent();
	    	
			BackgroundServiceProvider backgroundServiceProvider = (BackgroundServiceProvider)UI.getCurrent();
			BackgroundService backgroundService = backgroundServiceProvider.accuireBackgroundService();
			backgroundService.submit(
				new DefaultProgressCallable<Void>() {
					@Override
					public Void call() throws Exception {
						return new CorpusImporter().importCorpus(getProgressListener(), corpusFile, documentMetadataList, tempDir, ui, project);
					}
				},
				new ExecutionListener<Void>() {
					@Override
					public void done(Void result) {
		            	setProgressBarVisible(false);
		            	setEnabled(true);
					}
					@Override
					public void error(Throwable t) {
		            	setProgressBarVisible(false);
		            	setEnabled(true);
		    			Logger.getLogger(ProjectView.class.getName()).log(
		    					Level.SEVERE, 
		    					"Error importing the CATMA 5 Corpus!", 
		    					t);
		    			String errorMsg = t.getMessage();
		    			if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
		    				errorMsg = "";
		    			}
	
		    			Notification.show(
		    				"Error", 
		    				String.format(
		    						"Error importing the CATMA 5 Corpus! "
		    						+ "This import will be aborted!\n The underlying error message was:\n%1$s", 
		    						errorMsg), 
		    				Type.ERROR_MESSAGE);					
					}
				},
				progressListener);
		}
		catch (Exception e) {
        	setProgressBarVisible(false);
        	setEnabled(true);
			Logger.getLogger(ProjectView.class.getName()).log(
					Level.SEVERE, 
					"Error importing the CATMA 5 Corpus!", 
					e);
			String errorMsg = e.getMessage();
			if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
				errorMsg = "";
			}

			Notification.show(
				"Error", 
				String.format(
						"Error importing the CATMA 5 Corpus! "
						+ "This import will be aborted!\n The underlying error message was:\n%1$s", 
						errorMsg), 
				Type.ERROR_MESSAGE);	
		}
	}

	private void handleImportCollectionRequest() {
		try {
	    	if (project.hasUncommittedChanges()) {
	    		SingleTextInputDialog dlg = new SingleTextInputDialog(
	    			"Commit all changes", 
	    			"You have changes, that need to be committed first, please enter a short description for this commit:", 
	    			commitMsg -> {
	    				try {
		    				project.commitChanges(commitMsg);
		    				importCollection();
	    				}
	    				catch (Exception e) {
	    					setEnabled(true);
	    					((ErrorHandler)UI.getCurrent()).showAndLogError("error committing changes", e);
	    				}
	    			});
	    		dlg.show();
	    	}
	    	else {
	    		importCollection();
	    	}
    	}
    	catch (Exception e) {
            errorHandler.showAndLogError("Error accessing Project!", e);
    	}		
	}
	private void importCollection() {
		Set<SourceDocumentReference> selectedDocuments = getSelectedDocuments();
		
		if (selectedDocuments.size() != 1) {
			Notification.show("Info", "Please select the corresponding Document first!", Type.HUMANIZED_MESSAGE);
		}
		else {
			final SourceDocumentReference document = selectedDocuments.iterator().next();
			
			GenericUploadDialog uploadDialog =
					new GenericUploadDialog(String.format("Upload a Collection for %1$s:", document.toString()),
							new SaveCancelListener<byte[]>() {
				
				public void savePressed(byte[] result) {
					InputStream is = new ByteArrayInputStream(result);
					try {
						if (BOMFilterInputStream.hasBOM(result)) {
							is = new BOMFilterInputStream(
									is, Charset.forName("UTF-8")); //$NON-NLS-1$
						}
						
						Pair<AnnotationCollection, List<TagsetDefinitionImportStatus>> loadResult =
								project.loadAnnotationCollection(is, document);
						
						List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList = loadResult.getSecond();
						final AnnotationCollection annotationCollection = loadResult.getFirst();
						
						CollectionImportDialog tagsetImportDialog = 
							new CollectionImportDialog(
									tagsetDefinitionImportStatusList, 
									new SaveCancelListener<List<TagsetDefinitionImportStatus>>() {
								@Override
								public void savePressed(List<TagsetDefinitionImportStatus> result) {
									try {
										project.importCollection(result, annotationCollection);
									} catch (IOException e) {
										((CatmaApplication)UI.getCurrent()).showAndLogError(
											"Error importing Tagsets", e);
									}
								}
						});
						
						tagsetImportDialog.show();
						
					} catch (IOException e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(
							"Error loading external Tagsets", e);
					}
					finally {
						CloseSafe.close(is);
					}
				}
				
			});
			uploadDialog.show();
		}
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
		GenericUploadDialog uploadDialog =
				new GenericUploadDialog("Upload a Tag Library with one or more Tagsets:",
						new SaveCancelListener<byte[]>() {
			
			public void savePressed(byte[] result) {
				InputStream is = new ByteArrayInputStream(result);
				try {
					if (BOMFilterInputStream.hasBOM(result)) {
						is = new BOMFilterInputStream(
								is, Charset.forName("UTF-8")); //$NON-NLS-1$
					}
					
					List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList = 
							project.loadTagLibrary(is);
					
					TagsetImportDialog tagsetImportDialog = 
						new TagsetImportDialog(
								tagsetDefinitionImportStatusList, 
								new SaveCancelListener<List<TagsetDefinitionImportStatus>>() {
							@Override
							public void savePressed(List<TagsetDefinitionImportStatus> result) {
								try {
									project.importTagsets(result);
								} catch (IOException e) {
									((CatmaApplication)UI.getCurrent()).showAndLogError(
										"Error importing Tagsets", e);
								}
							}
						});
					
					tagsetImportDialog.show();
					
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
						"Error loading external Tagsets", e);
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
    		setEnabled(false);
    		
	    	if (project.hasUncommittedChanges()) {
	    		SingleTextInputDialog dlg = new SingleTextInputDialog(
	    			"Commit all changes", 
	    			"You have uncommitted changes, please enter a short description for this commit:", 
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
				checkForUnsynchronizedCommits();
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

			if (resource.isCollection()) {
				
				final AnnotationCollectionReference collectionRef = 
						((CollectionResource)selectedResources.iterator().next()).getCollectionReference();
				boolean beyondUsersResponsibility = 
						!collectionRef.isResponsible(project.getUser().getIdentifier());
				BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					beyondUsersResponsibility, 
					new Action() {
						@Override
						public void execute() {

							EditResourceDialog editCollectionDlg = 
								new EditResourceDialog(
									collectionRef.getResponsibleUser(), 
									collectionRef.getContentInfoSet(),
									ProjectView.this.membersByIdentfier.values(),
									new SaveCancelListener<Pair<String,ContentInfoSet>>() {
										@Override
										public void savePressed(Pair<String, ContentInfoSet> result) {
											collectionRef.setResponsibleUser(result.getFirst());
											try {
												project.update(collectionRef, result.getSecond());
											} catch (Exception e) {
												errorHandler.showAndLogError("Error updating collection", e);
												reloadAll();
											}
										}
									});
							editCollectionDlg.show();
						}
					});
			}
			else {
				final SourceDocumentReference document = 
						((DocumentResource)selectedResources.iterator().next()).getDocument();
				EditResourceDialog editDocumentDlg = new EditResourceDialog(
					document.getSourceDocumentInfo().getContentInfoSet(), 
					new SaveCancelListener<Pair<String,ContentInfoSet>>() {
						@Override
						public void savePressed(Pair<String, ContentInfoSet> result) {
							try {
								project.update(document, document.getSourceDocumentInfo().getContentInfoSet());
							}
							catch (Exception e) {
								errorHandler.showAndLogError("Error updating document", e);
							}
						}
					});
				editDocumentDlg.show();
			}
		}
	}

	private void handleDeleteTagsetRequest() {
		final Set<TagsetDefinition> tagsets = tagsetGrid.getSelectedItems();
		if (!tagsets.isEmpty()) {
			boolean beyondUsersResponsibility = 
					tagsets.stream()
					.filter(tagset -> !tagset.isResponsible(project.getUser().getIdentifier()))
					.findAny()
					.isPresent();
			
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
				new Action() {
					@Override
					public void execute() {

						List<String> tagsetNames = 
								tagsets.stream()
								.map(TagsetDefinition::getName)
								.sorted()
								.collect(Collectors.toList());
			
						ConfirmDialog.show(
								UI.getCurrent(), 
								"Warning", 
								String.format("Are you sure you want to delete Tagset(s) %1$s and all related data?", 
									String.join(",", tagsetNames)),
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
				});
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
			boolean beyondUsersResponsibility = !tagset.isResponsible(project.getUser().getIdentifier());
			
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
				new Action() {
					@Override
					public void execute() {
			
						EditTagsetDialog editTagsetDlg = new EditTagsetDialog(
							new TagsetMetadata(
									tagset.getName(), 
									tagset.getDescription(), 
									tagset.getResponsibleUser()),
							ProjectView.this.membersByIdentfier.values(),
							new SaveCancelListener<TagsetMetadata>() {
								@Override
								public void savePressed(TagsetMetadata result) {
									project.getTagManager().setTagsetMetadata(
											tagset, result);
								}
							});
						editTagsetDlg.show();
					}
				});
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
							TagsetDefinition tagset = new TagsetDefinition(
									idGenerator.generateTagsetId(), result);
							tagset.setResponsibleUser(project.getUser().getIdentifier());
							project.getTagManager().addTagsetDefinition(
								tagset);
						}
					});
        	
        collectionNameDlg.show();
    	
	}
	
	private Set<SourceDocumentReference> getSelectedDocuments() {
		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> resourceDataProvider = 
				(TreeDataProvider<Resource>) documentGrid.getDataProvider();
		
    	Set<Resource> selectedResources = documentGrid.getSelectedItems();
    	
    	Set<SourceDocumentReference> selectedDocuments = new HashSet<>();
    	
    	for (Resource resource : selectedResources) {
    		Resource root = 
        			resourceDataProvider.getTreeData().getParent(resource);

    		if (root == null) {
    			root = resource;
    		}
    		
    		DocumentResource documentResource = (DocumentResource)root;
    		selectedDocuments.add(documentResource.getDocument());
    	}
    	
    	return selectedDocuments;
	}

	private void handleAddCollectionRequest() {

    	Set<SourceDocumentReference> selectedDocuments = getSelectedDocuments();
    	
    	if (!selectedDocuments.isEmpty()) {
	    	SingleTextInputDialog collectionNameDlg = 
	    		new SingleTextInputDialog("Add Annotation Collection(s)", "Please enter the Collection name:", 
	    				new SaveCancelListener<String>() {
							
							@Override
							public void savePressed(String result) {
								for (SourceDocumentReference document : selectedDocuments) {
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
		WizardContext wizardContext = new WizardContext();
		wizardContext.put(DocumentWizard.WizardContextKey.PROJECT, project);
		DocumentWizard documentWizard = new DocumentWizard(wizardContext, new SaveCancelListener<WizardContext>() {
			@Override
			public void savePressed(WizardContext result) {
				
				handleSaveDocumentWizardContext(result);
			}

		});
		
		documentWizard.show();
	}

	private void handleSaveDocumentWizardContext(final WizardContext result) {
    	setEnabled(false);
    	setProgressBarVisible(true);
    	
    	final UI ui = UI.getCurrent();
    	
		BackgroundServiceProvider backgroundServiceProvider = (BackgroundServiceProvider)UI.getCurrent();
		BackgroundService backgroundService = backgroundServiceProvider.accuireBackgroundService();
		backgroundService.submit(
			new DefaultProgressCallable<Void>() {
				@SuppressWarnings("unchecked")
				@Override
				public Void call() throws Exception {
					
					IDGenerator idGenerator = new IDGenerator();
					boolean useApostropheAsSeparator = 
							(Boolean)result.get(DocumentWizard.WizardContextKey.APOSTROPHE_AS_SEPARATOR);
					String collectionNamePattern = (String)result.get(DocumentWizard.WizardContextKey.COLLECTION_NAME_PATTERN);
							
					Collection<TagsetImport> tagsetImports = 
							(Collection<TagsetImport>)result.get(DocumentWizard.WizardContextKey.TAGSET_IMPORT_LIST);
					Collection<UploadFile> uploadFiles = 
							(Collection<UploadFile>)result.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST);
					
					if (tagsetImports == null) {
						tagsetImports = Collections.emptyList();
					}
					
					// Ignoring Tagsets
					tagsetImports.stream().filter(ti -> ti.getImportState().equals(TagsetImportState.WILL_BE_IGNORED)).forEach(tagsetImport -> {
						for (TagDefinition tag : tagsetImport.getExtractedTagset()) {
							for (UploadFile uploadFile : uploadFiles) {
								if (uploadFile.getIntrinsicMarkupCollection() != null) {
									AnnotationCollection intrinsicMarkupCollection =
											uploadFile.getIntrinsicMarkupCollection();
									intrinsicMarkupCollection.removeTagReferences(intrinsicMarkupCollection.getTagReferences(tag));
								}
							}
						}
					});
					
					getProgressListener().setProgress("Creating imported Tagsets");
					// Creating Tagsets
					tagsetImports.stream().filter(ti -> ti.getImportState().equals(TagsetImportState.WILL_BE_CREATED)).forEach(tagsetImport -> {
						getProgressListener().setProgress(
								"Creating Tagset %1$s", tagsetImport.getTargetTagset().getName());
						ui.accessSynchronously(() -> {
							if (project.getTagManager().getTagLibrary().getTagsetDefinition(tagsetImport.getTargetTagset().getUuid()) != null) {
								// already imported, so it will be a merge
								tagsetImport.setImportState(TagsetImportState.WILL_BE_MERGED);
							}
							else {
								TagsetDefinition extractedTagset = 
										tagsetImport.getExtractedTagset();
								try {
									project.importTagsets(
										Collections.singletonList(
											new TagsetDefinitionImportStatus(
													extractedTagset, 
													project.getTagManager().getTagLibrary().getTagsetDefinition(extractedTagset.getUuid()) != null)));
								}
								catch (Exception e) {
									Logger.getLogger(ProjectView.class.getName()).log(
											Level.SEVERE, 
											String.format("Error importing tagset %1$s with ID %2$s", 
													extractedTagset.getName(), 
													extractedTagset.getUuid()), 
											e);
									String errorMsg = e.getMessage();
									if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
										errorMsg = "";
									}
			
									Notification.show(
										"Error", 
										String.format(
												"Error importing tagset %1$s! "
												+ "This tagset will be skipped!\n The underlying error message was:\n%2$s", 
												extractedTagset.getName(), errorMsg), 
										Type.ERROR_MESSAGE);					
								}
							}
							ui.push();
						});
					});
					
					// Merging Tagsets
					tagsetImports.stream().filter(ti -> ti.getImportState().equals(TagsetImportState.WILL_BE_MERGED)).forEach(tagsetImport -> {
						getProgressListener().setProgress(
								"Merging Tagset %1$s", tagsetImport.getTargetTagset().getName());
						ui.accessSynchronously(() -> {
							TagsetDefinition targetTagset = 
								project.getTagManager().getTagLibrary().getTagsetDefinition(tagsetImport.getTargetTagset().getUuid());
							
							for (TagDefinition tag : tagsetImport.getExtractedTagset()) {
								
								Optional<TagDefinition> optionalTag =
									targetTagset.getTagDefinitionsByName(tag.getName()).findFirst();
								
								if (optionalTag.isPresent()) {
									TagDefinition existingTag = optionalTag.get();
									
									tag.getUserDefinedPropertyDefinitions().forEach(pd -> {
										if (existingTag.getPropertyDefinition(pd.getName()) == null) {
											project.getTagManager().addUserDefinedPropertyDefinition(
													existingTag, new PropertyDefinition(pd));
										}
									});
									
									for (UploadFile uploadFile : uploadFiles) {
										if (uploadFile.getIntrinsicMarkupCollection() != null) {
											AnnotationCollection intrinsicMarkupCollection =
													uploadFile.getIntrinsicMarkupCollection();
											
											List<TagReference> tagReferences = 
												intrinsicMarkupCollection.getTagReferences(tag);
	
											intrinsicMarkupCollection.removeTagReferences(tagReferences);
											
											Multimap<TagInstance, TagReference> referencesByInstance = 
													ArrayListMultimap.create();
											tagReferences.forEach(tr -> referencesByInstance.put(tr.getTagInstance(), tr));
											
											for (TagInstance incomingTagInstance : referencesByInstance.keySet()) {
												TagInstance newTagInstance = 
														new TagInstance(
																idGenerator.generate(),
																existingTag.getUuid(),
																incomingTagInstance.getAuthor(),
																incomingTagInstance.getTimestamp(),
																existingTag.getUserDefinedPropertyDefinitions(),
																targetTagset.getUuid());
												
												for (Property oldProp : incomingTagInstance.getUserDefinedProperties()) {
													String oldPropDefId = oldProp.getPropertyDefinitionId();
													PropertyDefinition oldPropDef = 
															tag.getPropertyDefinitionByUuid(oldPropDefId);
	
													PropertyDefinition existingPropDef = 
														existingTag.getPropertyDefinition(oldPropDef.getName());
													
													
													newTagInstance.addUserDefinedProperty(
														new Property(
															existingPropDef.getUuid(), 
															oldProp.getPropertyValueList()));
												}
												
												ArrayList<TagReference> newReferences = new ArrayList<>();
												referencesByInstance.get(incomingTagInstance).forEach(tr -> {
													try {
														newReferences.add(
															new TagReference(
																newTagInstance, 
																tr.getTarget().toString(), 
																tr.getRange(), 
																tr.getUserMarkupCollectionUuid()));
													} catch (URISyntaxException e) {
														e.printStackTrace();
													}
												});
												
												intrinsicMarkupCollection.addTagReferences(newReferences);
											}
											
										}
									}								
								}
								else {
									tag.setTagsetDefinitionUuid(targetTagset.getUuid());
									
									project.getTagManager().addTagDefinition(targetTagset, tag);
								}
							}
							ui.push();
						});
					});
					
					
					// Importing docs and collections
					for (UploadFile uploadFile : uploadFiles) {
						getProgressListener().setProgress(
								"Importing Document %1$s", uploadFile.getTitle());
						ui.accessSynchronously(() -> {
							addUploadFile(uploadFile, useApostropheAsSeparator, collectionNamePattern);
							ui.push();
						});
					}

					return null;
				}
			},
			new ExecutionListener<Void>() {
				@Override
				public void done(Void result) {
	            	setProgressBarVisible(false);
	            	setEnabled(true);
				}
				@Override
				public void error(Throwable t) {
	            	setProgressBarVisible(false);
	            	setEnabled(true);
	                errorHandler.showAndLogError("Error adding Documents", t);
				}
			},
			progressListener);

	}
	
	
	private void addUploadFile(UploadFile uploadFile, boolean useApostropheAsSeparator, String collectionNamePattern) {
		
		SourceDocumentInfo sourceDocumentInfo = 
				new SourceDocumentInfo(
						uploadFile.getIndexInfoSet(useApostropheAsSeparator), 
						uploadFile.getContentInfoSet(), 
						uploadFile.getTechInfoSet());
		
		SourceContentHandler contentHandler =
				sourceDocumentInfo.getTechInfoSet().getMimeType().equals(FileType.XML2.getMimeType())?
						new XML2ContentHandler()
						:new TikaContentHandler();
						

		contentHandler.setSourceDocumentInfo(sourceDocumentInfo);		
		
		SourceDocument document = new SourceDocument(uploadFile.getUuid(), contentHandler);
		
		try {
			String content = document.getContent();
			
			FileOSType fileOSType = FileOSType.getFileOSType(content);
			
			sourceDocumentInfo.getTechInfoSet().setFileOSType(fileOSType);
			CRC32 checksum = new CRC32();
			checksum.update(content.getBytes());
			
			sourceDocumentInfo.getTechInfoSet().setChecksum(checksum.getValue());

			project.insert(document);
			
			AnnotationCollection intrinsicMarkupCollection = 
					uploadFile.getIntrinsicMarkupCollection();
			if (intrinsicMarkupCollection != null) {
				project.importCollection(Collections.emptyList(), intrinsicMarkupCollection);
			}
			
			if (collectionNamePattern != null && !collectionNamePattern.isEmpty()) {
				String collectionName = collectionNamePattern.replace("{{Title}}", uploadFile.getTitle());
				project.createUserMarkupCollection(collectionName, project.getSourceDocumentReference(document.getUuid()));
			}
			
		} catch (Exception e) {
			Logger.getLogger(ProjectView.class.getName()).log(
					Level.SEVERE, 
					String.format("Error loading content of %1$s", uploadFile.getTempFilename().toString()), 
					e);
			String errorMsg = e.getMessage();
			if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
				errorMsg = "";
			}

			Notification.show(
				"Error", 
				String.format(
						"Error loading content of %1$s! "
						+ "This document will be skipped!\n The underlying error message was:\n%2$s", 
						uploadFile.getTitle(), errorMsg), 
				Type.ERROR_MESSAGE);
		}
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
    		
    		if (root != null) {
	    		SourceDocumentReference document = ((DocumentResource)root).getDocument();
	    		AnnotationCollectionReference collectionReference = 
	    			(child==null?null:((CollectionResource)child).getCollectionReference());
	    		
	    		eventBus.post(new RouteToAnnotateEvent(project, document, collectionReference));
    		}
    	}
    }
	
	private void checkForUnsynchronizedCommits() {
        
        try {
			btSynchBell.setVisible(project.hasChangesToCommitOrPush());
		} catch (Exception e) {
			String msg = "Checking for unsynchronized changes failed!";
			logger.log(Level.SEVERE, msg, e);

			Notification.show("Info", msg, Type.WARNING_MESSAGE);
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
        
        btSynchBell = new IconButton(VaadinIcons.BELL);
        btSynchBell.addStyleName("project-view-synch-bell");
        getHugeCardBar().addComponentBeforeMoreOptions(btSynchBell);
        btSynchBell.setVisible(false);
        
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
	    				try {
		    				project.commitChanges(commitMsg);
		    				Notification.show(
		    					"Info", 
		    					"Your changes have been committed!", 
		    					Type.HUMANIZED_MESSAGE);
	    				}
	    				catch (Exception e) {
	    					errorHandler.showAndLogError("Error committing all changes!", e);
	    				}
	    			});
	    		dlg.show();
	    	}
	    	else {
	    		Notification.show("Info", "There are no uncommitted changes!", Type.HUMANIZED_MESSAGE); 
	    	}
    	}
    	catch (Exception e) {
            errorHandler.showAndLogError("Error accessing Project!", e);
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
		  	.setHidden(false);

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
			.setCaption("Name");
	
		tagsetGrid
		  	.addColumn(tagset -> 
		  		tagset.getResponsibleUser() == null?
		  				"Not assigned"
		  				:this.membersByIdentfier.get(tagset.getResponsibleUser()))
		  	.setCaption("Responsible")
		  	.setId(TagsetGridColumn.RESPONSIBLE.name())
		  	.setExpandRatio(1)
		  	.setHidden(true)
		  	.setHidable(true);

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
        	new AddMemberDialog(
        		project::assignOnProject,
        		(query) -> project.findUser(query.getFilter().isPresent() ? query.getFilter().get() : "", query.getOffset(), query.getLimit()), //$NON-NLS-1$
        		(evt) -> eventBus.post(new MembersChangedEvent())
        		).show());
        
        ContextMenu moreOptionsContextMenu = membersGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

        moreOptionsContextMenu.addItem("Edit Members", (click) -> handleEditMembers());
        moreOptionsContextMenu.addItem("Remove Members", (click) -> handleRemoveMembers());
        
        miInvite = moreOptionsContextMenu.addItem(
        	"Invite someone to the Project", click -> handleProjectInvitationRequest());
        
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
    public void reloadProject(ProjectReference projectReference) {
    	if (this.project == null) {
    		setProjectReference(projectReference);
    	}
    	else {
	    	setEnabled(false);
	    	setProgressBarVisible(true);
	    	
	    	final UI ui = UI.getCurrent();
	        this.project.open(new OpenProjectListener() {
	
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
	                
	                ProjectView.this.project.addPropertyChangeListener(
	                		RepositoryChangeEvent.tagReferencesChanged, 
	                		tagReferencesChangedListener);
	                
	                setEnabled(true);
	                reloadAll();
	                checkForUnsynchronizedCommits();
	            }
	            
	            @Override
	            public void failure(Throwable t) {
	            	setProgressBarVisible(false);
	            	setEnabled(true);
	                errorHandler.showAndLogError("error opening project", t);
	            }
	        });
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
                
                ProjectView.this.project.addPropertyChangeListener(
                		RepositoryChangeEvent.tagReferencesChanged,
                		tagReferencesChangedListener);
                setEnabled(true);
                reloadAll();
                
                checkForUnsynchronizedCommits();
            }

            @Override
            public void failure(Throwable t) {
            	setProgressBarVisible(false);
            	setEnabled(true);
                errorHandler.showAndLogError("Error opening Project.", t);
            }
        });
    }

    private void initData() {
        try {
        	Set<Member> projectMembers = project.getProjectMembers();
        	this.membersByIdentfier = 
        		projectMembers.stream()
        			.collect(Collectors.toMap(
        					Member::getIdentifier, 
        					Function.identity()));

        	TreeDataProvider<Resource> resourceDataProvider = buildResourceDataProvider(); 
        	documentGrid.setDataProvider(resourceDataProvider);
        	documentGrid.sort(DocumentGridColumn.NAME.name());
        	documentGrid.expand(resourceDataProvider.getTreeData().getRootItems());
        	
        	tagsetData = new ListDataProvider<>(project.getTagsets());
        	tagsetGrid.setDataProvider(tagsetData);
        	
        	ListDataProvider<Member> memberData = new ListDataProvider<>(projectMembers);
        	teamGrid.setDataProvider(memberData);
        	tagsetGrid.sort(TagsetGridColumn.NAME.name());
		} catch (Exception e) {
			errorHandler.showAndLogError("Error initializing data.", e);
		}
	}

    private TreeDataProvider<Resource> buildResourceDataProvider() throws Exception {
        if(project != null){
            TreeData<Resource> treeData = new TreeData<>();
            Collection<SourceDocumentReference> srcDocs = project.getSourceDocumentReferences();
            Locale locale = Locale.getDefault();
            for(SourceDocumentReference srcDoc : srcDocs) {
            	locale = 
            		srcDoc.getSourceDocumentInfo().getIndexInfoSet().getLocale();
            	
                DocumentResource docResource = 
                		new DocumentResource(
                			srcDoc, 
                			project.getProjectId());
                
                treeData.addItem(null, docResource);
                
                List<AnnotationCollectionReference> collections = 
                		srcDoc.getUserMarkupCollectionRefs();
                
            	List<Resource> collectionResources = collections
        		.stream()
        		.filter(collectionRef -> 
        			!miToggleResponsibiltityFilter.isChecked() 
        			|| collectionRef.isResponsible(project.getUser().getIdentifier()))
        		.map(collectionRef -> 
        			new CollectionResource(
        				collectionRef, 
        				project.getProjectId(),
        				collectionRef.getResponsibleUser()!= null?membersByIdentfier.get(collectionRef.getResponsibleUser()):null)
        		)
        		.collect(Collectors.toList());
        		
                
                if(!collections.isEmpty()){
                	
                    treeData.addItems(
                    	docResource,
                    	collectionResources
                    );
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
        				RBACPermission.PROJECT_MEMBERS_EDIT, projectReference);
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
    @Override
    public void attach() {
    	super.attach();
    	if (project != null) {
    		checkForUnsynchronizedCommits();
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
     * called when {@link ProjectChangedEvent} is fired when the Project's name changes and if members join
     * @param resourcesChangedEvent
     */
    @Subscribe
    public void handleResourceChanged(ProjectChangedEvent resourcesChangedEvent){
    	reloadAll();
    }
    
    @Subscribe
    public void handleDocumentChanged(DocumentChangeEvent documentChangeEvent) {
    	initData();
    	btSynchBell.setVisible(true);
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
    		
    		boolean beyondUsersResponsibility = 
    			resourceGrid.getSelectedItems().stream()
    			.filter(res -> !res.isResponsible(project.getUser().getIdentifier()))
    			.findAny()
    			.isPresent();
    		
    		BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
    				beyondUsersResponsibility, 
    				new Action() {
    					@Override
    					public void execute() {
					    	ConfirmDialog.show( 
					    		UI.getCurrent(), 
					    		"Info", 
					    		"Are you sure you want to delete the selected resources: "
					    		+ resourceGrid.getSelectedItems()
					    			.stream()
					    			.map(resource -> resource.getName())
					    			.collect(Collectors.joining(",")) //$NON-NLS-1$
					    		+ "?", 
					    		"Delete", 
					    		"Cancel", dlg -> {
					    			if (dlg.isConfirmed()) {
							           Stream<Resource> sortedResources =  resourceGrid.getSelectedItems()
							            .stream()
							            .sorted(new Comparator<Resource>() {
				
											@Override
											public int compare(Resource o1, Resource o2) {
												if (o1.isCollection() && o2.isCollection()) {
													return o1.getResourceId().compareTo(o2.getResourceId());
												}
												else if (o1.isCollection()) {
													return -1;
												}
												else if (o2.isCollection()){
													return 1;
												}
												else {
													return o1.getResourceId().compareTo(o2.getResourceId());
												}
											}
							            	
										});
							            
							            	
							            for (Resource resource: sortedResources.collect(Collectors.toList())) {
							            	try {
							            		resource.deleteFrom(project);
							                } catch (Exception e) {
							                    errorHandler.showAndLogError("Error deleting resource "+resource, e);
							                }
							            }
					    			}
					    		});
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
				
				if (tagReferencesChangedListener != null) {
					project.removePropertyChangeListener(
							RepositoryChangeEvent.tagReferencesChanged,
							tagReferencesChangedListener);
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
					return r.toString().toLowerCase().contains(searchInput.toLowerCase());
				}
				else {
					if (r.toString().toLowerCase().contains(searchInput.toLowerCase())) {
						return true;
					}
					else {
						return documentData.getChildren((Resource)r)
								.stream()
								.filter(child -> 
									child.toString().toLowerCase().contains(searchInput.toLowerCase()))
								.findAny()
								.isPresent();
					}
				}
			}
		};
	}

 }
