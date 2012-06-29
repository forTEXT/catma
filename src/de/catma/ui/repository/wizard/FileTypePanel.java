package de.catma.ui.repository.wizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Tree;

import de.catma.CatmaApplication;
import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.source.CharsetLanguageInfo;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentHandler;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.ui.DefaultProgressListener;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.WizardStepListener;
import de.catma.util.ContentInfoSet;

class FileTypePanel extends GridLayout implements DynamicWizardStep {
	
	private static class BackgroundLoaderResult {
		byte[] byteContent;
		String encoding;
		String mimeType;
		public BackgroundLoaderResult(byte[] byteContent, String encoding, String mimeType) {
			super();
			this.byteContent = byteContent;
			this.encoding = encoding;
			this.mimeType = mimeType;
		}
		
	}

	private ComboBox cbFileType;
	private Tree fileEncodingTree;
	private boolean onAdvance = false;
	private SourceDocumentInfo sourceDocumentInfo;
	private ProgressIndicator progressIndicator;
	private byte[] currentByteContent;
	private Label taPreview;
	private UploadPanel uploadPanel;
	private Label uploadLabel;
	private Panel previewPanel;
	private WizardStepListener wizardStepListener;
	private AddSourceDocWizardResult wizardResult;
	private Repository repository;
	
	public FileTypePanel(
			WizardStepListener wizardStepListener, AddSourceDocWizardResult wizardResult, 
			Repository repository) {
		super(2,4);
		this.wizardResult = wizardResult;
		this.sourceDocumentInfo = wizardResult.getSourceDocumentInfo();
		this.wizardStepListener = wizardStepListener;
		this.repository = repository;
		initComponents();
		initActions();
	}
	
	public void stepActivated() {
		onAdvance = false;
		try {
			if (wizardResult.getSourceDocumentID() == null) {
				wizardResult.setSourceDocumentID(
					repository.getIdFromURI(
						sourceDocumentInfo.getTechInfoSet().getURI()));
			}
			final String mimeTypeFromUpload = 
					sourceDocumentInfo.getTechInfoSet().getMimeType();
			final String sourceDocURL = 
					sourceDocumentInfo.getTechInfoSet().getURI().toURL().toString();
			final String sourceURIPath = 
					sourceDocumentInfo.getTechInfoSet().getURI().getPath();
			sourceDocumentInfo.setContentInfoSet(new ContentInfoSet());
			setVisiblePreviewComponents(false);
			setVisibleXSLTInputComponents(false);
			
			BackgroundService backgroundService = 
					((CatmaApplication)getApplication()).getBackgroundService();
			progressIndicator.setEnabled(true);
			progressIndicator.setCaption("Detecting file type");
			backgroundService.submit(
					new DefaultProgressCallable<BackgroundLoaderResult>() {
						public BackgroundLoaderResult call() throws Exception {
							
							SourceDocumentHandler sourceDocumentHandler = 
									new SourceDocumentHandler();
							URL url = new URL(sourceDocURL);
							InputStream is = null;
							String resultMimeType = mimeTypeFromUpload;
							
							if (url.getProtocol().equals("http")) {
								  HttpClient httpclient = new DefaultHttpClient();
								  HttpGet httpGet = new HttpGet(url.toURI());
								  
								  HttpResponseHandler responseHandler = new HttpResponseHandler();
								  
						          String responseBody = httpclient.execute(httpGet, responseHandler);
//						          System.out.println(responseBody);
						          is = new ByteArrayInputStream(responseBody.getBytes());
						          System.out.println("ENC: " + 
						        		  responseHandler.getEncoding());
						          
							}
							else {
								URLConnection urlConnection = 
										url.openConnection();
							
								urlConnection.connect();
					            String type = urlConnection.getContentType();
				                System.out.println(type);
								is = urlConnection.getInputStream();
							}
							try {
								byte[] byteContent = IOUtils.toByteArray(is);
			
//								if (mimeTypeFromUpload == null) {
//									resultMimeType = 
//											sourceDocumentHandler.getMimeType(
//													sourceURIPath, urlConnection, 
//													FileType.TEXT.getMimeType());
//								}
//								
								String encoding = Charset.defaultCharset().name();
								
//								if (resultMimeType.equals(FileType.TEXT.getMimeType())
//										||(resultMimeType.equals(FileType.HTML.getMimeType()))) {
//									encoding = 
//											sourceDocumentHandler.getEncoding(
//													urlConnection, 
//													byteContent, 
//													Charset.defaultCharset().name());	
//									System.out.println("ENCODING: " + encoding);
//								}
//	
								return new BackgroundLoaderResult(
										byteContent, encoding, resultMimeType);
							}
							finally {
								is.close();
							}
						
						}
					}, 
					new ExecutionListener<BackgroundLoaderResult>() {
						public void done(BackgroundLoaderResult result) {
							currentByteContent = result.byteContent;

							sourceDocumentInfo.getTechInfoSet().setMimeType(result.mimeType);
							
							FileType fileType = FileType.getFileType(result.mimeType);
							
							sourceDocumentInfo.getTechInfoSet().setFileType(fileType);
							
							if (fileType.equals(FileType.TEXT)) {
								System.out.println(result.encoding);
								
								Charset charset = Charset.forName(result.encoding);
								fileEncodingTree.select(charset);
								sourceDocumentInfo.getTechInfoSet().setCharset(charset);
							}
							else {
								try {
									sourceDocumentInfo.getTechInfoSet().setFileOSType(
											FileOSType.INDEPENDENT);
									SourceDocumentHandler sourceDocumentHandler = 
											new SourceDocumentHandler();
									SourceDocument sourceDocument =
											sourceDocumentHandler.loadSourceDocument(
											wizardResult.getSourceDocumentID(), 
											sourceDocumentInfo);
									
									sourceDocument.getSourceContentHandler().load(
											new ByteArrayInputStream(currentByteContent));
									wizardResult.setSourceDocument(sourceDocument);
								} catch (Exception e) {
									((CatmaApplication)getApplication()).showAndLogError(
											"Error detecting the file type!", e);
								}
							}
							if ((cbFileType.getValue() != null ) 
									&& cbFileType.getValue().equals(fileType)) {
								handleFileType();
							}
							else {
								cbFileType.setValue(fileType);
							}
							
							progressIndicator.setCaption("File type detection finished!");
							progressIndicator.setEnabled(false);
						}
						
						public void error(Throwable t) {
							((CatmaApplication)getApplication()).showAndLogError(
								"Error detecting file type", t);
						}
						
					}, new DefaultProgressListener(progressIndicator, getApplication()));
		}
		catch (Exception exc) {
			((CatmaApplication)getApplication()).showAndLogError(
				"Error detecting file type", exc);
		}
	}

