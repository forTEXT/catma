package de.catma.repository.db;

import java.util.Properties;

import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryFactory;
import de.catma.core.document.repository.RepositoryPropertyKey;
import de.catma.core.tag.TagManager;
import de.catma.indexer.Indexer;
import de.catma.indexer.IndexerFactory;
import de.catma.indexer.IndexerPropertyKey;

public class DBRepositoryFactory implements RepositoryFactory {

	public Repository createRepository(TagManager tagManager,
			Properties properties, int index) throws Exception {
		
		String indexerFactoryClassName = 
				properties.getProperty(IndexerPropertyKey.IndexerFactory.name());
		IndexerFactory indexerFactory = 
				(IndexerFactory)Class.forName(indexerFactoryClassName).newInstance();
		Indexer indexer = indexerFactory.createIndexer();
		
		return new DBRepository(
			RepositoryPropertyKey.Repository.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryFolderPath.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryAuthenticationRequired.isTrue(properties, index, false),
			indexer);
	}

}
