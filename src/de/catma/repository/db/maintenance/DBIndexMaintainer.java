package de.catma.repository.db.maintenance;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYVALUE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGINSTANCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGREFERENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Record5;
import org.jooq.Record8;
import org.jooq.Record9;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.document.repository.RepositoryPropertiesName;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.repository.db.CatmaDataSourceName;
import de.catma.repository.db.FileURLFactory;
import de.catma.repository.db.SourceDocumentHandler;
import de.catma.repository.db.jooqgen.catmaindex.Routines;
import de.catma.repository.db.mapper.IDFieldToIntegerMapper;

public class DBIndexMaintainer {

	private static final int MAX_FILE_CLEAN_COUNT = 10;
	private static final int MAX_ROW_COUNT = 10;
	private String repoFolderPath;
	private int fileCleanOffset = 0;
	private int repoTagReferenceRowOffset = 0;
	private int repoPropertyRowOffset = 0;
	private int indexTagReferenceRowOffset = 0;
	private int indexPropertyRowOffset = 0;

	private Logger logger;

	public DBIndexMaintainer(int fileCleanOffset, int repoTagReferenceRowOffset,
			int repoPropertyRowOffset, int indexTagReferenceRowOffset,
			int indexPropertyRowOffset) {

		this.fileCleanOffset = fileCleanOffset;

		this.repoTagReferenceRowOffset = repoTagReferenceRowOffset;
		this.repoPropertyRowOffset = repoPropertyRowOffset;
		this.indexTagReferenceRowOffset = indexTagReferenceRowOffset;
		this.indexPropertyRowOffset = indexPropertyRowOffset;
		this.logger = Logger.getLogger(DBIndexMaintainer.class.getName());
	}

	public void run() throws IOException {
		UserManager userManager = new UserManager();
		try {
			Properties properties = 
					(Properties) new InitialContext().lookup(
							RepositoryPropertiesName.CATMAPROPERTIES.name());
			this.repoFolderPath = 
				RepositoryPropertyKey.RepositoryFolderPath.getProperty(properties, 1);
			
			userManager.lockLogin();
			try {
				logger.info("starting index maintenance");
				if (userManager.getUserCount() == 0 ) {
					logger.info("no user logged in, proceeding with index maintenance");
					Context  context = new InitialContext();
					DataSource dataSource = (DataSource) context.lookup(
							CatmaDataSourceName.CATMADS.name());
					DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
					// all repo.tagreferences need an entry in index.tagref
					checkRepoTagReferences(db);
					// all repo.propertyvalues need an entry in index.property
					checkRepoProperties(db);
					
					// delete all index.tagref that are no longer in repo.tagreference
					checkStaleIndexTagReferences(db);
					// delete all index.proeprty that are no longer in repo.propertyvalue
					checkStaleIndexProperties(db);
					
					// cleaning up stale sourcedocument files
					cleanupSourceDocumentFiles(db);
	
					//TODO: check time and exit after fixed period
					
					//TODO: maintain term/position index
	
					logger.info("finished index maintenance");
				}
				else {
					logger.info("there are users logged in, skipping index maintenance");
				}
			}
			finally {
				userManager.unlockLogin();
			}
		}
		catch (Exception e) {
			throw new IOException(e);
		}	
	}

	private void cleanupSourceDocumentFiles(DSLContext db) {
		logger.info("cleaning up stale sourcedocument files");
		String sourceDocsPath =  
				repoFolderPath + "/" + 
				FileURLFactory.SOURCEDOCS_FOLDER + "/";
		
		File sourceDocsDir = new File(sourceDocsPath);
		String[] fileNames = sourceDocsDir.list();

		if (fileNames == null) {
			fileNames = new String[] {};
		}
		
		Arrays.sort(fileNames);
		if (fileNames.length <= fileCleanOffset) {
			fileCleanOffset = 0;
		}
		for (int idx=fileCleanOffset; idx<fileNames.length; idx++) {
			
			String sourceDocName = fileNames[idx];
			String localUri = SourceDocumentHandler.REPO_URI_SCHEME + sourceDocName;
			
			Record1<Integer> repoRow = db
					.selectOne()
					.from(SOURCEDOCUMENT)
					.where(SOURCEDOCUMENT.LOCALURI.eq(localUri))
					.fetchOne();
			
			if (repoRow == null) {
				File sourceDocFile = new File(sourceDocsPath, sourceDocName);
				
				logger.info(sourceDocFile.getAbsolutePath() + " is stale and will be removed now");
				sourceDocFile.delete();
			}
			
			if (idx-fileCleanOffset > MAX_FILE_CLEAN_COUNT) {
				fileCleanOffset+=(idx-fileCleanOffset);
				break;
			}
			
		}
		
	}

