/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.module.project.documentwizard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.StreamVariable.StreamingProgressEvent;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Html5File;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.dnd.FileDropTarget;
import com.wcs.wcslib.vaadin.widget.multifileupload.component.MultiUpload;
import com.wcs.wcslib.vaadin.widget.multifileupload.component.MultiUploadDropHandler;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.MultiFileUpload;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.UploadFinishedHandler;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.UploadStatePanel;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.UploadStateWindow;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.UploadStateWindow.WindowPosition;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.source.FileType;
import de.catma.ui.CatmaApplication;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;
import de.catma.util.IDGenerator;

class UploadStep extends VerticalLayout implements WizardStep {
	
	private static class FilesReceivedListenerBridge extends MultiUploadDropHandler {
		public FilesReceivedListenerBridge() {
			super(null);
		}

		private FilesReceivedListener filesReceivedListener;
		
		@Override
		public void addFilesReceivedListener(FilesReceivedListener filesReceivedListener) {
			this.filesReceivedListener = filesReceivedListener;
		}
		
		
		public void receiveFiles(List<Html5File> files) {
			this.filesReceivedListener.filesReceived(files);
		}
	}

	private ProgressStep progressStep;
	private ListDataProvider<UploadFile> fileDataProvider;
	private ArrayList<UploadFile> fileList;
	private Tika tika;
	private Grid<UploadFile> fileGrid;
	private MultiFileUpload upload;
	private InspectContentStep nextStep;
	private StepChangeListener stepChangeListener;
	private PushMode originalPushMode;
	private TextField urlInputField;
	private Button btFetch;
	private boolean init = false;
	private IDGenerator idGenerator;
	private ProgressBar progressBar;
	
	public UploadStep(WizardContext wizardContext, ProgressStepFactory progressStepFactory) {
		
		this.progressStep = progressStepFactory.create(1, "Upload Some Files");
		
		this.fileList = new ArrayList<>();
		wizardContext.put(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST, fileList);
		this.fileDataProvider = new ListDataProvider<UploadFile>(this.fileList);
		
		this.tika = new Tika();
		this.nextStep = new InspectContentStep(wizardContext, progressStepFactory);
		this.idGenerator = new IDGenerator();

		initComponents();
		initActions();
	}

	private void initActions() {
        
		btFetch.addClickListener(clickEvent -> fetchFileURL());
		urlInputField.addValueChangeListener(event -> fetchFileURL());

	}
	
