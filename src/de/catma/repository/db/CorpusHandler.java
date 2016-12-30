/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.repository.db;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS_SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS_STATICMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS_USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_CORPUS;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_USERMARKUPCOLLECTION;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.document.AccessMode;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.repository.db.jooq.TransactionalDSLContext;
import de.catma.repository.db.mapper.CorpusMapper;
import de.catma.repository.db.mapper.IDFieldToIntegerMapper;

class CorpusHandler {
	
	private DBRepository dbRepository;
	private Map<String, Corpus> corpora;
	private DataSource dataSource;

	CorpusHandler(DBRepository dbRepository) {
		this.dbRepository = dbRepository;
		corpora = new HashMap<String, Corpus>();

		this.dataSource = CatmaDataSourceName.CATMADS.getDataSource();
	}

	Collection<Corpus> getCorpora() {
		return Collections.unmodifiableCollection(corpora.values());
	}

	void loadCorpora(DSLContext db) {
		corpora = getCorpusList(db);
	}

	private Map<String, Corpus> getCorpusList(DSLContext db) {
		Map<Integer, Result<Record2<Integer, String>>> corpusSourceDocs = db
		.select(CORPUS_SOURCEDOCUMENT.CORPUSID, SOURCEDOCUMENT.LOCALURI)
		.from(SOURCEDOCUMENT)
		.join(CORPUS_SOURCEDOCUMENT)
			.on(CORPUS_SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(SOURCEDOCUMENT.SOURCEDOCUMENTID))
		.join(USER_SOURCEDOCUMENT)
			.on(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(SOURCEDOCUMENT.SOURCEDOCUMENTID))
			.and(USER_SOURCEDOCUMENT.USERID.eq(dbRepository.getCurrentUser().getUserId()))
		.join(USER_CORPUS)
			.on(USER_CORPUS.CORPUSID.eq(CORPUS_SOURCEDOCUMENT.CORPUSID))
			.and(USER_CORPUS.USERID.eq(dbRepository.getCurrentUser().getUserId()))
		.fetchGroups(CORPUS_SOURCEDOCUMENT.CORPUSID);
		
		Map<Integer, Result<Record2<Integer, Integer>>> corpusUmcs = db
		.select(CORPUS_USERMARKUPCOLLECTION.CORPUSID, USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
		.from(USERMARKUPCOLLECTION)
		.join(CORPUS_USERMARKUPCOLLECTION)
			.on(CORPUS_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
		.join(USER_USERMARKUPCOLLECTION)
			.on(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
			.and(USER_USERMARKUPCOLLECTION.USERID.eq(dbRepository.getCurrentUser().getUserId()))
		.join(USER_CORPUS)
			.on(USER_CORPUS.CORPUSID.eq(CORPUS_USERMARKUPCOLLECTION.CORPUSID))
			.and(USER_CORPUS.USERID.eq(dbRepository.getCurrentUser().getUserId()))
		.fetchGroups(CORPUS_USERMARKUPCOLLECTION.CORPUSID);
		
		Collection<Corpus> corpusList =  db
		.select()
		.from(CORPUS)
		.join(USER_CORPUS)
			.on(USER_CORPUS.CORPUSID.eq(CORPUS.CORPUSID))
			.and(USER_CORPUS.USERID.eq(dbRepository.getCurrentUser().getUserId()))
		.fetch()
		.map(new CorpusMapper(dbRepository, corpusSourceDocs, corpusUmcs));
		
		Map<String,Corpus> result = new HashMap<String, Corpus>();
		for (Corpus corpus : corpusList) {
			result.put(corpus.getId(), corpus);
		}
				
		return result;
	}

	void createCorpus(String name) throws IOException {
		TransactionalDSLContext db = new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			db.beginTransaction();
			
			Record idRecord = db
			.insertInto(
					CORPUS,
					CORPUS.NAME)
			.values(name)
			.returning(CORPUS.CORPUSID)
			.fetchOne();
		
			Integer corpusId = idRecord.getValue(CORPUS.CORPUSID);
			
			db
			.insertInto(
				USER_CORPUS,
					USER_CORPUS.USERID,
					USER_CORPUS.CORPUSID,
					USER_CORPUS.ACCESSMODE,
					USER_CORPUS.OWNER)
			.values(
				dbRepository.getCurrentUser().getUserId(),
				corpusId,
				AccessMode.WRITE.getNumericRepresentation(),
				(byte)1)
			.execute();
			
			db.commitTransaction();
			
			Corpus corpus = 
					new Corpus(String.valueOf(corpusId), name);
			corpora.put(corpus.getId(), corpus);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
				Repository.RepositoryChangeEvent.corpusChanged.name(),
				null, corpus);

		}
		catch (Exception dae) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(dae);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}
	
	void addSourceDocument(Corpus corpus, SourceDocument sourceDocument) throws IOException {
		TransactionalDSLContext db = new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		Integer corpusId = Integer.valueOf(corpus.getId());
		getCorpusAccess(db, corpusId, true);
		
		try {
			db.beginTransaction();
			
			db.batch(
				db.insertInto(
					CORPUS_SOURCEDOCUMENT,
						CORPUS_SOURCEDOCUMENT.CORPUSID,
						CORPUS_SOURCEDOCUMENT.SOURCEDOCUMENTID)
				.select(db
					.select(DSL.value(corpusId), SOURCEDOCUMENT.SOURCEDOCUMENTID)
					.from(SOURCEDOCUMENT)
					.where(SOURCEDOCUMENT.LOCALURI.eq(sourceDocument.getID()))),
				db
				.insertInto(
					USER_SOURCEDOCUMENT,
						USER_SOURCEDOCUMENT.USERID,
						USER_SOURCEDOCUMENT.SOURCEDOCUMENTID,
						USER_SOURCEDOCUMENT.ACCESSMODE,
						USER_SOURCEDOCUMENT.OWNER)
				.select(db
					.select(
						USER_CORPUS.USERID,
						SOURCEDOCUMENT.SOURCEDOCUMENTID,
						USER_CORPUS.ACCESSMODE,
						DSL.value(0, Byte.class))
					.from(USER_CORPUS)
					.join(CORPUS_SOURCEDOCUMENT)
						.on(CORPUS_SOURCEDOCUMENT.CORPUSID.eq(USER_CORPUS.CORPUSID))
					.join(SOURCEDOCUMENT)
						.on(SOURCEDOCUMENT.SOURCEDOCUMENTID
								.eq(CORPUS_SOURCEDOCUMENT.SOURCEDOCUMENTID))
						.and(SOURCEDOCUMENT.LOCALURI.eq(sourceDocument.getID()))
					.where(USER_CORPUS.CORPUSID.eq(corpusId))
					.and(USER_CORPUS.USERID.notIn(db
						.select(USER_SOURCEDOCUMENT.USERID)
						.from(USER_SOURCEDOCUMENT)
						.join(SOURCEDOCUMENT)
							.on(SOURCEDOCUMENT.SOURCEDOCUMENTID
									.eq(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID))
							.and(SOURCEDOCUMENT.LOCALURI.eq(sourceDocument.getID()))))))
			.execute();

			db.commitTransaction();
			
			corpus.addSourceDocument(sourceDocument);
			dbRepository.getPropertyChangeSupport().firePropertyChange(
				Repository.RepositoryChangeEvent.corpusChanged.name(),
				sourceDocument, corpus);
		}
		catch (Exception dae) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(dae);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
		
	}

	
	void addUserMarkupCollectionRef(Corpus corpus,
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
	
		TransactionalDSLContext db = new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		Integer corpusId = Integer.valueOf(corpus.getId());
		Integer umcId = Integer.valueOf(userMarkupCollectionReference.getId());
		
		getCorpusAccess(db, corpusId, true);
		try {
			db.beginTransaction();
			
			db.batch(
				db
				.insertInto(
					CORPUS_USERMARKUPCOLLECTION,
						CORPUS_USERMARKUPCOLLECTION.CORPUSID,
						CORPUS_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
				.values(
					corpusId,
					umcId),
				db
				.insertInto(
					USER_USERMARKUPCOLLECTION,
						USER_USERMARKUPCOLLECTION.USERID,
						USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID,
						USER_USERMARKUPCOLLECTION.ACCESSMODE,
						USER_USERMARKUPCOLLECTION.OWNER)
				.select(db
					.select(
						USER_CORPUS.USERID,
						DSL.value(umcId),
						USER_CORPUS.ACCESSMODE,
						DSL.value(0, Byte.class))
					.from(USER_CORPUS)
					.where(USER_CORPUS.CORPUSID.eq(corpusId))
					.and(USER_CORPUS.USERID.notIn(db
						.select(USER_USERMARKUPCOLLECTION.USERID)
						.from(USER_USERMARKUPCOLLECTION)
						.where(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(umcId))))))
			.execute();

			db.commitTransaction();
			
			corpus.addUserMarkupCollectionReference(
					userMarkupCollectionReference);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
				Repository.RepositoryChangeEvent.corpusChanged.name(),
				userMarkupCollectionReference, corpus);
		}
		catch (Exception dae) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(dae);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}

	AccessMode getCorpusAccess(
			DSLContext db, int corpusId, boolean checkWriteAccess) throws IOException {
		
		Record accessModeRecord =
		db
		.select(USER_CORPUS.ACCESSMODE)
		.from(USER_CORPUS)
		.where(USER_CORPUS.USERID.eq(dbRepository.getCurrentUser().getUserId()))
		.and(USER_CORPUS.CORPUSID.eq(corpusId))
		.fetchOne();
		
		if (accessModeRecord == null) {
			throw new IOException(
					"You seem to have no access to this corpus! " +
					"Please reload the repository!");
		}
		else if (checkWriteAccess && accessModeRecord.getValue(USER_CORPUS.ACCESSMODE) 
							!= AccessMode.WRITE.getNumericRepresentation()) {
			throw new IOException(
					"You seem to have no write access to this corpus! " +
					"Please reload this corpus!");
		}
		else {
			return AccessMode.getAccessMode(accessModeRecord.getValue(USER_CORPUS.ACCESSMODE));
		}
	}

	void delete(Corpus corpus) throws IOException {
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		Integer corpusId = Integer.valueOf(corpus.getId());
		
		Field<Integer> totalParticipantsField = db
		.select(DSL.count(USER_CORPUS.USERID))
		.from(USER_CORPUS)
		.where(USER_CORPUS.CORPUSID.eq(corpusId))
		.asField("totalParticipants");
		
		Record currentUserCorpusRecord = db
		.select(
				USER_CORPUS.USER_CORPUSID, 
				USER_CORPUS.ACCESSMODE, 
				USER_CORPUS.OWNER,
				USER_CORPUS.CORPUSID, 
			totalParticipantsField)
		.from(USER_CORPUS)
		.where(USER_CORPUS.USERID.eq(dbRepository.getCurrentUser().getUserId()))
		.and(USER_CORPUS.CORPUSID.eq(corpusId))
		.fetchOne();
		
		
		if ((currentUserCorpusRecord == null) 
				|| (currentUserCorpusRecord.getValue(USER_SOURCEDOCUMENT.ACCESSMODE) == null)) {
			throw new IllegalStateException(
					"you seem to have no access rights for this corpus!");
		}
		
		boolean isOwner = 
				currentUserCorpusRecord.getValue(USER_CORPUS.OWNER, Boolean.class);
		
		int totalParticipants = 
				(Integer)currentUserCorpusRecord.getValue("totalParticipants");
		try {
			db.beginTransaction();

			db
			.delete(USER_CORPUS)
			.where(USER_CORPUS.USER_CORPUSID
				.eq(currentUserCorpusRecord.getValue(USER_CORPUS.USER_CORPUSID)))
			.execute();
			
			if (isOwner 
					&& (totalParticipants == 1)) {
				
				db.batch(
					db
					.delete(CORPUS_SOURCEDOCUMENT)
					.where(CORPUS_SOURCEDOCUMENT.CORPUSID.eq(corpusId)),
					db
					.delete(CORPUS_USERMARKUPCOLLECTION)
					.where(CORPUS_USERMARKUPCOLLECTION.CORPUSID.eq(corpusId)),
					db
					.delete(CORPUS_STATICMARKUPCOLLECTION)
					.where(CORPUS_STATICMARKUPCOLLECTION.CORPUSID.eq(corpusId)),
					db
					.delete(CORPUS)
					.where(CORPUS.CORPUSID.eq(corpusId)))
				.execute();
				
			}
			
			db.commitTransaction();

			corpora.remove(corpus);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					Repository.RepositoryChangeEvent.corpusChanged.name(),
					corpus, null);

		}
		catch (Exception dae) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(dae);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}
	
