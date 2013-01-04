package de.catma.repository.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.db.CloseableSession;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentHandler;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.db.DBIndexer;
import de.catma.repository.db.model.DBCorpusSourceDocument;
import de.catma.repository.db.model.DBSourceDocument;
import de.catma.repository.db.model.DBUnseparableCharsequence;
import de.catma.repository.db.model.DBUserDefinedSeparatingCharacter;
import de.catma.repository.db.model.DBUserMarkupCollection;
import de.catma.repository.db.model.DBUserSourceDocument;
import de.catma.util.CloseSafe;
import de.catma.util.ContentInfoSet;
import de.catma.util.IDGenerator;

class DBSourceDocumentHandler {

	private static final String SOURCEDOCS_FOLDER = "sourcedocuments";
	private static final String REPO_URI_SCHEME = "catma://";
	
	private DBRepository dbRepository;
	private String sourceDocsPath;
	private Map<String,SourceDocument> sourceDocumentsByID;

	public DBSourceDocumentHandler(
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

		SourceDocumentInfo sourceDocumentInfo = 
				sourceDocument.getSourceContentHandler().getSourceDocumentInfo();
		
		URI sourceDocURI = sourceDocumentInfo.getTechInfoSet().getURI();
		
		if (sourceDocURI.getScheme().toLowerCase().equals("file")) {
			File sourceTempFile = new File(sourceDocURI);
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
		else {
			File repoSourceFile = null;
			
			try {
				repoSourceFile = 
					new File(
						new URI(getFileURL(sourceDocument.getID(), sourceDocsPath)));
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
			//FIXME: for online resources as word docs this will fail, because there is no charset, better treat this in the
			// same way as a file upload
			Writer repoSourceFileWriter =  
					new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(repoSourceFile),
							sourceDocumentInfo.getTechInfoSet().getCharset()));
			try {
				//TODO: keep BOM ... or don't keep it...?!
				repoSourceFileWriter.append(sourceDocument.getContent());
			}
			finally {
				CloseSafe.close(repoSourceFileWriter);
			}

		}
	}
	
	
	String getFileURL(String catmaUri, String... path) {
		StringBuilder builder = new StringBuilder("file://");
		for (String folder : path) {
			builder.append(folder);
		}
		builder.append(catmaUri.substring((REPO_URI_SCHEME).length()));
		return builder.toString();
	}

