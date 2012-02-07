package de.catma.serialization;

public interface SerializationHandlerFactory {

	public SourceDocumentInfoSerializationHandler getSourceDocumentInfoSerializationHandler();
	public StaticMarkupCollectionSerializationHandler getStaticMarkupCollectionSerializationHandler();
	public TagLibrarySerializationHandler getTagLibrarySerializationHandler();
	public UserMarkupCollectionSerializationHandler getUserMarkupCollectionSerializationHandler();
	
}
