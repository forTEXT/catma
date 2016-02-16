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

import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.UNSEPARABLE_CHARSEQUENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERDEFINED_SEPARATINGCHARACTER;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_USERMARKUPCOLLECTION;

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

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.document.AccessMode;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.TechInfoSet;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.repository.db.jooq.TransactionalDSLContext;
import de.catma.repository.db.mapper.IDFieldToIntegerMapper;
import de.catma.repository.db.mapper.SourceDocumentMapper;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

/**
 * This class is public only for implementation reasons, do not use outside this package or its subpackages.
 * 
 * @author marco.petris@web.de
 *
 */
public class SourceDocumentHandler {

	static final String SOURCEDOCS_FOLDER = "sourcedocuments";
	public static final String REPO_URI_SCHEME = "catma://";
	
	private DBRepository dbRepository;
	private String sourceDocsPath;
	private Map<String,SourceDocument> sourceDocumentsByID;
	private DataSource dataSource;

	public SourceDocumentHandler(
			DBRepository dbRepository, String repoFolderPath) {
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
		this.dataSource = CatmaDataSourceName.CATMADS.getDataSource();
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
			
			sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().setURI(
					new URI(
						new FileURLFactory().getFileURL(sourceDocument.getID(), 
						sourceDocsPath)));
			
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
			Integer sourceDocumentId = db
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
			.fetchOne()
			.map(new IDFieldToIntegerMapper(SOURCEDOCUMENT.SOURCEDOCUMENTID));

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
			
			db.commitTransaction();
			
			dbRepository.getIndexer().index(
				sourceDocument, 
				dbRepository.getBackgroundServiceProvider().getBackgroundService());

			sourceDocumentsByID.put(
					sourceDocument.getID(), sourceDocument);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.sourceDocumentChanged.name(),
					null, sourceDocument.getID());
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, e);			}
		finally {
			if (db!=null) {
				db.close();
			}
		}
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
	
	public void update(
			final SourceDocument sourceDocument, 
			final ContentInfoSet contentInfoSet) {
		
		final String localUri = sourceDocument.getID();
		final String author = contentInfoSet.getAuthor();
		final String publisher = contentInfoSet.getPublisher();
		final String title = contentInfoSet.getTitle();
		final String description = contentInfoSet.getDescription();
		try {
			DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
			getSourceDocumentAccess(db, localUri, true);
	
			db
			.update(SOURCEDOCUMENT)
			.set(SOURCEDOCUMENT.AUTHOR, author)
			.set(SOURCEDOCUMENT.TITLE, title)
			.set(SOURCEDOCUMENT.DESCRIPTION, description)
			.set(SOURCEDOCUMENT.PUBLISHER, publisher)
			.where(SOURCEDOCUMENT.LOCALURI.eq(localUri))
			.execute();
			
			sourceDocument.getSourceContentHandler().getSourceDocumentInfo().setContentInfoSet(
					contentInfoSet);
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.sourceDocumentChanged.name(),
					sourceDocument.getID(), sourceDocument);						
		}
		catch (Exception e) {
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, e);
		}
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

	public void remove(SourceDocument sourceDocument) throws IOException {
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		try {
			Field<Integer> totalParticipantsField = db
			.select(DSL.count(USER_SOURCEDOCUMENT.USERID))
			.from(USER_SOURCEDOCUMENT)
			.join(SOURCEDOCUMENT)
				.on(SOURCEDOCUMENT.SOURCEDOCUMENTID
						.eq(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID)
				.and(SOURCEDOCUMENT.LOCALURI.eq(sourceDocument.getID())))
			.asField("totalParticipants");
			
			
			Record currentUserSourceDocRecord = db
			.select(
				USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID, 
				USER_SOURCEDOCUMENT.ACCESSMODE, 
				USER_SOURCEDOCUMENT.OWNER,
				USER_SOURCEDOCUMENT.SOURCEDOCUMENTID, 
				totalParticipantsField)
			.from(USER_SOURCEDOCUMENT)
			.join(SOURCEDOCUMENT)
				.on(SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID))
				.and(SOURCEDOCUMENT.LOCALURI.eq(sourceDocument.getID()))
			.where(USER_SOURCEDOCUMENT.USERID.eq(dbRepository.getCurrentUser().getUserId()))
			.fetchOne();
			
			
			if ((currentUserSourceDocRecord == null) 
					|| (currentUserSourceDocRecord.getValue(USER_SOURCEDOCUMENT.ACCESSMODE) == null)) {
				throw new IllegalStateException(
						"you seem to have no access rights for this document!");
			}
			
			Integer sourceDocId = 
				currentUserSourceDocRecord.getValue(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID);

			int totalParticipants = 
					(Integer)currentUserSourceDocRecord.getValue("totalParticipants");
			
			db.beginTransaction();
			
			db
			.delete(USER_SOURCEDOCUMENT)
			.where(USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID
					.eq(currentUserSourceDocRecord.getValue(USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID)))
			.execute();
			
			if (!sourceDocument.getUserMarkupCollectionRefs().isEmpty()) {
				deleteUserMarkupCollections(db, sourceDocId);
			}
			
			db.commitTransaction();

			if (totalParticipants == 1) {
				dbRepository.getIndexer().removeSourceDocument(
						sourceDocument.getID());
			}
			
			sourceDocumentsByID.remove(sourceDocument.getID());
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.sourceDocumentChanged.name(),
					sourceDocument, null);
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
	private void deleteUserMarkupCollections(
			DSLContext db, Integer sourceDocumentId) throws IOException {
		
		List<Integer> userMarkupCollectionIds = db
			.select(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
			.from(USERMARKUPCOLLECTION)
			.where(USERMARKUPCOLLECTION.SOURCEDOCUMENTID.eq(sourceDocumentId))
			.fetch()
			.map(new IDFieldToIntegerMapper(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID));
		
		db
		.delete(USER_USERMARKUPCOLLECTION)
		.where(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.in(userMarkupCollectionIds))
		.and(USER_USERMARKUPCOLLECTION.USERID.eq(dbRepository.getCurrentUser().getUserId()))
		.execute();
		
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

	Pair<Integer,AccessMode> getSourceDocumentAccess(
			DSLContext db, String localUri, boolean checkWriteAccess) throws IOException {
		Record accessModeRecord =
		db
		.select(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID, USER_SOURCEDOCUMENT.ACCESSMODE)
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
		else {
			return new Pair<Integer, AccessMode>(
				accessModeRecord.getValue(USER_SOURCEDOCUMENT.SOURCEDOCUMENTID),
				AccessMode.getAccessMode(accessModeRecord.getValue(USER_SOURCEDOCUMENT.ACCESSMODE)));
		}
	}
}
