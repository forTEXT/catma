package de.catma.query;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.catma.LogProgressListener;
import de.catma.core.ExceptionHandler;
import de.catma.core.document.Range;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryManager;
import de.catma.core.tag.TagManager;
import de.catma.indexer.elasticsearch.ESIndexer;
import de.catma.queryengine.QueryJob;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.QueryResultRowArray;


public class PhraseQueryTest {

	private Client client;
	private Repository repository;

	@Before
	public void setup() {
		TagManager tagManager  = new TagManager();
		
		client = new TransportClient()
				.addTransportAddress(new InetSocketTransportAddress(
						"clea.bsdsystems.de", 9300));
		
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
	public void testSearchTerm() throws Throwable {
		try {
			ESIndexer esIndexer = new ESIndexer();
			List<String> term = new ArrayList<String>();
			term.add("pig");
			term.add("had");
			term.add("been");
			term.add("dead");
			Map<String, List<Range>> result = esIndexer.searchTerm(null, term);
		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
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
				"\"pig had been dead\"", new ESIndexer(), queryOptions);
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
		try {
			ESIndexer esIndexer = new ESIndexer();
			 QueryResultRowArray result = esIndexer.searchTag("/Order/analepsis", true);
		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}


	
	@After
	public void teardown() {
		client.close();
	}

	@Test
	public void testIndex() {
//		
//		SourceDocument sd = repository.getSourceDocument(
//				"http://www.gutenberg.org/cache/epub/13/pg13.txt");
//		try {
//
//			ActionFuture<DeleteIndexResponse> future = 
//					client.admin().indices().delete(new DeleteIndexRequest("document"));
//			future.actionGet();
			
//			client.admin().indices().prepareCreate("document").addMapping(
//					"book", XContentFactory.jsonBuilder().
//						startObject().startObject("book").startObject("content").
//							field("type", "string").
//							field("term_vector", "with_positions_offset").
//							field("store", "no").
//						endObject().endObject().endObject());
//						
//			
//			IndexResponse response = client.prepareIndex("document", "book", "1")
//			        .setSource(XContentFactory.jsonBuilder()
//			                    .startObject()
//			                        .field("content", sd.getContent())
//			                        .field("title", 
//			                        		sd.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getTitle())
//			                    .endObject()
//			                  )
//			        .setRefresh(true)
//			        .execute()
//			        .actionGet();
			
//		} catch (Exception e) {
//			ExceptionHandler.log(e);
//		}
	}
}
