package de.catma.heureclea.corpuscleanup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
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
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentHandler;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.serialization.tei.TeiTagLibrarySerializationHandler;
import de.catma.serialization.tei.TeiUserMarkupCollectionSerializationHandler;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.tag.PropertyValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class CorpusCleaner {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String user;
	private String pass;
	private String baseURL;
	private String targetCid;
	
	private Set<String> validSourceDocNames = new HashSet<>();
	private Set<String> validConcepts = new HashSet<>();
	private Set<String> baseConcepts = new HashSet<>();
	
	private int annotatorNumber = 1;
	private Map<String,String> annotatorMap = new HashMap<>();
	private String tagsetUUID = new IDGenerator().generate();
	private Version tagsetVersion = new Version();
	private Table<String, String, UserMarkupCollection> collections = HashBasedTable.create();
	private int includedTagInstanceCount = 0;
	private int excludedTagInstanceCount = 0;
	private int inputCollectionCount = 0;
	private int outputCollectionCount = 0;
	private int sourceDocCount = 0;
	private TagsetDefinition masterTagsetDefinition;
	private String targetDir;
	private String publisher = "heureCLÉA http://heureclea.de/";
	private String author = "Evelyn Gius, Janina Jacke, Jan Christoph Meister, Marco Petris";
	
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
		loadFromFile(args[3], validSourceDocNames);
		loadFromFile(args[4], validConcepts);
		loadBaseConcepts();
		loadMasterTagsetDefinition(args[5]);
		
		
		List<Pair<String,String>> corpusIdentities = new ArrayList<>();
		corpusIdentities.add(new Pair<>("2812","2825")); //compared
		corpusIdentities.add(new Pair<>("2818","2823")); //uncompared
		
		for (Pair<String,String> corpusIdentity : corpusIdentities) {
			collections = HashBasedTable.create();
			includedTagInstanceCount = 0;
			excludedTagInstanceCount = 0;
			inputCollectionCount = 0;
			outputCollectionCount = 0;
			sourceDocCount = 0;
			
			String cid = corpusIdentity.getFirst();
			targetCid = corpusIdentity.getSecond();
			
			targetDir = args[6];
			if (targetDir.endsWith("/")) {
				targetDir = targetDir.substring(0, targetDir.length()-1);
			}
			targetDir += "_" + targetCid; 
			
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
			
			for (JsonNode sourceDocNode : contentsNode) {
				handleSourceDocNode(sourceDocNode); 
				sourceDocCount++;
			}
			
			logger.info("finished with " + includedTagInstanceCount + " instances");
			logger.info("excluded instance count: " + excludedTagInstanceCount);
			logger.info("input sourcedocs/collections: " + sourceDocCount + "/" + inputCollectionCount);
			logger.info("output collections: " + outputCollectionCount);
		}
	}	
	
	private void loadMasterTagsetDefinition(String path) throws IOException {
		try (FileInputStream fis = new FileInputStream(path)) {
			TeiTagLibrarySerializationHandler teiTagLibrarySerializationHandler = 
					new TeiTagLibrarySerializationHandler(new TagManager());
			TagLibrary tagLibrary = teiTagLibrarySerializationHandler.deserialize(null, fis);
			masterTagsetDefinition = tagLibrary.iterator().next();
		}
		
	}


	private void loadBaseConcepts() {
		for (String concept : new String[] {
				"/Time Tagset/time",
				"/Time Tagset/narrative_levels",
				"/Time Tagset/timerelation_discours--histoire",
				"/Time Tagset/disagreement_approved"}) {
			baseConcepts.add(concept);
		}
		
		logger.info("base concepts: " + baseConcepts);
	}


	private void loadFromFile(String fileName, Set<String> container) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (FileInputStream fis = new FileInputStream(fileName)) {
			IOUtils.copy(fis, buffer);
		}
		
		String[] names = buffer.toString("UTF-8").split(",");
		for (String name : names) {
			if (!name.trim().isEmpty()) {
				container.add(name.trim());
			}
		}
		
	}

	private void handleSourceDocNode(JsonNode sourceDocNode) throws IOException {

		String sourceDocId = sourceDocNode.get("sourceDocID").asText();
		String sourceDocName = sourceDocNode.get("sourceDocName").asText();
		if (isValidSourceDoc(sourceDocName)) {
			
			JsonNode umcListNode = sourceDocNode.get("umcList");
			Set<String> handledCollections = new HashSet<>();
			for (JsonNode umcNode : umcListNode) {
				String umcId = umcNode.get("umcID").asText();

				if (!handledCollections.contains(umcId)) { //prevent duplicates
					handleUmcNode(sourceDocId, sourceDocName, umcNode);
					handledCollections.add(umcId);
					
					inputCollectionCount++;
				}
			}
			
			try {
				for (UserMarkupCollection targetUmc : collections.values()) {
					TeiUserMarkupCollectionSerializationHandler teiUserMarkupCollectionSerializationHandler = 
							new TeiUserMarkupCollectionSerializationHandler(new TagManager(), false);
					uploadUmc(teiUserMarkupCollectionSerializationHandler, targetUmc, sourceDocId);
					outputCollectionCount++;
				}
				
				collections.clear();
				
			} catch (ExecutionException e) {
				throw new IOException(e);
			}
			
		}
	}


	private void handleUmcNode(String sourceDocId, String sourceDocName, JsonNode umcNode) throws IOException {
		String umcId = umcNode.get("umcID").asText();
		String umcName = umcNode.get("umcName").asText();

		if (umcName.toUpperCase().contains("UIMA")) {
			logger.info("skipping UIMA collection " + umcName);
		}
		else {
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
		
		if (umc.getTagReferences().isEmpty()) {
			logger.info("Collection is empty, skipping " + umc);
			return;
		}
		
		String annotator = null;
		try {
			annotator = umc.toString().substring(0, umc.toString().indexOf(" "));
		}
		catch (Exception e) {
			logger.severe("not a valid umc, skipping " + umc);
			return;
		}

		logger.info("annotator: " +  annotator);
		
		TagsetDefinition sourceTagsetDefinition = umc.getTagLibrary().getTagsetDefinition(
				masterTagsetDefinition.getUuid());
				
		if (sourceTagsetDefinition == null) {
			logger.severe("not a valid umc TagsetDefinition is not present, skipping " + umc);
			return;
		}
		
		logger.info("synching " + umc);
		tagManager.synchronize(
			sourceTagsetDefinition,
			masterTagsetDefinition);
			
		umc.synchronizeTagInstances();
		
		
		logger.info("copying relevant tag references");
		
		TagLibrary tagLibrary = umc.getTagLibrary();
		TagLibrary targetLib = new TagLibrary(null, tagLibrary.getName());
		
		Set<String> includedInstances = new HashSet<>();
		Set<String> excludedInstances = new HashSet<>();
		TagsetDefinition targetTagsetDefinition = 
			new TagsetDefinition(
				null, tagsetUUID, "heureCLÉA time tagset", tagsetVersion);
		
		copyTagDefs(targetTagsetDefinition, tagLibrary);
		targetLib.add(targetTagsetDefinition);
		
		List<UserMarkupCollection> affectedCollections = new ArrayList<>();
		
		List<TagReference> diagreementApprTagRefs = new ArrayList<>();
		
		for (TagReference tagReference : umc.getTagReferences()) {
			
			TagDefinition tagDefinition = tagReference.getTagDefinition();
			String path = tagLibrary.getTagPath(tagDefinition);
			
			if (isValidPath(path, validConcepts)) {
				
				if (path.equals("/Time Tagset/disagreement_approved")) {
					diagreementApprTagRefs.add(tagReference);
				}
				else {
					String conceptName = getConcept(path);
					
					TagInstance tagInstance = tagReference.getTagInstance();
					PropertyDefinition propertyDefinition = 
						tagDefinition.getPropertyDefinitionByName(
								SystemPropertyName.catma_markupauthor.name());
					
					String annotatorAnonym = getAnnotatorAnonym(annotator);
					
					UserMarkupCollection targetUmc = getTargetUmc(
							conceptName, umc.getContentInfoSet(), 
							targetLib, annotator, annotatorAnonym, sourceDocName);
					
					//anonymizing authors
					tagInstance.getProperty(
						propertyDefinition.getUuid()).setPropertyValueList(
								new PropertyValueList(annotatorAnonym));
					
					targetUmc.addTagReference(tagReference);
					includedInstances.add(tagReference.getTagInstanceID());
				
					affectedCollections.add(targetUmc);
				}
			}		
			else {
				excludedInstances.add(tagReference.getTagInstanceID());
			}
			
		}
		
		for (TagReference tagReference : diagreementApprTagRefs) {
			TagDefinition tagDefinition = tagReference.getTagDefinition();
			
			TagInstance tagInstance = tagReference.getTagInstance();
			PropertyDefinition propertyDefinition = 
				tagDefinition.getPropertyDefinitionByName(
						SystemPropertyName.catma_markupauthor.name());
			
			String annotatorAnonym = getAnnotatorAnonym(annotator);
			
			//anonymizing authors
			tagInstance.getProperty(
				propertyDefinition.getUuid()).setPropertyValueList(
						new PropertyValueList(annotatorAnonym));

			for (UserMarkupCollection targetUmc : affectedCollections) {
				targetUmc.addTagReference(tagReference);
				includedInstances.add(tagReference.getTagInstanceID()+targetUmc.getId());
			}
		}
		
		logger.info("number of included instances: " + includedInstances.size());
		logger.info("number of excluded instances: " + excludedInstances.size());		
		logger.info("annotators " + annotatorMap.toString());
		
		includedTagInstanceCount += includedInstances.size();
		excludedTagInstanceCount += excludedInstances.size();
	}


	private void copyTagDefs(TagsetDefinition targetTsDef, TagLibrary tagLibrary) {
		TagsetDefinition sourceTsDef = tagLibrary.getTagsetDefinitions().iterator().next();
		
		for (TagDefinition tagDef : sourceTsDef) {
			String path = tagLibrary.getTagPath(tagDef);
			if (isValidPath(path, baseConcepts)) {
				
				if (isBasePath(path, baseConcepts)) {
					TagDefinition tagDefCopy = new TagDefinition(null, tagDef.getUuid(), tagDef.getName(), tagDef.getVersion(), null, null);
					for (PropertyDefinition pd : tagDef.getSystemPropertyDefinitions()) {
						tagDefCopy.addSystemPropertyDefinition(new PropertyDefinition(pd));
					}
					for (PropertyDefinition pd : tagDef.getUserDefinedPropertyDefinitions()) {
						tagDefCopy.addUserDefinedPropertyDefinition(new PropertyDefinition(pd));
					}	
					targetTsDef.addTagDefinition(tagDefCopy);
				}
				else {
					targetTsDef.addTagDefinition(tagDef);
				}
			}
		}
		
	}


	private boolean isBasePath(String path, Set<String> baseConcepts) {
		
		for (String validPath : baseConcepts) {
			if (path.equals(validPath)) {
				return true;
			}
		}
		return false;
	}


	private String getConcept(String path) {
		
		for (String validPath : validConcepts) {
			if (path.startsWith(validPath)) {
				return validPath.substring(validPath.lastIndexOf("/")+1);
			}
		}

		
		throw new IllegalArgumentException("concept for " + path + " not found!");
	}


	private String getAnnotatorAnonym(String ident) {
		String anno = annotatorMap.get(ident);
		if (anno == null) {
			anno = "Annotator" + annotatorNumber;
			annotatorNumber++;
			annotatorMap.put(ident, anno);
			logger.info("Mapping " + ident +"-->"+anno);
		}
		
		return anno;
	}


	private UserMarkupCollection getTargetUmc(
			String conceptName, ContentInfoSet contentInfoSet, TagLibrary targetLib,
			String annotator, String annotatorAnonym, String sourceDocName) {
		
		UserMarkupCollection targetCollection = collections.get(annotator, conceptName);
		
		if (targetCollection == null) {
			targetCollection = 
					new UserMarkupCollection(
							null, new ContentInfoSet(contentInfoSet), targetLib);
			targetCollection.getContentInfoSet().setTitle(
					annotatorAnonym + " " + sourceDocName + " " + conceptName);
			targetCollection.getContentInfoSet().setAuthor(author);
			targetCollection.getContentInfoSet().setDescription(
					conceptName + " annotations, source collection: " + contentInfoSet.getTitle());
			targetCollection.getContentInfoSet().setPublisher(publisher);
			collections.put(annotator, conceptName, targetCollection);
			logger.info("creating Collection " + targetCollection + " for " + annotator + "/" + annotatorAnonym);
		}
		
		return targetCollection;
	}


	private void uploadUmc(
		TeiUserMarkupCollectionSerializationHandler teiUserMarkupCollectionSerializationHandler, 
		UserMarkupCollection targetUmc, 
		String sourceDocId) throws IOException, ExecutionException {
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		teiUserMarkupCollectionSerializationHandler.serialize(
				targetUmc, sourceDocumentCache.get(sourceDocId), buffer);	
		
		new File(targetDir).mkdirs();
		
		File file = new File(targetDir, targetUmc.getContentInfoSet().getTitle() + ".xml");
		if (file.exists()) {
			throw new IllegalStateException("file already exists: " + file); 
		}
		try (FileOutputStream fos = new FileOutputStream(file)) {
			IOUtils.copy(new ByteArrayInputStream(buffer.toByteArray()), fos);
			logger.info("file " + targetUmc + " successful" ); 
		}
		
		StringBuilder urlBuilder = new StringBuilder(baseURL);

		urlBuilder.append("umc/add?");
		urlBuilder.append("sid="+sourceDocId);
		urlBuilder.append("&cid="+targetCid);
		int tries = 100;
		int curTry =1;
		while (curTry <= tries) {
			
			ClientResource client = 
					new ClientResource(Context.getCurrent(), Method.POST, urlBuilder.toString());
			try {
				client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, user, pass);
		
				client.post(buffer.toString("UTF-8"), MediaType.APPLICATION_XML);
				
				Status status = client.getStatus();
				
				if (!status.isSuccess()) {
					throw new IOException(status.toString());
				}
				else {
					logger.info("upload " + targetUmc + " successful" ); 
				}
				return;
			}
			catch(Exception e) {
				logger.log(Level.SEVERE, "error posting umc " +  targetUmc + " try " + curTry, e);
				curTry++;
			}
			finally {
				try {
					client.release();
				}
				catch (Exception e2) {
					logger.log(Level.SEVERE, "error releasing client", e2);
				}
			}
		}
		
		throw new IllegalStateException("couldn't upload " + targetUmc + " giving up!");
	}


	private boolean isValidPath(String path, Set<String> concepts) {
		for (String validPath : concepts) {
			if (path.startsWith(validPath)) {
				return true;
			}
		}
		return false;
	}


	private boolean isValidSourceDoc(String sourceDocName) {
		return validSourceDocNames.contains(sourceDocName.trim());
	}


	public static void main(String[] args) {
		try {
			new CorpusCleaner().run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
