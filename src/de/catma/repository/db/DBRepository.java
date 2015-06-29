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

import static de.catma.repository.db.jooqgen.catmarepository.Tables.PERMISSION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.ROLE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.ROLE_PERMISSION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_CORPUS;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_ROLE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_TAGLIBRARY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_USERMARKUPCOLLECTION;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.document.AccessMode;
import de.catma.document.Corpus;
import de.catma.document.repository.UnknownUserException;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollection;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;
import de.catma.indexer.IndexerFactory;
import de.catma.repository.db.executionshield.DBOperation;
import de.catma.repository.db.executionshield.ExecutionShield;
import de.catma.repository.db.jooq.TransactionalDSLContext;
import de.catma.repository.db.mapper.FieldToValueMapper;
import de.catma.repository.db.mapper.IDFieldToIntegerMapper;
import de.catma.repository.db.mapper.UserMapper;
import de.catma.serialization.SerializationHandlerFactory;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.user.Role;
import de.catma.user.User;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class DBRepository implements IndexedRepository {
	
	private String name;
	
	private CorpusHandler dbCorpusHandler;
	private SourceDocumentHandler dbSourceDocumentHandler;
	private TagLibraryHandler dbTagLibraryHandler;
	private UserMarkupCollectionHandler dbUserMarkupCollectionHandler;
	
	private IndexerFactory indexerFactory;
	private Indexer indexer;
	private SerializationHandlerFactory serializationHandlerFactory;
	private boolean authenticationRequired;
	private BackgroundServiceProvider backgroundServiceProvider;
	private TagManager tagManager;

	private DBUser currentUser;
	private PropertyChangeSupport propertyChangeSupport;
	private IDGenerator idGenerator;
	
	private boolean tagManagerListenersEnabled = true;

	private PropertyChangeListener tagsetDefinitionChangedListener;
	private PropertyChangeListener tagDefinitionChangedListener;
	private PropertyChangeListener tagLibraryChangedListener;
	private PropertyChangeListener userDefinedPropertyChangedListener;

	private String user;
	private String pass;
	private String url;

	private String tempDir;
	private String repoFolderPath;

	private DataSource dataSource;
	private ExecutionShield execShield;

	public DBRepository(
			String name, 
			String repoFolderPath, 
			boolean authenticationRequired, 
			TagManager tagManager, 
			BackgroundServiceProvider backgroundServiceProvider,
			IndexerFactory indexerFactory, 
			SerializationHandlerFactory serializationHandlerFactory,
			String url, String user, String pass, String tempDir) throws NamingException {
		

		this.name = name;
		this.repoFolderPath = repoFolderPath;
		this.authenticationRequired = authenticationRequired;
		this.tagManager = tagManager;
		this.backgroundServiceProvider = backgroundServiceProvider;
		this.indexerFactory = indexerFactory;
		this.serializationHandlerFactory = serializationHandlerFactory;
		this.user = user;
		this.pass = pass;
		this.url = url;
		this.tempDir = tempDir;
		
		this.propertyChangeSupport = new PropertyChangeSupport(this);
		this.idGenerator = new IDGenerator();
		this.execShield = new ExecutionShield();
	}
	
	private void initTagManagerListeners() {
		tagsetDefinitionChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(final PropertyChangeEvent evt) {
				
				if (!tagManagerListenersEnabled) {
					return;
				}
				try {
					if (evt.getOldValue() == null) { //insert
						@SuppressWarnings("unchecked")
						final Pair<TagLibrary, TagsetDefinition> args = 
								(Pair<TagLibrary, TagsetDefinition>)evt.getNewValue();
						execShield.execute(new DBOperation<Void>() {
							public Void execute() throws Exception {
								dbTagLibraryHandler.createTagsetDefinition(
										args.getFirst(), args.getSecond());
								return null;
							}
						});
					}
					else if (evt.getNewValue() == null) { //delete
						@SuppressWarnings("unchecked")
						final Pair<TagLibrary, TagsetDefinition> args = 
								(Pair<TagLibrary, TagsetDefinition>)evt.getOldValue();
						execShield.execute(new DBOperation<Void>() {
							public Void execute() throws Exception {
								dbTagLibraryHandler.removeTagsetDefinition(args.getSecond());
								return null;
							}
						});
					}
					else { //update
						execShield.execute(new DBOperation<Void>() {
							public Void execute() throws Exception {
								dbTagLibraryHandler.updateTagsetDefinition(
										(TagsetDefinition)evt.getNewValue());
								return null;
							}
						});
					}
				}
				catch (IOException e) {
					propertyChangeSupport.firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							e);	
				}
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagsetDefinitionChanged,
				tagsetDefinitionChangedListener);
		
		tagDefinitionChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(final PropertyChangeEvent evt) {
				
				if (!tagManagerListenersEnabled) {
					return;
				}
				try {
					if (evt.getOldValue() == null) {
						@SuppressWarnings("unchecked")
						final Pair<TagsetDefinition, TagDefinition> args = 
								(Pair<TagsetDefinition, TagDefinition>)evt.getNewValue();
						execShield.execute(new DBOperation<Void>() {
							public Void execute() throws Exception {
								dbTagLibraryHandler.createTagDefinition(
										args.getFirst(), args.getSecond());
								return null;
							}
						});
					}
					else if (evt.getNewValue() == null) {
						@SuppressWarnings("unchecked")
						final Pair<TagsetDefinition, TagDefinition> args = 
								(Pair<TagsetDefinition, TagDefinition>)evt.getOldValue();
						execShield.execute(new DBOperation<Void>() {
							public Void execute() throws Exception {
								dbTagLibraryHandler.removeTagDefinition(
										args.getFirst(), args.getSecond());
								return null;
							}
						});							
					}
					else {
						execShield.execute(new DBOperation<Void>() {
							public Void execute() throws Exception {
								dbTagLibraryHandler.updateTagDefinition(
										(TagDefinition)evt.getNewValue());
								return null;
							}
						});
					}
				}
				catch (Exception e) {
					propertyChangeSupport.firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							e);				
				}
			}

		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefinitionChangedListener);	
		
		
		userDefinedPropertyChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				
				if (!tagManagerListenersEnabled) {
					return;
				}
				Object oldValue = evt.getOldValue();
				Object newValue = evt.getNewValue();

				if (oldValue == null) { // insert
					
					@SuppressWarnings("unchecked")
					Pair<PropertyDefinition, TagDefinition> newPair = 
							(Pair<PropertyDefinition, TagDefinition>)newValue;
					
					dbTagLibraryHandler.savePropertyDefinition(
							newPair.getFirst(), newPair.getSecond());
				}
				else if (newValue == null) { // delete
					@SuppressWarnings("unchecked")
					Pair<PropertyDefinition, TagDefinition> oldPair = 
							(Pair<PropertyDefinition, TagDefinition>)oldValue;
					dbTagLibraryHandler.removePropertyDefinition(
							oldPair.getFirst(), oldPair.getSecond());
					
				}
				else { // update
					PropertyDefinition pd = (PropertyDefinition)evt.getNewValue();
					TagDefinition td = (TagDefinition)evt.getOldValue();
					dbTagLibraryHandler.updatePropertyDefinition(pd, td);
				}
				
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged,
				userDefinedPropertyChangedListener);
		
		tagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (!tagManagerListenersEnabled) {
					return;
				}
				
				if ((evt.getNewValue() != null) && (evt.getOldValue() != null)) { //update
					dbTagLibraryHandler.update(
							(TagLibraryReference)evt.getNewValue());
				}
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagLibraryChanged,
				tagLibraryChangedListener);
	}

	public void open(Map<String, String> userIdentification) throws Exception {
		initTagManagerListeners();
		
		Context context = new InitialContext();
		
		this.dataSource = CatmaDataSourceName.CATMADS.getDataSource();
		
		this.dbSourceDocumentHandler = 
				new SourceDocumentHandler(this, repoFolderPath);
		this.dbTagLibraryHandler = new TagLibraryHandler(this, idGenerator);
		this.dbUserMarkupCollectionHandler = 
				new UserMarkupCollectionHandler(this);
		
		this.dbCorpusHandler = new CorpusHandler(this);

		indexer = indexerFactory.createIndexer(Collections.<String, Object>emptyMap());
		
		loadCurrentUser(userIdentification);
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		loadContent(db);
	}
	
	private void loadCurrentUser(
			Map<String, String> userIdentification) throws IOException {

		TransactionalDSLContext db = new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		
		try {
	
			Record record = db
			.select()
			.from(USER)
			.where(USER.IDENTIFIER.eq(userIdentification.get("user.ident")))
			.fetchOne();
			
			if (record == null) {
				db.beginTransaction();
				
				Integer userRoleId = db
				.select(ROLE.ROLEID)
				.from(ROLE)
				.where(ROLE.IDENTIFIER.eq(Role.user.name()))
				.fetchOne()
				.value1();
						
				Record idRecord = db
				.insertInto(
					USER,
						USER.IDENTIFIER,
						USER.LOCKED)
				.values(
					userIdentification.get("user.ident"),
					(byte)0)
				.returning(USER.USERID)
				.fetchOne();
				
				db
				.insertInto(
					USER_ROLE,
						USER_ROLE.USERID,
						USER_ROLE.ROLEID)
				.values(
					idRecord.getValue(USER.USERID),
					userRoleId)
				.execute();
				
				currentUser = new DBUser(
					idRecord.getValue(USER.USERID), 
					userIdentification.get("user.ident"),
					false);
				
				db.commitTransaction();
			}
			else {
				currentUser = record.map(new UserMapper());
			}
	
			List<String> permissions = db
			.select(PERMISSION.IDENTIFIER)
			.from(PERMISSION)
			.join(ROLE_PERMISSION)
				.on(ROLE_PERMISSION.PERMISSIONID.eq(PERMISSION.PERMISSIONID))
			.join(USER_ROLE)
				.on(USER_ROLE.ROLEID.eq(ROLE_PERMISSION.ROLEID))
				.and(USER_ROLE.USERID.eq(currentUser.getUserId()))
			.fetch()
			.map(new FieldToValueMapper<String>(PERMISSION.IDENTIFIER));
			
			currentUser.setPermissions(permissions);
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

	private void loadContent(DSLContext db) 
			throws URISyntaxException, IOException, 
			InstantiationException, IllegalAccessException {
		dbSourceDocumentHandler.loadSourceDocuments(db);
		dbTagLibraryHandler.loadTagLibraryReferences(db);
		dbCorpusHandler.loadCorpora(db);
	}
	
	public void reload() throws IOException {
		try {
			DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
			dbSourceDocumentHandler.reloadSourceDocuments(db);
			dbTagLibraryHandler.reloadTagLibraryReferences(db);
			dbCorpusHandler.reloadCorpora(db);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	public void close() {
		
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagsetDefinitionChanged,
				tagsetDefinitionChangedListener);
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefinitionChangedListener);	
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagLibraryChanged,
				tagLibraryChangedListener);
		tagManager.removePropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged,
				userDefinedPropertyChangedListener);
		
		dbTagLibraryHandler.close();
		dbSourceDocumentHandler.close();
		
		indexer.close();

		dataSource = null;
	}
	
	
	public void addPropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		this.propertyChangeSupport.addPropertyChangeListener(
				propertyChangeEvent.name(), propertyChangeListener);
	}
	
	public void removePropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		this.propertyChangeSupport.removePropertyChangeListener(
				propertyChangeEvent.name(), propertyChangeListener);
	}

	
	public String getName() {
		return name;
	}

	public String getIdFromURI(URI uri) {
		return dbSourceDocumentHandler.getIDFromURI(uri);
	}
		
	public String getFileURL(String sourceDocumentID, String... path) {
		return dbSourceDocumentHandler.getFileURL(sourceDocumentID, path);
	}
	
	public void insert(SourceDocument sourceDocument) throws IOException {
		dbSourceDocumentHandler.insert(sourceDocument);
	}
	
	public void update(
			SourceDocument sourceDocument, ContentInfoSet contentInfoSet) {
		dbSourceDocumentHandler.update(sourceDocument, contentInfoSet);
	}
	
	public Collection<SourceDocument> getSourceDocuments() {
		return dbSourceDocumentHandler.getSourceDocuments();
	}
	
	public SourceDocument getSourceDocument(String id) {
		return dbSourceDocumentHandler.getSourceDocument(id);
	}
	
	public void delete(SourceDocument sourceDocument) throws IOException {
		dbSourceDocumentHandler.remove(sourceDocument);
		try {
			DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
			dbCorpusHandler.reloadCorpora(db);
		}
		catch (Exception e) {
			throw new IOException(e);
		}	
	}

	public SourceDocument getSourceDocument(UserMarkupCollectionReference umcRef) {
		return dbSourceDocumentHandler.getSourceDocument(umcRef);
	}
	
	public Collection<Corpus> getCorpora() {
		return dbCorpusHandler.getCorpora();
	}

	public void createCorpus(String name) throws IOException {
		dbCorpusHandler.createCorpus(name);
	}
	
	public void update(Corpus corpus, SourceDocument sourceDocument) throws IOException {
		dbCorpusHandler.addSourceDocument(corpus, sourceDocument);
	}
	
	public void update(Corpus corpus,
			StaticMarkupCollectionReference staticMarkupCollectionReference) {
		// TODO Auto-generated method stub
		
	}
	
	public void update(Corpus corpus, String name) throws IOException {
		dbCorpusHandler.rename(corpus, name);
	}
	
	public void update(Corpus corpus,
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		SourceDocument sd = getSourceDocument(userMarkupCollectionReference);
		if (!corpus.getSourceDocuments().contains(sd)) {
			update(corpus, sd);
		}
		dbCorpusHandler.addUserMarkupCollectionRef(
				corpus, userMarkupCollectionReference);
		
	}
	
	public void delete(Corpus corpus) throws IOException {
		dbCorpusHandler.delete(corpus);
	}
	
	public void createUserMarkupCollection(String name,
			SourceDocument sourceDocument) throws IOException {
		dbUserMarkupCollectionHandler.createUserMarkupCollection(
				name, sourceDocument);
	}
	
	public void importUserMarkupCollection(
			InputStream inputStream, SourceDocument sourceDocument)
			throws IOException {
		dbUserMarkupCollectionHandler.importUserMarkupCollection(
				inputStream, sourceDocument);
	}
	
	public void importUserMarkupCollection(
			InputStream inputStream,
			final SourceDocument sourceDocument, 
			UserMarkupCollectionSerializationHandler userMarkupCollectionSerializationHandler) 
					throws IOException {
		dbUserMarkupCollectionHandler.importUserMarkupCollection(
				inputStream, sourceDocument, userMarkupCollectionSerializationHandler);
	}
	
	/* (non-Javadoc)
	 * @see de.catma.document.repository.Repository#getUserMarkupCollection(de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference)
	 */
	public UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		return getUserMarkupCollection(userMarkupCollectionReference, false);
	}
	public UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference, boolean refresh) throws IOException {
		return dbUserMarkupCollectionHandler.getUserMarkupCollection(userMarkupCollectionReference, refresh);
	}

	public List<UserMarkupCollectionReference> getWritableUserMarkupCollectionRefs(
			SourceDocument sd) throws IOException {
		return dbUserMarkupCollectionHandler.getWritableUserMarkupCollectionRefs(sd);
	}

	public void update(final UserMarkupCollection userMarkupCollection,
			final List<TagReference> tagReferences) {
		try {
			boolean added = execShield.execute(new DBOperation<Boolean>() {
					public Boolean execute() throws Exception {
						if (userMarkupCollection.getTagReferences().containsAll(
								tagReferences)) {
							dbUserMarkupCollectionHandler.addTagReferences(
								userMarkupCollection, tagReferences);
							return true;
						}
						else {
							dbUserMarkupCollectionHandler.removeTagReferences(
								tagReferences);
							return false;
						}
					}
				});
			if (added) {
				propertyChangeSupport.firePropertyChange(
						RepositoryChangeEvent.tagReferencesChanged.name(), 
						null, tagReferences);
			}
			else {
				propertyChangeSupport.firePropertyChange(
						RepositoryChangeEvent.tagReferencesChanged.name(), 
						tagReferences, null);
			}
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e);				
		}
	}
	
	public void update(final List<UserMarkupCollection> userMarkupCollections,
			final TagsetDefinition tagsetDefinition) {
		try {
			execShield.execute( new DBOperation<Void>() {
						public Void execute() throws Exception {
							dbUserMarkupCollectionHandler.updateTagsetDefinitionInUserMarkupCollections(
									userMarkupCollections, tagsetDefinition);
							return null;
						}
					});
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionTagLibraryChanged.name(),
					tagsetDefinition, userMarkupCollections);
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e);				
		}
	}
	
	
	public void update(
			UserMarkupCollectionReference userMarkupCollectionReference,
			ContentInfoSet contentInfoSet) {
		dbUserMarkupCollectionHandler.update(
				userMarkupCollectionReference, contentInfoSet);
	}

	public void update(final TagInstance tagInstance, final Collection<Property> properties)
			throws IOException {
		execShield.execute( new DBOperation<Void>() {
			public Void execute() throws Exception {
				dbUserMarkupCollectionHandler.updateProperty(tagInstance, properties);
				propertyChangeSupport.firePropertyChange(
						RepositoryChangeEvent.propertyValueChanged.name(),
						tagInstance, properties);
				return null;
			}
		});
			
	}

	public void delete(
			UserMarkupCollectionReference userMarkupCollectionReference)
			throws IOException {
		dbUserMarkupCollectionHandler.delete(userMarkupCollectionReference);
		try {
			DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
			dbCorpusHandler.reloadCorpora(db);
		}
		catch (Exception e) {
			throw new IOException(e);
		}	
	}


	public StaticMarkupCollectionReference insert(
			StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public StaticMarkupCollection getStaticMarkupCollection(
			StaticMarkupCollectionReference staticMarkupCollectionReference) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void update(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void delete(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub

	}

	public void createTagLibrary(String name) throws IOException {
		dbTagLibraryHandler.createTagLibrary(name);
	}
	
	public void importTagLibrary(InputStream inputStream) throws IOException {
		dbTagLibraryHandler.importTagLibrary(inputStream);
	}
	
	public List<TagLibraryReference> getTagLibraryReferences() {
		return dbTagLibraryHandler.getTagLibraryReferences();
	}
	
	public TagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) 
			throws IOException{
		TagLibrary tagLibrary = tagManager.getTagLibrary(tagLibraryReference);
		if (tagLibrary == null) {
			tagLibrary = dbTagLibraryHandler.getTagLibrary(tagLibraryReference);
			tagManager.addTagLibrary(tagLibrary);
		}
		return tagLibrary;
	}

	public void delete(TagLibraryReference tagLibraryReference) throws IOException {
		dbTagLibraryHandler.delete(tagManager, tagLibraryReference);
	}

	public boolean isAuthenticationRequired() {
		return authenticationRequired;
	}
	
	public User getUser() {
		return currentUser;
	}
	
	public Indexer getIndexer() {
		return indexer;
	}
	
	DBUser getCurrentUser() {
		return currentUser;
	}
	
	PropertyChangeSupport getPropertyChangeSupport() {
		return propertyChangeSupport;
	}
	
	void setTagManagerListenersEnabled(boolean tagManagerListenersEnabled) {
		this.tagManagerListenersEnabled = tagManagerListenersEnabled;
	}
	
	SerializationHandlerFactory getSerializationHandlerFactory() {
		return serializationHandlerFactory;
	}
	
	BackgroundServiceProvider getBackgroundServiceProvider() {
		return backgroundServiceProvider;
	}
	
	TagLibraryHandler getDbTagLibraryHandler() {
		return dbTagLibraryHandler;
	}
	
	SourceDocumentHandler getDbSourceDocumentHandler() {
		return dbSourceDocumentHandler;
	}
	
	public TagManager getTagManager() {
		return tagManager;
	}
	
	UserMarkupCollectionHandler getDbUserMarkupCollectionHandler() {
		return dbUserMarkupCollectionHandler;
	}
	
	String getTempDir() {
		return tempDir;
	}
	
	public void share(
			Corpus corpus, String userIdentification, AccessMode accessMode) 
					throws IOException {
		TransactionalDSLContext db = new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		
		try {
			Integer targetUserId = getUserId(db, userIdentification);
			Integer corpusId = Integer.valueOf(corpus.getId());
			
			if (targetUserId != null) {
				AccessMode corpusAccess = 
						dbCorpusHandler.getCorpusAccess(
							db, Integer.valueOf(corpus.getId()),
							false);
				
				if (accessMode.equals(AccessMode.WRITE)
						&& (corpusAccess!= AccessMode.WRITE)) {
					accessMode = corpusAccess;
				}

				Record record = db
				.select(USER_CORPUS.USER_CORPUSID)
				.from(USER_CORPUS)
				.where(USER_CORPUS.USERID.eq(targetUserId)
					.and(USER_CORPUS.CORPUSID.eq(corpusId)))
				.fetchOne();
				
				Integer userCorpusId = null;
				
				if (record != null) {
					userCorpusId = record.map(
						new IDFieldToIntegerMapper(USER_CORPUS.USER_CORPUSID));
				}
				
				db.beginTransaction();
				
				if (userCorpusId == null) {
					db
					.insertInto(
						USER_CORPUS,
						USER_CORPUS.USERID,
						USER_CORPUS.CORPUSID,
						USER_CORPUS.ACCESSMODE,
						USER_CORPUS.OWNER)
					.values(
						targetUserId,
						corpusId,
						accessMode.getNumericRepresentation(),
						(byte)0)
					.execute();
				}
				else if (accessMode.equals(AccessMode.WRITE)) {
					db
					.update(USER_CORPUS)
					.set(USER_CORPUS.ACCESSMODE, accessMode.getNumericRepresentation())
					.where(USER_CORPUS.USER_CORPUSID.eq(userCorpusId))
					.execute();
				}
				for (SourceDocument sd : corpus.getSourceDocuments()) {
					share(db, targetUserId, sd, userIdentification, accessMode);
				}
				
				for (UserMarkupCollectionReference umcRef : 
					corpus.getUserMarkupCollectionRefs()) {
					share(db, targetUserId, umcRef, userIdentification, accessMode);
				}

				db.commitTransaction();
			}
			else {
				throw new UnknownUserException(userIdentification);
			}
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
	
	private void share(
		DSLContext db, Integer targetUserId, SourceDocument sourceDocument, 
		String userIdentification, AccessMode accessMode) throws IOException {
		
		Pair<Integer, AccessMode> sourceDocAccess = dbSourceDocumentHandler.getSourceDocumentAccess(
				db, sourceDocument.getID(), false);
		
		AccessMode sourceDocAccessMode = sourceDocAccess.getSecond();
		Integer sourceDocumentId = sourceDocAccess.getFirst();
		
		// allow at most the rights the sharing user has
		if (accessMode.equals(AccessMode.WRITE) && 
				(sourceDocAccessMode != AccessMode.WRITE)) {
			accessMode = sourceDocAccessMode;
		}
		
		 Record record = db
		.select(USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID)
		.from(USER_SOURCEDOCUMENT)
		.where(USER_SOURCEDOCUMENT.USERID.eq(targetUserId))
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
				targetUserId,
				sourceDocumentId,
				accessMode.getNumericRepresentation(),
				(byte)0)
			.execute();
		}
		else if (accessMode.equals(AccessMode.WRITE)) { // reshare only if this leads to more rights -> no revoke
			db
			.update(USER_SOURCEDOCUMENT)
			.set(USER_SOURCEDOCUMENT.ACCESSMODE, accessMode.getNumericRepresentation())
			.where(USER_SOURCEDOCUMENT.USER_SOURCEDOCUMENTID.eq(userSourceDocId))
			.execute();
		}
	}
	
	public void share(
			SourceDocument sourceDocument, 
			String userIdentification,
			AccessMode accessMode) throws IOException {
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		Integer userId = getUserId(db, userIdentification); 
		if (userId != null) {
			share(db, userId, sourceDocument, userIdentification, accessMode);
		}
		else {
			throw new UnknownUserException(userIdentification);
		}
	}
	
	private Integer getUserId(DSLContext db, String userIdentification) {
		Record idRecord = db 
		.select(USER.USERID)
		.from(USER)
		.where(USER.IDENTIFIER.equalIgnoreCase(userIdentification))
		.fetchOne();
		
		return (idRecord==null)?null:idRecord.getValue(USER.USERID);
	}

	public void share(UserMarkupCollectionReference userMarkupCollectionRef, 
			String userIdentification, AccessMode accessMode) throws IOException {
		TransactionalDSLContext db = new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		
		Integer userId = getUserId(db, userIdentification);
		if (userId != null) {
			try {
				db.beginTransaction();
				share(
					db, userId, 
					userMarkupCollectionRef, userIdentification, accessMode);
				db.commitTransaction();
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
		else {
			throw new UnknownUserException(userIdentification);
		}
	}
	
	private void share(
			DSLContext db, Integer targetUserId, 
			UserMarkupCollectionReference userMarkupCollectionRef, 
			String userIdentification, AccessMode accessMode) throws IOException {
		
		AccessMode currentAccessMode = 
			dbUserMarkupCollectionHandler.getUserMarkupCollectionAccessMode(
				db, Integer.valueOf(userMarkupCollectionRef.getId()), false);
				
		if (accessMode.equals(AccessMode.WRITE)  
			&& !currentAccessMode.equals(AccessMode.WRITE)) {
			accessMode = AccessMode.READ;
		}
		
		SourceDocument sourceDocument = 
				getSourceDocument(new UserMarkupCollectionReference(
						userMarkupCollectionRef.getId(), 
						userMarkupCollectionRef.getContentInfoSet()));
		
		share(db, targetUserId, sourceDocument, userIdentification, accessMode);
		
		Integer userMarkupCollectionId = 
				Integer.valueOf(userMarkupCollectionRef.getId());
		
		Record record = db
		.select(USER_USERMARKUPCOLLECTION.USER_USERMARKUPCOLLECTIOID)
		.from(USER_USERMARKUPCOLLECTION)
		.where(USER_USERMARKUPCOLLECTION.USERID.eq(targetUserId))
		.and(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
		.fetchOne();
		
		Integer userUmcId = null;
		
		if (record != null) {
			userUmcId = record.map(
				new IDFieldToIntegerMapper(
						USER_USERMARKUPCOLLECTION.USER_USERMARKUPCOLLECTIOID));
		}
		
		if (userUmcId == null) {
			db
			.insertInto(
				USER_USERMARKUPCOLLECTION,
					USER_USERMARKUPCOLLECTION.USERID,
					USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID,
					USER_USERMARKUPCOLLECTION.ACCESSMODE,
					USER_USERMARKUPCOLLECTION.OWNER)
			.values(
				targetUserId,
				userMarkupCollectionId,
				accessMode.getNumericRepresentation(),
				(byte)0)
			.execute();
		}
		else if (accessMode.equals(AccessMode.WRITE)) {
			db
			.update(USER_USERMARKUPCOLLECTION)
			.set(USER_USERMARKUPCOLLECTION.ACCESSMODE, accessMode.getNumericRepresentation())
			.where(USER_USERMARKUPCOLLECTION.USER_USERMARKUPCOLLECTIOID.eq(userUmcId))
			.execute();
		}
	}

	public void share(
			TagLibraryReference tagLibrary, 
			String userIdentification, AccessMode accessMode) throws IOException {
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		Integer tagLibraryId = Integer.valueOf(tagLibrary.getId());
		
		Integer targetUserId = getUserId(db, userIdentification);
		if (targetUserId != null) {
			
			AccessMode libAccess = 
					dbTagLibraryHandler.getLibraryAccess(
							db, tagLibraryId, false);
			
			if (accessMode.equals(AccessMode.WRITE) && libAccess != AccessMode.WRITE) {
				accessMode = libAccess;
			}
			
			Record record = db
			.select(USER_TAGLIBRARY.USER_TAGLIBRARYID)
			.from(USER_TAGLIBRARY)
			.where(USER_TAGLIBRARY.USERID.eq(targetUserId))
			.and(USER_TAGLIBRARY.TAGLIBRARYID.eq(tagLibraryId))
			.fetchOne();
			
			Integer userTagLibId = null;
			if (record != null) {
				userTagLibId = record.map(
					new IDFieldToIntegerMapper(USER_TAGLIBRARY.USER_TAGLIBRARYID));
			}
			
			if (userTagLibId == null) {
				db
				.insertInto(
					USER_TAGLIBRARY,
						USER_TAGLIBRARY.USERID,
						USER_TAGLIBRARY.TAGLIBRARYID,
						USER_TAGLIBRARY.ACCESSMODE,
						USER_TAGLIBRARY.OWNER)
				.values(
					targetUserId,
					tagLibraryId,
					accessMode.getNumericRepresentation(),
					(byte)0)
				.execute();
			}
			else if (accessMode.equals(AccessMode.WRITE)){
				db
				.update(USER_TAGLIBRARY)
				.set(USER_TAGLIBRARY.ACCESSMODE, accessMode.getNumericRepresentation())
				.where(USER_TAGLIBRARY.USER_TAGLIBRARYID.eq(userTagLibId))
				.execute();
			}
		}				
		else {
			throw new UnknownUserException(userIdentification);
		}
	}

	public File getFile(SourceDocument sourceDocument) {
		return new File(
			sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().getURI());
	}
}
