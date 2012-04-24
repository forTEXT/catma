package de.catma.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;

public interface UserMarkupCollectionSerializationHandler {

	public void serialize(UserMarkupCollection userMarkupCollection, 
		SourceDocument sourceDocument, OutputStream outputStream) throws IOException;
	
	public UserMarkupCollection deserialize(
			String id, InputStream inputStream) throws IOException;
	
}
