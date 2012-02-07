package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;

import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollection;
import de.catma.serialization.StaticMarkupCollectionSerializationHandler;

public class TeiStaticMarkupCollectionSerializationHandler implements
		StaticMarkupCollectionSerializationHandler {

	public void serialize(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub

	}

	public StaticMarkupCollection deserialize(InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = factory.createDocumentFromStream(inputStream);
			
			return deserialize(teiDocument);
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}
	}

	private StaticMarkupCollection deserialize(TeiDocument teiDocument) {
		// TODO Auto-generated method stub
		return null;
	}

}
