package de.catma.ui.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.serialization.tei.TeiUserMarkupCollectionSerializationHandler;
import de.catma.ui.CatmaApplication;

public class MarkupCollectionExportOptionsDialog extends Window {
	
	public MarkupCollectionExportOptionsDialog(Repository repository, SourceDocument sd, UserMarkupCollectionReference umcRef) {
		super("Markup Collection Export Options");
		initComponents(repository, sd, umcRef);
	}

	private void initComponents(final Repository repository, final SourceDocument sd, final UserMarkupCollectionReference umcRef) {
		setModal(true);
		VerticalLayout content = new VerticalLayout();
		setContent(content);
		
		content.setMargin(true);
		content.setSpacing(true);
		
		Button btExport = new Button("Export Markup Collection");
		content.addComponent(btExport);
		
		StreamResource resultStreamResource = 
				new StreamResource(
					new StreamSource() {
						@Override
						public InputStream getStream() {
							return createExportResultStream(repository, sd, umcRef, false);
						}
					}, umcRef.toString().replaceAll("\\s", "_") + ".xml");
			
		resultStreamResource.setCacheTime(0);
			
		new FileDownloader(resultStreamResource).extend(btExport);	
		
		
		Button btExportWithText = new Button("Export Markup Collection with text");
		content.addComponent(btExportWithText);
		
		StreamResource resultWithTextStreamResource = 
				new StreamResource(
					new StreamSource() {
						@Override
						public InputStream getStream() {
							return createExportResultStream(repository, sd, umcRef, true);
						}
					}, umcRef.toString().replaceAll("\\s", "_") + ".xml");
			
		resultWithTextStreamResource.setCacheTime(0);
			
		new FileDownloader(resultWithTextStreamResource).extend(btExportWithText);	
	}

	private InputStream createExportResultStream(Repository repository,
			SourceDocument sd, UserMarkupCollectionReference umcRef, boolean withText) {

		TeiUserMarkupCollectionSerializationHandler handler =
				new TeiUserMarkupCollectionSerializationHandler(
						repository.getTagManager(), withText);
		ByteArrayOutputStream teiDocOut = new ByteArrayOutputStream();
		try {
			handler.serialize(
				repository.getUserMarkupCollection(umcRef), sd, teiDocOut);
			
			final ByteArrayInputStream teiDownloadStream = 
					new ByteArrayInputStream(teiDocOut.toByteArray());

			return teiDownloadStream;
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				"Error exporting User Markup Collection!", e);
		}
		return null;
	}

}
