package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;

import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagManager;
import de.catma.serialization.TagLibrarySerializationHandler;

public class TeiTagLibrarySerializationHandler implements TagLibrarySerializationHandler {
	
	private TagManager tagManager;
	private TeiDocument teiDocument;
	
	public TeiTagLibrarySerializationHandler(
			TeiDocument teiDocument, TagManager tagManager) {
		super();
		this.tagManager = tagManager;
		this.teiDocument = teiDocument;
	}

	public TeiTagLibrarySerializationHandler(TagManager tagManager) {
		super();
		this.tagManager = tagManager;
	}
	
	public void serialize(TagLibrary tagLibrary) {
		TeiTagLibrarySerializer serializer = 
				new TeiTagLibrarySerializer(teiDocument);
		serializer.serialize(tagLibrary);
	}
	
	
	public TagLibrary deserialize() {
		TeiTagLibraryDeserializer deserializer = 
				new TeiTagLibraryDeserializer(teiDocument, tagManager);
		return deserializer.getTagLibrary();
	}
	
	public TagLibrary deserialize(
			String id, InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			teiDocument = 
					factory.createDocumentFromStream(id, inputStream);
			return deserialize();
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}
	}
	
}
