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

import static de.catma.repository.db.jooq.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooq.catmarepository.Tables.UNSEPARABLE_CHARSEQUENCE;
import static de.catma.repository.db.jooq.catmarepository.Tables.USERDEFINED_SEPARATINGCHARACTER;
import static de.catma.repository.db.jooq.catmarepository.Tables.USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooq.catmarepository.Tables.USER_SOURCEDOCUMENT;
import static de.catma.repository.db.jooq.catmarepository.Tables.USER_USERMARKUPCOLLECTION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.db.CloseableSession;
import de.catma.document.repository.AccessMode;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.TechInfoSet;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.db.DBIndexer;
import de.catma.repository.db.model.DBCorpusSourceDocument;
import de.catma.repository.db.model.DBSourceDocument;
import de.catma.repository.db.model.DBUserSourceDocument;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

class DBSourceDocumentHandler {

	private static final String SOURCEDOCS_FOLDER = "sourcedocuments";
	static final String REPO_URI_SCHEME = "catma://";
	
	private DBRepository dbRepository;
	private String sourceDocsPath;
	private Map<String,SourceDocument> sourceDocumentsByID;
	private DataSource dataSource;

	public DBSourceDocumentHandler(
			DBRepository dbRepository, String repoFolderPath) throws NamingException {
		this.dbRepository = dbRepository;
		this.sourceDocsPath = repoFolderPath + 
			"/" + 
			SOURCEDOCS_FOLDER + "/";
		File file = new File(sourceDocsPath);
		if (!file.exists()) {
			if (!file.mkdirs()) {
				throw new IllegalStateException(
					"cannot access/create repository folder " +  sourceDocsPath);
			}
		}
		this.sourceDocumentsByID = new HashMap<String, SourceDocument>();
		Context  context = new InitialContext();
		this.dataSource = (DataSource) context.lookup("catmads");
	}
	
	public String getIDFromURI(URI uri) {
		if (uri.getScheme().toLowerCase().equals("file")) {
			File file = new File(uri);
			return REPO_URI_SCHEME + file.getName();
		}
		else {
			return REPO_URI_SCHEME + new IDGenerator().generate();
		}
		
	}
	
	private void insertIntoFS(SourceDocument sourceDocument) throws IOException {

		try {
			File sourceTempFile = 
					new File(new URI(
						getFileURL(
								sourceDocument.getID(), 
								dbRepository.getTempDir() + "/" )));
					
			File repoSourceFile = 
					new File(
							this.sourceDocsPath
							+ sourceTempFile.getName());
			
			FileInputStream sourceTempFileStream = 
					new FileInputStream(sourceTempFile);
			FileOutputStream repoSourceFileOutputStream = 
					new FileOutputStream(repoSourceFile);
			try {
				IOUtils.copy(sourceTempFileStream, repoSourceFileOutputStream);
			}
			finally {
				CloseSafe.close(sourceTempFileStream);
				CloseSafe.close(repoSourceFileOutputStream);
			}
			
			sourceTempFile.delete();
		}
		catch (URISyntaxException se) {
			throw new IOException(se);
		}
	}
	
	
	String getFileURL(String catmaID, String... path) {
		StringBuilder builder = new StringBuilder("file://");
		for (String folder : path) {
			builder.append(folder);
		}
		builder.append(catmaID.substring((REPO_URI_SCHEME).length()));
		return builder.toString();
	}

