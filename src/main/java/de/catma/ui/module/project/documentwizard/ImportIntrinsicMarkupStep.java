package de.catma.ui.module.project.documentwizard;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.project.Project;
import de.catma.serialization.intrinsic.xml.XmlMarkupCollectionSerializationHandler;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.IDGenerator;

public class ImportIntrinsicMarkupStep extends VerticalLayout implements WizardStep {
	
	enum TagsetImportState {
		WILL_BE_CREATED("will be created"),
		WILL_BE_MERGED("will be merged"),
		WILL_BE_IGNORED("will be ignored"),
		;
		
		private String label;

		private TagsetImportState(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
		
	}
	
	static class TagsetImport {
		
		private String namespace;
		private TagsetDefinition extractedTagset;
		private TagsetDefinition targetTagset;
		private TagsetImportState importState;

		public TagsetImport(
				String namespace, 
				TagsetDefinition extractedTagset, 
				TagsetDefinition targetTagset, TagsetImportState importState) {
			super();
			this.namespace = namespace;
			this.extractedTagset = extractedTagset;
			this.targetTagset = targetTagset;
			this.importState = importState;
		}
		
		public String getNamespace() {
			return namespace;
		}
		public TagsetDefinition getExtractedTagset() {
			return extractedTagset;
		}
		
		public String getTargetName() {
			return targetTagset==null?"":targetTagset.getName();
		}
		
		public TagsetImportState getImportState() {
			return importState;
		}
		
		public void setImportState(TagsetImportState importState) {
			this.importState = importState;
		}
		
		public void setTargetTagset(TagsetDefinition targetTagset) {
			this.targetTagset = targetTagset;
		}
	}

	private ProgressStep progressStep;
	private ListDataProvider<UploadFile> fileDataProvider;
	private ListDataProvider<TagsetImport> tagsetDataProvider;
	private ProgressBar progressBar;
	private HorizontalLayout contentPanel;
	private Grid<UploadFile> fileGrid;
	private ActionGridComponent<Grid<UploadFile>> fileActionGridComponent;
	private Grid<TagsetImport> tagsetGrid;
	private ActionGridComponent<Grid<TagsetImport>> tagsetGridComponent;
	private WizardContext wizardContext;
	private Project project;
	private ArrayList<UploadFile> fileList;
	private ArrayList<TagsetImport> tagsetImportList;
	private StepChangeListener stepChangeListener;
	private WizardStep nextStep;

	public ImportIntrinsicMarkupStep(WizardContext wizardContext, ProgressStepFactory progressStepFactory) {
		this.progressStep = progressStepFactory.create(4, "Import Annotations");
		this.wizardContext = wizardContext; 

		this.project = (Project)wizardContext.get(DocumentWizard.WizardContextKey.PROJECT);
		
		this.fileList = new ArrayList<UploadFile>();
		this.tagsetImportList = new ArrayList<TagsetImport>();
		
		this.fileDataProvider = new ListDataProvider<UploadFile>(fileList);
		this.tagsetDataProvider = new ListDataProvider<TagsetImport>(tagsetImportList);
		this.nextStep = new AddAnnotationCollectionsStep(wizardContext, progressStepFactory, 5);
		initComponents();
		initActions();
	}

	private void initActions() {
		tagsetGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu().addItem(
				"Change action", miChangeAction -> handleChangeActionRequest());
	}

