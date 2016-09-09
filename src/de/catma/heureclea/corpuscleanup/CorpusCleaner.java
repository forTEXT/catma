package de.catma.heureclea.corpuscleanup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentHandler;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.serialization.tei.TeiUserMarkupCollectionSerializationHandler;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.tag.PropertyValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;

public class CorpusCleaner {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String user;
	private String pass;
	private String baseURL;
	private String targetCid;
	
	private Set<String> validUmcNames = new HashSet<>();
	
	private LoadingCache<String, SourceDocument> sourceDocumentCache = 
			CacheBuilder.newBuilder()
			.maximumSize(2)
			.build(new CacheLoader<String, SourceDocument>() {
				public SourceDocument load(String key) throws Exception {
					
					StringBuilder urlBuilder = new StringBuilder(baseURL);
					urlBuilder.append("src/get?");
					urlBuilder.append("sid="+key);
					
					ClientResource client = 
							new ClientResource(Context.getCurrent(), Method.GET, urlBuilder.toString());

					client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, user, pass);
					
					Representation sourceDocRepresentation = client.get(); 

					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					sourceDocRepresentation.write(bos);
					StandardContentHandler contentHandler = new StandardContentHandler();
					TechInfoSet techInfoSet = 
						new TechInfoSet(FileType.TEXT, Charset.forName("UTF-8"), null, null, null);
					contentHandler.setSourceDocumentInfo(new SourceDocumentInfo(null, null, techInfoSet));
					contentHandler.load(new ByteArrayInputStream(bos.toByteArray()));
					
					SourceDocument sourceDocument = new SourceDocumentHandler().loadSourceDocument(key, contentHandler);
					
					return sourceDocument;
				};
			});

	public CorpusCleaner() throws Exception {
	}
	
	
	public void run(String[] args) throws Exception {
		baseURL = args[0];
		if (!baseURL.endsWith("/")) {
			baseURL += "/";
		}
		user = args[1];
		pass = args[2];
		loadValidUmcs(args[3]);
		
		String cid = "52";
		targetCid = "2774";
		
		StringBuilder urlBuilder = new StringBuilder(baseURL);
		
		//TODO: corpus create
		//TODO: corpus share
		
		urlBuilder.append("corpus/list?");
		urlBuilder.append("cid="+cid);
		
		ClientResource client = 
				new ClientResource(Context.getCurrent(), Method.GET, urlBuilder.toString());

		client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, user, pass);
		
		Representation corpusRepresentation = client.get(); 
		ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();
				;
		corpusRepresentation.write(jsonStream);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode corpusJson = 
			mapper.readValue(jsonStream.toString("UTF-8"), ObjectNode.class);

		JsonNode contentsNode = corpusJson.get("contents");
		
		for (JsonNode sourceDocNode : contentsNode) {
			handleSourceDocNode(sourceDocNode); 
		}
	}
	
	
	private void loadValidUmcs(String fileName) throws FileNotFoundException, IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (FileInputStream fis = new FileInputStream(fileName)) {
			IOUtils.copy(fis, buffer);
		}
		
		String[] names = buffer.toString("UTF-8").split(",");
		for (String name : names) {
			if (!name.trim().isEmpty()) {
				validUmcNames.add(name.trim());
			}
		}
		
	}


	private void handleSourceDocNode(JsonNode sourceDocNode) throws IOException {

		String sourceDocId = sourceDocNode.get("sourceDocID").asText();
		String sourceDocName = sourceDocNode.get("sourceDocName").asText();
		JsonNode umcListNode = sourceDocNode.get("umcList");
		
		for (JsonNode umcNode : umcListNode) {
			handleUmcNode(sourceDocId, sourceDocName, umcNode);
		}
		
		
	}


	private void handleUmcNode(String sourceDocId, String sourceDocName, JsonNode umcNode) throws IOException {
		String umcId = umcNode.get("umcID").asText();
		String umcName = umcNode.get("umcName").asText();
		
		if (isIncluded(umcName)) {
			handleUmc(sourceDocId, sourceDocName, umcId, umcName);
		}
	}


	private void handleUmc(String sourceDocId, String sourceDocName, String umcId, String umcName) throws IOException {
		logger.info("handling " + umcName);
		
		StringBuilder urlBuilder = new StringBuilder(baseURL);

		urlBuilder.append("umc/get?");
		urlBuilder.append("uid="+umcId);
		
		ClientResource client = 
				new ClientResource(Context.getCurrent(), Method.GET, urlBuilder.toString());

		client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, user, pass);
		
		logger.info("loading " + umcName);
		Representation umcRepresentation = client.get(); 

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		umcRepresentation.write(buffer);

