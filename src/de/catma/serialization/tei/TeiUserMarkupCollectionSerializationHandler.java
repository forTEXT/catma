package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;

import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.tag.TagLibrary;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;

public class TeiUserMarkupCollectionSerializationHandler implements
		UserMarkupCollectionSerializationHandler {

	public void serialize(UserMarkupCollection userMarkupCollection) {
		// TODO Auto-generated method stub

	}

	public UserMarkupCollection deserialize(InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = factory.createDocumentFromStream(inputStream);
			TagLibrary tagLibrary = 
					new TeiTagLibrarySerializationHandler().deserialize(teiDocument);
			TeiUserMarkupCollectionDeserializer deserializer = 
					new TeiUserMarkupCollectionDeserializer(teiDocument, tagLibrary);
			return new UserMarkupCollection(tagLibrary, deserializer.getTagReferences());
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}
	}


}
