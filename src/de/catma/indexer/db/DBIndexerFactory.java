package de.catma.indexer.db;

import de.catma.indexer.Indexer;
import de.catma.indexer.IndexerFactory;

public class DBIndexerFactory implements IndexerFactory {

	public Indexer createIndexer() {
		return new DBIndexer();
	}

}
