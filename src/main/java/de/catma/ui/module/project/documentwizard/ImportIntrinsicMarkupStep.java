package de.catma.ui.module.project.documentwizard;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.renderers.HtmlRenderer;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.project.Project;
import de.catma.serialization.intrinsic.xml.XmlMarkupCollectionSerializationHandler;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.wizard.*;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.IDGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ImportIntrinsicMarkupStep extends VerticalLayout implements WizardStep {
	private static final Logger logger = Logger.getLogger(ImportIntrinsicMarkupStep.class.getName());

	public final static String DEFAULT_TAGSET_NAME = "Default Intrinsic XML Elements";

	private final WizardContext wizardContext;

	private final ProgressStep progressStep;
	private final WizardStep nextStep;

	private final Project project;
	private final ArrayList<UploadFile> uploadFiles;
	private final ListDataProvider<UploadFile> uploadFileListDataProvider;
	private final ArrayList<TagsetImport> tagsetImports;
	private final ListDataProvider<TagsetImport> tagsetImportListDataProvider;

	private StepChangeListener stepChangeListener;

	private ProgressBar progressBar;
	private HorizontalLayout contentLayout;
	private Grid<TagsetImport> tagsetImportGrid;
	private ActionGridComponent<Grid<TagsetImport>> tagsetImportActionGridComponent;

	public ImportIntrinsicMarkupStep(WizardContext wizardContext, ProgressStepFactory progressStepFactory) {
		this.wizardContext = wizardContext;

		this.progressStep = progressStepFactory.create(4, "Import Annotations");
		this.nextStep = new AddAnnotationCollectionsStep(wizardContext, progressStepFactory, 5);

		this.project = (Project) wizardContext.get(DocumentWizard.WizardContextKey.PROJECT);

		this.uploadFiles = new ArrayList<>();
		this.uploadFileListDataProvider = new ListDataProvider<>(uploadFiles);

		this.tagsetImports = new ArrayList<>();
		this.tagsetImportListDataProvider = new ListDataProvider<>(tagsetImports);

		initComponents();
		initActions();
	}

	private void initComponents() {
		setSizeFull();

		progressBar = new ProgressBar();
		progressBar.setCaption("Inspecting annotations...");
		progressBar.setVisible(false);
		progressBar.setIndeterminate(false);
		addComponent(progressBar);

		Label infoLabel = new Label(
				"We found the following tagsets in your intrinsic markup, you can change the action per tagset using the options menu:"
		);
		addComponent(infoLabel);

		contentLayout = new HorizontalLayout();
		contentLayout.setSizeFull();
		contentLayout.setMargin(false);

		tagsetImportGrid = new Grid<>(tagsetImportListDataProvider);
		tagsetImportGrid.setSizeFull();
		tagsetImportGrid.addColumn(tagsetImport -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer());
		tagsetImportGrid.addColumn(TagsetImport::getNamespace)
				.setCaption("Namespace")
				.setDescriptionGenerator(
						tagsetImport -> tagsetImport.getExtractedTagset().stream()
								.limit(10)
								.map(TagDefinition::getName)
								.collect(Collectors.joining(", "))
								+ (tagsetImport.getExtractedTagset().size() > 10 ? ", ..." : "")
				)
				.setExpandRatio(2);
		tagsetImportGrid.addColumn(TagsetImport::getTargetName).setCaption("Tagset").setExpandRatio(2);
		tagsetImportGrid.addColumn(tagsetImport -> tagsetImport.getImportState().toString()).setCaption("Action").setExpandRatio(1);

		tagsetImportActionGridComponent = new ActionGridComponent<>(new Label("Intrinsic Tagsets"), tagsetImportGrid);
		tagsetImportActionGridComponent.setMargin(false);
		tagsetImportActionGridComponent.getActionGridBar().setAddBtnVisible(false);

		contentLayout.addComponent(tagsetImportActionGridComponent);
		contentLayout.setExpandRatio(tagsetImportActionGridComponent, 0.6f);

		Grid<UploadFile> fileGrid = new Grid<>(uploadFileListDataProvider);
		fileGrid.setSizeFull();
		fileGrid.addColumn(uploadFile -> VaadinIcons.NOTEBOOK.getHtml(), new HtmlRenderer());
		fileGrid.addColumn(UploadFile::getTitle).setCaption("Title").setExpandRatio(2);

		ActionGridComponent<Grid<UploadFile>> fileActionGridComponent = new ActionGridComponent<>(new Label("Intrinsic Annotation Collections"), fileGrid);
		fileActionGridComponent.setMargin(false);
		fileActionGridComponent.setSelectionModeFixed(SelectionMode.SINGLE);
		fileActionGridComponent.getActionGridBar().setAddBtnVisible(false);
		fileActionGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
		fileActionGridComponent.getActionGridBar().setMargin(new MarginInfo(false, true, false, true));

		contentLayout.addComponent(fileActionGridComponent);
		contentLayout.setExpandRatio(fileActionGridComponent, 0.4f);

		addComponent(contentLayout);
		setExpandRatio(contentLayout, 1f);
	}

	private void handleChangeActionRequest() {
		Set<TagsetImport> selectedTagsetImports = tagsetImportGrid.getSelectedItems();

		if (selectedTagsetImports.isEmpty()) {
			Notification.show("Info", "Please select one or more tagsets first!", Type.HUMANIZED_MESSAGE);
			return;
		}

		try {
			ChangeImportActionDialog changeImportActionDialog = new ChangeImportActionDialog(
					project,
					tagsetImports.stream().map(TagsetImport::getExtractedTagset).collect(Collectors.toList()),
					new SaveCancelListener<TagsetDefinition>() {
						@Override
						public void savePressed(TagsetDefinition result) {
							selectedTagsetImports.forEach(selectedTagsetImport -> {
								if (result == null) {
									selectedTagsetImport.setImportState(TagsetImportState.WILL_BE_IGNORED);
								}
								else if (project.getTagManager().getTagLibrary().contains(result)) {
									selectedTagsetImport.setImportState(TagsetImportState.WILL_BE_MERGED);
								}
								else {
									selectedTagsetImport.setImportState(TagsetImportState.WILL_BE_CREATED);
								}

								selectedTagsetImport.setTargetTagset(result);
							});

							tagsetImportListDataProvider.refreshAll();
						}
					}
			);
			changeImportActionDialog.show();
		}
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Failed to load tagsets", e);
		}
	}

	private void initActions() {
		tagsetImportActionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu().addItem(
				"Change Action",
				menuItem -> handleChangeActionRequest()
		);
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

		contentLayout.setEnabled(false);
		progressBar.setVisible(true);
		progressBar.setIndeterminate(true);

		@SuppressWarnings("unchecked")
		final List<UploadFile> files = ((List<UploadFile>) wizardContext.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST)).stream()
				.filter(uploadFile -> uploadFile.getMimetype().equals(FileType.XML2.getMimeType())).collect(Collectors.toList());
		final boolean simpleXml = (boolean) wizardContext.get(DocumentWizard.WizardContextKey.SIMPLE_XML);
		final boolean useApostropheAsSeparator = (boolean) wizardContext.get(DocumentWizard.WizardContextKey.APOSTROPHE_AS_SEPARATOR);

		final TagManager intrinsicTagManager = new TagManager(new TagLibrary());

		BackgroundServiceProvider backgroundServiceProvider = (BackgroundServiceProvider) UI.getCurrent();

		backgroundServiceProvider.submit(
				"inspecting-intrinsic-markup",
				new DefaultProgressCallable<List<UploadFile>>() {
					@Override
					public List<UploadFile> call() throws Exception {
						IDGenerator idGenerator = new IDGenerator();

						for (UploadFile uploadFile : files) {
							SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo();
							sourceDocumentInfo.setTechInfoSet(uploadFile.getTechInfoSet());
							sourceDocumentInfo.setContentInfoSet(uploadFile.getContentInfoSet());
							sourceDocumentInfo.setIndexInfoSet(uploadFile.getIndexInfoSet(useApostropheAsSeparator));

							XML2ContentHandler xmlContentHandler = new XML2ContentHandler(simpleXml);
							xmlContentHandler.setSourceDocumentInfo(sourceDocumentInfo);

							SourceDocument sourceDocument = new SourceDocument(uploadFile.getUuid(), xmlContentHandler);

							XmlMarkupCollectionSerializationHandler xmlMarkupCollectionSerializationHandler = new XmlMarkupCollectionSerializationHandler(
									intrinsicTagManager,
									xmlContentHandler,
									project.getCurrentUser().getIdentifier()
							);

							try (FileInputStream fileInputStream = new FileInputStream(new File(uploadFile.getTempFilename()))) {
								AnnotationCollection annotationCollection = xmlMarkupCollectionSerializationHandler.deserialize(
										sourceDocument,
										idGenerator.generateCollectionId(),
										fileInputStream
								);
								uploadFile.setIntrinsicMarkupCollection(annotationCollection);
							}
						}

						return files;
					}
				},
				new ExecutionListener<List<UploadFile>>() {
					@Override
					public void done(List<UploadFile> result) {
						contentLayout.setEnabled(true);
						progressBar.setVisible(false);
						progressBar.setIndeterminate(false);

						uploadFiles.clear();
						uploadFiles.addAll(result);

						uploadFileListDataProvider.refreshAll();

						tagsetImports.clear();

						for (TagsetDefinition intrinsicTagsetDefinition : intrinsicTagManager.getTagLibrary()) {
							if (intrinsicTagsetDefinition.isEmpty()) {
								continue;
							}

							TagsetDefinition targetTagsetDefinition = project.getTagManager().getTagLibrary().getTagsetDefinition(
									intrinsicTagsetDefinition.getUuid()
							);
							boolean tagsetExistsInProject = targetTagsetDefinition != null;
							if (!tagsetExistsInProject) {
								targetTagsetDefinition = intrinsicTagsetDefinition;
							}

							String namespace = intrinsicTagsetDefinition.getName() == null ? "none" : intrinsicTagsetDefinition.getName();
							if (intrinsicTagsetDefinition.getName() == null) {
								intrinsicTagsetDefinition.setName(DEFAULT_TAGSET_NAME);
							}

							TagsetImport tagsetImport = new TagsetImport(
									namespace,
									intrinsicTagsetDefinition,
									targetTagsetDefinition,
									tagsetExistsInProject ? TagsetImportState.WILL_BE_MERGED : TagsetImportState.WILL_BE_CREATED
							);

							tagsetImports.add(tagsetImport);
						}

						tagsetImportListDataProvider.refreshAll();
						wizardContext.put(DocumentWizard.WizardContextKey.TAGSET_IMPORT_LIST, tagsetImports);

						if (stepChangeListener != null) {
							stepChangeListener.stepChanged(ImportIntrinsicMarkupStep.this);
						}
					}

					@Override
					public void error(Throwable t) {
						logger.log(Level.SEVERE, "Error inspecting files", t);

						Notification.show(
								"Error",
								String.format("Failed to inspect file contents!\nThe underlying error message was:\n%s", t.getMessage()),
								Type.ERROR_MESSAGE
						);
					}
				}
		);
	}
}
