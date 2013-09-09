package de.catma.repository.db;

import static de.catma.repository.db.DBSourceDocumentHandler.REPO_URI_SCHEME;

public class FileURLFactory {
	String getFileURL(String catmaID, String... path) {
		StringBuilder builder = new StringBuilder("file://");
		for (String folder : path) {
			builder.append(folder);
		}
		builder.append(catmaID.substring((REPO_URI_SCHEME).length()));
		return builder.toString();
	}
}
