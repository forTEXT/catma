package de.catma.serialization;

import java.io.IOException;
import java.io.InputStream;

import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollection;

public interface StaticMarkupCollectionSerializationHandler {
	
	public void serialize(StaticMarkupCollection staticMarkupCollection);
	public StaticMarkupCollection deserialize(
			String id, InputStream inputStream) throws IOException;

}
