package de.catma.repository.db.maintenance;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS_SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS_USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.STATICMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGLIBRARY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGREFERENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGSETDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.UNSEPARABLE_CHARSEQUENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERDEFINED_SEPARATINGCHARACTER;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_TAGLIBRARY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.repository.db.CatmaDataSourceName;
import de.catma.repository.db.FileURLFactory;
import de.catma.repository.db.jooqgen.catmarepository.Tables;
import de.catma.repository.db.mapper.FieldToValueMapper;
import de.catma.repository.db.mapper.IDFieldToIntegerMapper;

public class DBMaintainer {

	private String fullPropertyFilePath;
	private String repoFolderPath;
	private Logger logger;

	public DBMaintainer(String fullPropertyFilePath) {
		this.fullPropertyFilePath = fullPropertyFilePath;
		this.logger = Logger.getLogger(DBMaintainer.class.getName());
	}

	public void run() throws IOException {
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(fullPropertyFilePath));
			this.repoFolderPath = 
				RepositoryPropertyKey.RepositoryFolderPath.getProperty(properties, 0);
			Context  context = new InitialContext();
			DataSource dataSource = (DataSource) context.lookup(
					CatmaDataSourceName.CATMADS.name());
			
			DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
			
			cleanupSourceDocuments(db);
			cleanupUserMarkupCollections(db);
			cleanupTagLibraries(db);
			cleanupTagsetDefinitions(db);
			
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	private void cleanupTagsetDefinitions(DSLContext db) {
		List<Integer> tagsetDefinitionIds = db
		.select(TAGSETDEFINITION.TAGSETDEFINITIONID)
		.whereNotExists(db // independent taglibraries
			.selectOne()
			.from(USER_TAGLIBRARY)
			.where(USER_TAGLIBRARY.TAGLIBRARYID.eq(TAGSETDEFINITION.TAGLIBRARYID)))
		.andNotExists(db // dependent taglibries
			.selectOne()
			.from(USERMARKUPCOLLECTION)
			.where(USERMARKUPCOLLECTION.TAGLIBRARYID.eq(TAGSETDEFINITION.TAGLIBRARYID)))
		.limit(10)
		.fetch()
		.map(new IDFieldToIntegerMapper(TAGSETDEFINITION.TAGSETDEFINITIONID));
		
		if (!tagsetDefinitionIds.isEmpty()) {
			logger.info(
					"start deleting obsolete TagsetDefinitions " 
					+ tagsetDefinitionIds);
			
			db
			.delete(TAGSETDEFINITION)
			.where(TAGSETDEFINITION.TAGSETDEFINITIONID.in(tagsetDefinitionIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete TagsetDefinitions " 
					+ tagsetDefinitionIds);
		}
			
	}

	private void cleanupTagLibraries(DSLContext db) {
		//independent
		List<Integer> indTagLibraryIds = db
		.select(TAGLIBRARY.TAGLIBRARYID)
		.from(TAGLIBRARY)
		.where(TAGLIBRARY.INDEPENDENT.eq((byte)1))
		.andNotExists(
			db
			.selectOne()
			.from(USER_TAGLIBRARY)
			.where(USER_TAGLIBRARY.TAGLIBRARYID.eq(TAGLIBRARY.TAGLIBRARYID)))
		.andNotExists(
			db
			.selectOne()
			.from(TAGSETDEFINITION)
			.where(TAGSETDEFINITION.TAGLIBRARYID.eq(TAGLIBRARY.TAGLIBRARYID)))
		.limit(10)
		.fetch()
		.map(new IDFieldToIntegerMapper(TAGLIBRARY.TAGLIBRARYID));
		
		if (!indTagLibraryIds.isEmpty()) {
			logger.info(
					"start deleting obsolete independent TagLibraries " 
					+ indTagLibraryIds);
			db
			.delete(TAGLIBRARY)
			.where(TAGLIBRARY.TAGLIBRARYID.in(indTagLibraryIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete independent TagLibraries " 
					+ indTagLibraryIds);			
		}
		
		//dependent
		
		List<Integer> depTagLibraryIds = db
		.select(TAGLIBRARY.TAGLIBRARYID)
		.from(TAGLIBRARY)
		.where(TAGLIBRARY.INDEPENDENT.eq((byte)0))
		.andNotExists(db
			.selectOne()
			.from(USERMARKUPCOLLECTION)
			.where(USERMARKUPCOLLECTION.TAGLIBRARYID.eq(TAGLIBRARY.TAGLIBRARYID)))
		.andNotExists(
			db
			.selectOne()
			.from(TAGSETDEFINITION)
			.where(TAGSETDEFINITION.TAGLIBRARYID.eq(TAGLIBRARY.TAGLIBRARYID)))
		.limit(10)
		.fetch()
		.map(new IDFieldToIntegerMapper(TAGLIBRARY.TAGLIBRARYID));
		
		if (!depTagLibraryIds.isEmpty()) {
			logger.info(
					"start deleting obsolete dependent TagLibraries " 
					+ depTagLibraryIds);
			db
			.delete(TAGLIBRARY)
			.where(TAGLIBRARY.TAGLIBRARYID.in(depTagLibraryIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete dependent TagLibraries " 
					+ depTagLibraryIds);			
		}
	}

	private void cleanupUserMarkupCollections(DSLContext db) {
		List<Integer> userMarkupCollectionIds = db
		.select(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
		.from(USERMARKUPCOLLECTION)
		.whereNotExists(db
			.selectOne()
			.from(USER_SOURCEDOCUMENT)
			.where(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID
					.eq(USERMARKUPCOLLECTION.SOURCEDOCUMENTID)))
		.andNotExists(db
			.selectOne()
			.from(TAGREFERENCE)
			.where(TAGREFERENCE.USERMARKUPCOLLECTIONID
					.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)))
		.limit(10)
		.fetch()
		.map(new IDFieldToIntegerMapper(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID));
			
		if (!userMarkupCollectionIds.isEmpty()) {
			logger.info(
				"start deleting obsolete UserMarkupCollections " 
				+ userMarkupCollectionIds);
			db
			.delete(CORPUS_USERMARKUPCOLLECTION)
			.where(CORPUS_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.in(userMarkupCollectionIds))
			.execute();
			
			db
			.delete(USERMARKUPCOLLECTION)
			.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.in(userMarkupCollectionIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete UserMarkupCollections " 
					+ userMarkupCollectionIds);
		}
		
	}

	private void cleanupSourceDocuments(DSLContext db) {
		FileURLFactory fileURLFactory = new FileURLFactory();
		
		List<Integer> sourceDocumentIds = db
		.select(SOURCEDOCUMENT.SOURCEDOCUMENTID)
		.from(SOURCEDOCUMENT)
		.whereNotExists(db
				.selectOne()
				.from(USER_SOURCEDOCUMENT)
				.where(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID
						.eq(SOURCEDOCUMENT.SOURCEDOCUMENTID)))
		.andNotExists(db
				.selectOne()
				.from(STATICMARKUPCOLLECTION)
				.where(STATICMARKUPCOLLECTION.SOURCEDOCUMENTID
						.eq(SOURCEDOCUMENT.SOURCEDOCUMENTID)))
		.andNotExists(db
				.selectOne()
				.from(USERMARKUPCOLLECTION)
				.where(USERMARKUPCOLLECTION.SOURCEDOCUMENTID
						.eq(SOURCEDOCUMENT.SOURCEDOCUMENTID)))
		.limit(10)
		.fetch()
		.map(new IDFieldToIntegerMapper(SOURCEDOCUMENT.SOURCEDOCUMENTID));
				
		
		if (!sourceDocumentIds.isEmpty()) {
			logger.info("start deleting obsolete SourceDocuments " + sourceDocumentIds);
			
			db
			.delete(CORPUS_SOURCEDOCUMENT)
			.where(CORPUS_SOURCEDOCUMENT.SOURCEDOCUMENTID.in(sourceDocumentIds))
			.execute();
			db
			.delete(USERDEFINED_SEPARATINGCHARACTER)
			.where(USERDEFINED_SEPARATINGCHARACTER.SOURCEDOCUMENTID.in(sourceDocumentIds))
			.execute();
			db
			.delete(UNSEPARABLE_CHARSEQUENCE)
			.where(UNSEPARABLE_CHARSEQUENCE.SOURCEDOCUMENTID.in(sourceDocumentIds))
			.execute();
			
			List<String> localUriList = db
				.select(SOURCEDOCUMENT.LOCALURI)
				.from(SOURCEDOCUMENT)
				.where(SOURCEDOCUMENT.SOURCEDOCUMENTID.in(sourceDocumentIds))
				.fetch()
				.map(new FieldToValueMapper<String>(SOURCEDOCUMENT.LOCALURI));
			
			String sourceDocsPath =  
					repoFolderPath + "/" + 
					FileURLFactory.SOURCEDOCS_FOLDER + "/";
			
			for (String localUri : localUriList) {
				File sdFile = new File(fileURLFactory.getFileURL(localUri, sourceDocsPath));
				if (sdFile.exists()) {
					sdFile.delete();
				}
			}
			
			db
			.delete(SOURCEDOCUMENT)
			.where(SOURCEDOCUMENT.SOURCEDOCUMENTID.in(sourceDocumentIds))
			.execute();
			
			logger.info("finished deleting obsolete SourceDocuments " + sourceDocumentIds);
		}
		
	}
}
