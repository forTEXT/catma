package de.catma.serialization;

public interface SerializationHandlerFactory {

	public SourceDocumentInfoSerializationHandler getSourceDocumentInfoSerializationHandler();
	public StructureMarkupCollectionSerializationHandler getStructureMarkupCollectionSerializationHandler();
	public TagLibrarySerializationHandler getTagLibrarySerializationHandler();
	public UserMarkupCollectionSerializationHandler getUserMarkupCollectionSerializationHandler();
	
}
