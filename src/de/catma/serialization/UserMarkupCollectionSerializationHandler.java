package de.catma.serialization;

import java.io.IOException;
import java.io.InputStream;

import de.catma.core.document.standoffmarkup.user.UserMarkupCollection;

public interface UserMarkupCollectionSerializationHandler {

	public void serialize(UserMarkupCollection userMarkupCollection);
	public UserMarkupCollection deserialize(InputStream inputStream) throws IOException;
	
}
