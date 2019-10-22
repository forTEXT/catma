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
package de.catma.ui.module.project.document;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FilenameUtils;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.Range;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentHandler;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.DefaultProtocolHandler;
import de.catma.document.source.contenthandler.HttpProtocolHandler;
import de.catma.document.source.contenthandler.ProtocolHandler;
import de.catma.project.Project;
import de.catma.ui.CatmaApplication;
import de.catma.ui.dialog.HelpWindow;
import de.catma.ui.legacy.wizard.DynamicWizardStep;
import de.catma.ui.legacy.wizard.WizardStepListener;
import de.catma.util.IDGenerator;

class FileTypePanel extends HorizontalLayout implements DynamicWizardStep {
	
	private Grid<SourceDocumentResult> table;
	private boolean onAdvance = false;
	private TextArea taPreview;
	private WizardStepListener wizardStepListener;
	private AddSourceDocWizardResult wizardResult;
	private Project repository;
	private SourceDocument currentPreviewDocument = null;
	
	public FileTypePanel(
			WizardStepListener wizardStepListener, AddSourceDocWizardResult wizardResult, 
			Project repository) {
		this.wizardResult = wizardResult;
		this.wizardStepListener = wizardStepListener;
		this.repository = repository;
		initComponents();
		initActions();
	}
	
	public boolean canAdvance(){
		boolean canAdvance = true;
		for(SourceDocumentResult sdr : wizardResult.getSourceDocumentResults()){
			TechInfoSet techInfoSet = sdr.getSourceDocumentInfo().getTechInfoSet();
			boolean needsEncoding = techInfoSet.getFileType().isCharsetSupported();
			if(needsEncoding && techInfoSet.getCharset() == null){
				canAdvance = false;
				break;
			}
		}
		
		return canAdvance;
	}
	
