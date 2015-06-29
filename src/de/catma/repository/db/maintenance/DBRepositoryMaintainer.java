package de.catma.repository.db.maintenance;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS_SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS_STATICMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS_USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEF_POSSIBLEVALUE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYVALUE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.STATICMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGINSTANCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGLIBRARY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGREFERENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGSETDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.UNSEPARABLE_CHARSEQUENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERDEFINED_SEPARATINGCHARACTER;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_CORPUS;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_TAGLIBRARY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_USERMARKUPCOLLECTION;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.repository.db.CatmaDataSourceName;
import de.catma.repository.db.FileURLFactory;
import de.catma.repository.db.MaintenanceSemaphore;
import de.catma.repository.db.MaintenanceSemaphore.Type;
import de.catma.repository.db.mapper.FieldToValueMapper;
import de.catma.repository.db.mapper.IDFieldToIntegerMapper;

public class DBRepositoryMaintainer {

	private String repoFolderPath;
	private Logger logger;

	public DBRepositoryMaintainer() {
		this.logger = Logger.getLogger(DBRepositoryMaintainer.class.getName());
	}

	public void run() throws IOException {
		MaintenanceSemaphore mSem = null;
		
		try {
			this.repoFolderPath = 
				RepositoryPropertyKey.RepositoryFolderPath.getValue(1);
			DataSource dataSource = 
				CatmaDataSourceName.CATMADS.getDataSource();
			
			mSem = new MaintenanceSemaphore(Type.CLEANING);
			
			if (mSem.hasAccess()) {
				DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
				
				cleanupSourceDocuments(db);
				cleanupUserMarkupCollections(db);
				
				cleanupTagReferences(db);
				cleanupTagInstances(db);
				cleanupProperties(db);
				cleanupPropertyValues(db);
				
				cleanupTagLibraries(db);
				cleanupTagsetDefinitions(db);
				cleanupTagDefinitions(db);
				cleanupPropertyDefinitions(db);
				cleanupPropertyDefPossibleValues(db);
				
				cleanupCorpora(db);
			}
			else {
				logger.info("DB maintenance aborted, could not get semaphore access!");
			}
			
			mSem.release();
		}
		catch (Exception e) {
			if (mSem != null) {
				mSem.release();
			}
			throw new IOException(e);
		}
	}

