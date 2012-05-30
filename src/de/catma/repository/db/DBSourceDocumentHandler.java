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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.hibernate.Query;
import org.hibernate.Session;

import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.ISourceDocument;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.repository.db.model.DBSourceDocument;
import de.catma.repository.db.model.DBUserMarkupCollection;
import de.catma.repository.db.model.DBUserSourceDocument;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;

class DBSourceDocumentHandler {

	private static final String SOURCEDOCS_FOLDER = "sourcedocuments";
	private static final String REPO_URI_SCHEME = "catma://";
	
	private DBRepository dbRepository;
	private String sourceDocsPath;
	private Map<String,ISourceDocument> sourceDocumentsByID;

	public DBSourceDocumentHandler(
			DBRepository dbRepository, String repoFolderPath) {
		this.dbRepository = dbRepository;
		this.sourceDocsPath = repoFolderPath + 
			"/" + 
			SOURCEDOCS_FOLDER + "/";
		this.sourceDocumentsByID = new HashMap<String, ISourceDocument>();
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
	
	private void insertIntoFS(ISourceDocument sourceDocument) throws IOException {

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

	void insert(ISourceDocument sourceDocument) throws IOException {
		if (sourceDocument instanceof SourceDocument) {
			sourceDocument = 
					new DBSourceDocument((SourceDocument)sourceDocument);
		}
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			session.save(sourceDocument);
			
			DBUserSourceDocument dbUserSourceDocument = 
					new DBUserSourceDocument(
							dbRepository.getCurrentUser(), 
							(DBSourceDocument)sourceDocument);
			
			if (!sourceDocument.getSourceContentHandler().
					getSourceDocumentInfo().getTechInfoSet().getURI().getScheme().equals("file")) {
				((DBSourceDocument)sourceDocument).setSourceUri(
					sourceDocument.getSourceContentHandler().
						getSourceDocumentInfo().getTechInfoSet().getURI().toString());
			}
			session.save(dbUserSourceDocument);
			
			insertIntoFS(sourceDocument);
			
			dbRepository.getIndexer().index(sourceDocument);
			
			session.getTransaction().commit();
			
			this.sourceDocumentsByID.put(sourceDocument.getID(), sourceDocument);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.sourceDocumentChanged.name(),
					null, sourceDocument.getID());
		}
		catch (Exception e) {
			try {
				if (session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			}
			catch(Exception notOfInterest){}
			throw new IOException(e);
		}
		finally {
			CloseSafe.close(new ClosableSession(session));
		}
	}
	
	@SuppressWarnings("unchecked")
	void loadSourceDocuments(Session session) throws URISyntaxException {
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
				sd.getSourceContentHandler().getSourceDocumentInfo().
					getTechInfoSet().setURI(
							new URI(getFileURL(sd.getID(), sourceDocsPath)));
				
				for (DBUserMarkupCollection dbUmc : sd.getDbUserMarkupCollections()) {
					if (dbUmc.hasAccess(dbRepository.getCurrentUser())) {
						sd.addUserMarkupCollectionReference(
							new UserMarkupCollectionReference(
									dbUmc.getId(), dbUmc.getContentInfoSet()));
					}
				}
				this.sourceDocumentsByID.put(sd.getID(), sd);
			}
		}		
	}
	
	Collection<ISourceDocument> getSourceDocuments() {
		return  Collections.unmodifiableCollection(sourceDocumentsByID.values());
	}
	
	ISourceDocument getSourceDocument(String id) {
		return sourceDocumentsByID.get(id);
	}
	
	String getLocalUriFor(UserMarkupCollectionReference umcRef) {
		for (ISourceDocument sd : getSourceDocuments()) {
			if (sd.getUserMarkupCollectionRefs().contains(umcRef)) {
				return ((DBSourceDocument)sd).getLocalUri();
			}
		}
		
		return null;
	}

}
