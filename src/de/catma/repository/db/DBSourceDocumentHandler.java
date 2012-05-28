package de.catma.repository.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;

import de.catma.document.source.ISourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;

class DBSourceDocumentHandler {

	private static final String SOURCEDOCS_FOLDER = "sourcedocuments";
	private static final String REPO_URI_SCHEME = "catma://";
	
	private String sourceDocsPath;

	public DBSourceDocumentHandler(String repoFolderPath) {
		this.sourceDocsPath = repoFolderPath + 
			"/" + 
			SOURCEDOCS_FOLDER + "/";
	}
	
	public String getIDFromURI(URI uri) {
		if (uri.getScheme().toLowerCase().equals("file")) {
			File file = new File(uri);
			return REPO_URI_SCHEME + file.getName();
		}
		else {
			return REPO_URI_SCHEME + new IDGenerator().generate();
		}
		
	}
	
	public void insert(ISourceDocument sourceDocument) throws IOException {

		SourceDocumentInfo sourceDocumentInfo = 
				sourceDocument.getSourceContentHandler().getSourceDocumentInfo();
		
		URI sourceDocURI = sourceDocumentInfo.getTechInfoSet().getURI();
		
		if (sourceDocURI.getScheme().toLowerCase().equals("file")) {
			File sourceTempFile = new File(sourceDocURI);
			File repoSourceFile = 
					new File(
							this.sourceDocsPath
							+ sourceTempFile.getName());
			
			FileInputStream sourceTempFileStream = 
					new FileInputStream(sourceTempFile);
			FileOutputStream repoSourceFileOutputStream = 
					new FileOutputStream(repoSourceFile);
			try {
				IOUtils.copy(sourceTempFileStream, repoSourceFileOutputStream);
			}
			finally {
				CloseSafe.close(sourceTempFileStream);
				CloseSafe.close(repoSourceFileOutputStream);
			}
			
			sourceTempFile.delete();
		}
		else {
			File repoSourceFile = null;
			
			try {
				repoSourceFile = 
					new File(
						new URI(getFileURL(sourceDocument.getID(), sourceDocsPath)));
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
			
			System.out.println(repoSourceFile.getAbsolutePath());
			Writer repoSourceFileWriter =  
					new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(repoSourceFile),
							sourceDocumentInfo.getTechInfoSet().getCharset()));
			try {
				//TODO: keep BOM ... or don't keep it...?!
				repoSourceFileWriter.append(sourceDocument.getContent());
			}
			finally {
				CloseSafe.close(repoSourceFileWriter);
			}

		}
	}
	
	
	public String getFileURL(String catmaUri, String... path) {
		StringBuilder builder = new StringBuilder("file://");
		for (String folder : path) {
			builder.append(folder);
		}
		builder.append(catmaUri.substring((REPO_URI_SCHEME).length()));
		return builder.toString();
	}
}
