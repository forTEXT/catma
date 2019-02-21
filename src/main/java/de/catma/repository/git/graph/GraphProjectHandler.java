package de.catma.repository.git.graph;

import static de.catma.repository.git.graph.NodeType.AnnotationProperty;
import static de.catma.repository.git.graph.NodeType.DeletedProperty;
import static de.catma.repository.git.graph.NodeType.DeletedTag;
import static de.catma.repository.git.graph.NodeType.MarkupCollection;
import static de.catma.repository.git.graph.NodeType.Position;
import static de.catma.repository.git.graph.NodeType.Project;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
import static de.catma.repository.git.graph.NodeType.Property;
import static de.catma.repository.git.graph.NodeType.SourceDocument;
import static de.catma.repository.git.graph.NodeType.Tag;
import static de.catma.repository.git.graph.NodeType.TagInstance;
import static de.catma.repository.git.graph.NodeType.Tagset;
import static de.catma.repository.git.graph.NodeType.Term;
import static de.catma.repository.git.graph.NodeType.User;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasCollection;
import static de.catma.repository.git.graph.RelationType.hasDocument;
import static de.catma.repository.git.graph.RelationType.hasInstance;
import static de.catma.repository.git.graph.RelationType.hasParent;
import static de.catma.repository.git.graph.RelationType.hasPosition;
import static de.catma.repository.git.graph.RelationType.hasProject;
import static de.catma.repository.git.graph.RelationType.hasProperty;
import static de.catma.repository.git.graph.RelationType.hasRevision;
import static de.catma.repository.git.graph.RelationType.hasTag;
import static de.catma.repository.git.graph.RelationType.hasTagset;
import static de.catma.repository.git.graph.RelationType.isPartOf;
import static de.catma.repository.git.graph.RelationType.rt;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.driver.internal.value.NullValue;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.types.Node;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.SchedulerRepository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import de.catma.document.Range;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.project.ProjectReference;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.graph.indexer.SourceDocumentIndexerJob;
import de.catma.repository.git.graph.indexer.SourceDocumentIndexerJob.DataField;
import de.catma.repository.neo4j.SessionRunner;
import de.catma.repository.neo4j.StatementExcutor;
import de.catma.repository.neo4j.ValueContainer;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.user.User;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;

public class GraphProjectHandler {

	private Logger logger = Logger.getLogger(GraphProjectHandler.class.getName());
	private User user;
	private ProjectReference projectReference;
	private FileInfoProvider fileInfoProvider;
	private IDGenerator idGenerator;
	
	public GraphProjectHandler(
			ProjectReference projectReference, 
			User user, FileInfoProvider fileInfoProvider) {
		this.projectReference = projectReference;
		this.user = user;
		this.fileInfoProvider = fileInfoProvider;
		this.idGenerator = new IDGenerator();
	}

