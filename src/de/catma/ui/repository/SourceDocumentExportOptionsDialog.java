package de.catma.ui.repository;

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

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.ui.CatmaApplication;

public class SourceDocumentExportOptionsDialog extends Window {
	
	public SourceDocumentExportOptionsDialog(Repository repository, SourceDocument sd) {
		super("Source Document Export Options");
		initComponents(repository, sd);
	}

	private void initComponents(final Repository repository, final SourceDocument sd) {
		setModal(true);
		VerticalLayout content = new VerticalLayout();
		setContent(content);
		
		content.setMargin(true);
		content.setSpacing(true);
		
		Button btExport = new Button("Export Document");
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
		
		
		Button btExportPlain = new Button("Export Document as UTF-8 plain text");
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
			title = title.replaceAll("\\s", "_");
		}
		return (((title==null)||title.isEmpty())?sourceDocument.getID().replaceAll("[/:]", ""):title) +
			".txt";
	}

	private InputStream createUTF8ExportResultStream(
			SourceDocument sd) {
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		try {
			bos.write(sd.getContent().getBytes(Charset.forName("UTF8")));
		
			final ByteArrayInputStream bis = 
					new ByteArrayInputStream(bos.toByteArray());
			return bis;
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError("error exporting source document as plain text", e);
		}
		
		return null;
	}

	private InputStream createExportResultStream(Repository repository,
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
			title = title.replaceAll("\\s", "_");
		}
		return  
			(((title==null)||title.isEmpty())?sourceDocument.getID().replaceAll("[/:]", ""):title) +
			"." + sourceContentHandler.getSourceDocumentInfo().getTechInfoSet().getFileType().name().toLowerCase();
	}

}
