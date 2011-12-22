package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;

import de.catma.core.document.source.ContentInfoSet;
import de.catma.core.document.source.IndexInfoSet;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.source.SourceDocumentInfo;
import de.catma.core.document.source.TechInfoSet;
import de.catma.serialization.SourceDocumentInfoSerializationHandler;

public class TeiSourceDocumentInfoSerializationHandler implements
		SourceDocumentInfoSerializationHandler {

	public SourceDocumentInfo deserialize(InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = factory.createDocumentFromStream(inputStream);
			
			return deserialize(teiDocument);
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}	
	}

	public SourceDocumentInfo deserialize(TeiDocument teiDocument) {
		ContentInfoSet contentInfoSet = teiDocument.getContentInfoSet();	
		TechInfoSet techInfoSet = teiDocument.getTechInfoset();
		IndexInfoSet indexInfoSet = teiDocument.getIndexInfoSet();
		
		return new SourceDocumentInfo(indexInfoSet, contentInfoSet, techInfoSet);
	}

	public void serialize(SourceDocument sourceDocument) {
		// TODO Auto-generated method stub

	}

}
