package de.catma.fsrepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import de.catma.core.document.source.contenthandler.BOMFilterInputStream;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagLibraryReference;
import de.catma.serialization.TagLibrarySerializationHandler;

class FSTagLibraryHandler {

	private final static String TAGLIBRARY_FOLDER = "libraries";
	
	private static enum Field {
		name,
		fileURI,
		;
		private String toSimpleXQuery() {
			return "//" + this.toString();
		}
		
	}

	private String repoFolderPath;
	private String libFolderPath;
	private TagLibrarySerializationHandler tagLibrarySerializationHandler;
	
	public FSTagLibraryHandler(
			String repoFolderPath, 
			TagLibrarySerializationHandler tagLibrarySerializationHandler) {
		
		super();
		this.repoFolderPath = repoFolderPath;
		this.libFolderPath = this.repoFolderPath + System.getProperty("file.separator") + TAGLIBRARY_FOLDER;
		this.tagLibrarySerializationHandler = tagLibrarySerializationHandler;
	}
	
	public Set<TagLibraryReference> loadTagLibraryReferences() throws IOException {
		Set<TagLibraryReference> result = new HashSet<TagLibraryReference>();
		
		File libFolder = new File(this.libFolderPath);
		File[] libFiles = libFolder.listFiles();
		
		for (File tagLibFile : libFiles) {
			TagLibraryReference ref = loadTagLibraryReference(tagLibFile);
			if (ref != null) {
				result.add(ref);
			}
		}
		
		return result;
	}

	private TagLibraryReference loadTagLibraryReference(File tagLibFile) throws IOException {
		try {
			Document tagLibDoc = new Builder().build(tagLibFile);
		
			Nodes nameNode = tagLibDoc.query(Field.name.toSimpleXQuery());
			String libName = nameNode.get(0).getValue();
			Nodes fileURINode = tagLibDoc.query(Field.fileURI.toSimpleXQuery());
			String fileURI = fileURINode.get(0).getValue();
			
			return new TagLibraryReference(libName, fileURI);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public TagLibrary loadTagLibrary(TagLibraryReference tagLibraryReference) throws IOException {
		FilterInputStream is = new BOMFilterInputStream(
				new FileInputStream(tagLibraryReference.getId()), Charset.forName( "UTF-8" ));
		return tagLibrarySerializationHandler.deserialize(is); 
	}
	
}
