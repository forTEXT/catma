package de.catma.query;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.FileInputStream;
import java.util.Properties;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.catma.core.ExceptionHandler;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryManager;
import de.catma.core.document.source.SourceDocument;


public class PhraseQueryTest {

	private Client client;
	private Repository repository;

	@Before
	public void setup() {
		client = new TransportClient()
				.addTransportAddress(new InetSocketTransportAddress(
						"clea.bsdsystems.de", 9300));
		
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("test/catma.properties"));
			repository = new RepositoryManager(properties).getRepositories().get(0);
			repository.open();
		}
		catch( Exception e) {
			ExceptionHandler.log(e);
		}
	}


	@Test
	public void testSearch(){
		QueryBuilder qb1 = termQuery("content", "alice");
		SearchResponse response = client.prepareSearch("test")
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(qb1)
        .execute()
        .actionGet();
		
		System.out.print(response.toString());
	}
	
	@After
	public void teardown() {
		client.close();
	}

	@Test
	public void testQuery() {
		
		SourceDocument sd = repository.getSourceDocument(
				"http://www.gutenberg.org/cache/epub/13/pg13.txt");
		try {
			IndexResponse response = client.prepareIndex("document", "book", "1")
			        .setSource(XContentFactory.jsonBuilder()
			                    .startObject()
			                        .field("content", sd.getContent())
			                        .field("title",
			                        		sd.getSourceContentHandler().
			                        		getSourceDocumentInfo().
			                        		getContentInfoSet().
			                        		getTitle())
			                    .endObject()
			                  )
			        .execute()
			        .actionGet();
			
			
		} catch (Exception e) {
			ExceptionHandler.log(e);
		}
	}
}
