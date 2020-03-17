package de.catma.ui.module.project.documentwizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tika.Tika;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.vaadin.data.Binder.Binding;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.source.FileType;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.LanguageItem;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SingleOptionInputDialog;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;
import de.catma.ui.module.main.ErrorHandler;

public class CheckContentStep extends VerticalLayout implements WizardStep {
	

	private WizardContext wizardContext;
	private ProgressStep progressStep;
	private ListDataProvider<UploadFile> fileDataProvider;
	private Grid<UploadFile> fileGrid;
	private ArrayList<LanguageItem> languageItems;
	private TextArea taPreview;
	private ProgressBar progressBar;
	private ActionGridComponent<Grid<UploadFile>> fileActionGridComponent;
	private HorizontalLayout contentPanel;
	private AddMetadataStep nextStep;
	private StepChangeListener stepChangeListener;

	@SuppressWarnings("unchecked")
	public CheckContentStep(WizardContext wizardContext, ProgressStepFactory progressStepFactory) {
		
		this.wizardContext = wizardContext; 
		this.progressStep = progressStepFactory.create(2, "Check the content");
		
		ArrayList<UploadFile> fileList = (ArrayList<UploadFile>) wizardContext.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST);
		
		this.fileDataProvider = new ListDataProvider<UploadFile>(fileList);
		this.nextStep = new AddMetadataStep(wizardContext, progressStepFactory);

