package de.catma.index;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.catma.ExceptionHandler;
import de.catma.backgroundservice.DebugBackgroundServiceProvider;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.Indexer;
import de.catma.indexer.WildcardTermExtractor;
import de.catma.indexer.db.DBIndexer;
import de.catma.tag.TagManager;

public class IndexerTest {
	private Repository repository;

//	@Before
	public void setup() {
		TagManager tagManager  = new TagManager();
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("test/catma.properties"));
			RepositoryManager rm = new RepositoryManager(
							new DebugBackgroundServiceProvider(), 
							tagManager, properties);
			repository = rm.openRepository(
					rm.getRepositoryReferences().iterator().next(), null);
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
			
			Indexer indexer = new DBIndexer("jdbc:mysql://localhost/CatmaIndex", "root", null);
			indexer.index(sd);
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
			
			Indexer indexer = new DBIndexer("jdbc:mysql://localhost/CatmaIndex", "root", null);
			indexer.index(sd);
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
			
			Indexer indexer = new DBIndexer("jdbc:mysql://localhost/CatmaIndex", "root", null);
			indexer.index(sd);
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
						
			Indexer indexer = new DBIndexer("jdbc:mysql://localhost/CatmaIndex", "root", null);
			indexer.index(umc.getTagReferences(), sd.getID(), umc.getId(), umc.getTagLibrary());
			indexer.close();
		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}	
	}
	
	@Test
	public void testWildcardTermExtractor() throws IOException {
		Assert.assertArrayEquals(
			testWildcardTermExtracter("%lung"),
			new Object[] {"%lung"});
		
		Assert.assertArrayEquals(
				testWildcardTermExtracter("lung%"),
				new Object[] {"lung%"});
		
		Assert.assertArrayEquals(
				testWildcardTermExtracter("l%ung"),
				new Object[] {"l%ung"});
		
		Assert.assertArrayEquals(
				testWildcardTermExtracter("\\%lung"),
				new Object[] {"\\%lung"});
		
		Assert.assertArrayEquals(
				testWildcardTermExtracter("lung\\%"),
				new Object[] {"lung\\%"});
		
		Assert.assertArrayEquals(
				testWildcardTermExtracter("\\tlung"),
				new Object[] {"\\","tlung"});
		Assert.assertArrayEquals(
				testWildcardTermExtracter("\\t lung"),
				new Object[] {"\\","t", "lung"});
				
		Assert.assertArrayEquals(
				testWildcardTermExtracter("__lung"),
				new Object[] {"__lung"});
		Assert.assertArrayEquals(
				testWildcardTermExtracter("%_lung"),
				new Object[] {"%_lung"});
		Assert.assertArrayEquals(
				testWildcardTermExtracter("_%lung"),
				new Object[] {"_%lung"});
		
		Assert.assertArrayEquals(
				testWildcardTermExtracter("Ha_e"),
				new Object[] {"Ha_e"});
		Assert.assertArrayEquals(
				testWildcardTermExtracter("Ha__"),
				new Object[] {"Ha__"});
		Assert.assertArrayEquals(
				testWildcardTermExtracter("Ha__n"),
				new Object[] {"Ha__n"});
	}
	
	private Object[] testWildcardTermExtracter(String content) throws IOException {
		System.out.println("IN: " + content);
		WildcardTermExtractor termExtractor = 
				new WildcardTermExtractor(
						content, 
						Collections.<String>emptyList(), 
						Collections.<Character>emptyList(), 
						Locale.getDefault());
		System.out.println(
			"OUT: " + Arrays.toString(termExtractor.getOrderedTerms().toArray()));
		return termExtractor.getOrderedTerms().toArray();
	}
}
