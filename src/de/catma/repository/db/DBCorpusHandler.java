package de.catma.repository.db;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;

import de.catma.db.CloseableSession;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.repository.db.model.DBCorpus;
import de.catma.repository.db.model.DBCorpusSourceDocument;
import de.catma.repository.db.model.DBCorpusUserMarkupCollection;
import de.catma.repository.db.model.DBSourceDocument;
import de.catma.repository.db.model.DBUserCorpus;
import de.catma.util.CloseSafe;

public class DBCorpusHandler {
	
	private DBRepository dbRepository;
	private Set<Corpus> corpora;

	public DBCorpusHandler(DBRepository dbRepository) {
		this.dbRepository = dbRepository;
		corpora = new HashSet<Corpus>();
	}

	public Set<Corpus> getCorpora() {
		return Collections.unmodifiableSet(corpora);
	}

	@SuppressWarnings("unchecked")
	public void loadCorpora(Session session) {
		if (!dbRepository.getCurrentUser().isLocked()) {
			Query query = 
				session.createQuery(
					"select c from " + DBCorpus.class.getName() + " as c " +
					"inner join c.dbUserCorpus uc " +
					"inner join uc.dbUser as user " +
					"where user.userId = " + 
					dbRepository.getCurrentUser().getUserId());
			
			for (DBCorpus dbCorpus : (List<DBCorpus>)query.list()) {
				Corpus corpus = new Corpus(
					String.valueOf(dbCorpus.getCorpusId()), dbCorpus.getName());
				
				for (DBCorpusSourceDocument dbCorpusSourceDocument : 
					dbCorpus.getDbCorpusSourceDocuments()) {
					SourceDocument sd = dbRepository.getSourceDocument(
						dbCorpusSourceDocument.getDbSourceDocument().getLocalUri());
					
					if (sd != null) {
						corpus.addSourceDocument(sd);
						for (UserMarkupCollectionReference umcRef : 
								sd.getUserMarkupCollectionRefs()) {
							
							if (dbCorpus.contains(umcRef)) {
								corpus.addUserMarkupCollectionReference(umcRef);
							}
						}
						
						for (StaticMarkupCollectionReference smcRef : 
								sd.getStaticMarkupCollectionRefs()) {
							
							if (dbCorpus.contains(smcRef)) {
								corpus.addStaticMarkupCollectionReference(smcRef);
							}
						}
					}
				}
				corpora.add(corpus);
			}
		}
	}

	public void createCorpus(String name) throws IOException {
		Session session = dbRepository.getSessionFactory().openSession();
		try  {
			DBCorpus dbCorpus = new DBCorpus(name);
			DBUserCorpus dbUserCorpus = new DBUserCorpus();
			dbUserCorpus.setDbCorpus(dbCorpus);
			dbUserCorpus.setDbUser(dbRepository.getCurrentUser());
			dbCorpus.getDbUserCorpus().add(dbUserCorpus);
			
			session.beginTransaction();
			session.save(dbCorpus);
			
			session.getTransaction().commit();
			
			CloseSafe.close(new CloseableSession(session));

			Corpus corpus = 
					new Corpus(String.valueOf(dbCorpus.getCorpusId()), name);
			corpora.add(corpus);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
				Repository.RepositoryChangeEvent.corpusChanged.name(),
				null, corpus);
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}		
	}

	public void addSourceDocument(Corpus corpus, SourceDocument sourceDocument) throws IOException {
		Session session = dbRepository.getSessionFactory().openSession();
		try  {
			DBSourceDocument dbSourceDocument = 
					dbRepository.getDbSourceDocumentHandler().getDbSourceDocument(
							session, sourceDocument.getID());
			DBCorpusSourceDocument dbCorpusSourceDocument = 
					new DBCorpusSourceDocument(
						Integer.valueOf(corpus.getId()), 
						dbSourceDocument);
			session.beginTransaction();
			session.save(dbCorpusSourceDocument);
			session.getTransaction().commit();
			CloseSafe.close(new CloseableSession(session));
			
			corpus.addSourceDocument(sourceDocument);
			dbRepository.getPropertyChangeSupport().firePropertyChange(
				Repository.RepositoryChangeEvent.corpusChanged.name(),
				sourceDocument, corpus);
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}	
	}

	public void addUserMarkupCollectionRef(Corpus corpus,
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		Session session = dbRepository.getSessionFactory().openSession();
		try  {
			DBCorpusUserMarkupCollection dbCorpusUserMarkupCollection = 
					new DBCorpusUserMarkupCollection(
						Integer.valueOf(corpus.getId()),
						Integer.valueOf(userMarkupCollectionReference.getId()));
			session.beginTransaction();
			session.save(dbCorpusUserMarkupCollection);
			session.getTransaction().commit();
			CloseSafe.close(new CloseableSession(session));
			
			corpus.addUserMarkupCollectionReference(
					userMarkupCollectionReference);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
				Repository.RepositoryChangeEvent.corpusChanged.name(),
				userMarkupCollectionReference, corpus);
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}	
		
	}

	public void delete(Corpus corpus) throws IOException {
		Session session = dbRepository.getSessionFactory().openSession();
		try  {
			DBCorpus dbCorpus = 
					(DBCorpus) session.load(
							DBCorpus.class, Integer.valueOf(corpus.getId()));
			session.beginTransaction();
			session.delete(dbCorpus);
			session.getTransaction().commit();
			CloseSafe.close(new CloseableSession(session));
		
			corpora.remove(corpus);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					Repository.RepositoryChangeEvent.corpusChanged.name(),
					corpus, null);
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}	
	}
	
}