	void insert(final SourceDocument sourceDocument) throws IOException {
		
		dbRepository.getBackgroundServiceProvider().submit(
			"Adding Source Document...",
			new DefaultProgressCallable<String>() {
				public String call() throws Exception {
					
					TransactionalDSLContext db = new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
					try {
						ContentInfoSet contentInfoSet = 
								sourceDocument.getSourceContentHandler()
									.getSourceDocumentInfo().getContentInfoSet();
						TechInfoSet techInfoSet = 
								sourceDocument.getSourceContentHandler()
									.getSourceDocumentInfo().getTechInfoSet();
						IndexInfoSet indexInfoSet = 
								sourceDocument.getSourceContentHandler()
									.getSourceDocumentInfo().getIndexInfoSet();
						
						db.beginTransaction();
						Record idRecord = db
						.insertInto(
							SOURCEDOCUMENT,
								SOURCEDOCUMENT.TITLE,
								SOURCEDOCUMENT.PUBLISHER,
								SOURCEDOCUMENT.AUTHOR,
								SOURCEDOCUMENT.DESCRIPTION,
								SOURCEDOCUMENT.SOURCEURI,
								SOURCEDOCUMENT.FILETYPE,
								SOURCEDOCUMENT.CHARSET,
								SOURCEDOCUMENT.FILEOSTYPE,
								SOURCEDOCUMENT.CHECKSUM,
								SOURCEDOCUMENT.MIMETYPE,
								SOURCEDOCUMENT.XSLTDOCUMENTLOCALURI,
								SOURCEDOCUMENT.LOCALE,
								SOURCEDOCUMENT.LOCALURI)
						.values(
							contentInfoSet.getTitle(),
							contentInfoSet.getPublisher(),
							contentInfoSet.getAuthor(),
							contentInfoSet.getDescription(),
							(techInfoSet.getURI().getScheme().equals("file")?null:
								techInfoSet.getURI().toString()),
							techInfoSet.getFileType().name(),
							(techInfoSet.getCharset()==null)?null:techInfoSet.getCharset().toString(),
							techInfoSet.getFileOSType().name(),
							techInfoSet.getChecksum(),
							techInfoSet.getMimeType(),
							techInfoSet.getXsltDocumentLocalUri(),
							indexInfoSet.getLocale().toString(),
							sourceDocument.getID())
						.returning(SOURCEDOCUMENT.SOURCEDOCUMENTID)
						.fetchOne();

						Integer sourceDocumentId = 
								idRecord.getValue(SOURCEDOCUMENT.SOURCEDOCUMENTID);
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
							(byte)1)
						.execute();
						
						for (Character udsc : 
							indexInfoSet.getUserDefinedSeparatingCharacters()) {
							db
							.insertInto(
								USERDEFINED_SEPARATINGCHARACTER,
									USERDEFINED_SEPARATINGCHARACTER.CHR,
									USERDEFINED_SEPARATINGCHARACTER.SOURCEDOCUMENTID)
							.values(
								String.valueOf(udsc),
								sourceDocumentId)
							.execute();
						}
						
						for (String ucs : 
							indexInfoSet.getUnseparableCharacterSequences()) {
							db
							.insertInto(
								UNSEPARABLE_CHARSEQUENCE,
									UNSEPARABLE_CHARSEQUENCE.CHARSEQUENCE,
									UNSEPARABLE_CHARSEQUENCE.SOURCEDOCUMENTID)
							.values(
								ucs,
								sourceDocumentId)
							.execute();
						}
						
						insertIntoFS(sourceDocument);
						getProgressListener().setProgress(
								"Indexing Source Document");
						
						dbRepository.getIndexer().index(sourceDocument);

						db.commitTransaction();
						return sourceDocument.getID();
								
					}
					catch (DataAccessException dae) {
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
			},
			new ExecutionListener<String>() {
				public void done(String documentID) {
					sourceDocumentsByID.put(
							documentID, sourceDocument);
			
					dbRepository.getPropertyChangeSupport().firePropertyChange(
							RepositoryChangeEvent.sourceDocumentChanged.name(),
							null, sourceDocument.getID());
				}
				public void error(Throwable t) {
					dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.exceptionOccurred.name(),
						null, t);
				}
			}
		);
	}
	
	
	void loadSourceDocuments(DSLContext db) 
			throws URISyntaxException, IOException, InstantiationException, IllegalAccessException {
		this.sourceDocumentsByID = getSourceDocumentList(db);
	}