	private void showPreview() {
		SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler();
		try {
			SourceDocument sourceDocument =
					sourceDocumentHandler.loadSourceDocument(
						wizardResult.getSourceDocumentID(),
						sourceDocumentInfo);
			load(sourceDocument);
			taPreview.setValue(
					"<pre>" + sourceDocument.getContent(new Range(0, 2000)) + "</pre>");
			wizardResult.setSourceDocument(sourceDocument);
		} catch (Exception e) {
			((CatmaApplication)getApplication()).showAndLogError(
				"Error loading the preview for the document!", e);
		}
	}
	
	public void load(SourceDocument sourceDocument) throws IOException {

		sourceDocument.getSourceContentHandler().load(
				new ByteArrayInputStream(currentByteContent));
		
		FileOSType fileOSType = 
				FileOSType.getFileOSType(sourceDocument.getContent());
		
		sourceDocumentInfo.getTechInfoSet().setFileOSType(fileOSType);
		CRC32 checksum = new CRC32();
		checksum.update(currentByteContent);
		sourceDocumentInfo.getTechInfoSet().setChecksum(checksum.getValue());
	}

	private void initComponents() {
		setSpacing(true);
		setSizeFull();
		setMargin(true, false, false, false);

		
		cbFileType = new ComboBox("File type");
		for (FileType ft : FileType.values()) {
			cbFileType.addItem(ft);
		}
		cbFileType.setNullSelectionAllowed(false);
		cbFileType.setImmediate(true);
		
		addComponent(cbFileType, 0, 0);
		
		fileEncodingTree = new Tree("File encoding");
		fileEncodingTree.setImmediate(true);
		
		Map<String, Map<String, List<Charset>>> regionLanguageCharsetMapping = 
				CharsetLanguageInfo.SINGLETON.getRegionLanguageCharsetMapping();
		
		for (String region : regionLanguageCharsetMapping.keySet() ) {
			fileEncodingTree.addItem(region);
			Map<String, List<Charset>> languages = regionLanguageCharsetMapping.get(region);
			for (String language : languages.keySet()) {
				fileEncodingTree.addItem(language);
				fileEncodingTree.setParent(language, region);
				for (Charset charset : languages.get(language)) {
					fileEncodingTree.addItem(charset);
					fileEncodingTree.setParent(charset, language);
					fileEncodingTree.setChildrenAllowed(charset, false);
				}
			}
		}
		
		Map<String, List<Charset>> categoryCharsetMapping = 
				CharsetLanguageInfo.SINGLETON.getCategoryCharsetMapping();
		
		for (String category : categoryCharsetMapping.keySet()) {
			fileEncodingTree.addItem(category);
			for (Charset charset : categoryCharsetMapping.get(category)) {
				fileEncodingTree.addItem(charset);
				fileEncodingTree.setParent(charset, category);
				fileEncodingTree.setChildrenAllowed(charset, false);
			}
		}
		
		previewPanel = new Panel("Preview");
		previewPanel.getContent().setSizeUndefined();
		previewPanel.setHeight("300px");
		
		
		this.taPreview = new Label();
		this.taPreview.setContentMode(Label.CONTENT_XHTML);
		previewPanel.addComponent(taPreview);
		
		addComponent(fileEncodingTree, 0, 1);
		addComponent(previewPanel, 1, 1);
		
		this.uploadLabel = new Label("Upload the corresponding XSLT file:");
		this.uploadPanel = new UploadPanel();
		addComponent(uploadLabel, 0, 2, 1, 2);
		addComponent(uploadPanel, 0, 3, 1, 3);
		
		progressIndicator = new ProgressIndicator();
		progressIndicator.setEnabled(false);
		progressIndicator.setIndeterminate(true);
		progressIndicator.setWidth("100%");
		progressIndicator.setPollingInterval(500);
		
		addComponent(progressIndicator, 1, 0);
		setColumnExpandRatio(1, 1);
		
	}
	