//		System.out.println(buffer.toString("UTF-8"));
		
		logger.info("reading " + umcName);
		
		TagManager tagManager = new TagManager();
		
		TeiUserMarkupCollectionSerializationHandler teiUserMarkupCollectionSerializationHandler = 
				new TeiUserMarkupCollectionSerializationHandler(tagManager, false);
		
		UserMarkupCollection umc = teiUserMarkupCollectionSerializationHandler.deserialize(
			umcId, new ByteArrayInputStream(buffer.toByteArray()));
		
		logger.info("copying relevant tag references");
		
		TagLibrary tagLibrary = umc.getTagLibrary();
		TagLibrary targetLib = new TagLibrary(null, tagLibrary.getName());
		
		UserMarkupCollection targetUmc = 
			new UserMarkupCollection(
					null, new ContentInfoSet(umc.getContentInfoSet()), targetLib);
		targetUmc.getContentInfoSet().setTitle("myTestColl");
		
		Set<String> includedInstances = new HashSet<>();
		Set<String> excludedInstances = new HashSet<>();
		
		for (TagReference tagReference : umc.getTagReferences()) {
			
			TagDefinition tagDefinition = tagReference.getTagDefinition();
			String path = tagLibrary.getTagPath(tagDefinition);
			
			if (isValidPath(path)) {
				
				TagsetDefinition tagsetDefinition = tagLibrary.getTagsetDefinition(tagDefinition);
				if (!targetLib.contains(tagsetDefinition)) {
					targetLib.add(tagsetDefinition);
				}
				
				TagInstance tagInstance = tagReference.getTagInstance();
				PropertyDefinition propertyDefinition = 
					tagDefinition.getPropertyDefinitionByName(
							SystemPropertyName.catma_markupauthor.name());
				
				//anonymizing authors
				tagInstance.getProperty(
					propertyDefinition.getUuid()).setPropertyValueList(
							new PropertyValueList("Rabea"));
				
				targetUmc.addTagReference(tagReference);
				includedInstances.add(tagReference.getTagInstanceID());
			}		
			else {
				excludedInstances.add(tagReference.getTagInstanceID());
			}
			
		}
		logger.info("number of included instances: " + includedInstances.size());
		logger.info("number of excluded instances: " + excludedInstances.size());
		
		try {
			uploadUmc(teiUserMarkupCollectionSerializationHandler, targetUmc, sourceDocId);
		} catch (ExecutionException e) {
			throw new IOException(e);
		}
		
		
	}


	private void uploadUmc(
		TeiUserMarkupCollectionSerializationHandler teiUserMarkupCollectionSerializationHandler, 
		UserMarkupCollection targetUmc, 
		String sourceDocId) throws IOException, ExecutionException {
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		teiUserMarkupCollectionSerializationHandler.serialize(
				targetUmc, sourceDocumentCache.get(sourceDocId), buffer);	
		
		StringBuilder urlBuilder = new StringBuilder(baseURL);

		urlBuilder.append("umc/add?");
		urlBuilder.append("sid="+sourceDocId);
		urlBuilder.append("&cid="+targetCid);
		
		ClientResource client = 
				new ClientResource(Context.getCurrent(), Method.POST, urlBuilder.toString());

		client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, user, pass);

		client.post(buffer.toString("UTF-8"), MediaType.APPLICATION_XML);
		
		Status status = client.getStatus();
		
		if (!status.isSuccess()) {
			throw new IOException(status.toString());
		}
		else {
			logger.info("upload " + targetUmc + " successful" ); 
		}
	}


	private boolean isValidPath(String path) {
		return path.startsWith("/Time Tagset/time/dates");
	}


	private boolean isIncluded(String umcName) {
		return validUmcNames.contains(umcName.trim());
	}


	public static void main(String[] args) {
		try {
			new CorpusCleaner().run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
