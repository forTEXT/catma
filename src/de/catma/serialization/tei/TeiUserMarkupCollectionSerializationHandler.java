package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagManager;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;

public class TeiUserMarkupCollectionSerializationHandler implements
		UserMarkupCollectionSerializationHandler {

	private TagManager tagManager;
	
	public TeiUserMarkupCollectionSerializationHandler(TagManager tagManager) {
		super();
		this.tagManager = tagManager;
	}

	public void serialize(
		UserMarkupCollection userMarkupCollection, OutputStream outputStream) throws IOException {

	}

	public UserMarkupCollection deserialize(
			String id, InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = 
					factory.createDocumentFromStream(id, inputStream);
			TagLibrary tagLibrary = 
					new TeiTagLibrarySerializationHandler(
							tagManager).deserialize(teiDocument);
			TeiUserMarkupCollectionDeserializer deserializer = 
					new TeiUserMarkupCollectionDeserializer(teiDocument, tagLibrary);
			return new UserMarkupCollection(id, tagLibrary, deserializer.getTagReferences());
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}
	}


}