	private void checkStaleIndexProperties(DSLContext db) {
		logger.info("checking stale index properties");
		de.catma.repository.db.jooqgen.catmaindex.tables.Property indexProperty = 
				de.catma.repository.db.jooqgen.catmaindex.Tables.PROPERTY;

		Result<Record5<byte[], byte[], String, String, Integer>> result = db
		.select(
			indexProperty.TAGINSTANCEID,
			indexProperty.PROPERTYDEFINITIONID,
			indexProperty.NAME,
			indexProperty.VALUE,
			indexProperty.PROPERTYID)
		.from(indexProperty)
		.limit(indexPropertyRowOffset, MAX_ROW_COUNT)
		.fetch();
		
		indexPropertyRowOffset += result.size();
		
		ArrayList<Integer> toBeDeleted = new ArrayList<Integer>();
		
		for(Record5<byte[], byte[], String, String, Integer> row : result) {
			Record1<Integer> repoRow = db
			.selectOne()
			.from(PROPERTYVALUE)
			.join(PROPERTY)
				.on(PROPERTY.PROPERTYID.eq(PROPERTYVALUE.PROPERTYID))
			.join(PROPERTYDEFINITION)
				.on(PROPERTYDEFINITION.PROPERTYDEFINITIONID.eq(PROPERTY.PROPERTYDEFINITIONID))
				.and(PROPERTYDEFINITION.UUID.eq(row.value2()))
				.and(PROPERTYDEFINITION.NAME.eq(row.value3()))
			.join(TAGINSTANCE)
				.on(TAGINSTANCE.TAGINSTANCEID.eq(PROPERTY.TAGINSTANCEID))
				.and(TAGINSTANCE.UUID.eq(row.value1()))
			.where(PROPERTYVALUE.VALUE.eq(row.value4()))
			.fetchOne();
			
			if (repoRow == null) {
				logger.info("index property row " + row + " is stale and will be removed");
				toBeDeleted.add(row.value5());
			}
		}
		
		logger.info("index property entries " + toBeDeleted + " are removed from the index");
		if (!toBeDeleted.isEmpty()) {
			db
			.delete(indexProperty)
			.where(indexProperty.PROPERTYID.in(toBeDeleted))
			.execute();
	
			indexPropertyRowOffset -= toBeDeleted.size();
		}
	}

	private void checkStaleIndexTagReferences(DSLContext db) {
		logger.info("checking stale index tagreferences");

		de.catma.repository.db.jooqgen.catmaindex.tables.Tagreference indexTagReference =
				de.catma.repository.db.jooqgen.catmaindex.Tables.TAGREFERENCE;

		
		Result<Record9<String, String, String, byte[], byte[], String, Integer, Integer, Integer>> result = db
		.select(
			indexTagReference.DOCUMENTID, 
			indexTagReference.USERMARKUPCOLLECTIONID,
			indexTagReference.TAGDEFINITIONPATH,
			indexTagReference.TAGDEFINITIONID, 
			indexTagReference.TAGINSTANCEID, 
			indexTagReference.TAGDEFINITIONVERSION,
			indexTagReference.CHARACTERSTART,
			indexTagReference.CHARACTEREND,
			indexTagReference.TAGREFERENCEID)
		.from(indexTagReference)
		.limit(indexTagReferenceRowOffset, MAX_ROW_COUNT)
		.fetch();
		
		indexTagReferenceRowOffset += result.size();
		
		ArrayList<Integer> toBeDeleted = new ArrayList<Integer>();
		
		for (Record9<String, String, String, byte[], byte[], String, Integer, Integer, Integer> row : result) {
			Record1<Integer> repoRow = db
			.selectOne()
			.from(TAGREFERENCE)
			.join(TAGINSTANCE)
				.on(TAGINSTANCE.TAGINSTANCEID.eq(TAGREFERENCE.TAGINSTANCEID))
				.and(TAGINSTANCE.UUID.eq(row.value5()))
			.join(TAGDEFINITION)
				.on(TAGDEFINITION.TAGDEFINITIONID.eq(TAGINSTANCE.TAGDEFINITIONID))
				.and(TAGDEFINITION.UUID.eq(row.value4()))
				.and(Routines.gettagdefinitionpath(TAGDEFINITION.TAGDEFINITIONID).eq(row.value3()))
			.join(USERMARKUPCOLLECTION)
				.on(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
						.eq(TAGREFERENCE.USERMARKUPCOLLECTIONID))
				.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(Integer.valueOf(row.value2())))
			.join(SOURCEDOCUMENT)
				.on(SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USERMARKUPCOLLECTION.SOURCEDOCUMENTID))
				.and(SOURCEDOCUMENT.LOCALURI.eq(row.value1()))
			.where(TAGREFERENCE.CHARACTERSTART.eq(row.value7()))
			.and(TAGREFERENCE.CHARACTEREND.eq(row.value8()))
			.fetchOne();
			
