package de.catma.query;

import java.io.IOException;
import java.util.Date;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

public class PhraseQueryTest {

	Client client;

	@BeforeClass
	public void setup() {
		client = new TransportClient()
				.addTransportAddress(new InetSocketTransportAddress(
						"clea.bsdsystems.de", 9300));
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
