package de.catma.ui.legacy.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.project.Project;
import de.catma.ui.CatmaApplication;

public class SourceDocumentExportOptionsDialog extends Window {
	
	public SourceDocumentExportOptionsDialog(Project repository, SourceDocument sd) {
		super(Messages.getString("SourceDocumentExportOptionsDialog.sourceDocExportOptions")); //$NON-NLS-1$
		initComponents(repository, sd);
	}

	private void initComponents(final Project repository, final SourceDocument sd) {
		setModal(true);
		VerticalLayout content = new VerticalLayout();
		setContent(content);
		
		content.setMargin(true);
		content.setSpacing(true);
		
		Button btExport = new Button(Messages.getString("SourceDocumentExportOptionsDialog.exportDocument")); //$NON-NLS-1$
		content.addComponent(btExport);
		
		StreamResource resultStreamResource = 
				new StreamResource(
					new StreamSource() {
						@Override
						public InputStream getStream() {
							return createExportResultStream(repository, sd);
						}
					}, getFilename(sd) );
			
		resultStreamResource.setCacheTime(0);
			
		new FileDownloader(resultStreamResource).extend(btExport);	
		
		
		Button btExportPlain = new Button(Messages.getString("SourceDocumentExportOptionsDialog.exportDocAsPlainText")); //$NON-NLS-1$
		content.addComponent(btExportPlain);
		
		StreamResource resultUTF8StreamResource = 
				new StreamResource(
					new StreamSource() {
						@Override
						public InputStream getStream() {
							return createUTF8ExportResultStream(sd);
						}
					}, getFilenameUTF8(sd) );
			
		resultUTF8StreamResource.setCacheTime(0);
			
		new FileDownloader(resultUTF8StreamResource).extend(btExportPlain);	
	}

	private String getFilenameUTF8(SourceDocument sourceDocument) {
		SourceContentHandler sourceContentHandler = 
				sourceDocument.getSourceContentHandler();
		String title = 
				sourceContentHandler.getSourceDocumentInfo()
					.getContentInfoSet().getTitle();
		if (title!=null) {
			title = title.replaceAll("\\s", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return (((title==null)||title.isEmpty())?sourceDocument.getUuid().replaceAll("[/:]", ""):title) + //$NON-NLS-1$ //$NON-NLS-2$
			".txt"; //$NON-NLS-1$
	}

	private InputStream createUTF8ExportResultStream(
			SourceDocument sd) {
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		try {
			bos.write(sd.getContent().getBytes(Charset.forName("UTF8"))); //$NON-NLS-1$
		
			final ByteArrayInputStream bis = 
					new ByteArrayInputStream(bos.toByteArray());
			return bis;
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(Messages.getString("SourceDocumentExportOptionsDialog.errorExportAsPlainText"), e); //$NON-NLS-1$
		}
		
		return null;
	}

	private InputStream createExportResultStream(Project repository,
			SourceDocument sd) {
		try {
			final File file = repository.getFile(sd);
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private String getFilename(SourceDocument sourceDocument) {
		SourceContentHandler sourceContentHandler = 
				sourceDocument.getSourceContentHandler();
		String title = 
				sourceContentHandler.getSourceDocumentInfo()
					.getContentInfoSet().getTitle();
		if (title!=null) {
			title = title.replaceAll("\\s", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return  
			(((title==null)||title.isEmpty())?sourceDocument.getUuid().replaceAll("[/:]", ""):title) + //$NON-NLS-1$ //$NON-NLS-2$
			"." + sourceContentHandler.getSourceDocumentInfo().getTechInfoSet().getFileType().name().toLowerCase(); //$NON-NLS-1$
	}

}
