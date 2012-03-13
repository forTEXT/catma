package de.catma.serialization;

import java.io.IOException;
import java.io.InputStream;

import de.catma.core.tag.TagLibrary;

public interface TagLibrarySerializationHandler {
	public void serialize(TagLibrary tagLibrary) throws IOException;
	public TagLibrary deserialize(String id, InputStream inputStream) throws IOException;
}
