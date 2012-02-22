package de.catma.fsrepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;

import org.apache.commons.io.IOUtils;

import de.catma.core.ExceptionHandler;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.source.SourceDocumentHandler;
import de.catma.core.document.source.SourceDocumentInfo;
import de.catma.core.document.source.contenthandler.BOMFilterInputStream;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.core.util.IDGenerator;
import de.catma.serialization.DocumentSerializer;
import de.catma.serialization.SourceDocumentInfoSerializationHandler;

class FSSourceDocumentHandler {
	
	private static enum Field {
		digitalobject,
		sourceURI,
		rawcopyURI,
		infosetsURI,
		staticMarkupURI,
		userMarkupURI,
		;
		private String toSimpleXQuery() {
			return "//" + this.toString();
		}
	}

	private static final String DIGITALOBJECTS_FOLDER = "digitalobjects";
	
	private String repoFolderPath;
	private String digitalObjectsFolderPath;
	private String containerPath;
	private SourceDocumentInfoSerializationHandler sourceDocumentInfoSerializationHandler;

	public FSSourceDocumentHandler(
			String repoFolderPath, 
			SourceDocumentInfoSerializationHandler sourceDocumentInfoSerializationHandler) {
		super();
		this.repoFolderPath = repoFolderPath;
		this.sourceDocumentInfoSerializationHandler = sourceDocumentInfoSerializationHandler;
		this.digitalObjectsFolderPath = 
				this.repoFolderPath + System.getProperty("file.separator") + DIGITALOBJECTS_FOLDER;
		this.containerPath = 
				this.repoFolderPath + System.getProperty("file.separator") + FSRepository.CONTAINER_FOLDER;
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
			Document digitalObject = new Builder().build(digitalObjectFile);
			
			String infosetsDocumentURLString = 
					digitalObject.query(Field.infosetsURI.toSimpleXQuery()).get(0).getValue();
			
			URL infosetsURL = new URL(infosetsDocumentURLString);
			
			URLConnection infosetsURLConnection = infosetsURL.openConnection();
			infosetsInputStream = infosetsURLConnection.getInputStream();
			
			SourceDocumentInfo sourceDocumentInfo = 
					this.sourceDocumentInfoSerializationHandler.deserialize(
							new BOMFilterInputStream(infosetsInputStream, Charset.forName("UTF-8")));
			
			String sourceURIVal = digitalObject.query(Field.sourceURI.toSimpleXQuery()).get(0).getValue();
			URI sourceURI = new URI(sourceURIVal);
			sourceDocumentInfo.getTechInfoSet().setURI(sourceURI);
			SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler();
			SourceDocument sourceDocument = 
					sourceDocumentHandler.loadSourceDocument(sourceDocumentInfo);
			
			Nodes staticMarkupURINodes = digitalObject.query(Field.staticMarkupURI.toSimpleXQuery());
			
			for (int i=0; i<staticMarkupURINodes.size(); i++) {
				Node staticMarkupURINode = staticMarkupURINodes.get(i);
				String staticMarkupURI = staticMarkupURINode.getValue();
				StaticMarkupCollectionReference staticMarkupCollRef = 
						new StaticMarkupCollectionReference(staticMarkupURI, staticMarkupURI);
				sourceDocument.addStaticMarkupCollectionReference(staticMarkupCollRef);
			}
			
			Nodes userURINodes = digitalObject.query(Field.userMarkupURI.toSimpleXQuery());
			
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

	public void insert(SourceDocument sourceDocument) throws IOException {

		IDGenerator idGenerator = new IDGenerator();

		Element root = new Element(Field.digitalobject.name());
		
		SourceDocumentInfo sourceDocumentInfo = 
				sourceDocument.getSourceContentHandler().getSourceDocumentInfo();
		
		URI sourceDocURI = sourceDocumentInfo.getTechInfoSet().getURI();
		
		if (sourceDocURI.getScheme().toLowerCase().equals("file")) {
			File sourceTempFile = new File(sourceDocURI);
			File repoSourceFile = 
					new File(
							this.containerPath
							+ System.getProperty("file.separator")
							+ sourceTempFile.getName());
			FileInputStream sourceTempFileStream = new FileInputStream(sourceTempFile);
			FileOutputStream repoSourceFileOutputStream = new FileOutputStream(repoSourceFile);
			try {
				IOUtils.copy(sourceTempFileStream, repoSourceFileOutputStream);
			}
			finally {
				sourceTempFileStream.close();
				repoSourceFileOutputStream.close();
			}
			
			sourceTempFile.delete();
			
			sourceDocURI = repoSourceFile.toURI();
		}
		else {

			File repoSourceFile = 
					new File(
							this.containerPath
							+ System.getProperty("file.separator")
							+ "Source_" + idGenerator.generate());
			
			Writer repoSourceFileWriter =  
					new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(repoSourceFile),
							sourceDocumentInfo.getTechInfoSet().getCharset()));
			try {
				//TODO: keep BOM 
				repoSourceFileWriter.append(sourceDocument.getContent());
			}
			finally {
				repoSourceFileWriter.close();
			}
			
			if (sourceDocumentInfo.getTechInfoSet().isManagedResource()) {
				sourceDocURI = repoSourceFile.toURI();
			}
			else {
				Element rawCopyUriElement = new Element(Field.rawcopyURI.name());
				rawCopyUriElement.appendChild(repoSourceFile.toURI().toString());
			}
		}
		
		Element sourceUriElement = new Element(Field.sourceURI.name());
		sourceUriElement.appendChild(sourceDocURI.toString());
		root.appendChild(sourceUriElement);
		sourceDocumentInfo.getTechInfoSet().setURI(sourceDocURI);

		File repoInfosetsFile = new File(
				this.containerPath
				+ System.getProperty("file.separator")
				+ "Infosets_" + idGenerator.generate() + ".xml");
		
		sourceDocumentInfoSerializationHandler.serialize(
				sourceDocument, new FileOutputStream(repoInfosetsFile));
		
		Element infoSetsUriElement = new Element(Field.infosetsURI.name());
		infoSetsUriElement.appendChild(repoInfosetsFile.toURI().toString());
		root.appendChild(infoSetsUriElement);
		
		File repoDigitalObjectFile = new File(
				this.digitalObjectsFolderPath
				+ System.getProperty("file.separator")
				+ "do_" + idGenerator.generate() + ".xml");
		
		Document digitalObject = new Document(root);
		DocumentSerializer serializer = new DocumentSerializer();
		serializer.serialize(digitalObject, new FileOutputStream(repoDigitalObjectFile));
	}
}
