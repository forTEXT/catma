package de.catma.indexer.graph;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.repository.db.FileURLFactory;
import de.catma.repository.db.jooqgen.catmarepository.Tables;
import de.catma.repository.db.mapper.SourceDocumentMapper;

public class SourceDocumentBatchInserter {
	
	public SourceDocumentBatchInserter() {
		
	}

	public void insert(String graphdbPath) throws IOException, PropertyVetoException {
		
		String propertiesFile = 
				System.getProperties().containsKey("prop") ? System.getProperties().getProperty(
				"prop") : "catma.properties";

		Properties catmaProperties = new Properties();

		catmaProperties.load(
			new FileInputStream(propertiesFile));

		List<SourceDocument> resultlist = getSourceDocuments(catmaProperties);

		Map<String, String> graphConfig = new HashMap<>();
		graphConfig.put(
			"neostore.nodestore.db.mapped_memory", 
			catmaProperties.getProperty("neostore.nodestore.db.mapped_memory"));
		graphConfig.put(
				"neostore.relationshipstore.db.mapped_memory", 
				catmaProperties.getProperty("neostore.relationshipstore.db.mapped_memory"));
		graphConfig.put(
				"neostore.propertystore.db.mapped_memory", 
				catmaProperties.getProperty("neostore.propertystore.db.mapped_memory"));
		graphConfig.put(
				"neostore.propertystore.db.strings.mapped_memory", 
				catmaProperties.getProperty("neostore.propertystore.db.strings.mapped_memory"));
		graphConfig.put(
				"neostore.propertystore.db.arrays.mapped_memory", 
				catmaProperties.getProperty("neostore.propertystore.db.arrays.mapped_memory"));
		
		graphConfig.put("neostore.propertystore.db.index.keys.mapped_memory",
				catmaProperties.getProperty("neostore.propertystore.db.index.keys.mapped_memory", "5M"));
		graphConfig.put("neostore.propertystore.db.index.mapped_memory",
				catmaProperties.getProperty("neostore.propertystore.db.index.mapped_memory", "5M"));
		
		graphConfig.put(
				"dump_configuration", 
				catmaProperties.getProperty("dump_configuration", "true"));
		graphConfig.put(
				"cache_type",
				catmaProperties.getProperty("cache_type", "none"));
		graphConfig.put(
				"use_memory_mapped_buffers",
				catmaProperties.getProperty("use_memory_mapped_buffers", "true"));
		
		BatchInserter inserter = 
				BatchInserters.inserter(graphdbPath, graphConfig);
		
		long totalNodeCount = 0;
		long totalRelCount = 0;
		
		int totalSourceDocCount = resultlist.size();
		System.out.println(totalSourceDocCount + " sourcedocs need indexing");
		int currentSourceDocCount = 0;
		try {
			for (SourceDocument sourceDocument : resultlist) {
				currentSourceDocCount++;
				long currentNodeCount = 0;
				long currentRelCount = 0;
				try {
					System.out.println("indexing " + sourceDocument);
					List<String> unseparableCharacterSequences = 
							sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
								.getIndexInfoSet().getUnseparableCharacterSequences();
					List<Character> userDefinedSeparatingCharacters = 
							sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
								.getIndexInfoSet().getUserDefinedSeparatingCharacters();
					Locale locale = 
							sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
							.getIndexInfoSet().getLocale();
					System.out.println("term extraction " + sourceDocument);

					TermExtractor termExtractor = 
							new TermExtractor(
								sourceDocument.getContent(), 
								unseparableCharacterSequences, 
								userDefinedSeparatingCharacters, 
								locale);
					
					Map<String, List<TermInfo>> terms = termExtractor.getTerms();
			
					Map<String, Object> properties = new HashMap<String, Object>();
					
					System.out.println("node creation " + sourceDocument);

					properties.put(SourceDocumentProperty.localUri.name(), sourceDocument.getID());
					properties.put(SourceDocumentProperty.title.name(), sourceDocument.toString());
					
					long sdNode = inserter.createNode(properties, NodeType.SourceDocument);
					currentNodeCount++;
					
					properties.clear();
					Map<TermInfo,Long> termInfoToNodeId = new HashMap<TermInfo, Long>();
					TreeSet<TermInfo> orderedTermInfos = new TreeSet<TermInfo>(TermInfo.TOKENOFFSETCOMPARATOR);
					
					for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
						String term = entry.getKey();
						List<TermInfo> termInfos = entry.getValue();
						
						properties.put(TermProperty.literal.name(), term);
						properties.put(TermProperty.freq.name(), termInfos.size());
						
						long termNodeId = inserter.createNode(properties, NodeType.Term);
						currentNodeCount++;
						
						properties.clear();
						
						for (TermInfo ti : termInfos) {
							orderedTermInfos.add(ti);
							
							properties.put(
									PositionProperty.position.name(), ti.getTokenOffset());
							properties.put(
									PositionProperty.start.name(), ti.getRange().getStartPoint());
							properties.put(
									PositionProperty.end.name(), ti.getRange().getEndPoint());
							properties.put(
									PositionProperty.literal.name(), ti.getTerm());
							
							long positionNodeId = 
									inserter.createNode(properties, NodeType.Position);
							currentNodeCount++;
							
							termInfoToNodeId.put(ti, positionNodeId);
							inserter.createRelationship(
									termNodeId, 
									positionNodeId, 
									NodeRelationType.HAS_POSITION, 
									Collections.<String,Object>emptyMap());
							currentRelCount++;
							inserter.createRelationship(
									termNodeId, 
									sdNode, 
									NodeRelationType.IS_PART_OF, 
									Collections.<String, Object>emptyMap());
							currentRelCount++;
						}
					}
			
					
					
					TermInfo prevTi = null;
					properties.clear();
					
					for (TermInfo ti : orderedTermInfos) {
						if (prevTi != null) {
							inserter.createRelationship(
								termInfoToNodeId.get(prevTi), 
								termInfoToNodeId.get(ti), 
								NodeRelationType.ADJACENT_TO, 
								Collections.<String, Object>emptyMap());
							currentRelCount++;
						}
						prevTi = ti;
					}
				}
				catch (Exception e) {
					System.out.println(
						"Could not index doc " 
								+ sourceDocument.getID() + " " 
								+ sourceDocument.toString());
					e.printStackTrace();
				}
				totalNodeCount += currentNodeCount;
				totalRelCount += currentRelCount;
				System.out.println(
					currentSourceDocCount + "/" 
							+ totalSourceDocCount + " node creation finished, nodes " 
							+ currentNodeCount + " rels " + currentRelCount 
							+ " total nodes " + totalNodeCount
							+ " total rels " + totalRelCount);
			}
			System.out.println("createDeferredSchemaIndex SourceDoc.localUri");
			inserter.createDeferredSchemaIndex(NodeType.SourceDocument)
				.on(SourceDocumentProperty.localUri.name()).create();
			
			System.out.println("createDeferredSchemaIndex Term.literal");
			inserter.createDeferredSchemaIndex(NodeType.Term)
				.on(TermProperty.literal.name()).create();
	
			System.out.println("createDeferredSchemaIndex Term.freq");
			inserter.createDeferredSchemaIndex(NodeType.Term)
				.on(TermProperty.freq.name()).create();
			
			System.out.println("batch finished");
		}
		finally {
			inserter.shutdown();
		}
	}
	
	private List<SourceDocument> getSourceDocuments(Properties catmaProperties) throws PropertyVetoException {
	int repoIndex = 1; // assume that the first configured repo is the local db repo
		
		String user = RepositoryPropertyKey.RepositoryUser.getProperty(
				catmaProperties, repoIndex);
				
		String pass = RepositoryPropertyKey.RepositoryPass.getProperty(
				catmaProperties, repoIndex);
		
		String url = RepositoryPropertyKey.RepositoryUrl.getProperty(
				catmaProperties, repoIndex);

		String sourceDocsPath = RepositoryPropertyKey.RepositoryFolderPath.getProperty(
				catmaProperties, repoIndex) + "/" + 
						FileURLFactory.SOURCEDOCS_FOLDER + "/";
		
		ComboPooledDataSource cpds = new ComboPooledDataSource();
		
		cpds.setDriverClass( "org.gjt.mm.mysql.Driver" ); //loads the jdbc driver 
		cpds.setJdbcUrl(url); 
		cpds.setUser(user);
		cpds.setPassword(pass); 
		cpds.setIdleConnectionTestPeriod(10);

		DSLContext db = DSL.using(cpds, SQLDialect.MYSQL);

		Map<Integer, Result<Record>> userDefSepCharsRecords = db
		.select()
		.from(Tables.USERDEFINED_SEPARATINGCHARACTER)
		.fetchGroups(Tables.USERDEFINED_SEPARATINGCHARACTER.SOURCEDOCUMENTID);
		
		Map<Integer, Result<Record>> unseparableCharSeqRecords = db 
		.select()
		.from(Tables.UNSEPARABLE_CHARSEQUENCE)
		.fetchGroups(Tables.UNSEPARABLE_CHARSEQUENCE.SOURCEDOCUMENTID);
		
		Map<Integer, Result<Record>> userMarkupCollectionRecords = db
		.select()
		.from(Tables.USERMARKUPCOLLECTION)
		.fetchGroups(Tables.USERMARKUPCOLLECTION.SOURCEDOCUMENTID);
		
		List<SourceDocument> resultlist = db
		.select()
		.from(Tables.SOURCEDOCUMENT)
		.fetch()
		.map(new SourceDocumentMapper(
				sourceDocsPath, 
				userDefSepCharsRecords, unseparableCharSeqRecords,
				userMarkupCollectionRecords));
	
		cpds.close();
		
		return resultlist;
	}

	public static void main(String[] args) {
		try {
			if ((args == null) || (args.length != 1)) {
				System.out.println("Please give graphdb path as argument!");
				return;
			}
			
			File graphdbpath = new File(args[0]);
			if (!graphdbpath.exists() || !graphdbpath.isDirectory()) {
				System.out.println("graphdb path must exist as a directory!");
				return;
			}
			
			new SourceDocumentBatchInserter().insert(graphdbpath.getAbsolutePath());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