	void rename(Corpus corpus, String name) throws IOException {
		String oldName = corpus.toString();

		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		Integer corpusId = Integer.valueOf(corpus.getId());
		getCorpusAccess(db, corpusId, true);
		db
		.update(CORPUS)
		.set(CORPUS.NAME, name)
		.where(CORPUS.CORPUSID.eq(corpusId))
		.execute();
		
		corpus.setName(name);
		
		dbRepository.getPropertyChangeSupport().firePropertyChange(
				Repository.RepositoryChangeEvent.corpusChanged.name(),
				oldName, corpus);
	}
	
	void reloadCorpora(DSLContext db) {
		Map<String, Corpus> result = getCorpusList(db);
		for (Map.Entry<String, Corpus> entry : result.entrySet()) {

			if (!corpora.containsKey(entry.getKey())) {
				corpora.put(
						entry.getKey(), entry.getValue());
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						Repository.RepositoryChangeEvent.corpusChanged.name(),
						null, entry.getValue());
			}
			else {
				Corpus oldCorpus = corpora.get(entry.getKey());
				
				corpora.put(
						entry.getKey(), entry.getValue());
				
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						Repository.RepositoryChangeEvent.corpusChanged.name(),
						oldCorpus, entry.getValue());
			}
		}
		
		Iterator<Map.Entry<String,Corpus>> iter = 
				corpora.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, Corpus> entry = iter.next();
			Corpus corpus = entry.getValue();
			if (!result.containsKey(corpus.getId())) {
				iter.remove();
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						Repository.RepositoryChangeEvent.corpusChanged.name(),
						corpus, null);
			}
		}

	}

	void copyCorpora(int sourceUserId) throws IOException {
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		Map<Integer, Result<Record3<Integer,Integer,String>>> corpusSourceDocs = db
		.select(CORPUS_SOURCEDOCUMENT.CORPUSID, CORPUS_SOURCEDOCUMENT.SOURCEDOCUMENTID, SOURCEDOCUMENT.LOCALURI)
		.from(SOURCEDOCUMENT)
		.join(CORPUS_SOURCEDOCUMENT)
			.on(CORPUS_SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(SOURCEDOCUMENT.SOURCEDOCUMENTID))
		.join(USER_SOURCEDOCUMENT)
			.on(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(SOURCEDOCUMENT.SOURCEDOCUMENTID))
			.and(USER_SOURCEDOCUMENT.USERID.eq(sourceUserId))
		.join(USER_CORPUS)
			.on(USER_CORPUS.CORPUSID.eq(CORPUS_SOURCEDOCUMENT.CORPUSID))
			.and(USER_CORPUS.USERID.eq(sourceUserId))
		.fetchGroups(CORPUS_SOURCEDOCUMENT.CORPUSID);
		
		Map<Integer, Result<Record3<Integer, Integer, Integer>>> corpusUmcs = db
		.select(CORPUS_USERMARKUPCOLLECTION.CORPUSID, USERMARKUPCOLLECTION.SOURCEDOCUMENTID, USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
		.from(USERMARKUPCOLLECTION)
		.join(CORPUS_USERMARKUPCOLLECTION)
			.on(CORPUS_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
		.join(USER_USERMARKUPCOLLECTION)
			.on(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
			.and(USER_USERMARKUPCOLLECTION.USERID.eq(sourceUserId))
		.join(USER_CORPUS)
			.on(USER_CORPUS.CORPUSID.eq(CORPUS_USERMARKUPCOLLECTION.CORPUSID))
			.and(USER_CORPUS.USERID.eq(sourceUserId))
		.fetchGroups(CORPUS_USERMARKUPCOLLECTION.CORPUSID);
		
		Collection<Corpus> corpusList =  db
		.select()
		.from(CORPUS)
		.join(USER_CORPUS)
			.on(USER_CORPUS.CORPUSID.eq(CORPUS.CORPUSID))
			.and(USER_CORPUS.USERID.eq(sourceUserId))
		.fetch()
		.map(new CorpusMapper(dbRepository, Collections.emptyMap(), Collections.emptyMap()));
		
	
		try {
			for (Corpus corpus : corpusList) {
				
				try {
					db.beginTransaction();
					
					Record idRecord = db
					.insertInto(
							CORPUS,
							CORPUS.NAME)
					.values(corpus.toString())
					.returning(CORPUS.CORPUSID)
					.fetchOne();
					
					Integer corpusId = idRecord.getValue(CORPUS.CORPUSID);
					
					db
					.insertInto(
							USER_CORPUS,
							USER_CORPUS.USERID,
							USER_CORPUS.CORPUSID,
							USER_CORPUS.ACCESSMODE,
							USER_CORPUS.OWNER)
					.values(
							dbRepository.getCurrentUser().getUserId(),
							corpusId,
							AccessMode.WRITE.getNumericRepresentation(),
							(byte)1)
					.execute();
					
					db.commitTransaction();
					
					// share SourceDocs
					for (Record3<Integer,Integer,String>  sourceDocInfo : 
						corpusSourceDocs.get(Integer.valueOf(corpus.getId()))) {
						Integer sourceDocumentId = 
								sourceDocInfo.getValue(CORPUS_SOURCEDOCUMENT.SOURCEDOCUMENTID);
						String sourceDocLocalURI = 
								sourceDocInfo.getValue(SOURCEDOCUMENT.LOCALURI);
						
						SourceDocument sd = shareCorpusSourceDocument(db, sourceDocLocalURI, corpusId);
						
						// copy and import Markup Collections
						Result<Record3<Integer,Integer,Integer>> umcInfoResult = 
								corpusUmcs.get(Integer.valueOf(corpus.getId()));
						if (umcInfoResult != null) {
							for (Record3<Integer,Integer,Integer> umcInfo : umcInfoResult) {
								Integer umcSourceDocumentId = 
										umcInfo.getValue(USERMARKUPCOLLECTION.SOURCEDOCUMENTID);
								if (sourceDocumentId.intValue() == umcSourceDocumentId.intValue()) {
									Integer userMarkupCollectionId =
											umcInfo.getValue(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID);
								
									// the import is not a transaction by intention
									// import can become a pretty long running operation
									// and would block other users 
									// the last DB action prior to indexing 
									// is the linking between the umc and the user
									// so in case of failure the user won't have access to the
									// corrupt umc and the DB cleaner job will take care of it
									dbRepository.getDbUserMarkupCollectionHandler().importUserMarkupCollection(
											db, userMarkupCollectionId, sd, corpusId);
								}
							}
						}						
					}
				}
				catch (Exception dae) {
					db.rollbackTransaction();
					db.close();
					throw new IOException(dae);
				}
			}
			
			loadCorpora(db);
		}		
		finally {
			if (db!=null) {
				db.close();
			}
		}

		
	}

	private SourceDocument shareCorpusSourceDocument(
			DSLContext db, String sourceDocLocalURI, Integer corpusId) throws IOException {
		Integer sourceDocumentId = db
		.select(SOURCEDOCUMENT.SOURCEDOCUMENTID)
		.from(SOURCEDOCUMENT)
		.where(SOURCEDOCUMENT.LOCALURI.eq(sourceDocLocalURI))
		.fetchOne()
		.value1();
		
		
		 Record record = db
		.select(USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID)
		.from(USER_SOURCEDOCUMENT)
		.where(USER_SOURCEDOCUMENT.USERID.eq(dbRepository.getCurrentUser().getUserId()))
		.and(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(sourceDocumentId))
		.fetchOne();
		
		Integer userSourceDocId = null;
		
		if (record != null) {
			userSourceDocId = 
				record.map(new IDFieldToIntegerMapper(
						USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID));
		}
		
		if (userSourceDocId == null) {
			db
			.insertInto(
				USER_SOURCEDOCUMENT,
					USER_SOURCEDOCUMENT.USERID,
					USER_SOURCEDOCUMENT.SOURCEDOCUMENTID,
					USER_SOURCEDOCUMENT.ACCESSMODE,
					USER_SOURCEDOCUMENT.OWNER)
			.values(
				dbRepository.getCurrentUser().getUserId(),
				sourceDocumentId,
				AccessMode.WRITE.getNumericRepresentation(),
				(byte)0)
			.execute();
		}
		else { // eventually upgrade accessmode
			db
			.update(USER_SOURCEDOCUMENT)
			.set(USER_SOURCEDOCUMENT.ACCESSMODE, AccessMode.WRITE.getNumericRepresentation())
			.where(USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID.eq(userSourceDocId))
			.execute();
		}
		
		db
		.insertInto(
			CORPUS_SOURCEDOCUMENT,
				CORPUS_SOURCEDOCUMENT.CORPUSID,
				CORPUS_SOURCEDOCUMENT.SOURCEDOCUMENTID)
		.values(
			corpusId,
			sourceDocumentId)
		.execute();
		
		
		return dbRepository.getDbSourceDocumentHandler().getSourceDocument(sourceDocumentId);
	}
	
}