			if (repoRow == null) {
				logger.info("index tagreference row " + row + " is stale and will be removed");
				toBeDeleted.add(row.value9());
			}
		}

		logger.info("index tagreference entries " + toBeDeleted + " are removed from the index");
		if (!toBeDeleted.isEmpty()) {
			db
			.delete(indexTagReference)
			.where(indexTagReference.TAGREFERENCEID.in(toBeDeleted))
			.execute();
			indexTagReferenceRowOffset -= toBeDeleted.size();
		}
	}

	private void checkRepoProperties(DSLContext db) {
		logger.info("checking repo properties");
		Result<Record4<byte[],byte[],String,String>> result = db
		.select(
			TAGINSTANCE.UUID,
			PROPERTYDEFINITION.UUID,
			PROPERTYDEFINITION.NAME,
			PROPERTYVALUE.VALUE)
		.from(PROPERTYVALUE)
		.join(PROPERTY)
			.on(PROPERTY.PROPERTYID.eq(PROPERTYVALUE.PROPERTYID))
		.join(PROPERTYDEFINITION)
			.on(PROPERTYDEFINITION.PROPERTYDEFINITIONID.eq(PROPERTY.PROPERTYDEFINITIONID))
		.join(TAGINSTANCE)
			.on(TAGINSTANCE.TAGINSTANCEID.eq(PROPERTY.TAGINSTANCEID))
		.limit(repoPropertyRowOffset, MAX_ROW_COUNT)
		.fetch();
		
		if (result.size() < MAX_ROW_COUNT) {
			repoPropertyRowOffset = 0;
		}
		else {
			repoPropertyRowOffset += result.size();
		}
			
		de.catma.repository.db.jooqgen.catmaindex.tables.Property indexProperty = 
				de.catma.repository.db.jooqgen.catmaindex.Tables.PROPERTY;
		
		ArrayList<Record4<byte[],byte[],String,String>> rowsNeedIndexing = 
				new ArrayList<Record4<byte[],byte[],String,String>>();
		
		for (Record4<byte[],byte[],String,String> row : result) {
			List<Integer> indexedRows = db
			.select(indexProperty.PROPERTYID)
			.from(indexProperty)
			.where(indexProperty.TAGINSTANCEID.eq(row.value1()))
			.and(indexProperty.PROPERTYDEFINITIONID.eq(row.value2()))
			.and(indexProperty.NAME.eq(row.value3()))
			.and(indexProperty.VALUE.eq(row.value4()))
			.fetch()
			.map(new IDFieldToIntegerMapper(indexProperty.PROPERTYID));
			
			if (indexedRows.size() == 0) {
				rowsNeedIndexing.add(row);
			}
			else if (indexedRows.size() > 1) { 
				
				Set<Integer> toBeDeleted = new HashSet<Integer>(indexedRows);
				Integer remainingOne = toBeDeleted.iterator().next();
				toBeDeleted.remove(remainingOne);
				
				logger.info("over indexed property, deleting all " + toBeDeleted + " but one " + remainingOne);
				db
				.delete(indexProperty)
				.where(indexProperty.PROPERTYID.in(toBeDeleted))
				.execute();
			}
		}
		logger.info("there are " + rowsNeedIndexing.size() + " property rows that need indexing");
		for (Record4<byte[],byte[],String,String> row : rowsNeedIndexing) {
			logger.info("indexing property row " + row);
			db
			.insertInto(
				indexProperty,
					indexProperty.TAGINSTANCEID,
					indexProperty.PROPERTYDEFINITIONID,
					indexProperty.NAME,
					indexProperty.VALUE)
			.values(
				(Field<byte[]>)DSL.val(row.value1()),
				(Field<byte[]>)DSL.val(row.value2()),
				(Field<String>)DSL.val(row.value3()),
				(Field<String>)DSL.val(row.value4()))
			.execute();
		}
		
	}

	private void checkRepoTagReferences(DSLContext db) {
		logger.info("checking repo tagreferences");
		Result<Record8<String,Integer,byte[],byte[], Timestamp, Integer,Integer, Integer>> result = db
		.select(
			SOURCEDOCUMENT.LOCALURI, 
			USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID, 
			TAGDEFINITION.UUID, 
			TAGINSTANCE.UUID, 
			TAGDEFINITION.VERSION,
			TAGREFERENCE.CHARACTERSTART, TAGREFERENCE.CHARACTEREND,
			TAGDEFINITION.TAGDEFINITIONID)
		.from(TAGREFERENCE)
		.join(TAGINSTANCE)
			.on(TAGINSTANCE.TAGINSTANCEID.eq(TAGREFERENCE.TAGINSTANCEID))
		.join(TAGDEFINITION)
			.on(TAGDEFINITION.TAGDEFINITIONID.eq(TAGINSTANCE.TAGDEFINITIONID))
		.join(USERMARKUPCOLLECTION)
			.on(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
					.eq(TAGREFERENCE.USERMARKUPCOLLECTIONID))
		.join(SOURCEDOCUMENT)
			.on(SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USERMARKUPCOLLECTION.SOURCEDOCUMENTID))
		.limit(repoTagReferenceRowOffset, MAX_ROW_COUNT)
		.fetch();
		
		if (result.size() < MAX_ROW_COUNT) {
			repoTagReferenceRowOffset = 0;
		}
		else {
			repoTagReferenceRowOffset += result.size();
		}
		
		de.catma.repository.db.jooqgen.catmaindex.tables.Tagreference indexTagReference =
				de.catma.repository.db.jooqgen.catmaindex.Tables.TAGREFERENCE;
		
		ArrayList<Record8<String,Integer,byte[],byte[], Timestamp, Integer,Integer,Integer>> rowsNeedIndexing = 
			new  ArrayList<Record8<String,Integer,byte[],byte[],Timestamp,Integer,Integer,Integer>>();
		
		for (Record8<String,Integer,byte[],byte[],Timestamp, Integer,Integer, Integer> row : result) {
			Record1<Integer> indexedRow = db
			.selectOne()
			.from(indexTagReference)
			.where(indexTagReference.DOCUMENTID.eq(row.value1()))
			.and(indexTagReference.USERMARKUPCOLLECTIONID.eq(String.valueOf(row.value2())))
			.and(indexTagReference.TAGDEFINITIONPATH.eq(Routines.gettagdefinitionpath(row.value8())))
			.and(indexTagReference.TAGDEFINITIONID.eq(row.value3()))
			.and(indexTagReference.TAGINSTANCEID.eq(row.value4()))
			.and(indexTagReference.CHARACTERSTART.eq(row.value6()))
			.and(indexTagReference.CHARACTEREND.eq(row.value7()))
			.fetchOne();
			
			if (indexedRow == null) {
				rowsNeedIndexing.add(row);
			}
		}
		logger.info("there are " + rowsNeedIndexing.size() + " tagreference rows that need indexing");
		for (Record8<String,Integer,byte[],byte[],Timestamp, Integer,Integer,Integer> row : rowsNeedIndexing) {
			logger.info("indexing tagreference row " + row);
			db
			.insertInto(
				indexTagReference,
					indexTagReference.DOCUMENTID,
					indexTagReference.USERMARKUPCOLLECTIONID,
					indexTagReference.TAGDEFINITIONPATH,
					indexTagReference.TAGDEFINITIONID,
					indexTagReference.TAGINSTANCEID,
					indexTagReference.TAGDEFINITIONVERSION,
					indexTagReference.CHARACTERSTART,
					indexTagReference.CHARACTEREND)
			.select(db.select(
					(Field<String>)DSL.val(row.value1()),
					(Field<String>)DSL.val(String.valueOf(row.value2())),
					Routines.gettagdefinitionpath(row.value8()),
					(Field<byte[]>)DSL.val(row.value3()),
					(Field<byte[]>)DSL.val(row.value4()),
					(Field<String>)DSL.val(row.value5().toString()),
					(Field<Integer>)DSL.val(row.value6()),
					(Field<Integer>)DSL.val(row.value7())))
			.execute();
		}
	}
	
	public int getIndexPropertyRowOffset() {
		return indexPropertyRowOffset;
	}
	
	public int getIndexTagReferenceRowOffset() {
		return indexTagReferenceRowOffset;
	}
	
	public int getRepoPropertyRowOffset() {
		return repoPropertyRowOffset;
	}
	
	public int getRepoTagReferenceRowOffset() {
		return repoTagReferenceRowOffset;
	}
	
	public int getFileCleanOffset() {
		return fileCleanOffset;
	}
}
