package de.catma.serialization;

import java.io.IOException;
import java.io.InputStream;

import de.catma.tag.ITagLibrary;

public interface TagLibrarySerializationHandler {
	public void serialize(ITagLibrary tagLibrary) throws IOException;
	public ITagLibrary deserialize(
		String id, InputStream inputStream) throws IOException;
}
