package de.catma.indexer;

import de.catma.core.document.repository.Repository;

public interface IndexedRepository extends Repository {
	public Indexer getIndexer();
}
