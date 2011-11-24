package de.catma.serialization;

import java.io.InputStream;

import de.catma.core.tag.TagLibrary;

public interface TagLibrarySerializer {
	public void serialize(TagLibrary tagLibrary);
	public TagLibrary deserialize(InputStream inputstream);
}
