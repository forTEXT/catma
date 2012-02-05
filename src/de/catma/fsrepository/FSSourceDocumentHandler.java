package de.catma.fsrepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import de.catma.core.ExceptionHandler;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.source.SourceDocumentHandler;
import de.catma.core.document.source.SourceDocumentInfo;
import de.catma.core.document.source.contenthandler.BOMFilterInputStream;
import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.user.UserMarkupCollectionReference;
import de.catma.serialization.SourceDocumentInfoSerializationHandler;

class FSSourceDocumentHandler {
	
	private static enum Field {
		sourceURI,
		rawcopy,
		managed,
		infosetsURI,
		structureURI,
		userURI,
		;
		private String toSimpleXQuery() {
			return "//" + this.toString();
		}
	}

	private static final String DIGITALOBJECTS_FOLDER = "digitalobjects";
	
	private String repoFolderPath;
	private String digitalObjectsFolderPath;
	private SourceDocumentInfoSerializationHandler sourceDocumentInfoSerializationHandler;

	public FSSourceDocumentHandler(
			String repoFolderPath, 
			SourceDocumentInfoSerializationHandler sourceDocumentInfoSerializationHandler) {
		super();
		this.repoFolderPath = repoFolderPath;
		this.sourceDocumentInfoSerializationHandler = sourceDocumentInfoSerializationHandler;
		this.digitalObjectsFolderPath = 
				this.repoFolderPath + System.getProperty("file.separator") + DIGITALOBJECTS_FOLDER;
	}
	
	public Map<String,SourceDocument> loadSourceDocuments() {
		HashMap<String,SourceDocument> result = new HashMap<String, SourceDocument>();
		
		File digitalObjectsFolder = new File(this.digitalObjectsFolderPath);
		File[] digitalObjectFiles = digitalObjectsFolder.listFiles();
		for (File digitalObjectFile : digitalObjectFiles) {
			try {
				SourceDocument current = loadSourceDocument(digitalObjectFile);
				result.put(current.getID(), current);
			}
			catch(IOException ioe) {
				ExceptionHandler.log(ioe);
			}
		}
		
		return result;
	}

	public SourceDocument loadSourceDocument(File digitalObjectFile) throws IOException {
		InputStream infosetsInputStream  = null;
		try {
			Document xmlDoc = new Builder().build(digitalObjectFile);
			
			String infosetsDocumentURLString = xmlDoc.query(Field.infosetsURI.toSimpleXQuery()).get(0).getValue();
			URL infosetsURL = new URL(infosetsDocumentURLString);
			
			URLConnection infosetsURLConnection = infosetsURL.openConnection();
			infosetsInputStream = infosetsURLConnection.getInputStream();
			
			SourceDocumentInfo sourceDocumentInfo = 
					this.sourceDocumentInfoSerializationHandler.deserialize(
							new BOMFilterInputStream(infosetsInputStream, Charset.forName("UTF-8")));
			
			String sourceURIVal = xmlDoc.query(Field.sourceURI.toSimpleXQuery()).get(0).getValue();
			URI sourceURI = new URI(sourceURIVal);
			sourceDocumentInfo.getTechInfoSet().setURI(sourceURI);
			SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler();
			SourceDocument sourceDocument = 
					sourceDocumentHandler.loadSourceDocument(sourceDocumentInfo);
			
			Nodes structureURINodes = xmlDoc.query(Field.structureURI.toSimpleXQuery());
			
			for (int i=0; i<structureURINodes.size(); i++) {
				Node structureURINode = structureURINodes.get(i);
				String structureURI = structureURINode.getValue();
				StructureMarkupCollectionReference structureMarkupCollRef = 
						new StructureMarkupCollectionReference(structureURI, structureURI);
				sourceDocument.addStructureMarkupCollectionReference(structureMarkupCollRef);
			}
			
			Nodes userURINodes = xmlDoc.query(Field.userURI.toSimpleXQuery());
			
			for (int i=0; i<userURINodes.size(); i++) {
				Node userURINode = userURINodes.get(i);
				String userURI = userURINode.getValue();
				UserMarkupCollectionReference userMarkupCollRef = 
						new UserMarkupCollectionReference(userURI, userURI);
				sourceDocument.addUserMarkupCollectionReference(userMarkupCollRef);
			}
			
			infosetsInputStream.close();
			
			return sourceDocument;
		} catch (Exception e) {
			if (infosetsInputStream != null) {
				try {
					infosetsInputStream.close();
				}
				catch(Exception e2){}
			}
			throw new IOException(e);
		}
		
	}
	
	public String getDigitalObjectsFolderPath() {
		return digitalObjectsFolderPath;
	}

	public void insert(SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}
}