		initComponents();
		initActions();
	}

	private void initActions() {
		fileGrid.addItemClickListener(event -> updatePreview(event.getItem()));
		
		fileActionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu().addItem("Set language", menuItem -> {
			if (fileGrid.getSelectedItems().isEmpty()) {
				Notification.show("Info", "Please select one or more entries first!", Type.HUMANIZED_MESSAGE);
			}
			else {
				SingleOptionInputDialog<LanguageItem> languageSelectionDialog = 
					new SingleOptionInputDialog<>(
						"Language selection", "Please select a language:", languageItems, result -> {
					fileGrid.getSelectedItems().forEach(uploadFile -> uploadFile.setLanguage(result));
					fileDataProvider.refreshAll();
					fileGrid.getSelectedItems().stream().findFirst().ifPresent(uploadFile -> updatePreview(uploadFile));
				});
				
				languageSelectionDialog.show();
			}
		});
	}

	private void updatePreview(UploadFile uploadFile) {
		Tika tika = new Tika();
		Metadata metadata = new Metadata();
		MediaType type = MediaType.parse(uploadFile.getMimetype());
		
		if (type.getBaseType().toString().equals(FileType.TEXT.getMimeType())) {
			metadata.set(Metadata.CONTENT_TYPE, new MediaType(type, uploadFile.getCharset()).toString());
		}
		
		try {
			String content = "";
			SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo();
			IndexInfoSet indexInfoSet = 
				new IndexInfoSet(Collections.emptyList(), Collections.emptyList(), uploadFile.getLocale());
			
			if (uploadFile.getMimetype().equals(FileType.XML2.getMimeType())) {
				XML2ContentHandler contentHandler = new XML2ContentHandler();
				TechInfoSet techInfoSet = 
					new TechInfoSet(
						uploadFile.getOriginalFilename(), 
						uploadFile.getMimetype(), 
						uploadFile.getTempFilename());
				
				sourceDocumentInfo.setTechInfoSet(techInfoSet);
				contentHandler.setSourceDocumentInfo(sourceDocumentInfo);
				
				contentHandler.load();
				content = contentHandler.getContent();
			}
			else {
				try (FileInputStream fis = new FileInputStream(new File(uploadFile.getTempFilename()))) {
					content = tika.parseToString(fis, metadata, 3000);
				}
			}
			if (!content.isEmpty()) {
				content += " [...] ";
			}
			taPreview.setValue(content);
			if (indexInfoSet.isRightToLeftWriting()) {
				taPreview.addStyleName("document-wizard-rtl-preview");
			}
			else {
				taPreview.removeStyleName("document-wizard-rtl-preview");
			}
		}
		catch (Exception e) {
			Logger.getLogger(CheckContentStep.class.getName()).log(
					Level.SEVERE, 
					String.format("Error loading preview of %1$s", uploadFile.getOriginalFilename()), 
					e);
			String errorMsg = e.getMessage();
			if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
				errorMsg = "";
			}

			Notification.show(
				"Error", 
				String.format(
						"Error loading content of %1$s! "
						+ "Adding this file to your Project might fail!\n The underlying error message was:\n%2$s", 
						uploadFile.getOriginalFilename(), errorMsg), 
				Type.ERROR_MESSAGE);
		}
	}

	private void initComponents() {
		setSizeFull();
		progressBar = new ProgressBar();
		progressBar.setCaption("Inspecting files...");
		progressBar.setVisible(false);
		progressBar.setIndeterminate(false);
		addComponent(progressBar);
		
        Label infoLabel = new Label(
        		"Please check if the language has been detected correctly and if the "
        		+ "content preview looks fine. "
        		+ "For plain text files you might need to adjust the encoding.<br />"
        		+ "Double click on a row to change the settings.");
        infoLabel.setContentMode(ContentMode.HTML);
		addComponent(infoLabel);
		
		contentPanel = new HorizontalLayout();
		contentPanel.setSizeFull();
		contentPanel.setMargin(false);
		
		addComponent(contentPanel);
		setExpandRatio(contentPanel, 1f);
		
		
        fileGrid = new Grid<UploadFile>(fileDataProvider);
        fileGrid.setSizeFull();
		
		ComboBox<Charset> charsetEditor = new ComboBox<Charset>(null, Charset.availableCharsets().values());
		Locale[] availableLocales = Locale.getAvailableLocales();
		languageItems = new ArrayList<LanguageItem>();
		for (Locale locale : availableLocales) {
			languageItems.add(new LanguageItem(locale));
		}
		ComboBox<LanguageItem> languageEditor = new ComboBox<LanguageItem>(null, languageItems);
        
        fileGrid.addColumn(UploadFile::getOriginalFilename)
        	.setCaption("File")
        	.setWidth(150)
        	.setDescriptionGenerator(UploadFile::getOriginalFilename);
        fileGrid.addColumn(UploadFile::getMimetype)
        	.setCaption("Type")
        	.setWidth(150)
        	.setDescriptionGenerator(UploadFile::getMimetype);
        
        Binding<UploadFile, Charset> encBinding = fileGrid.getEditor().getBinder().bind(
        		charsetEditor, UploadFile::getCharset,(uploadFile, charset) -> {
        		uploadFile.setCharset(charset);
        		updatePreview(uploadFile);
        });
        fileGrid.getEditor().addOpenListener(event -> {
        	MediaType type = MediaType.parse(event.getBean().getMimetype());
        	encBinding.setReadOnly(!type.getBaseType().toString().equals(FileType.TEXT.getMimeType()));
        });
        
        fileGrid.addColumn(UploadFile::getCharset)
        	.setCaption("Characterset/Encoding")
        	.setExpandRatio(2)
        	.setEditorBinding(encBinding);

        fileGrid.addColumn(UploadFile::getLanguage)
        	.setCaption("Language")
        	.setExpandRatio(2)
        	.setEditorComponent(languageEditor, (uploadFile, language) -> {
        		uploadFile.setLanguage(language);
        		updatePreview(uploadFile);
        	});

        fileGrid.getEditor().setEnabled(true).setBuffered(false);
        
        
        fileActionGridComponent = new ActionGridComponent<Grid<UploadFile>>(new Label("Language and encoding"), fileGrid); 
        fileActionGridComponent.setMargin(false);
        fileActionGridComponent.getActionGridBar().setAddBtnVisible(false);
        
        contentPanel.addComponent(fileActionGridComponent);
     
        contentPanel.setExpandRatio(fileActionGridComponent, 0.6f);
        
		this.taPreview = new TextArea("Preview");
		this.taPreview.setReadOnly(true);
		this.taPreview.setSizeFull();
		
		contentPanel.addComponent(this.taPreview);
		contentPanel.setExpandRatio(this.taPreview, 0.4f);

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
	public void enter(boolean back) {
		if (back) {
			return;
		}
		
		@SuppressWarnings("unchecked")
		Collection<UploadFile> fileList = 
			(Collection<UploadFile>)wizardContext.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST);
		contentPanel.setEnabled(false);
		progressBar.setVisible(true);
		progressBar.setIndeterminate(true);
		final ArrayList<UploadFile> files = new ArrayList<UploadFile>(fileList);
		
		BackgroundServiceProvider backgroundServiceProvider = (BackgroundServiceProvider)UI.getCurrent();
		
			backgroundServiceProvider.submit("inspecting-files", new DefaultProgressCallable<List<UploadFile>>() {
				@Override
				public List<UploadFile> call() throws Exception {
				    Tika tika = new Tika();
					LanguageDetector languageDetector = LanguageDetector.getDefaultLanguageDetector();
					try {
						languageDetector.loadModels();
					} catch (IOException e) {
						((ErrorHandler)UI.getCurrent()).showAndLogError("Error loading language detection models!", e);
					}


					for (UploadFile uploadFile : files) {
						
						if (uploadFile.getMimetype().equals(FileType.XML2.getMimeType())) {
							XML2ContentHandler contentHandler = new XML2ContentHandler();
							SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo();
							TechInfoSet techInfoSet = new TechInfoSet(uploadFile.getOriginalFilename(), uploadFile.getMimetype(), uploadFile.getTempFilename());
							
							sourceDocumentInfo.setTechInfoSet(techInfoSet);
							contentHandler.setSourceDocumentInfo(sourceDocumentInfo);
							
							contentHandler.load();
							String content = contentHandler.getContent();
							LanguageResult languageResult = languageDetector.detect(content);
							if (languageResult.isReasonablyCertain() && languageResult.getLanguage() != null) {
								uploadFile.setLanguage(new LanguageItem(new Locale(languageResult.getLanguage())));
							}
						}
						else {
							Metadata metadata = new Metadata();
							try {
								
								try (FileInputStream fis = new FileInputStream(new File(uploadFile.getTempFilename()))) {
									String content = tika.parseToString(fis, metadata);
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
								
								
							} catch (Exception e) {
								Logger.getLogger(CheckContentStep.class.getName()).log(
										Level.SEVERE, 
										String.format("Error inspecting %1$s", uploadFile.getOriginalFilename()), 
										e);
								String errorMsg = e.getMessage();
								if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
									errorMsg = "";
								}

								Notification.show(
									"Error", 
									String.format(
											"Error inspecting content of %1$s! "
											+ "Adding this file to your Project might fail!\n The underlying error message was:\n%2$s", 
											uploadFile.getOriginalFilename(), errorMsg), 
									Type.ERROR_MESSAGE);								
							}
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
					if (!fileList.isEmpty()) {
						fileList.stream().findFirst().ifPresent(uploadFile -> {
							fileGrid.select(uploadFile);
							updatePreview(uploadFile);
						});
					}
					if (stepChangeListener != null) {
						stepChangeListener.stepChanged(CheckContentStep.this);
					}

				}
				@Override
				public void error(Throwable t) {
					Logger.getLogger(CheckContentStep.class.getName()).log(
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

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		this.stepChangeListener = stepChangeListener;
	}
}
