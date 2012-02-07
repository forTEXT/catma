package de.catma.serialization.tei;

import de.catma.serialization.SerializationHandlerFactory;
import de.catma.serialization.SourceDocumentInfoSerializationHandler;
import de.catma.serialization.StaticMarkupCollectionSerializationHandler;
import de.catma.serialization.TagLibrarySerializationHandler;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;

public class TeiSerializationHandlerFactory implements
		SerializationHandlerFactory {

	public SourceDocumentInfoSerializationHandler getSourceDocumentInfoSerializationHandler() {
		return new TeiSourceDocumentInfoSerializationHandler();
	}

	public StaticMarkupCollectionSerializationHandler getStaticMarkupCollectionSerializationHandler() {
		return new TeiStaticMarkupCollectionSerializationHandler();
	}

	public TagLibrarySerializationHandler getTagLibrarySerializationHandler() {
		return new TeiTagLibrarySerializationHandler();
	}

	public UserMarkupCollectionSerializationHandler getUserMarkupCollectionSerializationHandler() {
		return new TeiUserMarkupCollectionSerializationHandler();
	}

}
