package de.catma.query;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.catma.core.ExceptionHandler;
import de.catma.core.document.Range;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryManager;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.tag.TagManager;
import de.catma.indexer.elasticsearch.ESIndexer;


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
			return;
		}
		catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}



	@Test
	public void testSearch(){
		QueryBuilder qb1 = termQuery("content", "hunt");
		SearchResponse response = client.prepareSearch("document")
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(qb1)
        .execute()
        .actionGet();
		
		System.out.print(response.toString());
		for (SearchHit sh : response.hits()) {
			System.out.println(sh);
			Map<String, HighlightField> hFields = sh.getHighlightFields();
			for (Map.Entry<String, HighlightField> hField : hFields.entrySet()) {
				System.out.println(hField.getKey());
				System.out.println(hField.getValue().toString());
			}
		}
	}
	
	@After
	public void teardown() {
		client.close();
	}

	@Test
	public void testIndex() {
		
		SourceDocument sd = repository.getSourceDocument(
				"http://www.gutenberg.org/cache/epub/13/pg13.txt");
		try {

			ActionFuture<DeleteIndexResponse> future = 
					client.admin().indices().delete(new DeleteIndexRequest("document"));
			future.actionGet();
			
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
			
		} catch (Exception e) {
			ExceptionHandler.log(e);
		}
	}
}
