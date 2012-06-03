package de.catma.repository.fs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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

import de.catma.ExceptionHandler;
import de.catma.document.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentHandler;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.serialization.DocumentSerializer;
import de.catma.serialization.SourceDocumentInfoSerializationHandler;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;

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

	static final String DIGITALOBJECTS_FOLDER = "digitalobjects";
	
	private String repoFolderPath;
	private String digitalObjectsFolderPath;
	private String containerPath;
	private SourceDocumentInfoSerializationHandler sourceDocumentInfoSerializationHandler;
	private Map<String,String> sourceDoc2DigitalObject;

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
		this.sourceDoc2DigitalObject = new HashMap<String, String>();
	}
	
	public Map<String,SourceDocument> loadSourceDocuments() {
		HashMap<String,SourceDocument> result = new HashMap<String, SourceDocument>();
		
		File digitalObjectsFolder = new File(this.digitalObjectsFolderPath);
		File[] digitalObjectFiles = digitalObjectsFolder.listFiles();
		for (File digitalObjectFile : digitalObjectFiles) {
			try {
				SourceDocument current = loadSourceDocument(digitalObjectFile);
				result.put(current.getID(), current);
				sourceDoc2DigitalObject.put(
					current.getID(),
					digitalObjectFile.getAbsolutePath());
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
					digitalObject.query(
						Field.infosetsURI.toSimpleXQuery()).get(0).getValue();
			
			URL infosetsURL = 
					new URL(FSRepository.getFileURL(
							infosetsDocumentURLString, repoFolderPath));

			
			URLConnection infosetsURLConnection = infosetsURL.openConnection();
			infosetsInputStream = infosetsURLConnection.getInputStream();
			
			String sourceURIVal = 
					digitalObject.query(
						Field.sourceURI.toSimpleXQuery()).get(0).getValue();
							
			URI sourceURI = null;
			if (FSRepository.isCatmaUri(sourceURIVal)) {
				sourceURI = 
					new URI(
						FSRepository.getFileURL(sourceURIVal, repoFolderPath));
			}
			else {
				sourceURI = new URI(sourceURIVal);
			}

			SourceDocumentInfo sourceDocumentInfo = 
					this.sourceDocumentInfoSerializationHandler.deserialize(
							sourceURIVal,
							new BOMFilterInputStream(
									infosetsInputStream, 
									Charset.forName("UTF-8")));
			
			sourceDocumentInfo.getTechInfoSet().setURI(sourceURI);
			
			SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler();
			SourceDocument sourceDocument = 
					sourceDocumentHandler.loadSourceDocument(
							sourceURIVal, sourceDocumentInfo);
			
			Nodes staticMarkupURINodes = 
					digitalObject.query(Field.staticMarkupURI.toSimpleXQuery());
			
			for (int i=0; i<staticMarkupURINodes.size(); i++) {
				Node staticMarkupURINode = staticMarkupURINodes.get(i);
				StaticMarkupCollectionReference staticMarkupCollRef = 
						new StaticMarkupCollectionReference(
								staticMarkupURINode.getValue(), 
								staticMarkupURINode.getValue());
				sourceDocument.addStaticMarkupCollectionReference(staticMarkupCollRef);
			}
			
			Nodes userURINodes = digitalObject.query(Field.userMarkupURI.toSimpleXQuery());
			
			for (int i=0; i<userURINodes.size(); i++) {
				Node userURINode = userURINodes.get(i);
				UserMarkupCollectionReference userMarkupCollRef = 
						new UserMarkupCollectionReference(
								userURINode.getValue(), 
								new ContentInfoSet(userURINode.getValue()));
				sourceDocument.addUserMarkupCollectionReference(userMarkupCollRef);
			}
			
			CloseSafe.close(infosetsInputStream);
			return sourceDocument;
		}
		catch (Exception exc) {
			CloseSafe.close(infosetsInputStream);
			throw new IOException(exc);
		}
	}
	
	public String getDigitalObjectsFolderPath() {
		return digitalObjectsFolderPath;
	}

	
	public String getIDFromURI(URI uri) {
		if (uri.getScheme().equals("file")) {
			File file = new File(uri);
			return FSRepository.buildCatmaUri(
					DIGITALOBJECTS_FOLDER + "/"
					+ file.getName());
		}
		else {
			return uri.toString();
		}
	}
	
	public void insert(SourceDocument sourceDocument) throws IOException {

		IDGenerator idGenerator = new IDGenerator();

		Element root = new Element(Field.digitalobject.name());
		
		SourceDocumentInfo sourceDocumentInfo = 
				sourceDocument.getSourceContentHandler().getSourceDocumentInfo();
		
		URI sourceDocURI = sourceDocumentInfo.getTechInfoSet().getURI();
		String sourceDocURIString = sourceDocURI.toString();
		
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
				CloseSafe.close(sourceTempFileStream);
				CloseSafe.close(repoSourceFileOutputStream);
			}
			
			sourceTempFile.delete();
			
			sourceDocURIString = getIDFromURI(sourceDocURI);
		}
		else {
			String localCopyFileName = "Source_" + idGenerator.generate();
			File repoSourceFile = 
					new File(
							this.containerPath
							+ System.getProperty("file.separator")
							+ localCopyFileName);
			
			Writer repoSourceFileWriter =  
					new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(repoSourceFile),
							sourceDocumentInfo.getTechInfoSet().getCharset()));
			try {
				//TODO: keep BOM 
				repoSourceFileWriter.append(sourceDocument.getContent());
			}
			finally {
				CloseSafe.close(repoSourceFileWriter);
			}
			

			Element rawCopyUriElement = new Element(Field.rawcopyURI.name());
			rawCopyUriElement.appendChild(
					FSRepository.buildCatmaUri(
						DIGITALOBJECTS_FOLDER 
						+ "/"
						+ localCopyFileName));
		}
		
		Element sourceUriElement = new Element(Field.sourceURI.name());
		sourceUriElement.appendChild(sourceDocURIString);
		root.appendChild(sourceUriElement);
		sourceDocumentInfo.getTechInfoSet().setURI(sourceDocURI);

		String repoInfosetsFileName = 
				"Infosets_" + idGenerator.generate() + ".xml";
		File repoInfosetsFile = new File(
				this.containerPath
				+ System.getProperty("file.separator")
				+ repoInfosetsFileName);
		FileOutputStream infosetsFos = new FileOutputStream(repoInfosetsFile);
		try {
			sourceDocumentInfoSerializationHandler.serialize(
					sourceDocument, infosetsFos);
		}
		finally {
			CloseSafe.close(infosetsFos);
		}
		
		Element infoSetsUriElement = new Element(Field.infosetsURI.name());
		infoSetsUriElement.appendChild(
				FSRepository.CONTAINER_FOLDER + "/" + repoInfosetsFileName);
		root.appendChild(infoSetsUriElement);
		
		File repoDigitalObjectFile = new File(
				this.digitalObjectsFolderPath
				+ System.getProperty("file.separator")
				+ "do_" + idGenerator.generate() + ".xml");
		
		Document digitalObject = new Document(root);
		DocumentSerializer serializer = new DocumentSerializer();
		FileOutputStream fos = new FileOutputStream(repoDigitalObjectFile);
		try {
			serializer.serialize(digitalObject, fos);
		}
		finally {
			CloseSafe.close(fos);
		}
	}

	public void addUserMarkupCollectionReference(
			UserMarkupCollectionReference ref, SourceDocument sourceDocument) throws IOException {
		try {
			String doPath = sourceDoc2DigitalObject.get(sourceDocument.getID());
			File doFile = new File(doPath);
			Document digitalObject = new Builder().build(new File(doPath));
			
			Element userMarkupURI = new Element(Field.userMarkupURI.name());
			digitalObject.getRootElement().appendChild(userMarkupURI);
			userMarkupURI.appendChild(ref.getId());
			
			DocumentSerializer serializer = new DocumentSerializer();
			FileOutputStream fos = new FileOutputStream(doFile);
			try {
				serializer.serialize(digitalObject, fos);
			}
			finally {
				CloseSafe.close(fos);
			}
		}
		catch (Exception exc) {
			throw new IOException(exc);
		}
	}
}
