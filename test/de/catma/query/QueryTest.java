package de.catma.query;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.gwt.dev.util.collect.HashSet;

import de.catma.LogProgressListener;
import de.catma.core.ExceptionHandler;
import de.catma.core.document.Range;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryManager;
import de.catma.core.document.source.KeywordInContext;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.tag.TagManager;
import de.catma.indexer.Indexer;
import de.catma.indexer.KwicProvider;
import de.catma.indexer.db.DBIndexer;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.QueryJob;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;


public class QueryTest {

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
			repository.open();
		}
		catch( Exception e) {
			ExceptionHandler.log(e);
		}
	}
	
	
	@Test
	public void testSearchTerm1() throws Throwable {
		try {
			Indexer indexer = new DBIndexer();
			List<String> term = new ArrayList<String>();
			term.add("pig");
			term.add("had");
			term.add("been");
			term.add("dead");
			QueryResult result = 
					indexer.searchPhrase(null, "pig had been dead", term);
			indexer.close();
			
			for (QueryResultRow qrr : result) {
				System.out.println(qrr);
			}
		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}
	
	@Test
	public void testSearchTerm2() throws Throwable {
		try {
			Indexer indexer = new DBIndexer();
			List<String> termList = new ArrayList<String>();
			termList.add("he");
			termList.add("came");
			List<String> documentIDs = new ArrayList<String>();
			documentIDs.add("catma:///container/pg13.txt");
			QueryResult result = indexer.searchPhrase(documentIDs, "he came", termList);
			indexer.close();
			for (QueryResultRow qrr : result) {
				System.out.println(qrr);
			}

		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}
	
	@Test
	public void testSearchTerm3() throws Throwable {
		try {
			Indexer indexer = new DBIndexer();
			List<String> termList = new ArrayList<String>();
			termList.add("he");
			termList.add("came");
			List<String> documentIDs = new ArrayList<String>();
			documentIDs.add("catma:///container/pg13.txt");
			QueryResult result = indexer.searchPhrase(documentIDs, "he came", termList);
			indexer.close();
			for (QueryResultRow qrr : result) {
				System.out.println(qrr);
			}

		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}
	
	@Test
	public void testSearchTerm4() throws Throwable {
		try {
			Indexer indexer = new DBIndexer();
			List<String> termList = new ArrayList<String>();
			termList.add("and");
//			termList.add("suddenly");
			List<String> documentIDs = new ArrayList<String>();
//			documentIDs.add("catma:///container/pg13.txt");
			QueryResult result = indexer.searchPhrase(documentIDs, "and suddenly", termList);
			indexer.close();
			for (QueryResultRow qrr : result) {
				System.out.println(qrr);
			}

		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}
	
	@Test
	public void freqSearch1() throws Throwable {
		try {
			Indexer indexer = new DBIndexer();
	
			List<String> documentIDs = new ArrayList<String>();
//			documentIDs.add("catma:///container/pg13.txt");
			
			QueryResult result =
					indexer.searchFreqency(
						documentIDs,
						CompareOperator.GREATERTHAN, 850, 
						null, 0);
			indexer.close();
			for (QueryResultRow qrr : result) {
				System.out.println(qrr);
			}

		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	@Test
	public void kwicPhraseQueryResult1() {
		List<String> unseparableCharacterSequences = Collections.emptyList();
		List<Character> userDefinedSeparatingCharacters = Collections.emptyList();
		List<String> documentIDs = new ArrayList<String>();
		documentIDs.add("catma:///container/pg13.txt");
		QueryOptions queryOptions = new QueryOptions(
//				(List<String>)null,
				documentIDs,
				unseparableCharacterSequences,
				userDefinedSeparatingCharacters,
				Locale.ENGLISH);
		
		QueryJob job = new QueryJob(
				"\"To the day when you took me aboard of your ship\"", new DBIndexer(), queryOptions);
		job.setProgressListener(new LogProgressListener());
		try {
			
			QueryResultRowArray result = (QueryResultRowArray) job.call();
			Map<String, List<Range>> rangesGroupedByDocumentId = 
					new HashMap<String, List<Range>>();
			
			for (QueryResultRow row : result) {
				
				if (!rangesGroupedByDocumentId.containsKey(row.getSourceDocumentId())) {
					rangesGroupedByDocumentId.put(
							row.getSourceDocumentId(), new ArrayList<Range>());
				}
				
				rangesGroupedByDocumentId.get(
						row.getSourceDocumentId()).add(row.getRange());
			}
			for (Map.Entry<String, List<Range>> entry : 
							rangesGroupedByDocumentId.entrySet()) {
				System.out.println("documentId: " + entry.getKey());
				SourceDocument sd = 
						repository.getSourceDocument(entry.getKey());
				KwicProvider kwicProvider = new KwicProvider(sd);
				
				List<KeywordInContext> kwics = 
						kwicProvider.getKwic(entry.getValue(), 5);
				
				System.out.println("Results for " + sd);
				for (KeywordInContext kwic : kwics) {
					System.out.println(kwic);
				}
				
				System.out.println("\n");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void kwicPhraseQueryResult2() {
		List<String> unseparableCharacterSequences = Collections.emptyList();
		List<Character> userDefinedSeparatingCharacters = Collections.emptyList();
		List<String> documentIDs = new ArrayList<String>();
//		documentIDs.add("catma:///container/pg13.txt");
		QueryOptions queryOptions = new QueryOptions(
//				(List<String>)null,
				documentIDs,
				unseparableCharacterSequences,
				userDefinedSeparatingCharacters,
				Locale.ENGLISH);
		
		QueryJob job = new QueryJob(
				"\"and\"", new DBIndexer(), queryOptions);
		job.setProgressListener(new LogProgressListener());
		try {
			
			QueryResultRowArray result = (QueryResultRowArray) job.call();
			Map<String, List<Range>> rangesGroupedByDocumentId = 
					new HashMap<String, List<Range>>();
			
			for (QueryResultRow row : result) {
				
				if (!rangesGroupedByDocumentId.containsKey(row.getSourceDocumentId())) {
					rangesGroupedByDocumentId.put(
							row.getSourceDocumentId(), new ArrayList<Range>());
				}
				
				rangesGroupedByDocumentId.get(
						row.getSourceDocumentId()).add(row.getRange());
			}
			for (Map.Entry<String, List<Range>> entry : 
							rangesGroupedByDocumentId.entrySet()) {
				System.out.println("documentId: " + entry.getKey());
				SourceDocument sd = 
						repository.getSourceDocument(entry.getKey());
				KwicProvider kwicProvider = new KwicProvider(sd);
				
				List<KeywordInContext> kwics = 
						kwicProvider.getKwic(entry.getValue(), 5);
				
				System.out.println("Results for " + sd);
				for (KeywordInContext kwic : kwics) {
					System.out.println("\n" + kwic + "\n");
				}
				
				System.out.println("\n");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testPhraseQuery(){
		List<String> unseparableCharacterSequences = Collections.emptyList();
		List<Character> userDefinedSeparatingCharacters = Collections.emptyList();
		QueryOptions queryOptions = new QueryOptions(
				(List<String>)null,
				unseparableCharacterSequences,
				userDefinedSeparatingCharacters,
				Locale.ENGLISH);
		
		QueryJob job = new QueryJob(
				"\"pig had been dead\"", new DBIndexer(), queryOptions);
		job.setProgressListener(new LogProgressListener());
		try {
			System.out.println(job.call());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testSearchTag() throws Throwable {
		List<String> documentIDs = new ArrayList<String>();
//		documentIDs.add("catma:///container/pg13.txt");

		List<String> userMarkupCollIDs = new ArrayList<String>();
		
		try {
			Indexer indexer = new DBIndexer();
			QueryResult result = 
					indexer.searchTagDefinitionPath(
						documentIDs, userMarkupCollIDs, "/Order/analepsis");
			
			indexer.close();
			Set<String> tagInstances = new HashSet<String>();
			for (QueryResultRow qrr : result) {
				System.out.println(qrr);
				tagInstances.add(qrr.getTagInstanceId());
			}
			
			System.out.println("Instances found: " + tagInstances.size());
			System.out.println(Arrays.toString(tagInstances.toArray()));

		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

//	@Test
	public void testColocation() throws Throwable {
		try {
			Indexer esIndexer = new DBIndexer();
//			esIndexer.searchColocation(null, "you", "will", 10);
			esIndexer.close();
		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}
}
