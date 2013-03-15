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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import de.catma.db.CloseableSession;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.repository.db.model.DBCorpus;
import de.catma.repository.db.model.DBCorpusSourceDocument;
import de.catma.repository.db.model.DBUserCorpus;
import de.catma.util.CloseSafe;

public class DBCorpusHandler {
	
	private DBRepository dbRepository;
	private Map<String, Corpus> corpora;

	public DBCorpusHandler(DBRepository dbRepository) {
		this.dbRepository = dbRepository;
		corpora = new HashMap<String, Corpus>();
	}

	public Collection<Corpus> getCorpora() {
		return Collections.unmodifiableCollection(corpora.values());
	}

	public void loadCorpora(Session session) {
		corpora = getCorpusList(session);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Corpus> getCorpusList(Session session) {
		Map<String,Corpus> result = new HashMap<String, Corpus>();
		
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
				result.put(corpus.getId(), corpus);
			}
		}
		return result;
	}

	public void createCorpus(String name) throws IOException {
		Session session = dbRepository.getSessionFactory().openSession();
		try  {
			DBCorpus dbCorpus = new DBCorpus(name);
			DBUserCorpus dbUserCorpus = new DBUserCorpus(dbRepository.getCurrentUser(), dbCorpus);
			dbCorpus.getDbUserCorpus().add(dbUserCorpus);
			
			session.beginTransaction();
			session.save(dbCorpus);
			
			session.getTransaction().commit();
			
			CloseSafe.close(new CloseableSession(session));

			Corpus corpus = 
					new Corpus(String.valueOf(dbCorpus.getCorpusId()), name);
			corpora.put(corpus.getId(), corpus);
			
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
			SQLQuery query = session.createSQLQuery(
				"call CatmaRepository.addItemToCorpus(" +
					":umcID, :sourceDocumentLocalUri, :corpusID)");
			query.setParameter("umcID", null);
			query.setParameter("sourceDocumentLocalUri", sourceDocument.getID());
			query.setParameter("corpusID", Integer.valueOf(corpus.getId()));

			session.beginTransaction();
			query.executeUpdate();
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
			SQLQuery query = session.createSQLQuery(
				"call CatmaRepository.addItemToCorpus(" +
					":umcID, :sourceDocumentLocalUri, :corpusID)");
			query.setParameter("umcID", Integer.valueOf(userMarkupCollectionReference.getId()));
			query.setParameter("sourceDocumentLocalUri", null);
			query.setParameter("corpusID", Integer.valueOf(corpus.getId()));
			
			session.beginTransaction();
			query.executeUpdate();
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

	public void rename(Corpus corpus, String name) throws IOException {
		Session session = dbRepository.getSessionFactory().openSession();
		try  {
			DBCorpus dbCorpus = 
					(DBCorpus) session.load(
							DBCorpus.class, Integer.valueOf(corpus.getId()));
			session.beginTransaction();
			dbCorpus.setName(name);
			corpus.setName(name);
			session.getTransaction().commit();
			CloseSafe.close(new CloseableSession(session));
		
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					Repository.RepositoryChangeEvent.corpusChanged.name(),
					name, corpus);
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}	
	}

	public void reloadCorpora(Session session) {
		Map<String, Corpus> result = getCorpusList(session);
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
	
}
