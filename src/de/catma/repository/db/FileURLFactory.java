package de.catma.repository.db;

import static de.catma.repository.db.SourceDocumentHandler.REPO_URI_SCHEME;

public class FileURLFactory {
	
	public static final String SOURCEDOCS_FOLDER = SourceDocumentHandler.SOURCEDOCS_FOLDER;
	
	public String getFileURL(String catmaID, String... path) {
		StringBuilder builder = new StringBuilder("file://");
		for (String folder : path) {
			builder.append(folder);
		}
		builder.append(catmaID.substring((REPO_URI_SCHEME).length()));
		return builder.toString();
	}
}
