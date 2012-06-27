package de.catma.indexer.db;

import java.util.Map;

import de.catma.indexer.Indexer;
import de.catma.indexer.IndexerFactory;

public class DBIndexerFactory implements IndexerFactory {

	public Indexer createIndexer(Map<String,Object> properties) {
		return new DBIndexer(properties);
	}

}
