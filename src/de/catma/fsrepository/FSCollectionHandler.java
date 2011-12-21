package de.catma.fsrepository;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import de.catma.core.document.Collection;
import de.catma.core.document.source.SourceDocument;

public class FSCollectionHandler {

	private final static String COLLECTION_FOLDER = "collections";
	
	private static enum Field {
		name,
		source,
		user,
		structure,
		;
		private String toSimpleXQuery() {
			return "//" + this.toString();
		}
	}
	
	private String repoFolderPath;
	private String collectionFolderPath;
	
	public FSCollectionHandler(String repoFolderPath) {
		super();
		this.repoFolderPath = repoFolderPath;
		this.collectionFolderPath = 
				this.repoFolderPath + System.getProperty("file.separator") + COLLECTION_FOLDER;
	}
	
	public Set<Collection> loadCollections() throws IOException {
		Set<Collection> collections = new HashSet<Collection>();
		File collectionFolder = new File(collectionFolderPath);
		File[] collectionFiles = collectionFolder.listFiles();
		
		for (File collectionFile : collectionFiles) {
			Collection collection = loadCollection(collectionFile);
			if (collection !=null) {
				collections.add(collection);
			}
		}
		
		return collections;
	}

	private Collection loadCollection(File collectionFile) throws IOException {
		FSSourceDocumentHandler fsSourceDocumentHandler = 
				new FSSourceDocumentHandler(repoFolderPath);
		
		try {
			Document collectionDoc = new Builder().build(collectionFile);
			
			Nodes nameNode = collectionDoc.query(Field.name.toSimpleXQuery());
			String collectionName = nameNode.get(0).getValue();
			
			Nodes sourceNodes = collectionDoc.query(Field.source.toSimpleXQuery());
			for (int i=0; i<sourceNodes.size(); i++) {
				Node sourceNode = sourceNodes.get(i);
				
				SourceDocument sourceDocument = 
						fsSourceDocumentHandler.loadSourceDocument(sourceNode.getValue());
			}
			
			return null;
			
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	
	
}