	private void handleChangeActionRequest() {
		
		Set<TagsetImport> tagsetImports = tagsetGrid.getSelectedItems();
		
		if (tagsetImports.isEmpty()) {
			Notification.show(
				"Info", "Please select one or more Tagsets first!", Type.HUMANIZED_MESSAGE);
		}
		else {
			try {
				ChangeImportActionDialog changeImportActionDialog = 
					new ChangeImportActionDialog(
						project, 
						tagsetImportList.stream()
							.map(TagsetImport::getExtractedTagset)
							.collect(Collectors.toList()), 
						new SaveCancelListener<TagsetDefinition>() {
							@Override
							public void savePressed(TagsetDefinition result) {
								
								tagsetImports.stream().forEach(tagsetImport -> {
									if (result == null) {
										tagsetImport.setImportState(TagsetImportState.WILL_BE_IGNORED);
									}
									else if (project.getTagManager().getTagLibrary().contains(result)) {
										tagsetImport.setImportState(TagsetImportState.WILL_BE_MERGED);
									}
									else {
										tagsetImport.setImportState(TagsetImportState.WILL_BE_CREATED);
									}
									
									tagsetImport.setTargetTagset(result);
								});
								
								tagsetDataProvider.refreshAll();
								
							}
						});
				
				changeImportActionDialog.show();
			}
			catch (Exception e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("Error loading Tagsets!", e);
			}
		}
	}

	private void initComponents() {
		setSizeFull();
		progressBar = new ProgressBar();
		progressBar.setCaption("Inspecting Annotations...");
		progressBar.setVisible(false);
		progressBar.setIndeterminate(false);
		addComponent(progressBar);
		
        Label infoLabel = new Label(
        		"We found the following Tagsets in your intrinsic markup, you can change the action per Tagset using the options menu:");
        infoLabel.setContentMode(ContentMode.HTML);
		addComponent(infoLabel);
		
		contentPanel = new HorizontalLayout();
		contentPanel.setSizeFull();
		contentPanel.setMargin(false);
		
		addComponent(contentPanel);
		setExpandRatio(contentPanel, 1f);
		
        tagsetGrid = new Grid<TagsetImport>(tagsetDataProvider);
        tagsetGrid.setSizeFull();
        tagsetGrid.addColumn(tagsetImport -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer());
		tagsetGrid
			.addColumn(tagsetImport -> tagsetImport.getNamespace())
			.setCaption("Namespace")
			.setDescriptionGenerator(
				tagsetImport -> tagsetImport.getExtractedTagset().stream()
				.limit(10)
				.map(TagDefinition::getName)
				.collect(Collectors.joining(", ")) 
				+ (tagsetImport.getExtractedTagset().size()>10?" ...":""))
			.setExpandRatio(2);

		tagsetGrid
		.addColumn(tagsetImport -> tagsetImport.getTargetName())
		.setCaption("Tagset")
		.setExpandRatio(2);
		
		tagsetGrid.addColumn(tagsetImport -> tagsetImport.getImportState().toString())
        .setCaption("Action")
        .setExpandRatio(1);
        
        tagsetGridComponent = new ActionGridComponent<Grid<TagsetImport>>(new Label("Intrinsic Tagsets"), tagsetGrid);
        tagsetGridComponent.setMargin(false);
        tagsetGridComponent.getActionGridBar().setAddBtnVisible(false);
        
        contentPanel.addComponent(tagsetGridComponent);
        contentPanel.setExpandRatio(tagsetGridComponent, 0.6f);
        
		
        fileGrid = new Grid<UploadFile>(fileDataProvider);
        fileGrid.setSizeFull();
        fileGrid.addColumn(tagset -> VaadinIcons.NOTEBOOK.getHtml(), new HtmlRenderer());

        fileGrid.addColumn(UploadFile::getTitle)
    	.setCaption("Title")
    	.setExpandRatio(2);

        fileActionGridComponent = new ActionGridComponent<Grid<UploadFile>>(new Label("Intrinsic Annotation Collections"), fileGrid); 
        fileActionGridComponent.setMargin(false);
        fileActionGridComponent.getActionGridBar().setAddBtnVisible(false);
        fileActionGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
        
        fileActionGridComponent.setSelectionModeFixed(SelectionMode.SINGLE);
        fileActionGridComponent.getActionGridBar().setMargin(new MarginInfo(false, true, false, true));
        
        contentPanel.addComponent(fileActionGridComponent);
     
        contentPanel.setExpandRatio(fileActionGridComponent, 0.4f);

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
		return true;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		this.stepChangeListener = stepChangeListener;
	}
	
