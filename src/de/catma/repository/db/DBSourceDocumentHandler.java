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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import de.catma.repository.db.model.DBSourceDocument;
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

	void insert(SourceDocument sourceDocument) throws IOException {
		DBSourceDocument dbSourceDocument = 
					new DBSourceDocument(sourceDocument);
		
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			session.save(dbSourceDocument);
			
			DBUserSourceDocument dbUserSourceDocument = 
					new DBUserSourceDocument(
							dbRepository.getCurrentUser(), 
							dbSourceDocument);
			
			session.save(dbUserSourceDocument);

			insertIntoFS(sourceDocument);
			
			dbRepository.getIndexer().index(sourceDocument);
			
			session.getTransaction().commit();
			
			this.sourceDocumentsByID.put(sourceDocument.getID(), sourceDocument);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.sourceDocumentChanged.name(),
					null, sourceDocument.getID());
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
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
						Collections.<String>emptyList(), //TODO: load list
						Collections.<Character>emptyList(), //TODO: load list
						new Locale(sd.getLocale()));
				ContentInfoSet contentInfoSet = 
						new ContentInfoSet(sd.getAuthor(), sd.getDescription(), 
								sd.getPublisher(), sd.getTitle());
				TechInfoSet techInfoSet = 
						new TechInfoSet(
							FileType.valueOf(sd.getFileType()),
							Charset.forName(sd.getCharset()),
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

	public void update(final String localUri, ContentInfoSet contentInfoSet) {
		final String author = contentInfoSet.getAuthor();
		final String publisher = contentInfoSet.getPublisher();
		final String title = contentInfoSet.getTitle();
		final String description = contentInfoSet.getDescription();
		
		dbRepository.getBackgroundServiceProvider().submit(
				new DefaultProgressCallable<Void>() {
					public Void call() throws Exception {
						
						Session session = 
								dbRepository.getSessionFactory().openSession();
						try {
							DBSourceDocument dbSourceDocument = 
									getDbSourceDocument(session, localUri);
							
							dbSourceDocument.setAuthor(author);
							dbSourceDocument.setTitle(title);
							dbSourceDocument.setDescription(description);
							dbSourceDocument.setPublisher(publisher);
							
							session.beginTransaction();
							session.save(dbSourceDocument);
							session.getTransaction().commit();
							CloseSafe.close(new CloseableSession(session));
						}
						catch (Exception exc) {
							CloseSafe.close(new CloseableSession(session,true));
							throw new IOException(exc);
						}
						return null;
					}
				},
				new ExecutionListener<Void>() {
					public void done(Void notOfInterest) { /* noop */ }
					public void error(Throwable t) {
						t.printStackTrace();
						// TODO Auto-generated method stub
						
					}
				}
			);;
	}
}