	private Map<String,SourceDocument> getSourceDocumentList(DSLContext db) {
		Map<String,SourceDocument> result = new HashMap<String, SourceDocument>();

		if (!dbRepository.getCurrentUser().isLocked()) {
			
			Map<Integer, Result<Record>> userDefSepCharsRecords = db
			.select()
			.from(USERDEFINED_SEPARATINGCHARACTER)
			.join(USER_SOURCEDOCUMENT)
				.on(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USERDEFINED_SEPARATINGCHARACTER.SOURCEDOCUMENTID))
				.and(USER_SOURCEDOCUMENT.USERID.eq(dbRepository.getCurrentUser().getUserId()))
			.fetchGroups(USERDEFINED_SEPARATINGCHARACTER.SOURCEDOCUMENTID);
			
			Map<Integer, Result<Record>> unseparableCharSeqRecords = db 
			.select()
			.from(UNSEPARABLE_CHARSEQUENCE)
			.join(USER_SOURCEDOCUMENT)
				.on(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(UNSEPARABLE_CHARSEQUENCE.SOURCEDOCUMENTID))
				.and(USER_SOURCEDOCUMENT.USERID.eq(dbRepository.getCurrentUser().getUserId()))
			.fetchGroups(UNSEPARABLE_CHARSEQUENCE.SOURCEDOCUMENTID);
			
			Map<Integer, Result<Record>> userMarkupCollectionRecords = db
			.select()
			.from(USERMARKUPCOLLECTION)
			.join(USER_USERMARKUPCOLLECTION)
				.on(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
						.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
				.and(USER_USERMARKUPCOLLECTION.USERID.eq(dbRepository.getCurrentUser().getUserId()))
			.fetchGroups(USERMARKUPCOLLECTION.SOURCEDOCUMENTID);
			
			List<SourceDocument> resultlist = db
			.select()
			.from(SOURCEDOCUMENT)
			.join(USER_SOURCEDOCUMENT)
				.on(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(SOURCEDOCUMENT.SOURCEDOCUMENTID))
				.and(USER_SOURCEDOCUMENT.USERID.eq(dbRepository.getCurrentUser().getUserId()))
			.fetch()
			.map(new SourceDocumentMapper(
					sourceDocsPath, 
					userDefSepCharsRecords, unseparableCharSeqRecords,
					userMarkupCollectionRecords));
		
			for (SourceDocument sd : resultlist) {
				result.put(sd.getID(), sd);
			}
		}
		
		return result;
	}


	Collection<SourceDocument> getSourceDocuments() {
		return  Collections.unmodifiableCollection(sourceDocumentsByID.values());
	}
	
	SourceDocument getSourceDocument(String id) {
		return sourceDocumentsByID.get(id);
	}
	
	String getLocalUriFor(UserMarkupCollectionReference umcRef) {
		for (SourceDocument sd : getSourceDocuments()) {
			for (UserMarkupCollectionReference ref : sd.getUserMarkupCollectionRefs()) {
				if (ref.getId().equals(umcRef.getId())) {
					return sd.getID();
				}
			}
		}
		
		return null;
	}
	
	DBSourceDocument getDbSourceDocument(Session session, String localUri) {
		Criteria criteria = session.createCriteria(DBSourceDocument.class)
			     .add(Restrictions.eq("localUri", localUri));
		
		DBSourceDocument result = (DBSourceDocument) criteria.uniqueResult();
		
		return result;
	}

	public void update(
			final SourceDocument sourceDocument, 
			final ContentInfoSet contentInfoSet) {
		
		final String localUri = sourceDocument.getID();
		final String author = contentInfoSet.getAuthor();
		final String publisher = contentInfoSet.getPublisher();
		final String title = contentInfoSet.getTitle();
		final String description = contentInfoSet.getDescription();
		
		dbRepository.getBackgroundServiceProvider().submit(
				"Update Source Document...",
				new DefaultProgressCallable<Void>() {
					public Void call() throws Exception {
						DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
						checkSourceDocumentAccess(db, localUri, true);

						db
						.update(SOURCEDOCUMENT)
						.set(SOURCEDOCUMENT.AUTHOR, author)
						.set(SOURCEDOCUMENT.TITLE, title)
						.set(SOURCEDOCUMENT.DESCRIPTION, description)
						.set(SOURCEDOCUMENT.PUBLISHER, publisher)
						.where(SOURCEDOCUMENT.LOCALURI.eq(localUri))
						.execute();
						
						return null;
					}
				},
				new ExecutionListener<Void>() {
					public void done(Void nothing) {
						sourceDocument.getSourceContentHandler().getSourceDocumentInfo().setContentInfoSet(
								contentInfoSet);
						dbRepository.getPropertyChangeSupport().firePropertyChange(
								RepositoryChangeEvent.sourceDocumentChanged.name(),
								sourceDocument.getID(), sourceDocument);						
					}
					public void error(Throwable t) {
						dbRepository.getPropertyChangeSupport().firePropertyChange(
								RepositoryChangeEvent.exceptionOccurred.name(),
								null, t);
					}
				}
			);
						
	}

