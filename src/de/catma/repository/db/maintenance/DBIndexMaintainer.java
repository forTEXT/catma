package de.catma.repository.db.maintenance;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGINSTANCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGREFERENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record8;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.repository.db.CatmaDataSourceName;
import de.catma.repository.db.jooqgen.catmaindex.Routines;

public class DBIndexMaintainer {
	
	private static final int MAX_ROW_COUNT = 10;
	private int tagReferenceRowOffset = 0;

	private Logger logger;

	public DBIndexMaintainer() {
		this.logger = Logger.getLogger(DBIndexMaintainer.class.getName());
	}
	
	public void run() throws IOException {
		UserManager userManager = new UserManager();
		try {
			userManager.lockLogin();
			
			if (userManager.getUserCount() == 0 ) {
				Context  context = new InitialContext();
				DataSource dataSource = (DataSource) context.lookup(
						CatmaDataSourceName.CATMADS.name());
				DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
	
				// all repo.tagreferences need an entry in index.tagref
				// all repo.propertyvalues need an entry in index.property
				
				// delete all index.tagref that are no longer in repo.tagreference
				// delete all index.proeprty that are no longer in repo.propertyvalue
				
				checkTagReferences(db);
			}
			
			userManager.unlockLogin();
		}
		catch (Exception e) {
			userManager.unlockLogin();
			throw new IOException(e);
		}	
	}

	private void checkTagReferences(DSLContext db) {
		
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
			.on(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(TAGREFERENCE.USERMARKUPCOLLECTIONID))
		.join(SOURCEDOCUMENT)
			.on(SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USERMARKUPCOLLECTION.SOURCEDOCUMENTID))
		.limit(tagReferenceRowOffset, MAX_ROW_COUNT)
		.fetch();
		
		tagReferenceRowOffset += MAX_ROW_COUNT;
		
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
			.and(indexTagReference.TAGDEFINITIONID.eq(row.value3()))
			.and(indexTagReference.TAGINSTANCEID.eq(row.value4()))
			.and(indexTagReference.CHARACTERSTART.eq(row.value6()))
			.and(indexTagReference.CHARACTEREND.eq(row.value7()))
			.fetchOne();
			
			if (indexedRow == null) {
				rowsNeedIndexing.add(row);
			}
		}
		
		for (Record8<String,Integer,byte[],byte[],Timestamp, Integer,Integer,Integer> row : rowsNeedIndexing) {
			Field<String> f = DSL.val(row.value1());
			
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
}
