package de.catma.fsrepository;

import java.io.File;
import java.io.IOException;

import nu.xom.Builder;
import nu.xom.Document;
import de.catma.core.document.source.SourceDocument;

public class FSSourceDocumentHandler {
	
	private static enum Field {
		sourceURI,
		rawcopy,
		managed,
		infosets,
		structure,
		user,
		;
		private String toSimpleXQuery() {
			return "//" + this.toString();
		}
	}

	private static final String DIGITALOBJECTS_FOLDER = "digitalobjects";
	
	private String repoFolderPath;
	private String digitalObjectsFolderPath;

	public FSSourceDocumentHandler(String repoFolderPath) {
		super();
		this.repoFolderPath = repoFolderPath;
		this.digitalObjectsFolderPath = 
				this.repoFolderPath + System.getProperty("file.separator") + DIGITALOBJECTS_FOLDER;
	}

	public SourceDocument loadSourceDocument(String digitalObjectFileName) throws IOException {
		try {
			Document xmlDoc = new Builder().build(new File(this.digitalObjectsFolderPath));
			
			String infosetsDocumentURL = xmlDoc.query(Field.infosets.toSimpleXQuery()).get(0).getValue();
			
			
			
			return null;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	
}