	void close() {
		for (SourceDocument sourceDocument : sourceDocumentsByID.values()) {
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.sourceDocumentChanged.name(),
					sourceDocument, null);
		}
		sourceDocumentsByID.clear();
	}

	public SourceDocument getSourceDocument(UserMarkupCollectionReference umcRef) {
		for (SourceDocument sd : sourceDocumentsByID.values()) {
			if (sd.getUserMarkupCollectionReference(umcRef.getId()) != null) {
				return sd;
			}
		}
		return null;
	}

	public void remove2(SourceDocument sourceDocument) throws IOException {
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		try {
			Field<Integer> totalParticipantsField = db
			.select(DSL.count(USER_SOURCEDOCUMENT.USERID))
			.from(USER_SOURCEDOCUMENT)
			.join(SOURCEDOCUMENT)
				.on(SOURCEDOCUMENT.SOURCEDOCUMENTID
						.eq(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID))
			.asField("totalParticipants");
			
			
			Record currentUserSourceDocRecord = db
			.select(
				USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID, 
				USER_SOURCEDOCUMENT.ACCESSMODE, 
				USER_SOURCEDOCUMENT.OWNER, 
				totalParticipantsField)
			.from(USER_SOURCEDOCUMENT)
			.join(SOURCEDOCUMENT)
				.on(SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID))
			.where(USER_SOURCEDOCUMENT.USERID.eq(dbRepository.getCurrentUser().getUserId()))
			.fetchOne();
			
			
			if ((currentUserSourceDocRecord == null) 
					|| (currentUserSourceDocRecord.getValue(USER_SOURCEDOCUMENT.ACCESSMODE) == null)) {
				throw new IllegalStateException(
						"you seem to have no access rights for this document!");
			}
			
			boolean isOwner = 
				(currentUserSourceDocRecord.getValue(USER_SOURCEDOCUMENT.OWNER).intValue()==0)?false:true;
			
			int totalParticipants = 
					(Integer)currentUserSourceDocRecord.getValue("totalParticipants");
			
			db.beginTransaction();
			if (!isOwner 
					|| (totalParticipants > 1)) {
				db
				.delete(USER_SOURCEDOCUMENT)
				.where(USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID
					.eq(currentUserSourceDocRecord.getValue(USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID)));
			}
			else {
				
			}
			
			
			db.commitTransaction();
		}
		catch (DataAccessException dae) {
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
	@SuppressWarnings("unchecked")
	public void remove(SourceDocument sourceDocument) throws IOException {

		Session session = dbRepository.getSessionFactory().openSession();
		try {
			DBSourceDocument dbSourceDocument = 
					getDbSourceDocument(session, sourceDocument.getID());
			Set<DBUserSourceDocument> dbUserSourceDocuments = 
					dbSourceDocument.getDbUserSourceDocuments();
			
			DBUserSourceDocument currentUserSourceDocument = null;
			for (DBUserSourceDocument dbUserSourceDocument : dbUserSourceDocuments) {
				if (dbUserSourceDocument.getDbUser().getUserId().equals(
						dbRepository.getCurrentUser().getUserId())) {
					currentUserSourceDocument = dbUserSourceDocument;
					break;
				}
			}
			if (currentUserSourceDocument == null) {
				throw new IllegalStateException(
						"you seem to have no access rights for this document!");
			}
			
			session.beginTransaction();
			if (!currentUserSourceDocument.isOwner() 
					|| (dbUserSourceDocuments.size() > 1)) {
				session.delete(currentUserSourceDocument);
			}
			else {
				session.delete(currentUserSourceDocument);
				
				Criteria criteria = 
						session.createCriteria(DBCorpusSourceDocument.class).add(
								Restrictions.eq("dbSourceDocument", dbSourceDocument));
				
				if (!criteria.list().isEmpty()) {
					for (DBCorpusSourceDocument dbCorpusSourceDocument 
							: (List<DBCorpusSourceDocument>)criteria.list()) {
						session.delete(dbCorpusSourceDocument);				
					}
				}
				for (UserMarkupCollectionReference umcRef : 
					sourceDocument.getUserMarkupCollectionRefs()) {
					dbRepository.getDbUserMarkupCollectionHandler().delete(session,umcRef);
				}
				
				if (dbRepository.getIndexer() instanceof DBIndexer) {
					((DBIndexer)dbRepository.getIndexer()).removeSourceDocument(
							session, sourceDocument.getID());
				}
				else {
					dbRepository.getIndexer().removeSourceDocument(
							sourceDocument.getID());	
				}
				
				session.delete(dbSourceDocument);
			}				
			session.getTransaction().commit();
			
			sourceDocumentsByID.remove(sourceDocument.getID());
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.sourceDocumentChanged.name(),
					sourceDocument, null);
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
	}

	void reloadSourceDocuments(DSLContext db) 
			throws URISyntaxException, IOException, 
			InstantiationException, IllegalAccessException {
		
		Map<String,SourceDocument> result = getSourceDocumentList(db);
		
		for (Map.Entry<String, SourceDocument> entry : result.entrySet()) {
			// new document?
			SourceDocument oldDoc = sourceDocumentsByID.get(entry.getKey());
			sourceDocumentsByID.put(
					entry.getKey(), entry.getValue());
			if (oldDoc == null) {
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.sourceDocumentChanged.name(),
						null, entry.getKey());
			}
			else {
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.sourceDocumentChanged.name(),
						oldDoc.getID(),
						entry.getValue());						
			}
			reloadUserMarkupCollections(oldDoc, entry.getValue());
			
		}
		
		Iterator<Map.Entry<String,SourceDocument>> iter = sourceDocumentsByID.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, SourceDocument> entry = iter.next();
			SourceDocument sd = entry.getValue();
			if (!result.containsKey(sd.getID())) {
				iter.remove();
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.sourceDocumentChanged.name(),
						sd, null);
				
			}
		}
	}

	private void reloadUserMarkupCollections(SourceDocument oldDoc, SourceDocument sd) {
		for (UserMarkupCollectionReference umcRef : sd.getUserMarkupCollectionRefs()) {
			UserMarkupCollectionReference oldUmcRef = null;
			
			if (oldDoc != null) { 
				oldUmcRef = oldDoc.getUserMarkupCollectionReference(umcRef.getId());
			}
			
			if (oldUmcRef == null) {
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.userMarkupCollectionChanged.name(),
						null, new Pair<UserMarkupCollectionReference, SourceDocument>(
								umcRef, sd));
			}
			else {
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.userMarkupCollectionChanged.name(),
						oldUmcRef.getContentInfoSet(), umcRef);
			}
		}
		if (oldDoc != null) {
			for (UserMarkupCollectionReference umcRef : oldDoc.getUserMarkupCollectionRefs()) {
				if (sd.getUserMarkupCollectionReference(umcRef.getId()) == null) {
					dbRepository.getPropertyChangeSupport().firePropertyChange(
							RepositoryChangeEvent.userMarkupCollectionChanged.name(),
							umcRef, null);
				}
			}
		}		
	}

	void checkSourceDocumentAccess(
			DSLContext db, String localUri, boolean checkWriteAccess) throws IOException {
		Record accessModeRecord =
		db
		.select(USER_SOURCEDOCUMENT.ACCESSMODE)
		.from(USER_SOURCEDOCUMENT)
		.join(SOURCEDOCUMENT)
			.on(SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID))
			.and(SOURCEDOCUMENT.LOCALURI.eq(localUri))
		.where(USER_SOURCEDOCUMENT.USERID.eq(dbRepository.getCurrentUser().getUserId()))
		.fetchOne();
		
		if (accessModeRecord == null) {
			throw new IOException(
					"You seem to have no access to this Document! " +
					"Please reload the repository!");
		}
		else if (checkWriteAccess && accessModeRecord.getValue(USER_SOURCEDOCUMENT.ACCESSMODE) 
							!= AccessMode.WRITE.getNumericRepresentation()) {
			throw new IOException(
					"You seem to have no write access to this document! " +
					"Please reload this document!");
		}
	}
	
	Pair<DBSourceDocument, DBUserSourceDocument> getSourceDocumentAccess(
			Session session, String localUri, boolean checkWriteAccess) throws IOException {
	
		DBSourceDocument dbSourceDocument = getDbSourceDocument(session, localUri);
		
		Set<DBUserSourceDocument> dbUserSourceDocuments = 
				dbSourceDocument.getDbUserSourceDocuments();
		
		DBUserSourceDocument currentUserSourceDocument = null;
		for (DBUserSourceDocument dbUserSourceDocument : dbUserSourceDocuments) {
			if (dbUserSourceDocument.getDbUser().getUserId().equals(
					dbRepository.getCurrentUser().getUserId())) {
				currentUserSourceDocument = dbUserSourceDocument;
				break;
			}
		}

		if (currentUserSourceDocument == null) {
			throw new IOException(
					"You seem to have no access to this Document! " +
					"Please reload the repository!");
		}
		else if (checkWriteAccess && currentUserSourceDocument.getAccessMode() 
							!= AccessMode.WRITE.getNumericRepresentation()) {
			throw new IOException(
					"You seem to have no write access to this document! " +
					"Please reload this document!");
		}
		else {
			return new Pair<DBSourceDocument, DBUserSourceDocument>(
					dbSourceDocument, currentUserSourceDocument);
		}
	}

}
