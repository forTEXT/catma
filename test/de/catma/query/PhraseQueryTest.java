package de.catma.query;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.IOException;
import java.util.Date;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class PhraseQueryTest {

	Client client;

	@BeforeClass
	public void setup() {
		client = new TransportClient()
				.addTransportAddress(new InetSocketTransportAddress(
						"clea.bsdsystems.de", 9300));
	}

	public void testSearch(){
		QueryBuilder qb1 = termQuery("content", "alice");
		SearchResponse response = client.prepareSearch("test")
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(qb1)
        .execute()
        .actionGet();
		
		System.out.print(response.toString());
	}
	
	@AfterClass
	public void teardown() {
		client.close();
	}

	public void testQuery() {
		try {
			IndexResponse response = client.prepareIndex("document", "book", "1")
			        .setSource(jsonBuilder()
			                    .startObject()
			                        .field("content", "")
			                        .field("postDate", new Date())
			                        .field("message", "trying out Elastic Search")
			                    .endObject()
			                  )
			        .execute()
			        .actionGet();
		} catch (ElasticSearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
