package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;

import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagManager;
import de.catma.serialization.TagLibrarySerializationHandler;

public class TeiTagLibrarySerializationHandler implements TagLibrarySerializationHandler {
	
	private TagManager tagManager;
	
	public TeiTagLibrarySerializationHandler(TagManager tagManager) {
		super();
		this.tagManager = tagManager;
	}


	public void serialize(TagLibrary tagLibrary) {
		// TODO Auto-generated method stub

	}
	
	
	public TagLibrary deserialize(TeiDocument teiDocument) {
		TeiTagLibraryDeserializer deserializer = 
				new TeiTagLibraryDeserializer(teiDocument, tagManager);
		return deserializer.getTagLibrary();
	}
	
	public TagLibrary deserialize(
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
	
}
