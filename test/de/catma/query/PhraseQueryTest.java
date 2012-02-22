package de.catma.query;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
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
			                        		sd.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getTitle())
			                    .endObject()
			                  )
			        .execute()
			        .actionGet();
		} catch (Exception e) {
			ExceptionHandler.log(e);
		}
	}
}
