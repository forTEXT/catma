package de.catma.ui.module.project.documentwizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tika.Tika;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.source.FileType;
import de.catma.document.source.LanguageItem;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;
import de.catma.ui.module.main.ErrorHandler;

public class MetadataStep extends HorizontalLayout implements WizardStep {
	

	private WizardContext wizardContext;
	private ProgressStep progressStep;
	private ArrayList<UploadFile> fileList;
	private ListDataProvider<UploadFile> fileDataProvider;
	private Grid<UploadFile> fileGrid;
	private ArrayList<LanguageItem> languageItems;
	private TextArea taPreview;
	private ProgressBar progressBar;

	@SuppressWarnings("unchecked")
	public MetadataStep(WizardContext wizardContext, ProgressStepFactory progressStepFactory) {
		
		this.wizardContext = wizardContext; 
		this.progressStep = progressStepFactory.create(2, "Add some metadata");
		
		this.fileList = (ArrayList<UploadFile>) wizardContext.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST);
		
		this.fileDataProvider = new ListDataProvider<UploadFile>(this.fileList);

		initComponents();
		initActions();
	}

	private void initActions() {
		fileGrid.addSelectionListener(new SelectionListener<UploadFile>() {
			@Override
			public void selectionChange(SelectionEvent<UploadFile> event) {
				event.getFirstSelectedItem().ifPresent(uploadFile -> {
					Tika tika = new Tika();
					Metadata metadata = new Metadata();
					MediaType type = MediaType.parse(uploadFile.getMimetype());
					
					if (type.getBaseType().toString().equals(FileType.TEXT.getMimeType())) {
						metadata.set(Metadata.CONTENT_TYPE, new MediaType(type, uploadFile.getCharset()).toString());
					}
					
					try {
						String content = "";
						
						if (uploadFile.getMimetype().equals(FileType.XML2.getMimeType())) {
							XML2ContentHandler contentHandler = new XML2ContentHandler();
							SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo();
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
					}
					catch (Exception e) {
						Logger.getLogger(MetadataStep.class.getName()).log(
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
				});
				
			}
		});
	}

	private void initComponents() {
		setSizeFull();
		progressBar = new ProgressBar();
		progressBar.setCaption("Inspecting files...");
		progressBar.setVisible(false);
		progressBar.setIndeterminate(false);
		addComponent(progressBar);
		
        fileGrid = new Grid<UploadFile>("Documents", fileDataProvider);
        fileGrid.setSizeFull();
        
		TextField titleEditor = new TextField();
		TextField authorEditor = new TextField();
		TextField descEditor = new TextField();
		TextField publisherEditor = new TextField();
		
		ComboBox<Charset> charsetEditor = new ComboBox<Charset>(null, Charset.availableCharsets().values());
		Locale[] availableLocales = Locale.getAvailableLocales();
		languageItems = new ArrayList<LanguageItem>();
		for (Locale locale : availableLocales) {
			languageItems.add(new LanguageItem(locale));
		}
		ComboBox<LanguageItem> languageEditor = new ComboBox<LanguageItem>(null, languageItems);
        
        fileGrid.addColumn(UploadFile::getOriginalFilename).setCaption("File");
        fileGrid.addColumn(UploadFile::getMimetype).setCaption("Type");
        fileGrid.addColumn(UploadFile::getCharset).setCaption("Characterset/Encoding").setEditorComponent(charsetEditor, UploadFile::setCharset);
        fileGrid.addColumn(UploadFile::getLanguage).setCaption("Language").setEditorComponent(languageEditor, UploadFile::setLanguage);
        fileGrid.addColumn(UploadFile::getTitle).setCaption("Title").setEditorComponent(titleEditor, UploadFile::setTitle);
        fileGrid.addColumn(UploadFile::getAuthor).setCaption("Author").setEditorComponent(authorEditor, UploadFile::setAuthor);
        fileGrid.addColumn(UploadFile::getPublisher).setCaption("Publisher").setEditorComponent(publisherEditor, UploadFile::setPublisher);
        fileGrid.addColumn(UploadFile::getDescription).setCaption("Description").setEditorComponent(descEditor, UploadFile::setDescription);
        fileGrid.getEditor().setEnabled(true);
        
        addComponent(fileGrid);
     
        setExpandRatio(fileGrid, 0.5f);
        
		this.taPreview = new TextArea("Preview");
		this.taPreview.setReadOnly(true);
		this.taPreview.setSizeFull();
		
		addComponent(this.taPreview);
		setExpandRatio(this.taPreview, 0.5f);

	}

	@Override
	public ProgressStep getProgressStep() {
		return progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		return null; // intended
	}
	
	@Override
	public void enter(boolean back) {
		@SuppressWarnings("unchecked")
		Collection<UploadFile> fileList = 
			(Collection<UploadFile>)wizardContext.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST);
		
		LanguageDetector languageDetector = LanguageDetector.getDefaultLanguageDetector();
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
							MediaType type = MediaType.parse(uploadFile.getMimetype());
							
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
								Logger.getLogger(MetadataStep.class.getName()).log(
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
					progressBar.setVisible(false);
					progressBar.setIndeterminate(false);
					
					fileList.clear();
					fileList.addAll(result);
					
					fileDataProvider.refreshAll();
				}
				@Override
				public void error(Throwable t) {
					Logger.getLogger(MetadataStep.class.getName()).log(
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
		// noop
	}

	@Override
	public boolean canFinish() {
		return true;
	}
}
