package de.catma.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.catma.document.source.ISourceDocument;
import de.catma.document.standoffmarkup.usermarkup.IUserMarkupCollection;

public interface UserMarkupCollectionSerializationHandler {

	public void serialize(IUserMarkupCollection userMarkupCollection, 
		ISourceDocument sourceDocument, OutputStream outputStream) throws IOException;
	
	public IUserMarkupCollection deserialize(
			String id, InputStream inputStream) throws IOException;
	
}
