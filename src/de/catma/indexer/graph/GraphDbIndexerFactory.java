package de.catma.indexer.graph;

import java.util.Map;

import de.catma.indexer.Indexer;
import de.catma.indexer.IndexerFactory;

public class GraphDbIndexerFactory implements IndexerFactory {

	@Override
	public Indexer createIndexer(Map<String, Object> properties)
			throws Exception {
		return new GraphDBIndexer(properties);
	}

}
