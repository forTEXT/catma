package de.catma.index;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.catma.ExceptionHandler;
import de.catma.backgroundservice.DebugBackgroundServiceProvider;
import de.catma.document.Range;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.source.KeywordInContext;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;
import de.catma.indexer.KwicProvider;
import de.catma.indexer.WildcardTermExtractor;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.tag.TagManager;

public class IndexerTest {
	private IndexedRepository repository;

	@Before
	public void setup() {
		TagManager tagManager  = new TagManager();
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("test/catma.properties"));
			RepositoryManager rm = new RepositoryManager(
							new DebugBackgroundServiceProvider(), 
							tagManager, properties);
			Map<String, String> userIdentification = new HashMap<String, String>();
			userIdentification.put("user.ident", "mp");
			repository = (IndexedRepository)rm.openRepository(
					rm.getRepositoryReferences().iterator().next(), userIdentification);
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
			
			Indexer indexer = repository.getIndexer();
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
			
			Indexer indexer = repository.getIndexer();
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
			
			Indexer indexer = repository.getIndexer();
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
						
			Indexer indexer = repository.getIndexer();
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
	
	@Test
	public void testKwicProvider() {
		
		SourceDocument sd = repository.getSourceDocument(
				"catma://CATMA_0dc558bf-1c2b-40d3-81bc-38f947414b73");
		if (repository instanceof IndexedRepository) {
			Indexer indexer = ((IndexedRepository)repository).getIndexer();
			List<String> umcIdList = new ArrayList<String>();
			umcIdList.add("35");
			try {
				QueryResult qr = indexer.searchTagDefinitionPath(umcIdList, "MoveMe2");
				
				KwicProvider kp = new KwicProvider(sd);
				for (QueryResultRow row : qr) {
					System.out.println(
						"\n-->marked Text@"+row.getRange()+
						"-->" + sd.getContent(row.getRange()));
					KeywordInContext kwic = 
							kp.getKwic(row.getRange(), 5);
					
					System.out.println("-->kwic-->"+kwic);
				}
				
				
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
	}
	
	@Test
	public void testKwicProvider2() throws Exception {
		SourceDocument sd = repository.getSourceDocument(
				"catma://CATMA_0dc558bf-1c2b-40d3-81bc-38f947414b73");
		try {
			KwicProvider kp = new KwicProvider(sd);
			
			Range range = new Range(1746,1831);
			System.out.println(
					"\n-->marked Text@"+range+
					"-->" + sd.getContent(range));
			KeywordInContext kwic = kp.getKwic(range, 5);
			
			System.out.println("-->kwic-->"+kwic);
			Assert.assertTrue(kwic.getLeftContext().equals("und sah dann, den "));
			Assert.assertTrue(kwic.getRightContext().equals(" die Anhöhen am anderen Ufer"));
			
			range = new Range(1749,1829);
			System.out.println(
					"\n-->marked Text@"+range+
					"-->" + sd.getContent(range));
			kwic = kp.getKwic(range, 5);
			
			System.out.println("-->kwic-->"+kwic);
			Assert.assertTrue(kwic.getLeftContext().equals("sah dann, den Ell"));
			Assert.assertTrue(kwic.getRightContext().equals("nd die Anhöhen am anderen"));
			
			range = new Range(0,21);
			System.out.println(
					"\n-->marked Text@"+range+
					"-->" + sd.getContent(range));
			kwic = kp.getKwic(range, 5);
			
			System.out.println("-->kwic-->"+kwic);
			Assert.assertTrue(kwic.getLeftContext().equals(""));
			Assert.assertTrue(kwic.getRightContext().equals(" EBook of Das Urteil,"));
			
			range = new Range(sd.getLength()-20,sd.getLength());
			System.out.println(
					"\n-->marked Text@"+range+
					"-->" + sd.getContent(range));
			kwic = kp.getKwic(range, 5);
			
			System.out.println("-->kwic-->"+kwic);
			Assert.assertTrue(kwic.getLeftContext().equals("our email newsletter to hear "));
			Assert.assertTrue(kwic.getRightContext().equals(""));
			
			range = new Range(4,21);
			System.out.println(
					"\n-->marked Text@"+range+
					"-->" + sd.getContent(range));
			kwic = kp.getKwic(range, 5);
			
			System.out.println("-->kwic-->"+kwic);
			Assert.assertTrue(kwic.getLeftContext().equals("The "));
			Assert.assertTrue(kwic.getRightContext().equals(" EBook of Das Urteil,"));
			
			range = new Range(sd.getLength()-20,sd.getLength()-1);
			System.out.println(
					"\n-->marked Text@"+range+
					"-->" + sd.getContent(range));
			kwic = kp.getKwic(range, 5);
			
			System.out.println("-->kwic-->"+kwic);
			Assert.assertTrue(kwic.getLeftContext().equals("our email newsletter to hear "));
			Assert.assertTrue(kwic.getRightContext().equals("\n"));
		}
		catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
}