	public void ensureProjectRevisionIsLoaded(
			String revisionHash, 
			Supplier<Stream<SourceDocument>> sourceDocumentsSupplier,
			Supplier<Stream<UserMarkupCollectionReference>> collectionsSupplier) throws Exception {
		ValueContainer<Boolean> revisionExists = new ValueContainer<>();
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(p:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]-> "
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRevisionHash}}) "
					+ " RETURN p.projectId ", 
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRevisionHash", revisionHash
					)
				);
				
				revisionExists.setValue(statementResult.hasNext());
			}
		});
		
		if (!revisionExists.getValue()) {
			//TODO: implement load
			
			StatementExcutor.execute(new SessionRunner() {
				@Override
				public void run(Session session) throws Exception {

					session.run(
						"MERGE (u:"+nt(User)+"{userId:{pUserId}})"
						+ "MERGE (p:"+nt(Project)+"{projectId:{pProjectId}})"
						+ "MERGE (u)-[:"+rt(hasProject)+"]->(p)"
						+ " MERGE (p) "
						+ " -[:"+rt(hasRevision)+"]-> "
						+ "(pr:"+nt(ProjectRevision)+"{revisionHash:{pRevisionHash}}) ", 
						Values.parameters(
							"pUserId", user.getIdentifier(),
							"pProjectId", projectReference.getProjectId(),
							"pRevisionHash", revisionHash
						)
					);
					
					sourceDocumentsSupplier.get().forEach(sourceDocument -> {
						SourceDocumentInfo info = 
							sourceDocument.getSourceContentHandler().getSourceDocumentInfo();
						session.run(
							" MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
							+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
							+ "(pr:"+nt(ProjectRevision)+"{revisionHash:{pRevisionHash}}) "
							+ " MERGE (pr)-[:"+rt(hasDocument)+"]->"
							+ "(:"+nt(SourceDocument)+"{"
							+ "sourceDocumentId:{pSourceDocumentId}, "
							+ "revisionHash:{pSourceDocumentRevisionHash}, "
							+ "locale:{pLocale}, "
							+ "publisher:{pPublisher}, "
							+ "author:{pAuthor}, "
							+ "description:{pDescription},"
							+ "title:{pTitle}, "
							+ "checksum:{pChecksum}, "
							+ "fileOSType:{pFileOSType}, "
							+ "indexCompleted:{pIndexCompleted}"
							+ "}) ",
							Values.parameters(
								"pUserId", user.getIdentifier(),
								"pProjectId", projectReference.getProjectId(),
								"pRevisionHash", revisionHash,
								"pSourceDocumentId", sourceDocument.getID(),
								"pSourceDocumentRevisionHash", sourceDocument.getRevisionHash(),
								"pLocale", info.getIndexInfoSet().getLocale().toString(),
								"pPublisher", info.getContentInfoSet().getPublisher(),
								"pAuthor", info.getContentInfoSet().getAuthor(),
								"pDescription", info.getContentInfoSet().getDescription(),
								"pTitle", info.getContentInfoSet().getTitle(),
								"pChecksum", info.getTechInfoSet().getChecksum(),
								"pFileOSType", info.getTechInfoSet().getFileOSType().name(),
								"pIndexCompleted", false
							)
						);
//						try {
//							scheduleSourceDocumentForIndexing(
//									revisionHash, 
//									sourceDocument, 
//									fileInfoProvider.getTokenizedSourceDocumentPath(sourceDocument.getID()));
//						} catch (Exception e) {
//							throw new RuntimeException(e);
//						}
					});
					
					collectionsSupplier.get().forEach(collectionReference -> {
						
						session.run(
							"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
							+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
							+ "(pr:"+nt(ProjectRevision)+"{revisionHash:{pRevisionHash}})-[:"+rt(hasDocument)+"]->"
							+ "(s:"+nt(SourceDocument)+"{sourceDocumentId:{pSourceDocumentId}}) "
							+ "MERGE (s)-[:"+rt(hasCollection)+"]->"
							+ "(:"+nt(MarkupCollection)+"{"
							+ "collectionId:{pCollectionId},"
							+ "revisionHash:{pUmcRevisionHash},"
							+ "name:{pName}"
							+ "})",
							Values.parameters(
								"pUserId", user.getIdentifier(),
								"pProjectId", projectReference.getProjectId(),
								"pRevisionHash", revisionHash,
								"pSourceDocumentId", collectionReference.getSourceDocumentId(),
								"pCollectionId", collectionReference.getId(),
								"pUmcRevisionHash", collectionReference.getRevisionHash(),
								"pName", collectionReference.getName()
							)
						);
					});
				}
			});
			
		}
		
	}

	public void addSourceDocument(
			String oldRootRevisionHash, String rootRevisionHash, SourceDocument sourceDocument, 
			Path tokenizedSourceDocumentPath) throws Exception {
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				SourceDocumentInfo info = sourceDocument.getSourceContentHandler().getSourceDocumentInfo();
				
				session.run(
					" MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
						+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
						+ "(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}}) "
					+ " SET pr.revisionHash = {pRootRevisionHash}"
					+ " WITH pr "
					+ " MERGE (pr)-[:"+rt(hasDocument)+"]->"
							+ "(:"+nt(SourceDocument)+"{"
								+ "sourceDocumentId:{pSourceDocumentId}, "
								+ "revisionHash:{pSourceDocumentRevisionHash}, "
								+ "locale:{pLocale}, "
								+ "publisher:{pPublisher}, "
								+ "author:{pAuthor}, "
								+ "description:{pDescription},"
								+ "title:{pTitle}, "
								+ "checksum:{pChecksum}, "
								+ "fileOSType:{pFileOSType}, "
								+ "indexCompleted:{pIndexCompleted}"
							+ "}) ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pOldRootRevisionHash", oldRootRevisionHash,
						"pRootRevisionHash", rootRevisionHash,
						"pSourceDocumentId", sourceDocument.getID(),
						"pSourceDocumentRevisionHash", sourceDocument.getRevisionHash(),
						"pLocale", info.getIndexInfoSet().getLocale().toString(),
						"pPublisher", info.getContentInfoSet().getPublisher(),
						"pAuthor", info.getContentInfoSet().getAuthor(),
						"pDescription", info.getContentInfoSet().getDescription(),
						"pTitle", info.getContentInfoSet().getTitle(),
						"pChecksum", info.getTechInfoSet().getChecksum(),
						"pFileOSType", info.getTechInfoSet().getFileOSType().name(),
						"pIndexCompleted", false
					)
				);

			}
			
		});

		scheduleSourceDocumentForIndexing(rootRevisionHash, sourceDocument, tokenizedSourceDocumentPath);
	}

	private void scheduleSourceDocumentForIndexing(
			String rootRevisionHash, SourceDocument sourceDocument, Path tokenizedSourceDocumentPath) throws Exception {
		String projectId = projectReference.getProjectId();
		
        SchedulerRepository schedRep = SchedulerRepository.getInstance();
    	
        Scheduler scheduler = schedRep.lookup("CATMAQuartzScheduler");
        
        JobDataMap jobDataMap = new JobDataMap();
        
        jobDataMap.put(
        	DataField.title.name(), 
        	sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getTitle());
        jobDataMap.put(DataField.userId.name(), user.getIdentifier());
        jobDataMap.put(DataField.projectId.name(), projectId);
		jobDataMap.put(DataField.rootRevisionHash.name(), rootRevisionHash);
		jobDataMap.put(DataField.sourceDocumentId.name(), sourceDocument.getID());
		jobDataMap.put(DataField.sourceDocumentRevisionHash.name(), sourceDocument.getRevisionHash());
		jobDataMap.put(DataField.tokenizedSourceDocumentPath.name(), tokenizedSourceDocumentPath.toString());

		String userName = user.getIdentifier();
		
    	JobDetail jobDetail = 
        	JobBuilder.newJob(SourceDocumentIndexerJob.class)
        	.withIdentity(
        			userName+"_" +projectId+"_"+rootRevisionHash+"_"+sourceDocument.getID()+"_"+sourceDocument.getRevisionHash(),
        			userName)
    		.setJobData(jobDataMap)
    		.build();
		
    	Trigger trigger = TriggerBuilder.newTrigger()
		    .withIdentity(userName+"_" +projectId+"_"+rootRevisionHash+"_"+sourceDocument.getID()+"_"+sourceDocument.getRevisionHash(), userName)
		    .startNow()
		    .build();
    	
    	scheduler.scheduleJob(jobDetail, trigger);
	}

	public int getSourceDocumentsCount(String rootRevisionHash) throws Exception {
		final int[] result = {0};
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
						"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"+""
								+ "(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
								+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
								+ "(s:"+nt(SourceDocument)+") "
								+ "RETURN count(s) as counter ",
						Values.parameters(
								"pUserId", user.getIdentifier(),
								"pProjectId", projectReference.getProjectId(),
								"pRootRevisionHash", rootRevisionHash
						)
				);

				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					Value value = record.get("counter");
					result[0] = value.asInt();
				}
			}

		});
		return result[0];
	}

	public Collection<SourceDocument> getSourceDocuments(String rootRevisionHash) throws Exception {
		ArrayList<SourceDocument> sourceDocuments = new ArrayList<>();
		
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"+""
					+ "(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(s:"+nt(SourceDocument)+") "
					+ "OPTIONAL MATCH (s)-[:"+rt(hasCollection)+"]->(c:"+nt(MarkupCollection)+") "
					+ "RETURN s, COLLECT(c) as collections ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash
					)
				);

				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					SourceDocument sourceDocument = createSourceDocument(record.get("s"));
					record.get("collections")
						.asList(collectionValue -> createUserMarkupCollectionReference(
							collectionValue.asNode(), 
							sourceDocument.getID(), 
							sourceDocument.toString())
						).forEach(
								umcRef -> 
								sourceDocument.addUserMarkupCollectionReference(umcRef));

					sourceDocuments.add(sourceDocument);
				}
			}
			
		});

		return sourceDocuments;
	}

	private SourceDocument createSourceDocument(Value sourceDocumentValue) throws Exception {
		String sourceDocumentId = sourceDocumentValue.get("sourceDocumentId").asString();
		String locale = sourceDocumentValue.get("locale").asString();
		String author = sourceDocumentValue.get("author").asString();
		String description = sourceDocumentValue.get("description").asString();
		String publisher = sourceDocumentValue.get("publisher").asString();
		String title = sourceDocumentValue.get("title").asString();
		Long checksum = sourceDocumentValue.get("checksum").equals(NullValue.NULL)?null:sourceDocumentValue.get("checksum").asLong();
		FileOSType fileOSType = FileOSType.valueOf(sourceDocumentValue.get("fileOSType").asString());
		
		SourceContentHandler contentHandler = new StandardContentHandler();
		TechInfoSet techInfoSet =
				new TechInfoSet(
						FileType.TEXT, 
						StandardCharsets.UTF_8, 
						fileOSType, 
						checksum);
		
		techInfoSet.setURI(fileInfoProvider.getSourceDocumentFileURI(sourceDocumentId));

		SourceDocumentInfo sourceDocumentInfo = 
				new SourceDocumentInfo(
					new IndexInfoSet(
						Collections.emptyList(), //TODO
						Collections.emptyList(), //TODO
						Locale.forLanguageTag(locale)), 
					new ContentInfoSet(author, description, publisher, title), 
					techInfoSet);
		contentHandler.setSourceDocumentInfo(sourceDocumentInfo);
		
		return new SourceDocument(sourceDocumentId, contentHandler);
	}

	public SourceDocument getSourceDocument(String rootRevisionHash, String sourceDocumentId) throws Exception {
		ValueContainer<SourceDocument> sourceDocumentContainer = new ValueContainer<>();
		
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(s:"+nt(SourceDocument)+"{sourceDocumentId:{pSourceDocumentId}}) "
					+ "OPTIONAL MATCH (s)-[:"+rt(hasCollection)+"]->(c:"+nt(MarkupCollection)+") "
					+ "RETURN s, COLLECT(c) as collections ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pSourceDocumentId", sourceDocumentId
					)
				);

				if (statementResult.hasNext()) {
					Record record = statementResult.next();
					
					SourceDocument sourceDocument = createSourceDocument(record.get("s"));
					record.get("collections")
						.asList(collectionValue -> createUserMarkupCollectionReference(
							collectionValue.asNode(), 
							sourceDocument.getID(), 
							sourceDocument.toString())
						).forEach(
								umcRef -> 
								sourceDocument.addUserMarkupCollectionReference(umcRef));

					sourceDocumentContainer.setValue(sourceDocument);
				}
			}
			
		});

		return sourceDocumentContainer.getValue();
	}

	public void addUserMarkupCollection(
		String rootRevisionHash,
		String collectionId, String name, String umcRevisionHash, SourceDocument sourceDocument, String oldRootRevisionHash) throws Exception {

		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}}) "
					+ "SET pr.revisionHash = {pRootRevisionHash} "
					+ "WITH pr "
					+ "MATCH (pr)-[:"+rt(hasDocument)+"]->"
					+ "(s:"+nt(SourceDocument)+"{sourceDocumentId:{pSourceDocumentId}}) "
					+ "MERGE (s)-[:"+rt(hasCollection)+"]->"
					+ "(:"+nt(MarkupCollection)+"{"
						+ "collectionId:{pCollectionId},"
						+ "revisionHash:{pUmcRevisionHash},"
						+ "name:{pName}"
					+ "})",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pOldRootRevisionHash", oldRootRevisionHash,
						"pRootRevisionHash", rootRevisionHash,
						"pSourceDocumentId", sourceDocument.getID(),
						"pCollectionId", collectionId,
						"pUmcRevisionHash", umcRevisionHash,
						"pName", name
					)
				);
			}
		});
	}

	public void addTagset(String rootRevisionHash, TagsetDefinition tagsetDefinition, String oldRootRevisionHash) throws Exception {
		String tagsetId = tagsetDefinition.getUuid();
		String tagsetRevisionHash = tagsetDefinition.getRevisionHash();
		String name = tagsetDefinition.getName();
		String description = ""; //TODO
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}}) "
					+ "SET pr.revisionHash = {pRootRevisionHash} "
					+ "WITH pr "
					+ "MERGE (pr)-[:"+rt(hasTagset)+"]->"
					+ "(:"+nt(Tagset)+"{"
						+ "tagsetId:{pTagsetId},"
						+ "revisionHash:{pTagsetRevisionHash},"
						+ "name:{pName},"
						+ "description:{pDescription}"
					+ "})",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pOldRootRevisionHash", oldRootRevisionHash,
						"pRootRevisionHash", rootRevisionHash,
						"pTagsetId", tagsetId,
						"pTagsetRevisionHash", tagsetRevisionHash,
						"pName", name,
						"pDescription", description
					)
				);
			}
		});
	}
	
	public void addTagDefinition(String rootRevisionHash, TagDefinition tagDefinition,
			TagsetDefinition tagsetDefinition, String oldRootRevisionHash) throws Exception {
		
		if (tagDefinition.getPropertyDefinition(PropertyDefinition.SystemPropertyName.catma_markupauthor.name()) == null) {
			PropertyDefinition authorPropertyDefinition = 
					new PropertyDefinition(
						idGenerator.generate(),
						PropertyDefinition.SystemPropertyName.catma_markupauthor.name(),
						Collections.singleton(user.getIdentifier()));
			tagDefinition.addSystemPropertyDefinition(authorPropertyDefinition);
		}
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+"(ts:"+nt(Tagset)+"{tagsetId:{pTagsetId}}) "
					+"SET pr.revisionHash = {pRootRevisionHash} "
					+"SET ts.revisionHash = {pTagsetRevisionHash} "
					+"WITH ts "
					+"MERGE (ts)-[:"+rt(hasTag)+"]->"
					+"(t:"+nt(Tag)+"{"
						+"tagId:{pTagId},"
						+"name:{pName},"
						+"color:{pColor},"
						+"author:{pAuthor},"
						+"creationDate:{pCreationDate},"
						+"modifiedDate:{pModifiedDate}"
					+"}) "
					+(tagDefinition.getParentUuid()==null?"": "WITH ts, t ")
					+"MATCH (ts)-[:"+rt(hasTag)+"]->(parent:"+nt(Tag)+"{tagId:{parentTagId}}) "
					+"MERGE (t)-[:"+rt(hasParent)+"]->(parent) ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pOldRootRevisionHash", oldRootRevisionHash,
						"pTagsetId", tagsetDefinition.getUuid(),
						"pTagsetRevisionHash", tagsetDefinition.getRevisionHash(),
						"pTagId", tagDefinition.getUuid(),
						"pColor", ColorConverter.toHex(Integer.valueOf(tagDefinition.getColor())),
						"pName", tagDefinition.getName(),
						"pAuthor", tagDefinition.getAuthor(),
						"pCreationDate", ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
						"pModifiedDate", tagDefinition.getVersion().toString(), //TODO: change to java.util.time
						"parentTagId", tagDefinition.getParentUuid()
					)
				);
				
				for (PropertyDefinition propertyDefinition : tagDefinition.getUserDefinedPropertyDefinitions()) {
					session.run(
							"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
							+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
							+"(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
							+"(ts:"+nt(Tagset)+")-[:"+rt(hasTag)+"]->"
							+"(t:"+nt(Tag)+"{tagId:{pTagId}}) "
							+"SET pr.revisionHash = {pRootRevisionHash} "
							+"SET ts.revisionHash = {pTagsetRevisionHash} "
							+"MERGE (t)-[:"+rt(hasProperty)+"]->"
							+"(:"+nt(Property)+"{"
								+ "uuid:{pUuid},"
								+ "name:{pName},"
								+ "values:{pValues} "
							+"})",
							Values.parameters(
								"pUserId", user.getIdentifier(),
								"pProjectId", projectReference.getProjectId(),
								"pRootRevisionHash", rootRevisionHash,
								"pOldRootRevisionHash", oldRootRevisionHash,
								"pTagsetRevisionHash", tagsetDefinition.getRevisionHash(),
								"pTagId", tagDefinition.getUuid(),
								"pUuid", propertyDefinition.getUuid(),
								"pName", propertyDefinition.getName(),
								"pValues", propertyDefinition.getPossibleValueList()
							)
						);					
				}
			}
		});
		
	}

	public void updateTagDefinition(
			String rootRevisionHash, TagDefinition tagDefinition, 
			TagsetDefinition tagsetDefinition, String oldRootRevisionHash) throws Exception {
		
		if (tagDefinition.getAuthor() == null) {
			PropertyDefinition authorPropertyDefinition = 
					new PropertyDefinition(
						idGenerator.generate(),
						PropertyDefinition.SystemPropertyName.catma_markupauthor.name(),
						Collections.singleton(user.getIdentifier()));
			tagDefinition.addSystemPropertyDefinition(authorPropertyDefinition);
		}
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+"(ts:"+nt(Tagset)+")-[:"+rt(hasTag)+"]->"
					+"(t:"+nt(Tag)+"{tagId:{pTagId}}) "
					+"SET pr.revisionHash = {pRootRevisionHash} "
					+"SET ts.revisionHash = {pTagsetRevisionHash} "
					+"SET "
					+"t.name={pName}, "
					+"t.color={pColor}, "
					+"t.modifiedDate={pModifiedDate}",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pOldRootRevisionHash", oldRootRevisionHash,
						"pTagsetRevisionHash", tagsetDefinition.getRevisionHash(),
						"pTagId", tagDefinition.getUuid(),
						"pColor", ColorConverter.toHex(Integer.valueOf(tagDefinition.getColor())),
						"pName", tagDefinition.getName(),
						"pModifiedDate", tagDefinition.getVersion().toString() //TODO: change to java.util.time
					)
				);
				
				for (PropertyDefinition propertyDefinition : tagDefinition.getUserDefinedPropertyDefinitions()) {
					createOrUpdatePropertyDefinition(
						session, 
						rootRevisionHash, 
						propertyDefinition, 
						tagDefinition, 
						tagsetDefinition, 
						oldRootRevisionHash);					
				}
				
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+"(:"+nt(Tagset)+"{tagsetId:{pTagsetId}})-[:"+rt(hasTag)+"]->"
					+"(:"+nt(Tag)+"{tagId:{pTagId}})-[:"+rt(hasProperty)+"]->"
					+"(p:"+nt(Property)+") "
					+"WHERE NOT(p.uuid in {pPropertyIdList}) "
					+"REMOVE p:"+nt(Property)+ " "
					+"SET p:"+nt(DeletedProperty)+ " ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pTagsetId", tagsetDefinition.getUuid(),
						"pTagId", tagDefinition.getUuid(),
						"pPropertyIdList", tagDefinition.getUserDefinedPropertyDefinitions()
							.stream()
							.map(PropertyDefinition::getUuid)
							.collect(Collectors.toList()),
						"pTagsetRevisionHash", tagsetDefinition.getRevisionHash()
					)
				);				
			}
		});
		
	}

	public List<UserMarkupCollectionReference> getUserMarkupCollectionReferences(String rootRevisionHash, int offset, int limit) throws Exception {
		List<UserMarkupCollectionReference> collectionContainer = Lists.newArrayList();
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(s:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
					+ "(c:"+nt(MarkupCollection)+") "
					+ "RETURN s.sourceDocumentId, s.title, c.collectionId, c.revisionHash, c.name SKIP {pOffset} LIMIT {pLimit} ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pOffset", offset,
						"pLimit", limit
					)
				);
				
				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					
					collectionContainer.add(
						createUserMarkupCollectionReference(
							record.get("c.collectionId"), 
							record.get("c.revisionHash"), 
							record.get("c.name"),
							record.get("s.sourceDocumentId"),
							record.get("s.title")));
				}
			}
		});
		
		
		return collectionContainer;
	}

	private UserMarkupCollectionReference createUserMarkupCollectionReference(
			Node userMarkupCollectionNode, String sourceDocumentId, String sourceDocumentTitle) {
			
		return new UserMarkupCollectionReference(
				userMarkupCollectionNode.get("collectionId").asString(), 
				userMarkupCollectionNode.get("revisionHash").asString(), 
				new ContentInfoSet(userMarkupCollectionNode.get("name").asString()),
				sourceDocumentId);
	}
	private UserMarkupCollectionReference createUserMarkupCollectionReference(
		Value idValue, Value revisionHashValue, Value nameValue, 
		Value sourceDocumentIdValue, Value sourceDocumentTitleValue) {
		
		return new UserMarkupCollectionReference(
				idValue.asString(), 
				revisionHashValue.asString(), 
				new ContentInfoSet(nameValue.asString()),
				sourceDocumentIdValue.asString());
	}

	public int getUserMarkupCollectionReferenceCount(String rootRevisionHash) throws Exception {
		ValueContainer<Integer> countContainer = new ValueContainer<>(0);
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
					+ "(c:"+nt(MarkupCollection)+") "
					+ "RETURN count(c) as umcCount ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash
					)
				);
				
				if (statementResult.hasNext()) {
					countContainer.setValue(statementResult.next().get("umcCount").asInt());
				}
			}
		});

		return countContainer.getValue();
	}

	public Collection<TagsetDefinition> getTagsets(String rootRevisionHash) throws Exception {
		ArrayList<TagsetDefinition> tagsets = new  ArrayList<>();
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+ "(ts:"+nt(Tagset)+") "
					+ "OPTIONAL MATCH (ts)-[:"+rt(hasTag)+"]->(t:"+nt(Tag)+") "
					+ "OPTIONAL MATCH (ts)-[:"+rt(hasTag)+"]->(dt:"+nt(DeletedTag)+") "
					+ "OPTIONAL MATCH (t)-[:"+rt(hasProperty)+"]->(p:"+nt(Property)+") "
					+ "OPTIONAL MATCH (t)-[:"+rt(hasParent)+"]->(pt:"+nt(Tag)+") "
					+ "WITH ts, dt, t, pt, COLLECT(p) as properties "
					+ "RETURN ts, COLLECT([t, properties, pt.tagId]) as tags, COLLECT(dt.tagId) as deletedTags ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash
					)
				);
				
				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					
					TagsetDefinition tagsetDefinition = 
						createTagset(
							record.get("ts").asNode(), 
							record.get("deletedTags").asList(deletedTag -> deletedTag.asString()));
					
					List<Object> tagValuePairs = record.get("tags").asList();
					
					for (Object tagValuePair : tagValuePairs) {
						@SuppressWarnings("unchecked")
						List<Object> tagDefTripleList = (List<Object>)tagValuePair;
						Node tagNode = (Node)tagDefTripleList.get(0);
						if (tagNode != null) {
							@SuppressWarnings("unchecked")
							List<Node> propertyNodes = (List<Node>)tagDefTripleList.get(1);
							String parentTagId = (tagDefTripleList.get(2)==null)?null:tagDefTripleList.get(2).toString();
							TagDefinition tagDefinition = 
								createTag(tagNode, parentTagId, tagsetDefinition.getUuid(), propertyNodes);
							tagsetDefinition.addTagDefinition(tagDefinition);
						}
					}

					tagsets.add(tagsetDefinition);
				}
			}
		});
		
		
		return tagsets;
	}

	public int getTagsetsCount(String rootRevisionHash) throws Exception {
		final int[] result = {0};

		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
						"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
								+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
								+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
								+ "(ts:"+nt(Tagset)+") "
								+ "RETURN count(ts) as counter",
						Values.parameters(
								"pUserId", user.getIdentifier(),
								"pProjectId", projectReference.getProjectId(),
								"pRootRevisionHash", rootRevisionHash
						)
				);

				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					Value value = record.get("counter");
					result[0] = value.asInt();
				}
			}
		});
		return result[0];
	}

	private PropertyDefinition createPropertyDefinition(Node propertyNode) {
	
		String uuid = propertyNode.get("uuid").asString();
		String name = propertyNode.get("name").asString();
		List<String> values = propertyNode.get("values").asList(value -> value.asString());
		
		
		return new PropertyDefinition(uuid, name, values);
	}

	private TagsetDefinition createTagset(Node tagsetNode, List<String> deletedTagUuids) {

		String tagsetId = tagsetNode.get("tagsetId").asString();
		String name = tagsetNode.get("name").asString();
		String revisionHash = tagsetNode.get("revisionHash").asString();
		
		TagsetDefinition tagsetDefinition = 
			new TagsetDefinition(
				null, tagsetId, name, new Version(), new HashSet<>(deletedTagUuids)); //TODO:
		tagsetDefinition.setRevisionHash(revisionHash);
		
		return tagsetDefinition;
	}

	private TagDefinition createTag(Node tagNode, String parentTagId, String tagsetUuid, Collection<Node> properties) {
		
		String tagId = tagNode.get("tagId").asString();
		String name = tagNode.get("name").asString();
		String color = tagNode.get("color").asString();
		String modifiedDate = tagNode.get("modifiedDate").asString();
		String author = tagNode.get("author").asString();
		
		TagDefinition tagDef = new TagDefinition(null, tagId, name, new Version(modifiedDate), null, parentTagId, tagsetUuid);
		tagDef.addSystemPropertyDefinition(
			new PropertyDefinition(
				idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_displaycolor.name()),
				PropertyDefinition.SystemPropertyName.catma_displaycolor.name(),
				Collections.singleton(ColorConverter.toRGBIntAsString(color))));
		tagDef.addSystemPropertyDefinition(
			new PropertyDefinition(
				idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_markupauthor.name()),
				PropertyDefinition.SystemPropertyName.catma_markupauthor.name(),
				Collections.singleton(author)));		
		
		for (Node propertyNode : properties) {
			tagDef.addUserDefinedPropertyDefinition(createPropertyDefinition(propertyNode));
		}
		
		return tagDef;
	}

	public void addPropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition, TagsetDefinition tagsetDefinition, String oldRootRevisionHash) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+"(ts:"+nt(Tagset)+")-[:"+rt(hasTag)+"]->"
					+"(t:"+nt(Tag)+"{tagId:{pTagId}}) "
					+"SET pr.revisionHash = {pRootRevisionHash} "
					+"SET ts.revisionHash = {pTagsetRevisionHash} "
					+"MERGE (t)-[:"+rt(hasProperty)+"]->"
					+"(:"+nt(Property)+"{"
						+ "uuid:{pUuid},"
						+ "name:{pName},"
						+ "values:{pValues} "
					+"})",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pOldRootRevisionHash", oldRootRevisionHash,
						"pTagsetRevisionHash", tagsetDefinition.getRevisionHash(),
						"pTagId", tagDefinition.getUuid(),
						"pUuid", propertyDefinition.getUuid(),
						"pName", propertyDefinition.getName(),
						"pValues", propertyDefinition.getPossibleValueList()
					)
				);
			}
		});		
	}
	
	public void createOrUpdatePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition, TagsetDefinition tagsetDefinition, String oldRootRevisionHash) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				createOrUpdatePropertyDefinition(
					session, 
					rootRevisionHash, 
					propertyDefinition, 
					tagDefinition, 
					tagsetDefinition, 
					oldRootRevisionHash);
			}
		});		
	}
	
	private void createOrUpdatePropertyDefinition(Session session, String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition, TagsetDefinition tagsetDefinition, String oldRootRevisionHash) throws Exception {
		session.run(
				"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
				+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
				+"(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
				+"(ts:"+nt(Tagset)+")-[:"+rt(hasTag)+"]->"
				+"(t:"+nt(Tag)+"{tagId:{pTagId}})"
				+"SET pr.revisionHash = {pRootRevisionHash} "
				+"SET ts.revisionHash = {pTagsetRevisionHash} "
				+"WITH t "
				+"MERGE (t)-[:"+rt(hasProperty)+"]->"
				+ "(p:"+nt(Property)+"{uuid:{pUuid}}) "
				+"ON CREATE SET p.name = {pName}, p.values={pValues} "
				+"ON MATCH SET p.name = {pName}, p.values={pValues} ",
				Values.parameters(
					"pUserId", user.getIdentifier(),
					"pProjectId", projectReference.getProjectId(),
					"pRootRevisionHash", rootRevisionHash,
					"pOldRootRevisionHash", oldRootRevisionHash,
					"pTagsetRevisionHash", tagsetDefinition.getRevisionHash(),
					"pTagId", tagDefinition.getUuid(),
					"pUuid", propertyDefinition.getUuid(),
					"pName", propertyDefinition.getName(),
					"pValues", propertyDefinition.getPossibleValueList()
				)
			);		
	}

	public UserMarkupCollection getUserMarkupCollection(String rootRevisionHash,
			final TagLibrary tagLibrary, UserMarkupCollectionReference userMarkupCollectionReference) throws Exception {
		
		UserMarkupCollection userMarkupCollection = 
			new UserMarkupCollection(
				userMarkupCollectionReference.getId(), 
				userMarkupCollectionReference.getContentInfoSet(), 
				tagLibrary,
				userMarkupCollectionReference.getSourceDocumentId());
		
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+"(s:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
					+"(c:"+nt(MarkupCollection)+"{collectionId:{pCollectionId}})-[:"+rt(hasInstance)+"]->"
					+"(ti:"+nt(TagInstance)+")<-[:"+rt(hasInstance)+"]-"
					+"(td:"+nt(Tag)+")<-[:"+rt(hasTag)+"]-"
					+"(tsd:"+nt(Tagset)+") "
					+"OPTIONAL MATCH (ti)-[:"+rt(hasProperty)+"]->(ap:"+nt(AnnotationProperty)+") "
					+"RETURN s.sourceDocumentId, ti, td.tagId, tsd.tagsetId, COLLECT(ap) as properties ",
					Values.parameters(
							"pUserId", user.getIdentifier(),
							"pProjectId", projectReference.getProjectId(),
							"pRootRevisionHash", rootRevisionHash,
							"pCollectionId", userMarkupCollection.getId()
						)
					);

				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					
					String sourceDocumentId = record.get("s.sourceDocumentId").asString();
					Node tagInstanceNode = record.get("ti").asNode();
					String tagDefinitionId = record.get("td.tagId").asString();
					String tagsetId = record.get("tsd.tagsetId").asString();
					List<Node> properties =  record.get("properties").asList(value -> value.asNode());

					TagDefinition tagDefinition = 
						tagLibrary.getTagsetDefinition(tagsetId).getTagDefinition(tagDefinitionId);
					
					List<TagReference> tagReferences = 
						createTagReferences(
							tagDefinition, tagInstanceNode, 
							userMarkupCollection.getId(), sourceDocumentId, properties);

					userMarkupCollection.addTagReferences(tagReferences);
				}
				
			}
		});
		
		return userMarkupCollection;
	}

	public void addTagReferences(String rootRevisionHash, UserMarkupCollection userMarkupCollection,
			List<TagReference> tagReferences) throws Exception {
		final ArrayListMultimap<TagInstance, Range> tagInstancesAndRanges = ArrayListMultimap.create();
		
		tagReferences.forEach(tagReference -> {
			tagInstancesAndRanges.put(tagReference.getTagInstance(), tagReference.getRange());
		});
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				
				for (TagInstance ti : tagInstancesAndRanges.keys()) {
					List<Range> ranges = tagInstancesAndRanges.get(ti);
					List<Integer> flatRanges = 
						ranges
						.stream()
						.sorted()
						.flatMap(range -> Stream.of(range.getStartPoint(), range.getEndPoint()))
						.collect(Collectors.toList());
					
					
					if (ti.getAuthor() == null) {
						ti.setAuthor(user.getIdentifier());
					}
					
					session.run(
						"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
						+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
						+ "(pr:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
						+ "(:"+nt(Tagset)+")-[:"+rt(hasTag)+"]->"
						+ "(t:"+nt(Tag)+"{tagId:{pTagId}}), "
						+ "(pr)-[:"+rt(hasDocument)+"]->"
						+ "(:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
						+ "(c:"+nt(MarkupCollection)+"{collectionId:{pCollectionId}}) "
						+ "WITH t, c "
						+ "MERGE (t)-[:"+rt(hasInstance)+"]->"
						+ "(:"+nt(TagInstance)+"{"
						+ "tagInstanceId:{pTagInstanceId},"
						+ "author:{pAuthor}, "
						+ "creationDate:{pCreationDate}, "
						+ "ranges:{pRanges} "
						+ "})<-[:"+rt(hasInstance)+"]-(c)",
						Values.parameters(
							"pUserId", user.getIdentifier(),
							"pProjectId", projectReference.getProjectId(),
							"pRootRevisionHash", rootRevisionHash,
							"pTagId", ti.getTagDefinitionId(),
							"pCollectionId", userMarkupCollection.getId(),
							"pTagInstanceId", ti.getUuid(),
							"pAuthor", ti.getAuthor(),
							"pCreationDate",  ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
							"pRanges", flatRanges
						)
					);
					
					updateProperties(
						session, rootRevisionHash, 
						ti, ti.getUserDefinedProperties());
				}
				
			}
		});		
		
	}

	public void removeTagReferences(String rootRevisionHash, UserMarkupCollection userMarkupCollection,
			List<TagReference> tagReferences) throws Exception {
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
					+ "(:"+nt(MarkupCollection)+"{collectionId:{pCollectionId}})-[:"+rt(hasInstance)+"]->"
					+ "(ti:"+nt(TagInstance)+") "
					+ "WHERE ti.tagInstanceId in {pTagInstanceIdList} "
					+ "OPTIONAL MATCH (ti)-[:"+rt(hasProperty)+"]->(ap:"+nt(AnnotationProperty)+") "
					+ "DETACH DELETE ti, ap ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pCollectionId", userMarkupCollection.getId(),
						"pTagInstanceIdList", 
							tagReferences
							.stream()
							.map(ti -> ti.getTagInstance().getUuid())
							.distinct()
							.collect(Collectors.toList())
					)
				);
			}
		});			
	}

	private List<TagReference> createTagReferences(
			TagDefinition tagDefinition, Node tagInstanceNode, 
			String collectionId, String sourceDocumentId, List<Node> properties) throws Exception {
		String author = tagInstanceNode.get("author").asString();
		String timestamp = tagInstanceNode.get("creationDate").asString();
		

		TagInstance tagInstance = new TagInstance(
				tagInstanceNode.get("tagInstanceId").asString(), 
				tagDefinition.getUuid(),
				author, timestamp, 
				tagDefinition.getUserDefinedPropertyDefinitions(),
				tagDefinition.getTagsetDefinitionUuid());
		
		for (Node propertyNode : properties) {
			tagInstance.addUserDefinedProperty(createAnnotationProperty(propertyNode));
		}
		
		List<Integer> rangeOffsets = 
			tagInstanceNode.get("ranges").asList(value -> value.asInt());
		
		List<TagReference> tagReferenceList= Lists.newArrayList();
		
		
		String sourceDocumentUri = String.format(
				"http://catma.de/gitlab/%s/%s/%s", //TODO:
				projectReference.getProjectId(),
				GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME,
				sourceDocumentId
		);
		
		for (int startOffsetIdx = 0; startOffsetIdx<rangeOffsets.size()-1; startOffsetIdx+=2) {
			Range range = new Range(rangeOffsets.get(startOffsetIdx), rangeOffsets.get(startOffsetIdx+1));
			TagReference tagReference = new TagReference(
					tagInstance, sourceDocumentUri, range, collectionId);
			tagReferenceList.add(tagReference);
		}
		
		return tagReferenceList;
	}

	private Property createAnnotationProperty(Node propertyNode) {
		
		String uuid = propertyNode.get("uuid").asString();
		
		List<String> values = propertyNode.get("values").asList(value -> value.asString());
		
		return new Property(uuid, values);
	}
	
	public void removeProperties(
			String rootRevisionHash,
			String collectionId, String collectionRevisionHash, 
			String propertyDefId) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
					+ "(c:"+nt(MarkupCollection)+"{collectionId:{pCollectionId}}) "
					+ "SET c.revisionHash = {pCollectionRevisionHash} "
					+ "WITH c "
					+ "MATCH (c)-[:"+rt(hasInstance)+"]->(:"+nt(TagInstance)+")"
					+ "-[:"+rt(hasProperty)+"]->(ap:"+nt(AnnotationProperty)+"{uuid:{pPropertyDefId}}) "
					+ "DETACH DELETE ap ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pCollectionId", collectionId,
						"pCollectionRevisionHash", collectionRevisionHash,
						"pPropertyDefId", propertyDefId
					)
				);
		
			}
		});	
	}

	public void updateProperties(
			String rootRevisionHash, TagInstance tagInstance, Collection<Property> properties) throws Exception {		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				updateProperties(session, rootRevisionHash, tagInstance, properties);
			}
		});
	}

	private void updateProperties(Session session, String rootRevisionHash, TagInstance tagInstance,
			Collection<Property> properties) {
		for (Property property : properties) {
			session.run(
				"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
				+ "(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
				+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
				+ "(:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
				+ "(:"+nt(MarkupCollection)+")-[:"+rt(hasInstance)+"]->"
				+ "(i:"+nt(TagInstance)+"{tagInstanceId:{pTagInstanceId}})"
				+ "WITH i "
				+ "MERGE (i)-[:"+rt(hasProperty)+"]->"
				+ "(ap:"+nt(AnnotationProperty)+"{uuid:{pUuid}})"
				+ "ON CREATE SET ap.values={pValues} "
				+ "ON MATCH SET ap.values={pValues} ",
				Values.parameters(
					"pUserId", user.getIdentifier(),
					"pProjectId", projectReference.getProjectId(),
					"pRootRevisionHash", rootRevisionHash,
					"pTagInstanceId", tagInstance.getUuid(),
					"pUuid", property.getPropertyDefinitionId(),
					"pValues", property.getPropertyValueList()
				)
			);
		}
	}

	public Multimap<String, String> getAnnotationIdsByCollectionId(
			String rootRevisionHash, TagDefinition tagDefinition) throws Exception {
		final Multimap<String, String> annotationIdsByCollectionId = 
				HashMultimap.create();
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
					+ "(c:"+nt(MarkupCollection)+")-[:"+rt(hasInstance)+"]->"
					+ "(i:"+nt(TagInstance)+")"
					+ "<-[:"+rt(hasInstance)+"]-"+"(:"+nt(Tag)+"{tagId:{pTagId}})"
					+ "RETURN c.collectionId, i.tagInstanceId "
					,
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pTagId", tagDefinition.getUuid()
					)
				);
				
				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					String collectionId = record.get("c.collectionId").asString();
					String tagInstanceId = record.get("i.tagInstanceId").asString();
					annotationIdsByCollectionId.put(collectionId, tagInstanceId);
				}
		
			}
		});
		return annotationIdsByCollectionId;
	}

	public Multimap<String, TagReference> getTagReferencesByCollectionId(
			String rootRevisionHash, PropertyDefinition propertyDefinition, TagLibrary tagLibrary) throws Exception {
		final Multimap<String, TagReference> annotationIdsByCollectionId = 
				HashMultimap.create();
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+"(s:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
					+"(c:"+nt(MarkupCollection)+")-[:"+rt(hasInstance)+"]->"
					+"(ti:"+nt(TagInstance)+")<-[:"+rt(hasInstance)+"]-"
					+"(td:"+nt(Tag)+")<-[:"+rt(hasTag)+"]-"
					+"(tsd:"+nt(Tagset)+"), "
					+"(ti)-[:"+rt(hasProperty)+"]->(ap:"+nt(AnnotationProperty)+"{uuid:{pPropertyDefId}}) "
					+"RETURN s.sourceDocumentId, c.collectionId, ti, td.tagId, tsd.tagsetId, COLLECT(ap) as properties ",
					Values.parameters(
							"pUserId", user.getIdentifier(),
							"pProjectId", projectReference.getProjectId(),
							"pRootRevisionHash", rootRevisionHash,
							"pPropertyDefId", propertyDefinition.getUuid()
						)
					);

				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					
					String sourceDocumentId = record.get("s.sourceDocumentId").asString();
					String collectionId = record.get("c.collectionId").asString();
					Node tagInstanceNode = record.get("ti").asNode();
					String tagDefinitionId = record.get("td.tagId").asString();
					String tagsetId = record.get("tsd.tagsetId").asString();
					List<Node> properties =  record.get("properties").asList(value -> value.asNode());

					TagDefinition tagDefinition = 
						tagLibrary.getTagsetDefinition(tagsetId).getTagDefinition(tagDefinitionId);
					
					List<TagReference> tagReferences = 
						createTagReferences(
							tagDefinition, tagInstanceNode, 
							collectionId, sourceDocumentId, properties);

					annotationIdsByCollectionId.putAll(collectionId, tagReferences);
				}
				
			}
		});
		return annotationIdsByCollectionId;
	}
	
	public void removeTagInstances(
			String rootRevisionHash, String collectionId, Collection<String> tagInstanceIds, 
			String collectionRevisionHash) throws Exception{
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
					+ "(c:"+nt(MarkupCollection)+"{collectionId:{pCollectionId}})-[:"+rt(hasInstance)+"]->"
					+ "(i:"+nt(TagInstance)+") "
					+ "WHERE i.tagInstanceId IN {pTagInstanceIds} "
					+ "OPTIONAL MATCH (i)-[:"+rt(hasProperty)+"]->"
					+ "(ap:"+nt(AnnotationProperty)+") "
					+ "SET c.revisionHash = {pCollectionRevisionHash} "
					+ "DETACH DELETE i, ap"
					,
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pCollectionId", collectionId,
						"pTagInstanceIds", tagInstanceIds,
						"pCollectionRevisionHash", collectionRevisionHash
					)
				);
		
			}
		});
		
	}

	public void removeTagDefinition(String rootRevisionHash, TagDefinition tagDefinition,
			TagsetDefinition tagsetDefinition) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+"(ts:"+nt(Tagset)+"{tagsetId:{pTagsetId}})-[:"+rt(hasTag)+"]->"
					+"(t:"+nt(Tag)+"{tagId:{pTagId}}) "
					+"OPTIONAL MATCH (t)-[:"+rt(hasProperty)+"]->(p:"+nt(Property)+") "
					+"OPTIONAL MATCH (t)-[:"+rt(hasProperty)+"]->(dp:"+nt(DeletedProperty)+") "
					+"SET ts.revisionHash = {pTagsetRevisionHash} "
					+"REMOVE t:"+nt(Tag)+ " "
					+"SET t:"+nt(DeletedTag)+ " "
					+"DETACH DELETE p, dp ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pTagsetId", tagsetDefinition.getUuid(),
						"pTagId", tagDefinition.getUuid(),
						"pTagsetRevisionHash", tagsetDefinition.getRevisionHash()
					)
				);
			}
		});			
	}

	public void updateProjectRevisionHash(String oldRootRevisionHash, String rootRevisionHash) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}}) "
					+"SET pr.revisionHash = {pRootRevisionHash} ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pOldRootRevisionHash", oldRootRevisionHash,
						"pRootRevisionHash", rootRevisionHash
					)
				);
			}
		});				
	}

	public void updateCollectionRevisionHash(String rootRevisionHash, UserMarkupCollectionReference ref) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(:"+nt(SourceDocument)+")-[:"+rt(hasCollection)+"]->"
					+ "(c:"+nt(MarkupCollection)+"{collectionId:{pCollectionId}}) "
					+ "SET c.revisionHash = {pCollectionRevisionHash} "
					,
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pCollectionId", ref.getId(),
						"pCollectionRevisionHash", ref.getRevisionHash()
					)
				);
		
			}
		});
		
	}

	public void removePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition, TagsetDefinition tagsetDefinition, String oldRootRevisionHash) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+"(ts:"+nt(Tagset)+"{tagsetId:{pTagsetId}})-[:"+rt(hasTag)+"]->"
					+"(:"+nt(Tag)+"{tagId:{pTagId}})-[:"+rt(hasProperty)+"]->"
					+"(p:"+nt(Property)+"{uuid:{pPropertyId}}) "
					+"SET ts.revisionHash = {pTagsetRevisionHash} "
					+"REMOVE p:"+nt(Property)+ " ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pTagsetId", tagsetDefinition.getUuid(),
						"pTagId", tagDefinition.getUuid(),
						"pPropertyId", propertyDefinition.getUuid(),
						"pTagsetRevisionHash", tagsetDefinition.getRevisionHash()
					)
				);
			}
		});		
	}

	public void removeTagset(String rootRevisionHash, TagsetDefinition tagsetDefinition, String oldRootRevisionHash) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+"(ts:"+nt(Tagset)+"{tagsetId:{pTagsetId}})"
					+"OPTIONAL MATCH (ts)-[:"+rt(hasTag)+"]->(t:"+nt(Tag)+"{tagId:{pTagId}}) "
					+"OPTIONAL MATCH (t)-[:"+rt(hasProperty)+"]->(p:"+nt(Property)+") "
					+"SET pr.revisionHash = {pRootRevisionHash} "
					+"DETACH DELETE ts, t, p ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pOldRootRevisionHash", oldRootRevisionHash,
						"pTagsetId", tagsetDefinition.getUuid()
					)
				);
			}
		});	
	}

	public void updateTagset(String rootRevisionHash, TagsetDefinition tagsetDefinition) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+"(ts:"+nt(Tagset)+"{tagsetId:{pTagsetId}}) "
					+"SET ts.revisionHash = {pTagsetRevisionHash}, "
					+"ts.name = {pName} ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pTagsetId", tagsetDefinition.getUuid(),
						"pTagsetRevisionHash", tagsetDefinition.getRevisionHash(),
						"pName", tagsetDefinition.getName()
					)
				);
			}
		});		
	}

	public void removeCollection(
			String rootRevisionHash, UserMarkupCollectionReference userMarkupCollectionReference,
			String oldRootRevisionHash) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+"(d:"+nt(SourceDocument)+"{sourceDocumentId:{pSourceDocumentId}})"+ "-[:"+rt(hasCollection)+"]->"
					+"(c:"+nt(MarkupCollection)+"{collectionId:{pCollectionId}}) "
					+"OPTIONAL MATCH (c)-[:"+rt(hasInstance)+"]->(ti:"+nt(TagInstance)+") "
					+"OPTIONAL MATCH (ti)-[:"+rt(hasProperty)+"]->(ap:"+nt(AnnotationProperty)+") "
					+"SET pr.revisionHash = {pRootRevisionHash} "
					+"DETACH DELETE ap, ti, c ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pOldRootRevisionHash", oldRootRevisionHash,
						"pSourceDocumentId", userMarkupCollectionReference.getSourceDocumentId(),
						"pCollectionId", userMarkupCollectionReference.getId()
					)
				);
			}
		});	
	}

	public void removeDocument(String rootRevisionHash, SourceDocument sourceDocument, String oldRootRevisionHash) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+"(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+"(d:"+nt(SourceDocument)+"{sourceDocumentId:{pSourceDocumentId}}) "
					+"OPTIONAL MATCH (d)<-[:"+rt(isPartOf)+"]-(t:"+nt(Term)+") "
					+"OPTIONAL MATCH (t)-[:"+rt(hasPosition)+"]->(ps:"+nt(Position)+") "
					+"SET pr.revisionHash = {pRootRevisionHash} "
					+"DETACH DELETE ps, t, d ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHash,
						"pOldRootRevisionHash", oldRootRevisionHash,
						"pSourceDocumentId", sourceDocument.getID()
					)
				);
			}
		});	
	}
}
