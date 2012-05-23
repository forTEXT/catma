package de.catma.index;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import de.catma.core.ExceptionHandler;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryManager;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.core.tag.TagManager;
import de.catma.indexer.Indexer;
import de.catma.indexer.db.DBIndexer;

public class IndexerTest {
	private Repository repository;

	@Before
	public void setup() {
		TagManager tagManager  = new TagManager();
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("test/catma.properties"));
			repository = 
					new RepositoryManager(
							tagManager, properties).getRepositories().get(0);
			repository.open(null);
		}
		catch( Exception e) {
			ExceptionHandler.log(e);
		}
	}
	
	@Test
	public void testIndexSourceDoc() throws Throwable {
		try {
			SourceDocument sd = repository.getSourceDocument(
					"catma:///container/pg13.txt");
			
			Indexer indexer = new DBIndexer();
			indexer.index(
					sd, 
					Collections.<String>emptyList(), 
					Collections.<Character>emptyList(), 
					Locale.ENGLISH);
		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	@Test
	public void testIndexSourceDoc2() throws Throwable {
		try {
			SourceDocument sd = repository.getSourceDocument(
					"catma:///container/pg11.txt");
			
			Indexer indexer = new DBIndexer();
			indexer.index(
					sd, 
					Collections.<String>emptyList(), 
					Collections.<Character>emptyList(), 
					Locale.ENGLISH);
		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}
	
	@Test
	public void testIndexSourceDoc3() throws Throwable {
		try {
			SourceDocument sd = repository.getSourceDocument(
					"catma:///container/rose_for_emily.txt");
			
			Indexer indexer = new DBIndexer();
			indexer.index(
					sd, 
					Collections.<String>emptyList(), 
					Collections.<Character>emptyList(), 
					Locale.ENGLISH);
		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}
	
	@Test
	public void indexUserMarkupColl1() throws Throwable {
		try {
			SourceDocument sd = repository.getSourceDocument(
					"catma:///container/rose_for_emily.txt");
			UserMarkupCollectionReference ref = 
					sd.getUserMarkupCollectionRefs().get(0);
			
			UserMarkupCollection umc = 
					repository.getUserMarkupCollection(ref);
						
			Indexer indexer = new DBIndexer();
			indexer.index(umc.getTagReferences(), sd.getID(), umc.getId(), umc.getTagLibrary());
			indexer.close();
		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}	
	}
}
