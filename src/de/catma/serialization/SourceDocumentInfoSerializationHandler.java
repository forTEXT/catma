package de.catma.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;


public interface SourceDocumentInfoSerializationHandler {

	public SourceDocumentInfo deserialize(
			String id, InputStream inputStream) throws IOException;
	public void serialize(
			SourceDocument sourceDocument, 
			OutputStream outputStream) throws IOException;
	
}