	private void fetchFileURL() {
		if (urlInputField.getValue() != null && !urlInputField.getValue().trim().isEmpty()) {
			String urlValue = urlInputField.getValue();
			try {
				if (urlValue.toLowerCase().startsWith("www")) { //$NON-NLS-1$
					urlValue = "http://" + urlValue; //$NON-NLS-1$
				}
				URL url = new URL(urlValue);
				urlInputField.setValue("");
				
				URLConnection conn = url.openConnection();
				String urlConnContentEncoding = conn.getContentEncoding();
				final String fileId = idGenerator.generateDocumentId();
				String tempDir = 
						((CatmaApplication)UI.getCurrent()).acquirePersonalTempFolder();
				final File tempFile = new File(new File(tempDir), fileId);
				if (tempFile.exists()) {
					tempFile.delete();
				}
				final String originalFilename = urlValue;
				
				progressBar.setIndeterminate(true);
				progressBar.setVisible(true);
				
				BackgroundServiceProvider backgroundServiceProvider = (BackgroundServiceProvider)UI.getCurrent();
				backgroundServiceProvider.submit("fetch-url", new DefaultProgressCallable<Long>() {
					@Override
					public Long call() throws Exception {
						ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
						
						try (FileOutputStream fos = new FileOutputStream(tempFile)) {
							FileChannel outChannel = fos.getChannel();
							outChannel
							  .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
							return outChannel.size();
						}
						
					}
					
				}, new ExecutionListener<Long>() {
					@Override
					public void done(Long result) {
						try {
							progressBar.setVisible(false);
							progressBar.setIndeterminate(false);
							UploadFile uploadFile = 
									new UploadFile(
										fileId, 
										tempFile.toURI(), 
										originalFilename, 
										urlConnContentEncoding, result);
							String type = urlConnContentEncoding;
							
							Metadata metadata = new Metadata();
							if (url.getFile() != null && !url.getFile().isEmpty()) {
								metadata.set(Metadata.RESOURCE_NAME_KEY, url.getFile());
							}
							
							MediaType mediaType = MediaType.parse(uploadFile.getMimetype());
							if (mediaType != null) {
								metadata.set(Metadata.CONTENT_TYPE, mediaType.toString());
							}
							try (FileInputStream fis = new FileInputStream(tempFile)) {
								tika.parseToString(fis, metadata);
							}
							mediaType = MediaType.parse(metadata.get("Content-Type"));
							uploadFile.setEncoding(mediaType.getParameters().get("charset"));
							uploadFile.setMimetype(metadata.get("Content-Type"));
							
							if (type != null && type.toLowerCase().trim().equals(FileType.ZIP.getMimeType())) {
								handleZipFile(uploadFile);
							}
							else {
								fileList.add(uploadFile);
								fileDataProvider.refreshAll();
								stepChangeListener.stepChanged(UploadStep.this);
							}
						}
						catch (Exception e) {
							e.printStackTrace();
							String errorMsg = e.getMessage();
							if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
								errorMsg = "";
							}
							Notification.show(
								"Error", 
								String.format(
									"Error loading %s, file will be skipped!\nThe underlying error message was:\n%s",
									originalFilename, errorMsg) , Type.WARNING_MESSAGE);
						}

					}
					@Override
					public void error(Throwable t) {
						t.printStackTrace();
						progressBar.setVisible(false);
						progressBar.setIndeterminate(false);
						String errorMsg = t.getMessage();
						if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
							errorMsg = "";
						}
						Notification.show(
							"Error", 
							String.format(
								"Error loading %s, file will be skipped!\n"
										+ "This can happen for a lot of reasons, e.g. archive providers blocking third party access to their archives "
										+ "or problems with an SSL connection. Try to download the file with your browser and then use the upload mechanism.\n\n"
										+ "The underlying error message was:\n"
										+ "%s",
								originalFilename, errorMsg) , Type.WARNING_MESSAGE);					
						}
				});
				
				
				
			} catch (Exception e) {
				e.printStackTrace();
				String errorMsg = e.getMessage();
				if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
					errorMsg = "";
				}
				Notification.show(
					"Error", 
					String.format(
						"Error loading %s, file will be skipped!\n"
						+ "This can happen for a lot of reasons, e.g. archive providers blocking third party access to their archives "
						+ "or problems with an SSL connection. Try to download the file with your browser and then use the upload mechanism.\n\n"
						+ "The underlying error message was:\n"
						+ "%s",
						urlValue, errorMsg) , Type.WARNING_MESSAGE);
			}
		}		
	}

	@Override
	public void attach() {
		
		super.attach();
		
		originalPushMode = UI.getCurrent().getPushConfiguration().getPushMode();
		UI.getCurrent().getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
		
		if (!init) {
			FilesReceivedListenerBridge filesReceivedListenerBridge = new FilesReceivedListenerBridge();
			((MultiUpload)upload.getSmartUpload().getUpload()).registerDropComponent(filesReceivedListenerBridge);
			
	        new FileDropTarget<>(fileGrid, event -> {
	        	filesReceivedListenerBridge.receiveFiles(new ArrayList<>(event.getFiles()));
	        });
	        init = true;
		}        
	}
	
	@Override
	public void detach() {
		if (originalPushMode == null) {
			originalPushMode = PushMode.MANUAL;
		}

		UI.getCurrent().getPushConfiguration().setPushMode(originalPushMode);
		Window window = upload.getUploadStatePanel().getWindow();
		UI.getCurrent().removeWindow(window);
		
		super.detach();
	}


	private void initComponents() {
		HorizontalLayout uploadPanel = new HorizontalLayout();
		uploadPanel.setWidth("100%");
		uploadPanel.setSpacing(true);
		uploadPanel.setMargin(false);
		addComponent(uploadPanel);
		
		UploadStateWindow uploadStateWindow = new UploadStateWindow();
		uploadStateWindow.setModal(true);
		uploadStateWindow.setWindowPosition(WindowPosition.CENTER);
		
		upload = new MultiFileUpload(new UploadFinishedHandler() {
			
			@Override
			public void handleFile(InputStream stream, String fileName, String mimeType, long length, int filesLeftInQueue) {
				try {
					String tempDir = 
							((CatmaApplication)UI.getCurrent()).acquirePersonalTempFolder();
					
					final String fileId = idGenerator.generateDocumentId();
					File tempFile = new File(new File(tempDir), fileId);
					
					if (tempFile.exists()) {
						tempFile.delete();
					}
					try (FileOutputStream fos = new FileOutputStream(tempFile)) {
						IOUtils.copy(stream, fos);
					}
					
					try (FileInputStream fis = new FileInputStream(tempFile)) {
						String type = tika.detect(fis, fileName);
						
						UploadFile uploadFile = 
							new UploadFile(fileId, tempFile.toURI(), fileName, type, length);
						
						if (type.toLowerCase().trim().equals(FileType.ZIP.getMimeType())) {
							handleZipFile(uploadFile);
						}
						else {
							fileList.add(uploadFile);
							fileDataProvider.refreshAll();
							stepChangeListener.stepChanged(UploadStep.this);
						}
					}
				}
				catch (Exception e) {
					
					String errorMsg = e.getMessage();
					if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
						errorMsg = "";
					}
					Notification.show(
						"Error", 
						String.format(
							"Error uploading %s, file will be skipped!\nThe underlying error message was:\n%s",
							fileName, errorMsg) , Type.ERROR_MESSAGE);
				}
			}
		}, uploadStateWindow) {
			@Override
			protected UploadStatePanel createStatePanel(UploadStateWindow uploadStateWindow) {
				return new MyUploadStatePanel(uploadStateWindow);
			}
		};
		
		uploadPanel.addComponent(upload);
        int maxFileSize = 104857600; // 100 MB
        upload.setMaxFileSize(maxFileSize);
        String errorMsgPattern = "File is too big (max = {0}): {2} ({1})";
        upload.setSizeErrorMsgPattern(errorMsgPattern);
        upload.setCaption("Upload files from your local computer");
        upload.setPanelCaption("Uploading file");
        upload.setMaxFileCount(300);
        upload.getSmartUpload().setUploadButtonCaptions("", "");
        upload.getSmartUpload().setUploadButtonIcon(VaadinIcons.UPLOAD);

        urlInputField = new TextField("or add a URL");
        urlInputField.setValueChangeMode(ValueChangeMode.BLUR);
        urlInputField.setWidth("100%");
        uploadPanel.addComponent(urlInputField);
        btFetch = new Button(VaadinIcons.CLOUD_DOWNLOAD);
        uploadPanel.addComponent(btFetch);
        uploadPanel.setComponentAlignment(btFetch, Alignment.BOTTOM_LEFT);
        
        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        uploadPanel.addComponent(progressBar);
        
        
        fileGrid = new Grid<UploadFile>("or drag and drop some files to the list", fileDataProvider);
        fileGrid.setSizeFull();
        
        fileGrid.addColumn(UploadFile::getOriginalFilename).setCaption("File");
        fileGrid.addColumn(UploadFile::getMimetype).setCaption("Type");
        
        
        addComponent(fileGrid);
     
        setExpandRatio(fileGrid, 1.0f);
	}

	private void handleZipFile(UploadFile uploadFile) throws IOException {
		URI uri = uploadFile.getTempFilename();
		ZipFile zipFile = new ZipFile(uri.getPath());
		Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
		
		String tempDir = ((CatmaApplication)UI.getCurrent()).acquirePersonalTempFolder();
		IDGenerator idGenerator = new IDGenerator();
		
		while (entries.hasMoreElements()) {
			ZipArchiveEntry entry = entries.nextElement();
			String fileName = FilenameUtils.getName(entry.getName());
			if (fileName.startsWith(".")) {
				continue; // we treat them as hidden files, that's probably what most users would expect
			}
			final String fileId = idGenerator.generateDocumentId();
			
			File entryDestination = new File(tempDir, fileId);
			if (entryDestination.exists()) {
				entryDestination.delete();
			}
			
			entryDestination.getParentFile().mkdirs();
			if(entry.isDirectory()){
				entryDestination.mkdirs();
			}
			else {
				try (BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry)); 
						BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(entryDestination))) {
					IOUtils.copy(bis, bos);
				}
				
				try (FileInputStream fis = new FileInputStream(entryDestination)) {
					String type = tika.detect(fis, fileName);
					UploadFile extractedUploadFile = 
							new UploadFile(
									fileId,
									entryDestination.toURI(), 
									fileName, type, entry.getSize());
					
					fileList.add(extractedUploadFile);
					fileDataProvider.refreshAll();
					stepChangeListener.stepChanged(this);
				}
			}
		}
		
		ZipFile.closeQuietly(zipFile);
	}

	@Override
	public ProgressStep getProgressStep() {
		return this.progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		return nextStep;
	}

	@Override
	public boolean isValid() {
		return !fileList.isEmpty();
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		this.stepChangeListener = stepChangeListener;
	}
	
	private class MyUploadStatePanel extends UploadStatePanel {

		public MyUploadStatePanel(UploadStateWindow window) {
			super(window);
		}
		
		@Override
		public void onProgress(StreamingProgressEvent event) {
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException ex) {
//                ex.printStackTrace();
//            }
			super.onProgress(event);
		}
		
	}
}
