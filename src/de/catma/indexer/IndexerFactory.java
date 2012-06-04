package de.catma.indexer;

import java.util.Properties;

public interface IndexerFactory {
	public Indexer createIndexer(Properties properties);
}