	@Override
	public void enter(boolean back) {
		if (back) {
			return;
		}
		
		contentPanel.setEnabled(false);
		progressBar.setVisible(true);
		progressBar.setIndeterminate(true);

		@SuppressWarnings("unchecked")
		final ArrayList<UploadFile> files = new ArrayList<UploadFile>(
			((Collection<UploadFile>)wizardContext.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST)).stream()
				.filter(uploadFile -> uploadFile.getMimetype().equals(FileType.XML2.getMimeType()))
				.collect(Collectors.toList()));
		final TagManager tagmanager = new TagManager(new TagLibrary());
		
		BackgroundServiceProvider backgroundServiceProvider = (BackgroundServiceProvider)UI.getCurrent();
		
		backgroundServiceProvider.submit("inspecting-intrinsic-markup", new DefaultProgressCallable<List<UploadFile>>() {
			@Override
			public List<UploadFile> call() throws Exception {
				IDGenerator idGenerator = new IDGenerator();
				
				for (UploadFile uploadFile : files) {
					XML2ContentHandler contentHandler = new XML2ContentHandler();
					SourceDocument doc = new SourceDocument(uploadFile.getUuid(), contentHandler);
					SourceDocumentInfo documentInfo = new SourceDocumentInfo();
					TechInfoSet techInfoSet = new TechInfoSet();
					techInfoSet.setURI(uploadFile.getTempFilename());
					documentInfo.setTechInfoSet(techInfoSet);
					contentHandler.setSourceDocumentInfo(documentInfo);
					
					XmlMarkupCollectionSerializationHandler handler =
							new XmlMarkupCollectionSerializationHandler(tagmanager, contentHandler);
					try (FileInputStream fis = new FileInputStream(new File(uploadFile.getTempFilename()))) {
						AnnotationCollection collection = 
							handler.deserialize(doc, idGenerator.generateCollectionId(), fis);
						uploadFile.setIntrinsicMarkupCollection(collection);
					}
				}
				
				return files;
			}
		}, new ExecutionListener<List<UploadFile>>() {
			@Override
			public void done(List<UploadFile> result) {
				contentPanel.setEnabled(true);
				progressBar.setVisible(false);
				progressBar.setIndeterminate(false);
				
				fileList.clear();
				fileList.addAll(result);
				
				fileDataProvider.refreshAll();
				
				tagsetImportList.clear();
				String defaultIntrinsicXMLElmentsName = "Default Intrinsic XML Elements";
				for (TagsetDefinition tagset : tagmanager.getTagLibrary()) {
					if (!tagset.isEmpty()) {
						TagsetDefinition targetTagset = 
							project.getTagManager().getTagLibrary().getTagsetDefinition(tagset.getUuid());
						boolean inProject = false;
						if (targetTagset == null) {
							targetTagset = tagset;
						}
						else {
							inProject = true;
						}
						
						String namespace = tagset.getName()==null?"none":tagset.getName(); 
						if (tagset.getName() == null) {
							tagset.setName(defaultIntrinsicXMLElmentsName);
						}
						
						TagsetImport tagsetImport = 
							new TagsetImport(
								namespace, 
								tagset, 
								targetTagset,
								inProject?TagsetImportState.WILL_BE_MERGED:TagsetImportState.WILL_BE_CREATED);
						
						tagsetImportList.add(tagsetImport);
					}
				}
				
				tagsetDataProvider.refreshAll();
				if (stepChangeListener != null) {
					stepChangeListener.stepChanged(ImportIntrinsicMarkupStep.this);
				}
			}
			@Override
			public void error(Throwable t) {
				Logger.getLogger(ImportIntrinsicMarkupStep.class.getName()).log(
						Level.SEVERE, 
						"Error inspecting files", 
						t);
				String errorMsg = t.getMessage();
				if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
					errorMsg = "";
				}

				Notification.show(
					"Error", 
					String.format(
							"Error inspecting the contents! "
							+ "\n The underlying error message was:\n%1$s", 
							errorMsg), 
					Type.ERROR_MESSAGE);						
			}
		});
	}
}
