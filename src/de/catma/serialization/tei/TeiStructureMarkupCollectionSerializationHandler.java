package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;

import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollection;
import de.catma.serialization.StructureMarkupCollectionSerializationHandler;

public class TeiStructureMarkupCollectionSerializationHandler implements
		StructureMarkupCollectionSerializationHandler {

	public void serialize(StructureMarkupCollection structureMarkupCollection) {
		// TODO Auto-generated method stub

	}

	public StructureMarkupCollection deserialize(InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = factory.createDocumentFromStream(inputStream);
			
			return deserialize(teiDocument);
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}
	}

	private StructureMarkupCollection deserialize(TeiDocument teiDocument) {
		// TODO Auto-generated method stub
		return null;
	}

}
