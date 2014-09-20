package de.catma.indexer.graph;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.UNSEPARABLE_CHARSEQUENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERDEFINED_SEPARATINGCHARACTER;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;

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
		.from(USERDEFINED_SEPARATINGCHARACTER)
		.fetchGroups(USERDEFINED_SEPARATINGCHARACTER.SOURCEDOCUMENTID);
		
		Map<Integer, Result<Record>> unseparableCharSeqRecords = db 
		.select()
		.from(UNSEPARABLE_CHARSEQUENCE)
		.fetchGroups(UNSEPARABLE_CHARSEQUENCE.SOURCEDOCUMENTID);
		
		Map<Integer, Result<Record>> userMarkupCollectionRecords = db
		.select()
		.from(USERMARKUPCOLLECTION)
		.fetchGroups(USERMARKUPCOLLECTION.SOURCEDOCUMENTID);
		
		List<SourceDocument> resultlist = db
		.select()
		.from(SOURCEDOCUMENT)
		.fetch()
		.map(new SourceDocumentMapper(
				sourceDocsPath, 
				userDefSepCharsRecords, unseparableCharSeqRecords,
				userMarkupCollectionRecords));
	
		BatchInserter inserter = 
				BatchInserters.inserter(graphdbPath);
		try {
			for (SourceDocument sourceDocument : resultlist) {
			
				List<String> unseparableCharacterSequences = 
						sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
							.getIndexInfoSet().getUnseparableCharacterSequences();
				List<Character> userDefinedSeparatingCharacters = 
						sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
							.getIndexInfoSet().getUserDefinedSeparatingCharacters();
				Locale locale = 
						sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
						.getIndexInfoSet().getLocale();
				
				TermExtractor termExtractor = 
						new TermExtractor(
							sourceDocument.getContent(), 
							unseparableCharacterSequences, 
							userDefinedSeparatingCharacters, 
							locale);
				
				Map<String, List<TermInfo>> terms = termExtractor.getTerms();
		
				Map<String, Object> properties = new HashMap<String, Object>();
				
				
				properties.put(SourceDocumentProperty.localUri.name(), sourceDocument.getID());
				properties.put(SourceDocumentProperty.title.name(), sourceDocument.toString());
				
				long sdNode = inserter.createNode(properties, NodeType.SourceDocument);
		
				properties.clear();
				Map<TermInfo,Long> termInfoToNodeId = new HashMap<TermInfo, Long>();
				TreeSet<TermInfo> orderedTermInfos = new TreeSet<TermInfo>(TermInfo.TOKENOFFSETCOMPARATOR);
				
				for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
					String term = entry.getKey();
					List<TermInfo> termInfos = entry.getValue();
					
					properties.put(TermProperty.literal.name(), term);
					properties.put(TermProperty.freq.name(), termInfos.size());
					
					long termNodeId = inserter.createNode(properties, NodeType.Term);
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
						
						termInfoToNodeId.put(ti, positionNodeId);
						inserter.createRelationship(
								termNodeId, 
								positionNodeId, 
								NodeRelationType.HAS_POSITION, 
								Collections.<String,Object>emptyMap());
						inserter.createRelationship(
								termNodeId, 
								sdNode, 
								NodeRelationType.IS_PART_OF, 
								Collections.<String, Object>emptyMap());
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
					}
					prevTi = ti;
				}
			}
			
			inserter.createDeferredSchemaIndex(NodeType.SourceDocument)
				.on(SourceDocumentProperty.localUri.name()).create();
	
			inserter.createDeferredSchemaIndex(NodeType.Term)
				.on(TermProperty.literal.name()).create();
	
			inserter.createDeferredSchemaIndex(NodeType.Term)
				.on(TermProperty.freq.name()).create();
		}
		finally {
			inserter.shutdown();
		}
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

