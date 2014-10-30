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
package de.catma.ui.repository.wizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.Range;
import de.catma.document.repository.Repository;
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
import de.catma.ui.CatmaApplication;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.WizardStepListener;

class FileTypePanel extends GridLayout implements DynamicWizardStep {

	private Table table;
	private boolean onAdvance = false;
	private Label taPreview;
//	private UploadPanel uploadPanel;
//	private Label uploadLabel;
	private Panel previewPanel;
	private WizardStepListener wizardStepListener;
	private AddSourceDocWizardResult wizardResult;
	private Repository repository;
	
	public FileTypePanel(
			WizardStepListener wizardStepListener, AddSourceDocWizardResult wizardResult, 
			Repository repository) {
		super(2,4);
		this.wizardResult = wizardResult;
		this.wizardStepListener = wizardStepListener;
		this.repository = repository;
		initComponents();
		initActions();
	}
	
	public void stepActivated(boolean forward) {
		if (!forward) {
			return;
		}
		
		if(wizardResult.GetSourceDocumentResults().size() > 0){
			return;
		}
		
		onAdvance = false; // TODO: this is currently being set to true in the HandleFileType event handler
		
		try {
			
			final TechInfoSet inputTechInfoSet = wizardResult.getInputTechInfoSet();
			final URI inputFileURI = inputTechInfoSet.getURI();						
			final String inputFileID = repository.getIdFromURI(inputFileURI);
			final String mimeTypeFromUpload = inputTechInfoSet.getMimeType();
			
			ProtocolHandler protocolHandler = getProtocolHandlerForUri(inputFileURI, inputFileID, mimeTypeFromUpload);
			
			String inputMimeType = protocolHandler.getMimeType();
			inputTechInfoSet.setMimeType(inputMimeType);
			
			ArrayList<SourceDocumentResult> sourceDocumentResults = makeSourceDocumentResultsFromInputFile(inputTechInfoSet);
			
			BeanItemContainer<SourceDocumentResult> container = (BeanItemContainer<SourceDocumentResult>)table.getContainerDataSource();
			for (SourceDocumentResult sdr : sourceDocumentResults) {
				container.addBean(sdr);
			}
			
			wizardResult.AddSourceDocumentResults(sourceDocumentResults);
			
			if(sourceDocumentResults.size() > 0){
				table.select(sourceDocumentResults.get(0));
			}
			
//			setVisiblePreviewComponents(false);
//			setVisibleXSLTInputComponents(false);
		}
		catch (Exception exc) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				"Error detecting file type", exc);
		}
	}
	
	private ProtocolHandler getProtocolHandlerForUri(URI inputFileURI, String fileID, String inputFileMimeType) throws MalformedURLException, IOException{
		if (inputFileURI.toURL().getProtocol().toLowerCase().equals("http")) {
			
			final String destinationFileUri = repository.getFileURL(
					fileID, ((CatmaApplication)UI.getCurrent()).getTempDirectory() + "/");
			
			return new HttpProtocolHandler(
					inputFileURI, destinationFileUri);
		}
		else {
			return new DefaultProtocolHandler(
					inputFileURI, inputFileMimeType);
		}
	}
	
	private ArrayList<SourceDocumentResult> makeSourceDocumentResultsFromInputFile(TechInfoSet inputTechInfoSet) throws MalformedURLException, IOException {
		
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
			// TODO: open zipfile, and find all the contents
		}
		
		
		for (SourceDocumentResult sdr : output) {
			TechInfoSet sdrTechInfoSet = sdr.getSourceDocumentInfo().getTechInfoSet();
			String sdrSourceDocumentId = sdr.getSourceDocumentID();
			SourceDocumentInfo sdrSourceDocumentInfo = sdr.getSourceDocumentInfo();
			FileType sdrFileType = FileType.getFileType(sdrTechInfoSet.getMimeType());
			
			ProtocolHandler protocolHandler = getProtocolHandlerForUri(sdrTechInfoSet.getURI(), sdrSourceDocumentId, sdrTechInfoSet.getMimeType());
			
			if (sdrFileType.equals(FileType.TEXT)
					||sdrFileType.equals(FileType.HTML)) {
				
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
		SourceDocument sourceDocument = sdr.getSourceDocument();
		try{
			taPreview.setValue(
					"<pre>" + sourceDocument.getContent(new Range(0, 2000)) + "</pre>");			
		} catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				"Error loading the preview for the document!", e);
		}		
	}
	
	private void loadSourceDocumentAndContent(SourceDocumentResult sdr) {
		try {
			SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler();
			SourceDocument sourceDocument =
					sourceDocumentHandler.loadSourceDocument(
							sdr.getSourceDocumentID(),
							sdr.getSourceDocumentInfo());
			sdr.setSourceDocument(sourceDocument);
			
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
		} catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					"Error loading the document content!", e);
		}
	}
	
	private void initComponents() {
		setSpacing(true);
		setSizeFull();
		setMargin(true);
		
		BeanItemContainer<SourceDocumentResult> container = new BeanItemContainer<SourceDocumentResult>(SourceDocumentResult.class);
		container.addNestedContainerProperty("sourceDocumentInfo.techInfoSet.fileName");
		container.addNestedContainerProperty("sourceDocumentInfo.techInfoSet.fileType");
		container.addNestedContainerProperty("sourceDocumentInfo.techInfoSet.charset");
		
		table = new Table("Documents", container);
		
		//TODO: investigate whether using a FieldFactory would make things easier..
		table.addGeneratedColumn("sourceDocumentInfo.techInfoSet.fileType", 
				new ComboBoxColumnGenerator(Arrays.asList(FileType.values()), makeComboBoxListenerGenerator())
		);
		
		table.addGeneratedColumn("sourceDocumentInfo.techInfoSet.charset", 
				new ComboBoxColumnGenerator(Charset.availableCharsets().values(), makeComboBoxListenerGenerator()));
		
		table.setVisibleColumns(new Object[]{
				"sourceDocumentInfo.techInfoSet.fileName",
				"sourceDocumentInfo.techInfoSet.fileType",
				"sourceDocumentInfo.techInfoSet.charset"
		});
		table.setColumnHeaders(new String[]{"File Name", "File Type", "Encoding"});
		
		table.setSelectable(true);
		table.setNullSelectionAllowed(false);
		table.setImmediate(true);
		
		addComponent(table, 0, 0);
		
		VerticalLayout previewContent = new VerticalLayout();
		previewContent.setMargin(true);
		previewPanel = new Panel("Preview", previewContent);
		previewPanel.getContent().setSizeUndefined();
		previewPanel.setHeight("300px");
		
		this.taPreview = new Label();
		this.taPreview.setContentMode(ContentMode.HTML);
		previewContent.addComponent(taPreview);
		
		addComponent(previewPanel, 1, 0);
		
//		this.uploadLabel = new Label("Upload the corresponding XSLT file:");
//		this.uploadPanel = new UploadPanel();
//		addComponent(uploadLabel, 0, 2, 1, 2);
//		addComponent(uploadPanel, 0, 3, 1, 3);

		setColumnExpandRatio(1, 1);
	}
	
	private ValueChangeListenerGenerator makeComboBoxListenerGenerator(){
		return new ValueChangeListenerGenerator() {
			public ValueChangeListener generateValueChangeListener(Table source, final Object itemId, Object columnId) {
				return new Property.ValueChangeListener() {
					public void valueChange(ValueChangeEvent event) {
						SourceDocumentResult sdr = (SourceDocumentResult) itemId;
						
						FileType fileType = sdr.getSourceDocumentInfo().getTechInfoSet().getFileType();
						
						Property encodingProperty = table.getContainerProperty(itemId, "sourceDocumentInfo.techInfoSet.charset");
						boolean readOnly = fileType != FileType.HTML && fileType != FileType.TEXT;
						encodingProperty.setReadOnly(readOnly);	
						
						if(readOnly){
							sdr.getSourceDocumentInfo().getTechInfoSet().setCharset(null);
						}					
						
						loadSourceDocumentAndContent(sdr);
						showSourceDocumentPreview(sdr);
					}
				};
			}
		};
	}
	
	private void initActions() {
		table.addValueChangeListener(new ValueChangeListener() {
			public void valueChange(ValueChangeEvent event) {
				if(table.getValue() == null){
					return;
				}
				
				SourceDocumentResult sdr = (SourceDocumentResult)table.getValue();
				
				handleFileType(sdr);
			}
		});
		
		// TODO add listeners to uploadpanel and handle xslt loading
	}
	
	private void handleFileType(SourceDocumentResult sdr) {
		switch(sdr.getSourceDocumentInfo().getTechInfoSet().getFileType()) {
			case TEXT : {
//				setVisibleXSLTInputComponents(false);
//				setVisiblePreviewComponents(true);
				
				//TODO: enable encoding combobox
				showSourceDocumentPreview(sdr);
				onAdvance = true;
				break;
			}
//			case XML : {
//				setVisiblePreviewComponents(false);
//				setVisibleXSLTInputComponents(true);
//				onAdvance = false;
//				break;
//			}
			default : {
//				setVisibleXSLTInputComponents(false);
//				setVisiblePreviewComponents(true);
				
				//TODO: disable encoding combobox
				showSourceDocumentPreview(sdr);
				onAdvance = true;
			}
		}
		wizardStepListener.stepChanged(FileTypePanel.this);
	}
	
//	private void setVisibleXSLTInputComponents(boolean visible) {
//		uploadLabel.setVisible(visible);
//		uploadPanel.setVisible(visible);
//	}
//	
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
	
	public void stepDeactivated(boolean forward){ /* noop */}

	public void stepAdded() {/* noop */}

}
