package de.catma.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.source.SourceDocumentInfo;


public interface SourceDocumentInfoSerializationHandler {

	public SourceDocumentInfo deserialize(InputStream inputStream) throws IOException;
	public void serialize(SourceDocument sourceDocument, OutputStream outputStream) throws IOException;
	
}