	void insert(final SourceDocument sourceDocument) throws IOException {
		
		dbRepository.getBackgroundServiceProvider().submit(
			"Adding Source Document...",
			new DefaultProgressCallable<String>() {
				public String call() throws Exception {
					
					DBSourceDocument dbSourceDocument = 
							new DBSourceDocument(sourceDocument);
				
				
					Session session =
							dbRepository.getSessionFactory().openSession();
					try {
						session.beginTransaction();
						session.save(dbSourceDocument);
						
						DBUserSourceDocument dbUserSourceDocument = 
								new DBUserSourceDocument(
										dbRepository.getCurrentUser(), 
										dbSourceDocument);
						
						session.save(dbUserSourceDocument);

						insertIntoFS(sourceDocument);
						getProgressListener().setProgress(
								"Indexing Source Document");
						dbRepository.getIndexer().index(sourceDocument);
						
						session.getTransaction().commit();
						return sourceDocument.getID();
					}
					catch (Exception e) {
						CloseSafe.close(new CloseableSession(session,true));
						throw new IOException(e);
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
	
	
	@SuppressWarnings("unchecked")
	void loadSourceDocuments(Session session) 
			throws URISyntaxException, IOException, InstantiationException, IllegalAccessException {
		if (!dbRepository.getCurrentUser().isLocked()) {
			Query query = 
				session.createQuery(
					"select sd from " 
					+ DBSourceDocument.class.getSimpleName() + " as sd "
					+ " inner join sd.dbUserSourceDocuments as usd "
					+ " inner join usd.dbUser as user " 
					+ " left join fetch sd.dbUserMarkupCollections as usc "
					+ " left join fetch usc.dbUserUserMarkupCollections uumc "
					+ " where user.userId = " + dbRepository.getCurrentUser().getUserId());
			
			for (DBSourceDocument sd : (List<DBSourceDocument>)query.list()) {
				IndexInfoSet indexInfoSet = 
					new IndexInfoSet(
						getUnseparableCharacterSequences(sd.getDbUnseparableCharsequences()),
						getUserDefinedSeparatingCharacters(sd.getDbUserDefinedSeparatingCharacters()),
						new Locale(sd.getLocale()));
				ContentInfoSet contentInfoSet = 
						new ContentInfoSet(sd.getAuthor(), sd.getDescription(), 
								sd.getPublisher(), sd.getTitle());
				TechInfoSet techInfoSet = 
						new TechInfoSet(
							FileType.valueOf(sd.getFileType()),
							(sd.getCharset()==null)?null:Charset.forName(sd.getCharset()),
							FileOSType.valueOf(sd.getFileOstype()),
							sd.getChecksum(),
							sd.getXsltDocumentLocalUri());
				techInfoSet.setURI(
						new URI(getFileURL(sd.getLocalUri(), sourceDocsPath))); 
				
				SourceDocumentInfo sourceDocumentInfo = 
						new SourceDocumentInfo(indexInfoSet, contentInfoSet, techInfoSet);
				SourceDocumentHandler sdh = new SourceDocumentHandler();
				SourceDocument sourceDocument = 
					sdh.loadSourceDocument(sd.getLocalUri(), sourceDocumentInfo);
				
				for (DBUserMarkupCollection dbUmc : sd.getDbUserMarkupCollections()) {
					if (dbUmc.hasAccess(dbRepository.getCurrentUser())) {
						sourceDocument.addUserMarkupCollectionReference(
							new UserMarkupCollectionReference(
									dbUmc.getId(), 
									new ContentInfoSet(
										dbUmc.getAuthor(),
										dbUmc.getDescription(),
										dbUmc.getPublisher(),
										dbUmc.getTitle())));
					}
				}
				this.sourceDocumentsByID.put(sourceDocument.getID(), sourceDocument);
			}
		}		
	}
	
	private List<Character> getUserDefinedSeparatingCharacters(
			Set<DBUserDefinedSeparatingCharacter> dbUserDefinedSeparatingCharacters) {
		if (dbUserDefinedSeparatingCharacters.isEmpty()) {
			return Collections.<Character>emptyList();
		}
		else {
			ArrayList<Character> uscList = new ArrayList<Character>();
			for (DBUserDefinedSeparatingCharacter dbUsc : dbUserDefinedSeparatingCharacters) {
				if (dbUsc.getCharacter().length() == 1) {
					uscList.add(dbUsc.getCharacter().toCharArray()[0]);
				}
			}
			return uscList;
		}
	}

	private List<String> getUnseparableCharacterSequences(
			Set<DBUnseparableCharsequence> dbUnseparableCharsequences) {

		if (dbUnseparableCharsequences.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			ArrayList<String> ucsList = new ArrayList<String>();
			for (DBUnseparableCharsequence dbUcs : dbUnseparableCharsequences) {
				ucsList.add(dbUcs.getCharsequence());
			}
			return ucsList;
		}
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
				new DefaultProgressCallable<ContentInfoSet>() {
					public ContentInfoSet call() throws Exception {
						
						Session session = 
								dbRepository.getSessionFactory().openSession();
						try {
							DBSourceDocument dbSourceDocument = 
									getDbSourceDocument(session, localUri);
							
							ContentInfoSet oldContentInfoSet = 
									new ContentInfoSet(
											dbSourceDocument.getAuthor(),
											dbSourceDocument.getDescription(),
											dbSourceDocument.getPublisher(),
											dbSourceDocument.getTitle());
							
							dbSourceDocument.setAuthor(author);
							dbSourceDocument.setTitle(title);
							dbSourceDocument.setDescription(description);
							dbSourceDocument.setPublisher(publisher);
							
							session.beginTransaction();
							session.save(dbSourceDocument);
							session.getTransaction().commit();
							CloseSafe.close(new CloseableSession(session));

							return oldContentInfoSet;
						}
						catch (Exception exc) {
							CloseSafe.close(new CloseableSession(session,true));
							throw new IOException(exc);
						}
					}
				},
				new ExecutionListener<ContentInfoSet>() {
					public void done(ContentInfoSet oldContentInfoSet) {
						sourceDocument.getSourceContentHandler().getSourceDocumentInfo().setContentInfoSet(
								contentInfoSet);
						dbRepository.getPropertyChangeSupport().firePropertyChange(
								RepositoryChangeEvent.sourceDocumentChanged.name(),
								oldContentInfoSet, contentInfoSet);						
					}
					public void error(Throwable t) {
						dbRepository.getPropertyChangeSupport().firePropertyChange(
								RepositoryChangeEvent.exceptionOccurred.name(),
								null, t);
					}
				}
			);;
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
}
