package de.catma.ui.repository.wizard;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Tree;

import de.catma.CleaApplication;
import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.core.document.source.CharsetLanguageInfo;
import de.catma.core.document.source.FileType;
import de.catma.core.document.source.SourceDocumentHandler;
import de.catma.core.document.source.SourceDocumentInfo;
import de.catma.ui.DefaultProgressListener;

public class FileTypePanel extends GridLayout implements DynamicWizardStep {
	
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

	private ComboBox fileType;
	private Tree fileEncoding;
	private boolean onAdvance = false;
	private SourceDocumentInfo sourceDocumentInfo;
	private ProgressIndicator progressIndicator;

	public FileTypePanel(WizardStepListener listener, SourceDocumentInfo sourceDocumentInfo) {
		super(2,2);
		this.sourceDocumentInfo = sourceDocumentInfo;
		
		initComponents();

	}
	
	public void stepActivated() {
		try {
			final String mimeTypeFromUpload = sourceDocumentInfo.getMimeType();
			final String sourceDocURL = sourceDocumentInfo.getURI().toURL().toString();
			final String sourceURIPath = sourceDocumentInfo.getURI().getPath();
			
			BackgroundService backgroundService = ((CleaApplication)getApplication()).getBackgroundService();
			progressIndicator.setEnabled(true);
			progressIndicator.setCaption("Detecting file type");
			backgroundService.submit(
					new DefaultProgressCallable<BackgroundLoaderResult>() {
						public BackgroundLoaderResult call() throws Exception {
							
							SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler();
							URLConnection urlConnection = new URL(sourceDocURL).openConnection();
							String resultMimeType = mimeTypeFromUpload;
							if (mimeTypeFromUpload == null) {
								resultMimeType = 
										sourceDocumentHandler.getMimeType(
												sourceURIPath, urlConnection, "text/plain");
							}
							InputStream is = urlConnection.getInputStream();
							byte[] byteContent = IOUtils.toByteArray(is);
							
							String encoding = Charset.defaultCharset().name();
							
							if (resultMimeType.equals(FileType.TEXT.getMimeType())
									||(resultMimeType.equals(FileType.HTML.getMimeType()))) {
								encoding = 
										sourceDocumentHandler.getEncoding(
												urlConnection, 
												byteContent, 
												Charset.defaultCharset().name());	
							}

							return new BackgroundLoaderResult(byteContent, encoding, resultMimeType);
						}
					}, 
					new ExecutionListener<BackgroundLoaderResult>() {
						public void done(BackgroundLoaderResult result) {
							sourceDocumentInfo.setMimeType(result.mimeType);
							fileType.setValue(FileType.getFileType(result.mimeType));
							System.out.println(result.encoding);
							fileEncoding.select(Charset.forName(result.encoding));
							progressIndicator.setCaption("File type detection finished!");
							progressIndicator.setEnabled(false);
						}
					}, new DefaultProgressListener(progressIndicator, getApplication()));
		}
		catch (Exception exc) {
			exc.printStackTrace(); //TODO handle
		}
	}


	private void initComponents() {
		setMargin(true);
		setSpacing(true);
		setWidth("100%");
		
		fileType = new ComboBox("File type");
		for (FileType ft : FileType.values()) {
			fileType.addItem(ft);
		}
		fileType.setNullSelectionAllowed(false);
		
		addComponent(fileType, 0, 0);
		
		fileEncoding = new Tree("File encoding");
		
		Map<String, Map<String, List<Charset>>> regionLanguageCharsetMapping = 
				CharsetLanguageInfo.SINGLETON.getRegionLanguageCharsetMapping();
		
		for (String region : regionLanguageCharsetMapping.keySet() ) {
			fileEncoding.addItem(region);
			Map<String, List<Charset>> languages = regionLanguageCharsetMapping.get(region);
			for (String language : languages.keySet()) {
				fileEncoding.addItem(language);
				fileEncoding.setParent(language, region);
				for (Charset charset : languages.get(language)) {
					fileEncoding.addItem(charset);
					fileEncoding.setParent(charset, language);
					fileEncoding.setChildrenAllowed(charset, false);
				}
			}
		}
		
		Map<String, List<Charset>> categoryCharsetMapping = 
				CharsetLanguageInfo.SINGLETON.getCategoryCharsetMapping();
		
		for (String category : categoryCharsetMapping.keySet()) {
			fileEncoding.addItem(category);
			for (Charset charset : categoryCharsetMapping.get(category)) {
				fileEncoding.addItem(charset);
				fileEncoding.setParent(charset, category);
				fileEncoding.setChildrenAllowed(charset, false);
			}
		}
		
		addComponent(fileEncoding, 0, 1);
		
		progressIndicator = new ProgressIndicator();
		progressIndicator.setEnabled(false);
		progressIndicator.setIndeterminate(true);
		progressIndicator.setWidth("100%");
		progressIndicator.setPollingInterval(500);
		
		addComponent(progressIndicator, 1, 0);
		setColumnExpandRatio(1, 1);
		
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
}
