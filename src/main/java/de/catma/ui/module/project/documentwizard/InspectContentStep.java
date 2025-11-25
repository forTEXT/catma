package de.catma.ui.module.project.documentwizard;

import com.vaadin.data.Binder.Binding;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.*;
import com.vaadin.ui.Notification.Type;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.source.*;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SingleOptionInputDialog;
import de.catma.ui.dialog.wizard.*;
import de.catma.ui.module.main.ErrorHandler;
import org.apache.tika.Tika;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InspectContentStep extends VerticalLayout implements WizardStep {
	private static final Logger logger = Logger.getLogger(InspectContentStep.class.getName());

	private final WizardContext wizardContext;

	private final ProgressStep progressStep;
	private final WizardStep nextStep;

	private final ArrayList<UploadFile> uploadFiles;
	private final ListDataProvider<UploadFile> uploadFileListDataProvider;

	private final Tika tika;

	private StepChangeListener stepChangeListener;

	private ProgressBar progressBar;
	private HorizontalLayout contentLayout;
	private Grid<UploadFile> fileGrid;
	private Binding<UploadFile, Charset> uploadFileCharsetBinding;
	private ArrayList<LanguageItem> languageItems;
	private ActionGridComponent<Grid<UploadFile>> fileActionGridComponent;
	private CheckBox cbUseApostrophe;
	private CheckBox cbSimpleXml;
	private TextArea taPreview;

	public InspectContentStep(WizardContext wizardContext, ProgressStepFactory progressStepFactory, Tika tika) {
		this.wizardContext = wizardContext;
		this.wizardContext.put(DocumentWizard.WizardContextKey.APOSTROPHE_AS_SEPARATOR, false);
		this.wizardContext.put(DocumentWizard.WizardContextKey.SIMPLE_XML, false);

		this.progressStep = progressStepFactory.create(2, "Inspect the Content");
		this.nextStep = new AddMetadataStep(wizardContext, progressStepFactory);

		this.tika = tika;

		this.uploadFiles = new ArrayList<>();
		this.uploadFileListDataProvider = new ListDataProvider<>(uploadFiles);

		initComponents();
		initActions();
	}

	private void initComponents() {
		setSizeFull();

		progressBar = new ProgressBar();
		progressBar.setCaption("Inspecting files...");
		progressBar.setVisible(false);
		progressBar.setIndeterminate(false);
		addComponent(progressBar);

		Label infoLabel = new Label(
				"Please check if the language has been detected correctly and if the content preview looks fine. "
						+ "For plain text files you might need to adjust the encoding.\n"
						+ "Double click on a row to change the settings."
		);
		addComponent(infoLabel);

		contentLayout = new HorizontalLayout();
		contentLayout.setSizeFull();
		contentLayout.setMargin(false);

		VerticalLayout leftColumnLayout = new VerticalLayout();
		leftColumnLayout.setMargin(false);
		leftColumnLayout.setSizeFull();

		fileGrid = new Grid<>(uploadFileListDataProvider);
		fileGrid.setSizeFull();
		fileGrid.addColumn(UploadFile::getOriginalFilename)
				.setCaption("File")
				.setWidth(150)
				.setDescriptionGenerator(UploadFile::getOriginalFilename);
		fileGrid.addColumn(UploadFile::getMimetype)
				.setCaption("Type")
				.setWidth(150)
				.setDescriptionGenerator(UploadFile::getMimetype);

		uploadFileCharsetBinding = fileGrid.getEditor().getBinder().bind(
				new ComboBox<>(null, Charset.availableCharsets().values()),
				UploadFile::getCharset,
				(uploadFile, charset) -> {
					uploadFile.setCharset(charset);
					updatePreview(uploadFile);
				}
		);
		fileGrid.addColumn(UploadFile::getCharset)
				.setCaption("Character Set / Encoding")
				.setExpandRatio(2)
				.setEditorBinding(uploadFileCharsetBinding);

		languageItems = new ArrayList<>();
		for (Locale locale : Locale.getAvailableLocales()) {
			languageItems.add(new LanguageItem(locale));
		}
		fileGrid.addColumn(UploadFile::getLanguage)
				.setCaption("Language")
				.setExpandRatio(2)
				.setEditorComponent(
						new ComboBox<>(null, languageItems),
						(uploadFile, language) -> {
							uploadFile.setLanguage(language);
							updatePreview(uploadFile);
						}
				);

		fileGrid.getEditor().setEnabled(true).setBuffered(false);

		fileActionGridComponent = new ActionGridComponent<>(new Label("Language and Encoding"), fileGrid);
		fileActionGridComponent.setMargin(false);
		fileActionGridComponent.getActionGridBar().setAddBtnVisible(false);

		leftColumnLayout.addComponent(fileActionGridComponent);
		leftColumnLayout.setExpandRatio(fileActionGridComponent, 1.0f);

		cbUseApostrophe = new CheckBox("Always use the apostrophe as a word separator");
		cbUseApostrophe.setDescription("This influences the segmentation of the text, i.e. how the wordlist is created.");
		leftColumnLayout.addComponent(cbUseApostrophe);

		cbSimpleXml = new CheckBox("Simple XML mode");
		cbSimpleXml.setDescription("Preserves whitespace and does not insert any newlines (only affects XML files).");
		leftColumnLayout.addComponent(cbSimpleXml);

		contentLayout.addComponent(leftColumnLayout);
		contentLayout.setExpandRatio(leftColumnLayout, 0.6f);

		taPreview = new TextArea("Preview");
		taPreview.setReadOnly(true);
		taPreview.setSizeFull();

		contentLayout.addComponent(taPreview);
		contentLayout.setExpandRatio(taPreview, 0.4f);

		addComponent(contentLayout);
		setExpandRatio(contentLayout, 1f);
	}

	private String loadXmlFileContent(UploadFile uploadFile) throws IOException {
		SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo();
		sourceDocumentInfo.setTechInfoSet(new TechInfoSet(
				uploadFile.getOriginalFilename(),
				uploadFile.getMimetype(),
				uploadFile.getTempFilename()
		));

		XML2ContentHandler xmlContentHandler = new XML2ContentHandler(cbSimpleXml.getValue());
		xmlContentHandler.setSourceDocumentInfo(sourceDocumentInfo);

		xmlContentHandler.load();
		return xmlContentHandler.getContent();
	}

	private void updatePreview(UploadFile uploadFile) {
		try {
			final int maxPreviewLength = 3000;
			String previewContent;

			if (uploadFile.getMimetype().equals(FileType.XML2.getMimeType())) {
				// handle XML
				previewContent = loadXmlFileContent(uploadFile);
			}
			else {
				// handle non-XML (parse with Tika)
				MediaType mediaType = MediaType.parse(uploadFile.getMimetype());

				Metadata metadata = new Metadata();
				if (mediaType.getBaseType().toString().equals(FileType.TEXT.getMimeType())) {
					metadata.set(Metadata.CONTENT_TYPE, new MediaType(mediaType, uploadFile.getCharset()).toString());
				}

				try (FileInputStream fileInputStream = new FileInputStream(new File(uploadFile.getTempFilename()))) {
					previewContent = tika.parseToString(fileInputStream, metadata, maxPreviewLength);
				}
			}

			// truncate the preview content if necessary (for XML files the entire content is loaded)
			if (previewContent.length() > maxPreviewLength) {
				previewContent = previewContent.substring(0, maxPreviewLength);
			}

			// append an ellipsis to the preview content if it is likely to have been truncated
			if (previewContent.length() == maxPreviewLength) {
				Pattern pattern = Pattern.compile(".*\\s\\z", Pattern.DOTALL);
				Matcher matcher = pattern.matcher(previewContent);
				previewContent += matcher.matches() ? "[...]" : " [...]";
			}

			taPreview.setValue(previewContent);

			IndexInfoSet indexInfoSet = new IndexInfoSet(Collections.emptyList(), Collections.emptyList(), uploadFile.getLocale());
			if (indexInfoSet.isRightToLeftWriting()) {
				taPreview.addStyleName("document-wizard-rtl-preview");
			}
			else {
				taPreview.removeStyleName("document-wizard-rtl-preview");
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, String.format("Error loading preview of %s", uploadFile.getOriginalFilename()), e);

			Notification.show(
					"Error",
					String.format(
							"Failed to load content of %s! Adding this file to your project might fail.\nThe underlying error message was:\n%s",
							uploadFile.getOriginalFilename(),
							e.getMessage()
					),
					Type.ERROR_MESSAGE
			);
		}
	}

	private void initActions() {
		fileGrid.addItemClickListener(event -> updatePreview(event.getItem()));
		fileGrid.getEditor().addOpenListener(event -> {
			MediaType mediaType = MediaType.parse(event.getBean().getMimetype());
			uploadFileCharsetBinding.setReadOnly(!mediaType.getBaseType().toString().equals(FileType.TEXT.getMimeType()));
		});

		fileActionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu().addItem(
				"Set Language",
				menuItem -> {
					if (fileGrid.getSelectedItems().isEmpty()) {
						Notification.show("Info", "Please select one or more entries first!", Type.HUMANIZED_MESSAGE);
						return;
					}

					SingleOptionInputDialog<LanguageItem> languageSelectionDialog = new SingleOptionInputDialog<>(
							"Language Selection",
							"Please select a language:",
							languageItems,
							result -> {
								fileGrid.getSelectedItems().forEach(uploadFile -> uploadFile.setLanguage(result));
								uploadFileListDataProvider.refreshAll();
								fileGrid.getSelectedItems().stream().findFirst().ifPresent(this::updatePreview);
							}
					);
					languageSelectionDialog.show();
				}
		);

		cbUseApostrophe.addValueChangeListener(event -> wizardContext.put(DocumentWizard.WizardContextKey.APOSTROPHE_AS_SEPARATOR, event.getValue()));

		cbSimpleXml.addValueChangeListener(event -> {
			wizardContext.put(DocumentWizard.WizardContextKey.SIMPLE_XML, event.getValue());
			fileGrid.getSelectedItems().stream().findFirst().ifPresent(this::updatePreview);
		});
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
		final List<UploadFile> files = (List<UploadFile>) wizardContext.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST);

		BackgroundServiceProvider backgroundServiceProvider = (BackgroundServiceProvider) UI.getCurrent();

		backgroundServiceProvider.submit(
				"inspecting-files",
				new DefaultProgressCallable<List<UploadFile>>() {
					@Override
					public List<UploadFile> call() throws Exception {
						LanguageDetector languageDetector = LanguageDetector.getDefaultLanguageDetector();
						try {
							languageDetector.loadModels();
						}
						catch (IOException e) {
							((ErrorHandler) UI.getCurrent()).showAndLogError("Failed to load language detection models", e);
						}

						for (UploadFile uploadFile : files) {
							if (uploadFile.getMimetype().equals(FileType.XML2.getMimeType())) {
								// handle XML
								String content = loadXmlFileContent(uploadFile);

								LanguageResult languageResult = languageDetector.detect(content);
								if (languageResult.isReasonablyCertain() && languageResult.getLanguage() != null) {
									uploadFile.setLanguage(new LanguageItem(new Locale(languageResult.getLanguage())));
								}
							}
							else {
								// handle non-XML (parse with Tika)
								// TODO: figure out and document why this works differently than in updatePreview & TikaContentHandler
								try (FileInputStream fileInputStream = new FileInputStream(new File(uploadFile.getTempFilename()))) {
									Metadata metadata = new Metadata();
									String content = tika.parseToString(fileInputStream, metadata);

									String contentType = metadata.get(Metadata.CONTENT_TYPE);
									MediaType mediaType = MediaType.parse(contentType);
									String charset = mediaType.getParameters().get("charset");
									if (charset != null) {
										uploadFile.setCharset(Charset.forName(charset));
									}

									LanguageResult languageResult = languageDetector.detect(content);
									if (languageResult.isReasonablyCertain() && languageResult.getLanguage() != null) {
										uploadFile.setLanguage(new LanguageItem(new Locale(languageResult.getLanguage())));
									}
								}
								catch (Exception e) {
									logger.log(Level.SEVERE, String.format("Error inspecting %s", uploadFile.getOriginalFilename()), e);

									Notification.show(
											"Error",
											String.format(
													"Failed to load content of %s! Adding this file to your project might fail.\n" +
															"The underlying error message was:\n%s",
													uploadFile.getOriginalFilename(),
													e.getMessage()
											),
											Type.ERROR_MESSAGE
									);
								}
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

						// load preview for first file
						if (!result.isEmpty()) {
							UploadFile firstFile = result.get(0);
							fileGrid.select(firstFile);
							updatePreview(firstFile);
						}

						if (stepChangeListener != null) {
							stepChangeListener.stepChanged(InspectContentStep.this);
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
