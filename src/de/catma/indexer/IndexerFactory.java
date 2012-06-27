package de.catma.indexer;

import java.util.Map;

public interface IndexerFactory {
	public Indexer createIndexer(Map<String,Object> properties);
}
