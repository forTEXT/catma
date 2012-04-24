package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.catma.core.document.ContentInfoSet;
import de.catma.core.document.source.IndexInfoSet;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.source.SourceDocumentInfo;
import de.catma.core.document.source.TechInfoSet;
import de.catma.serialization.DocumentSerializer;
import de.catma.serialization.SourceDocumentInfoSerializationHandler;

public class TeiSourceDocumentInfoSerializationHandler implements
		SourceDocumentInfoSerializationHandler {

	public SourceDocumentInfo deserialize(
			String id, InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = 
					factory.createDocumentFromStream(id, inputStream);
			
			return deserialize(teiDocument);
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}	
	}

	private SourceDocumentInfo deserialize(TeiDocument teiDocument) {
		ContentInfoSet contentInfoSet = teiDocument.getContentInfoSet();	
		TechInfoSet techInfoSet = teiDocument.getTechInfoset();
		IndexInfoSet indexInfoSet = teiDocument.getIndexInfoSet();
		
		return new SourceDocumentInfo(indexInfoSet, contentInfoSet, techInfoSet);
	}

	public void serialize(
			SourceDocument sourceDocument, 
			OutputStream outputStream) throws IOException {
		
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			
			TeiDocument teiDocument = factory.createEmptyDocument(
					sourceDocument.getID());
			SourceDocumentInfo sourceDocumentInfo = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo();
			
			teiDocument.getTeiHeader().setValues(
				sourceDocumentInfo.getContentInfoSet(), 
				sourceDocumentInfo.getTechInfoSet(),
				sourceDocumentInfo.getIndexInfoSet());
			
			DocumentSerializer serializer = new DocumentSerializer();
			serializer.serialize(teiDocument.getDocument(), outputStream);
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}	
	}

}