	private void cleanupCorpora(DSLContext db) {
		List<Integer> corpusIds = db
		.select(CORPUS.CORPUSID)
		.from(CORPUS)
		.whereNotExists(db
			.selectOne()
			.from(USER_CORPUS)
			.where(USER_CORPUS.CORPUSID.eq(CORPUS.CORPUSID)))
		.andNotExists(db
			.selectOne()
			.from(CORPUS_USERMARKUPCOLLECTION)
			.where(CORPUS_USERMARKUPCOLLECTION.CORPUSID.eq(CORPUS.CORPUSID)))
		.andNotExists(db
			.selectOne()
			.from(CORPUS_SOURCEDOCUMENT)
			.where(CORPUS_SOURCEDOCUMENT.CORPUSID.eq(CORPUS.CORPUSID)))	
		.andNotExists(db
			.selectOne()
			.from(CORPUS_STATICMARKUPCOLLECTION)
			.where(CORPUS_STATICMARKUPCOLLECTION.CORPUSID.eq(CORPUS.CORPUSID)))	
		.fetch()
		.map(new IDFieldToIntegerMapper(CORPUS.CORPUSID));
		
		if (!corpusIds.isEmpty()) {
			logger.info(
					"start deleting obsolete Corpora " 
					+ corpusIds);
			
			db
			.delete(CORPUS)
			.where(CORPUS.CORPUSID.in(corpusIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete Corpora " 
					+ corpusIds);
		}
	}

	private void cleanupPropertyValues(DSLContext db) {
		List<Integer> propertyValueIds = db
		.select(PROPERTYVALUE.PROPERTYVALUEID)
		.from(PROPERTYVALUE)
		.whereNotExists(db
			.selectOne()
			.from(TAGREFERENCE)
			.join(PROPERTY)
				.on(PROPERTY.TAGINSTANCEID.eq(TAGREFERENCE.TAGINSTANCEID))
			.where(PROPERTY.PROPERTYID.eq(PROPERTYVALUE.PROPERTYID)))
		.limit(500)
		.fetch()
		.map(new IDFieldToIntegerMapper(PROPERTYVALUE.PROPERTYVALUEID));
	
		if (!propertyValueIds.isEmpty()) {
			logger.info(
					"start deleting obsolete PropertyValues " 
					+ propertyValueIds);
			db
			.delete(PROPERTYVALUE)
			.where(PROPERTYVALUE.PROPERTYVALUEID.in(propertyValueIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete PropertyValues " 
					+ propertyValueIds);
		}
			
		
	}

	private void cleanupProperties(DSLContext db) {
		List<Integer> propertyIds = db
		.select(PROPERTY.PROPERTYID)
		.from(PROPERTY)
		.whereNotExists(db
			.selectOne()
			.from(TAGREFERENCE)
			.where(TAGREFERENCE.TAGINSTANCEID.eq(PROPERTY.TAGINSTANCEID)))
		.andNotExists(db
			.selectOne()
			.from(PROPERTYVALUE)
			.where(PROPERTYVALUE.PROPERTYID.eq(PROPERTY.PROPERTYID)))
		.limit(500)
		.fetch()
		.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
		
		if (!propertyIds.isEmpty()) {
			logger.info(
					"start deleting obsolete Properties " 
					+ propertyIds);
			db
			.delete(PROPERTY)
			.where(PROPERTY.PROPERTYID.in(propertyIds))
			.execute();
			
			logger.info(
					"start deleting obsolete Properties " 
					+ propertyIds);
		}
	}

	private void cleanupTagInstances(DSLContext db) {
		List<Integer> tagInstanceIds = db
		.select(TAGINSTANCE.TAGINSTANCEID)
		.from(TAGINSTANCE)
		.whereNotExists(db
			.selectOne()
			.from(TAGREFERENCE)
			.where(TAGREFERENCE.TAGINSTANCEID.eq(TAGINSTANCE.TAGINSTANCEID)))
		.andNotExists(db
			.selectOne()
			.from(PROPERTY)
			.where(PROPERTY.TAGINSTANCEID.eq(TAGINSTANCE.TAGINSTANCEID)))
		.limit(500)
		.fetch()
		.map(new IDFieldToIntegerMapper(TAGINSTANCE.TAGINSTANCEID));
		
		if (!tagInstanceIds.isEmpty()) {
			logger.info(
					"start deleting obsolete TagInstances " 
					+ tagInstanceIds);
			db
			.delete(TAGINSTANCE)
			.where(TAGINSTANCE.TAGINSTANCEID.in(tagInstanceIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete TagInstances " 
					+ tagInstanceIds);
		}
		
	}

	private void cleanupTagReferences(DSLContext db) {
		List<Integer> tagReferenceIds = db
		.select(TAGREFERENCE.TAGREFERENCEID)
		.from(TAGREFERENCE)
		.whereNotExists(db
			.selectOne()
			.from(USER_SOURCEDOCUMENT)
			.join(USERMARKUPCOLLECTION)
				.on(USERMARKUPCOLLECTION.SOURCEDOCUMENTID
						.eq(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID))
			.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
					.eq(TAGREFERENCE.USERMARKUPCOLLECTIONID)))
		.orNotExists(db
			.selectOne()
			.from(USER_USERMARKUPCOLLECTION)
			.where(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
				.eq(TAGREFERENCE.USERMARKUPCOLLECTIONID)))
		.limit(500)
		.fetch()
		.map(new IDFieldToIntegerMapper(TAGREFERENCE.TAGREFERENCEID));
		
		if (!tagReferenceIds.isEmpty()) {
			logger.info(
					"start deleting obsolete TagReferences " 
					+ tagReferenceIds);
			
			db
			.delete(TAGREFERENCE)
			.where(TAGREFERENCE.TAGREFERENCEID.in(tagReferenceIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete TagReferences " 
					+ tagReferenceIds);
		}
	}

	private void cleanupPropertyDefPossibleValues(DSLContext db) {
		List<Integer> propetyDefPossibleValuesIds = db
		.select(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFPOSSIBLEVALUEID)
		.from(PROPERTYDEF_POSSIBLEVALUE)
		.whereNotExists(db
			.selectOne()
			.from(USER_TAGLIBRARY)
			.join(TAGSETDEFINITION)
				.on(TAGSETDEFINITION.TAGLIBRARYID.eq(USER_TAGLIBRARY.TAGLIBRARYID))
			.join(TAGDEFINITION)
				.on(TAGDEFINITION.TAGSETDEFINITIONID.eq(TAGSETDEFINITION.TAGSETDEFINITIONID))
			.join(PROPERTYDEFINITION)
				.on(PROPERTYDEFINITION.TAGDEFINITIONID.eq(TAGDEFINITION.TAGDEFINITIONID))
			.where(PROPERTYDEFINITION.PROPERTYDEFINITIONID
					.eq(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID)))
		.andNotExists(db
			.selectOne()
			.from(USERMARKUPCOLLECTION)
			.join(TAGSETDEFINITION)
				.on(TAGSETDEFINITION.TAGLIBRARYID.eq(USERMARKUPCOLLECTION.TAGLIBRARYID))
			.join(TAGDEFINITION)
				.on(TAGDEFINITION.TAGSETDEFINITIONID.eq(TAGSETDEFINITION.TAGSETDEFINITIONID))
			.join(PROPERTYDEFINITION)
				.on(PROPERTYDEFINITION.TAGDEFINITIONID.eq(TAGDEFINITION.TAGDEFINITIONID))
			.where(PROPERTYDEFINITION.PROPERTYDEFINITIONID
					.eq(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID)))
		.limit(100)
		.fetch()
		.map(new IDFieldToIntegerMapper(
				PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFPOSSIBLEVALUEID));
		
		if (!propetyDefPossibleValuesIds.isEmpty()) {
			logger.info(
					"start deleting obsolete PropertyDefPossibleValues " 
					+ propetyDefPossibleValuesIds);
			db
			.delete(PROPERTYDEF_POSSIBLEVALUE)
			.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFPOSSIBLEVALUEID
					.in(propetyDefPossibleValuesIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete PropertyDefPossibleValues " 
					+ propetyDefPossibleValuesIds);
		}
	}

	private void cleanupPropertyDefinitions(DSLContext db) {
		List<Integer> propertyDefinitionIds = db
		.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
		.from(PROPERTYDEFINITION)
		.whereNotExists(db
			.selectOne()
			.from(USER_TAGLIBRARY)
			.join(TAGSETDEFINITION)
				.on(TAGSETDEFINITION.TAGLIBRARYID.eq(USER_TAGLIBRARY.TAGLIBRARYID))
			.join(TAGDEFINITION)
				.on(TAGDEFINITION.TAGSETDEFINITIONID.eq(TAGSETDEFINITION.TAGSETDEFINITIONID))
			.where(TAGDEFINITION.TAGDEFINITIONID.eq(PROPERTYDEFINITION.TAGDEFINITIONID)))
		.andNotExists(db
			.selectOne()
			.from(PROPERTY)
			.where(PROPERTY.PROPERTYDEFINITIONID.eq(PROPERTYDEFINITION.PROPERTYDEFINITIONID)))
		.andNotExists(db
			.selectOne()
			.from(USERMARKUPCOLLECTION)
			.join(TAGSETDEFINITION)
				.on(TAGSETDEFINITION.TAGLIBRARYID.eq(USERMARKUPCOLLECTION.TAGLIBRARYID))
			.join(TAGDEFINITION)
				.on(TAGDEFINITION.TAGSETDEFINITIONID.eq(TAGSETDEFINITION.TAGSETDEFINITIONID))
			.where(TAGDEFINITION.TAGDEFINITIONID.eq(PROPERTYDEFINITION.TAGDEFINITIONID)))
		.andNotExists(db
			.selectOne()
			.from(PROPERTYDEF_POSSIBLEVALUE)
			.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID
					.eq(PROPERTYDEFINITION.PROPERTYDEFINITIONID)))
		.limit(100)
		.fetch()
		.map(new IDFieldToIntegerMapper(PROPERTYDEFINITION.PROPERTYDEFINITIONID));
		
		if (!propertyDefinitionIds.isEmpty()) {
			logger.info(
					"start deleting obsolete PropertyDefinitions " 
					+ propertyDefinitionIds);
			db
			.delete(PROPERTYDEFINITION)
			.where(PROPERTYDEFINITION.PROPERTYDEFINITIONID.in(propertyDefinitionIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete PropertyDefinitions " 
					+ propertyDefinitionIds);			
		}
	}

	private void cleanupTagDefinitions(DSLContext db) {
		de.catma.repository.db.jooqgen.catmarepository.tables.Tagdefinition td2 = 
				TAGDEFINITION.as("td2");
		
		List<Integer> tagDefinitionIds = db
		.select(TAGDEFINITION.TAGDEFINITIONID)
		.from(TAGDEFINITION)
		.whereNotExists(db
			.selectOne()
			.from(USER_TAGLIBRARY)
			.join(TAGSETDEFINITION)
				.on(TAGSETDEFINITION.TAGLIBRARYID.eq(USER_TAGLIBRARY.TAGLIBRARYID))
			.where(TAGSETDEFINITION.TAGSETDEFINITIONID
					.eq(TAGDEFINITION.TAGSETDEFINITIONID)))
		.andNotExists(db
			.selectOne()
			.from(USERMARKUPCOLLECTION)
			.join(TAGSETDEFINITION)
				.on(TAGSETDEFINITION.TAGLIBRARYID.eq(USERMARKUPCOLLECTION.TAGLIBRARYID))
			.where(TAGSETDEFINITION.TAGSETDEFINITIONID
				.eq(TAGDEFINITION.TAGSETDEFINITIONID)))
		.andNotExists(db
			.selectOne()
			.from(PROPERTYDEFINITION)
			.where(PROPERTYDEFINITION.TAGDEFINITIONID
					.eq(TAGDEFINITION.TAGDEFINITIONID)))
		.andNotExists(db
			.selectOne()
			.from(td2)
			.where(td2.PARENTID.eq(TAGDEFINITION.TAGDEFINITIONID)))
		.limit(100)
		.fetch()
		.map(new IDFieldToIntegerMapper(TAGDEFINITION.TAGDEFINITIONID));
			
		if (!tagDefinitionIds.isEmpty()) {
			logger.info(
					"start deleting obsolete TagDefinitions " 
					+ tagDefinitionIds);
			
			db
			.delete(TAGDEFINITION)
			.where(TAGDEFINITION.TAGDEFINITIONID.in(tagDefinitionIds))
			.execute();
					
			logger.info(
					"finished deleting obsolete TagDefinitions " 
					+ tagDefinitionIds);
		}
		
	}

	private void cleanupTagsetDefinitions(DSLContext db) {
		List<Integer> tagsetDefinitionIds = db
		.select(TAGSETDEFINITION.TAGSETDEFINITIONID)
		.from(TAGSETDEFINITION)
		.whereNotExists(db // independent taglibraries
			.selectOne()
			.from(USER_TAGLIBRARY)
			.where(USER_TAGLIBRARY.TAGLIBRARYID
					.eq(TAGSETDEFINITION.TAGLIBRARYID)))
		.andNotExists(db // dependent taglibraries
			.selectOne()
			.from(USERMARKUPCOLLECTION)
			.where(USERMARKUPCOLLECTION.TAGLIBRARYID
					.eq(TAGSETDEFINITION.TAGLIBRARYID)))
		.andNotExists(db
			.selectOne()
			.from(TAGDEFINITION)
			.where(TAGDEFINITION.TAGSETDEFINITIONID
					.eq(TAGSETDEFINITION.TAGSETDEFINITIONID)))
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
		.orNotExists(db
			.selectOne()
			.from(USER_USERMARKUPCOLLECTION)
			.where(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
				.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)))
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
			.delete(USER_USERMARKUPCOLLECTION)
			.where(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.in(userMarkupCollectionIds))
			.execute();
			
			db
			.delete(CORPUS_USERMARKUPCOLLECTION)
			.where(CORPUS_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
					.in(userMarkupCollectionIds))
			.execute();
			
			db
			.delete(USERMARKUPCOLLECTION)
			.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
					.in(userMarkupCollectionIds))
			.execute();
			
			logger.info(
					"finished deleting obsolete UserMarkupCollections " 
					+ userMarkupCollectionIds);
		}
		
	}

	private void cleanupSourceDocuments(DSLContext db) throws URISyntaxException {
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
			.where(USERDEFINED_SEPARATINGCHARACTER.SOURCEDOCUMENTID
					.in(sourceDocumentIds))
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
				File sdFile = 
						new File(new URI(fileURLFactory.getFileURL(localUri, sourceDocsPath)));
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
