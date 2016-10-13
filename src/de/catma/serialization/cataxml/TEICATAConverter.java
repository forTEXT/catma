package de.catma.serialization.cataxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentHandler;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.serialization.tei.TeiUserMarkupCollectionSerializationHandler;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import nu.xom.Comment;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

public class TEICATAConverter {

	private String baseURL;
	private String user;
	private String pass;
	private TagsetDefinition structureTagsetDefinition;
	private String structureTagsetUuid;

	public void run(String[] args) throws Exception {
		String outputDir = args[0];
		user = args[1];
		pass = args[2];
		String cid = args[3];
		baseURL = args[4];
		
		StringBuilder urlBuilder = new StringBuilder(baseURL);

		urlBuilder.append("corpus/list?");
		urlBuilder.append("cid="+cid);
		
		ClientResource client = 
				new ClientResource(Context.getCurrent(), Method.GET, urlBuilder.toString());

		client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, user, pass);
		
		Representation corpusRepresentation = client.get(); 
		ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();
				
		corpusRepresentation.write(jsonStream);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode corpusJson = 
			mapper.readValue(jsonStream.toString("UTF-8"), ObjectNode.class);

		JsonNode contentsNode = corpusJson.get("contents");
		Document document = new Document(new Element("project"));
		document.insertChild(new Comment("  Created by CATMA, http://www.catma.de  "), 0);
		Element desc = new Element("description");
		desc.appendChild("This project is a sample coded data file consisting "
				+ "of 8 speeches made by the 4 candidates at the 2000 "
				+ "presidential election.");
		document.getRootElement().appendChild(desc);

		for (JsonNode sourceDocNode : contentsNode) {
			handleSourceDocNode(sourceDocNode, document); 
		}

		try (FileOutputStream fos = new FileOutputStream(new File(outputDir, "output.xml"))) {
		
			Serializer serializer = new Serializer(fos, "UTF-8");
			serializer.setIndent(2);
			serializer.write(document);
		}
	}
	
	
	
	private void handleSourceDocNode(JsonNode sourceDocNode, Document outputDocument) throws Exception {
		String sourceDocId = sourceDocNode.get("sourceDocID").asText();
		String sourceDocName = sourceDocNode.get("sourceDocName").asText();
		
		StringBuilder urlBuilder = new StringBuilder(baseURL);
		urlBuilder.append("src/get?");
		urlBuilder.append("sid="+sourceDocId);
		
		ClientResource client = 
				new ClientResource(Context.getCurrent(), Method.GET, urlBuilder.toString());

		client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, user, pass);
		
		Representation sourceDocRepresentation = client.get(); 

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		sourceDocRepresentation.write(bos);
		StandardContentHandler contentHandler = new StandardContentHandler();
		TechInfoSet techInfoSet = 
			new TechInfoSet(FileType.TEXT, Charset.forName("UTF-8"), null, null, null);
		contentHandler.setSourceDocumentInfo(new SourceDocumentInfo(null, new ContentInfoSet(sourceDocName), techInfoSet));
		contentHandler.load(new ByteArrayInputStream(bos.toByteArray()));
		
		SourceDocument sourceDocument = new SourceDocumentHandler().loadSourceDocument(sourceDocId, contentHandler);

		JsonNode umcListNode = sourceDocNode.get("umcList");
		for (JsonNode umcNode : umcListNode) {

			handleUmcNode(sourceDocument, umcNode, outputDocument);	
			break; //TODO: only one supported so far
		}
	}



	private void handleUmcNode(SourceDocument sourceDocument, JsonNode umcNode, Document outputDocument) throws IOException {
		String umcId = umcNode.get("umcID").asText();
//		String umcName = umcNode.get("umcName").asText();

		StringBuilder urlBuilder = new StringBuilder(baseURL);

		urlBuilder.append("umc/get?");
		urlBuilder.append("uid="+umcId);
		
		ClientResource client = 
				new ClientResource(Context.getCurrent(), Method.GET, urlBuilder.toString());

		client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, user, pass);
		
		Representation umcRepresentation = client.get(); 

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		umcRepresentation.write(buffer);

		TagManager tagManager = new TagManager();
		
		TeiUserMarkupCollectionSerializationHandler teiUserMarkupCollectionSerializationHandler = 
				new TeiUserMarkupCollectionSerializationHandler(tagManager, false);
		
		UserMarkupCollection umc = teiUserMarkupCollectionSerializationHandler.deserialize(
			umcId, new ByteArrayInputStream(buffer.toByteArray()));
		
		if (this.structureTagsetDefinition != null) {
			umc.getTagLibrary().add(structureTagsetDefinition);
		}
		
		CATAMarkupCollectionSerializationHandler cataMarkupCollectionSerializationHandler = 
				new CATAMarkupCollectionSerializationHandler(outputDocument, "CATMA_F5C057C0-5AC4-4DF8-9640-32C4E64F5ACA");
		
		cataMarkupCollectionSerializationHandler.serialize(umc, sourceDocument, null);
		if (this.structureTagsetDefinition == null) {
			this.structureTagsetDefinition = cataMarkupCollectionSerializationHandler.getStructureTagsetDefintion();
		}
	}



	public static void main(String[] args) {
		try {
			new TEICATAConverter().run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