	public void stepActivated(boolean forward) {
		if (!forward) {
			return;
		}
		
		wizardResult.clearAllSourceDocumentResults();
		
		try {
			
			final TechInfoSet inputTechInfoSet = wizardResult.getInputTechInfoSet();

			ArrayList<SourceDocumentResult> sourceDocumentResults = makeSourceDocumentResultsFromInputFile(inputTechInfoSet);

			ListDataProvider<SourceDocumentResult> dataProvider = new ListDataProvider<SourceDocumentResult>(sourceDocumentResults);
			table.setDataProvider(dataProvider);
			
			wizardResult.addSourceDocumentResults(sourceDocumentResults);
			
			if(sourceDocumentResults.size() > 0){
				table.select(sourceDocumentResults.get(0));
			}
			
			onAdvance = canAdvance();
		}
		catch (Exception exc) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				"Error detecting file type", exc); 
		}
	}
	
	private ProtocolHandler getProtocolHandlerForUri(
			URI inputFileURI, String fileID, String inputFileMimeType)
					throws MalformedURLException, IOException{
		if (inputFileURI.toURL().getProtocol().toLowerCase().equals("http") //$NON-NLS-1$
				|| inputFileURI.toURL().getProtocol().toLowerCase().equals("https")) { //$NON-NLS-1$
			
			final String destinationFileUri = repository.getFileURL(
					fileID, ((CatmaApplication)UI.getCurrent()).accquirePersonalTempFolder() + "/"); //$NON-NLS-1$
			
			return new HttpProtocolHandler(
					inputFileURI, destinationFileUri);
		}
		else {
			return new DefaultProtocolHandler(
					inputFileURI, inputFileMimeType);
		}
	}
	
	private ArrayList<SourceDocumentResult> makeSourceDocumentResultsFromInputFile(
			TechInfoSet inputTechInfoSet) throws MalformedURLException, IOException {
		
		ArrayList<SourceDocumentResult> output = new ArrayList<SourceDocumentResult>();
		
		FileType inputFileType = FileType.getFileType(inputTechInfoSet.getMimeType());
		
		if (inputFileType != FileType.ZIP) {
			SourceDocumentResult outputSourceDocumentResult = new SourceDocumentResult();
			
			String sourceDocumentID = repository.getIdFromURI(inputTechInfoSet.getURI());
			outputSourceDocumentResult.setSourceDocumentID(sourceDocumentID);
			
			SourceDocumentInfo outputSourceDocumentInfo = outputSourceDocumentResult.getSourceDocumentInfo();			
			outputSourceDocumentInfo.setTechInfoSet(new TechInfoSet(inputTechInfoSet));
			outputSourceDocumentInfo.setContentInfoSet(new ContentInfoSet());
			
			outputSourceDocumentInfo.getTechInfoSet().setFileType(inputFileType);
			
			output.add(outputSourceDocumentResult);
		}
		else { 
			URI uri = inputTechInfoSet.getURI();
			ZipFile zipFile = new ZipFile(uri.getPath());
			Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
			
			String tempDir = ((CatmaApplication)UI.getCurrent()).accquirePersonalTempFolder();
			IDGenerator idGenerator = new IDGenerator();
			
			while (entries.hasMoreElements()) {
				ZipArchiveEntry entry = entries.nextElement();
				String fileName = FilenameUtils.getName(entry.getName());
				if (fileName.startsWith(".")) {
					continue; // we treat them as hidden files, that's probably what most users would expect
				}
				String fileId = idGenerator.generateDocumentId();
				
				File entryDestination = new File(tempDir, fileId);
				if (entryDestination.exists()) {
					entryDestination.delete();
				}
				
				entryDestination.getParentFile().mkdirs();
				if(entry.isDirectory()){
					entryDestination.mkdirs();
				}
				else {
					BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(entryDestination));
					IOUtils.copy(bis, bos);
					IOUtils.closeQuietly(bis);
					IOUtils.closeQuietly(bos);
					
					SourceDocumentResult outputSourceDocumentResult = new SourceDocumentResult();
					URI newURI = entryDestination.toURI();
					
					String repositoryId = repository.getIdFromURI(newURI); // we need to do this as a catma:// is appended
					
					outputSourceDocumentResult.setSourceDocumentID(repositoryId);
					
					SourceDocumentInfo outputSourceDocumentInfo = outputSourceDocumentResult.getSourceDocumentInfo();
					FileType newFileType = FileType.getFileTypeFromName(fileName);
					TechInfoSet newTechInfoSet = new TechInfoSet(fileName, newFileType.getMimeType(), newURI);
					newTechInfoSet.setFileType(newFileType);
					
					outputSourceDocumentInfo.setTechInfoSet(newTechInfoSet);
					outputSourceDocumentInfo.setContentInfoSet(new ContentInfoSet());
					
					output.add(outputSourceDocumentResult);
				}
			}
			
			ZipFile.closeQuietly(zipFile);
		}

		
		for (SourceDocumentResult sdr : output) {
			TechInfoSet sdrTechInfoSet = sdr.getSourceDocumentInfo().getTechInfoSet();
			String sdrSourceDocumentId = sdr.getSourceDocumentID();
			
			ProtocolHandler protocolHandler = getProtocolHandlerForUri(sdrTechInfoSet.getURI(), sdrSourceDocumentId, sdrTechInfoSet.getMimeType());
			String mimeType = protocolHandler.getMimeType();
			
			sdrTechInfoSet.setMimeType(mimeType);			
			FileType sdrFileType = FileType.getFileType(mimeType);
			sdrTechInfoSet.setFileType(sdrFileType);
			
			if (sdrFileType.isCharsetSupported()) {
				Charset charset = Charset.forName(protocolHandler.getEncoding());
				sdrTechInfoSet.setCharset(charset);
			}
			else {
				sdrTechInfoSet.setFileOSType(FileOSType.INDEPENDENT);
			}
			
			loadSourceDocumentAndContent(sdr);
		}
		
		return output;
	}

	private void showSourceDocumentPreview(SourceDocumentResult sdr){
		if (currentPreviewDocument != null) {
			currentPreviewDocument.unload();
		}

		currentPreviewDocument = sdr.getSourceDocument();
		try{
			this.taPreview.setReadOnly(false);
			
			taPreview.setValue(
					currentPreviewDocument.getContent(new Range(0, 2000)));		
		} catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				"Error loading preview", e);
		}		
		this.taPreview.setReadOnly(true);
	}
	
	private boolean loadSourceDocumentAndContent(SourceDocumentResult sdr) {
		try {
			SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler();
			SourceDocument sourceDocument =
					sourceDocumentHandler.loadSourceDocument(
							sdr.getSourceDocumentID(),
							sdr.getSourceDocumentInfo());
			sdr.setSourceDocument(sourceDocument);
			try {
				TechInfoSet techInfoSet = sdr.getSourceDocumentInfo().getTechInfoSet();
				String documentId = sdr.getSourceDocumentID();
				
				ProtocolHandler protocolHandler = getProtocolHandlerForUri(techInfoSet.getURI(), documentId, techInfoSet.getMimeType());
				
				byte[] currentByteContent = protocolHandler.getByteContent();
	
				sourceDocument.getSourceContentHandler().load(
						new ByteArrayInputStream(currentByteContent));
	
				FileOSType fileOSType = FileOSType.getFileOSType(sourceDocument.getContent());
				
				sdr.getSourceDocumentInfo().getTechInfoSet().setFileOSType(fileOSType);
				CRC32 checksum = new CRC32();
				checksum.update(currentByteContent);
				sdr.getSourceDocumentInfo().getTechInfoSet().setChecksum(checksum.getValue());
				return true;
			}
			finally {
				sourceDocument.unload();
			}
		} catch (Exception e) {
			TechInfoSet techInfoSet = sdr.getSourceDocumentInfo().getTechInfoSet();
			if (techInfoSet.getFileType().equals(FileType.DOC)) {
				Logger.getLogger(FileTypePanel.class.getName()).log(
					Level.WARNING, 
					"error loading DOC file, retrying load as DOCX: " //$NON-NLS-1$
							+ techInfoSet.getFileName(), 
					e);
				techInfoSet.setFileType(FileType.DOCX);
				return loadSourceDocumentAndContent(sdr);
			}
			else {
				Notification.show(
					"Info",
					MessageFormat.format("Sorry, CATMA wasn''t able to process the file as {0} with charset {1}\\n\\nThe original error message is: {2}", 
							techInfoSet.getFileType(), 
							(techInfoSet.getFileType().isCharsetSupported()?
									((techInfoSet.getCharset()==null)?"unknown":techInfoSet.getCharset())
									:""), //$NON-NLS-1$
							e.getLocalizedMessage()),
					Notification.Type.WARNING_MESSAGE);
				return false;
			}
		}
	}
	
	private void initComponents() {
		setSpacing(true);
		setSizeFull();
		setMargin(new MarginInfo(true, false, false, true));

		ComboBox<FileType> fileTypeEditor = new ComboBox<FileType>(null, FileType.getActiveFileTypes());
		ComboBox<Charset> charsetEditor = new ComboBox<Charset>(null, Charset.availableCharsets().values());
		fileTypeEditor.addValueChangeListener(
			valueChangeEvent -> 
			charsetEditor.setEnabled(valueChangeEvent.getValue().isCharsetSupported()));
		
		table = new Grid<>("Documents");
		table.setSizeFull();
		
		table.addColumn(
				sourceDocumentResult -> 
					sourceDocumentResult.getSourceDocumentInfo().getTechInfoSet().getFileName())
			.setWidth(200)
			.setCaption("File Name");
		table.addColumn(
				sourceDocumentResult -> 
					sourceDocumentResult.getSourceDocumentInfo().getTechInfoSet().getFileType())
			.setEditorComponent(
					fileTypeEditor, 
					(sourceDocumentResult, fileType) -> {
						sourceDocumentResult.getSourceDocumentInfo().getTechInfoSet().setFileType(fileType);
						if (!fileType.isCharsetSupported()) {
							sourceDocumentResult.getSourceDocumentInfo().getTechInfoSet().setCharset(null);
						}
					})
			.setWidth(200)
			.setCaption("File Type");
		table.addColumn(
				sourceDocumentResult -> 
					sourceDocumentResult.getSourceDocumentInfo().getTechInfoSet().getCharset())
			.setEditorComponent(
				charsetEditor, 
				(sourceDocumentResult, charset) -> 
					sourceDocumentResult.getSourceDocumentInfo().getTechInfoSet().setCharset(charset))
			.setWidth(200)
			.setCaption("Encoding");
		
		table.getEditor().setEnabled(true);
		table.getEditor().addSaveListener(event -> {
			if (loadSourceDocumentAndContent(event.getBean())) {
				onAdvance = canAdvance();
				
				showSourceDocumentPreview(event.getBean());
			}
			else {
				taPreview.setValue(""); //$NON-NLS-1$
			}			
		});
		
		table.setSelectionMode(SelectionMode.SINGLE);
		
		addComponent(table);
		setExpandRatio(table, 0.5f);
		
		VerticalLayout previewContent = new VerticalLayout();
		previewContent.setMargin(true);

		this.taPreview = new TextArea("Preview");
		this.taPreview.setReadOnly(true);
		this.taPreview.setSizeFull();
		
		addComponent(this.taPreview);
		setExpandRatio(this.taPreview, 0.5f);
		
		HelpWindow helpWindow = new HelpWindow(
			"File Type", 
			"<p>This step allows you to adjust the file type and the encoding which have been detected automatically by CATMA.</p><p>If the characters in the preview don't look right to you, double click on the Document row and try to find the correct type and encoding.");
		
		Button btHelp = helpWindow.createHelpWindowButton();
		
		addComponent(btHelp);
		setComponentAlignment(btHelp, Alignment.TOP_RIGHT);
	}
	
	private void initActions() {
		table.addSelectionListener(new SelectionListener<SourceDocumentResult>() {
			
			@Override
			public void selectionChange(SelectionEvent<SourceDocumentResult> event) {
				event.getFirstSelectedItem().ifPresent(item -> handleFileType(item));
			}
		});
		
	}
	
	private void handleFileType(SourceDocumentResult sdr) {
		switch(sdr.getSourceDocumentInfo().getTechInfoSet().getFileType()) {
			case TEXT : {
				showSourceDocumentPreview(sdr);
				onAdvance = true;
				break;
			}
			default : {
				showSourceDocumentPreview(sdr);
				onAdvance = true;
			}
		}
		wizardStepListener.stepChanged(FileTypePanel.this);
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
		return "Document File Type";
	}
	
	public boolean onFinish() {
		return false;
	}
	
	public boolean onFinishOnly() {
		return false;
	}
	
	public void stepDeactivated(boolean forward){
		if (!forward) {
			wizardResult.clearAllSourceDocumentResults();
		}
	}

	public void stepAdded() {/* noop */}

}