	private void initActions() {
		cbFileType.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				handleFileType();
			}

		});
		
		fileEncodingTree.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				if (fileEncodingTree.getValue() instanceof Charset) {
					sourceDocumentInfo.getTechInfoSet().setCharset(
							(Charset)fileEncodingTree.getValue());
					showPreview();
				}
			}
		});
		
		// TODO add listeners to uploadpanel and handle xslt loading
	}
	
	private void handleFileType() {
		sourceDocumentInfo.getTechInfoSet().setFileType(
				(FileType)cbFileType.getValue());
		onAdvance = true;
		switch(sourceDocumentInfo.getTechInfoSet().getFileType()) {
			case TEXT : {
				setVisibleXSLTInputComponents(false);
				setVisiblePreviewComponents(true);
				fileEncodingTree.setVisible(true);
				showPreview();
				break;
			}
			case XML : {
				setVisiblePreviewComponents(false);
				setVisibleXSLTInputComponents(true);
				onAdvance = false;
				break;
			}
			default : {
				setVisibleXSLTInputComponents(false);
				setVisiblePreviewComponents(true);
				fileEncodingTree.setVisible(false);
				showPreview();
			}
		}
		wizardStepListener.stepChanged(FileTypePanel.this);
	}
	
	private void setVisibleXSLTInputComponents(boolean visible) {
		uploadLabel.setVisible(visible);
		uploadPanel.setVisible(visible);
	}
	
	private void setVisiblePreviewComponents(boolean visible) {
		previewPanel.setVisible(visible);
		if (visible) {
			taPreview.setValue("");
		}
	}
	
	public Component getContent() {
		return this;
	}
	
	public boolean onAdvance() {
		return onAdvance;
	}
	
	public boolean onBack() {
		return true;
	}
	
	@Override
	public String getCaption() {
		return "Source Document file type";
	}
	
	public boolean onFinish() {
		return false;
	}
	
	public boolean onFinishOnly() {
		return false;
	}
	
	public void stepDeactivated(){ /*not needed*/}


}
