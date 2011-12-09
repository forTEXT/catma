package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;

import de.catma.core.tag.TagLibrary;
import de.catma.serialization.TagLibrarySerializationHandler;

public class TeiTagLibrarySerializationHandler implements TagLibrarySerializationHandler {
	
	public void serialize(TagLibrary tagLibrary) {
		// TODO Auto-generated method stub

	}
	
	
	public TagLibrary deserialize(TeiDocument teiDocument) {
		TeiTagLibraryDeserializer deserializer = new TeiTagLibraryDeserializer(teiDocument);
		return deserializer.getTagLibrary();
	}
	
	public TagLibrary deserialize(InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = factory.createDocumentFromStream(inputStream);
			
			return deserialize(teiDocument);
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}
	}
	
}
