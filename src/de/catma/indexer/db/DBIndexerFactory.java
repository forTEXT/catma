package de.catma.indexer.db;

import java.util.Properties;

import de.catma.indexer.Indexer;
import de.catma.indexer.IndexerFactory;
import de.catma.indexer.IndexerPropertyKey;

public class DBIndexerFactory implements IndexerFactory {

	public Indexer createIndexer(Properties properties) {
		return new DBIndexer(
				properties.getProperty(IndexerPropertyKey.IndexerUrl.name()),
				properties.getProperty(IndexerPropertyKey.IndexerUser.name()),
				properties.getProperty(IndexerPropertyKey.IndexerPass.name()));
	}

}
