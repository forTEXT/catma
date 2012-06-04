package de.catma.repository.db;

import java.util.Properties;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryFactory;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.indexer.Indexer;
import de.catma.indexer.IndexerFactory;
import de.catma.indexer.IndexerPropertyKey;
import de.catma.serialization.SerializationHandlerFactory;
import de.catma.tag.TagManager;

public class DBRepositoryFactory implements RepositoryFactory {

	public Repository createRepository(BackgroundServiceProvider backgroundServiceProvider,
			TagManager tagManager, Properties properties, int index)
			throws Exception {
		
		String indexerFactoryClassName = 
				RepositoryPropertyKey.IndexerFactory.getProperty(properties, index);
		IndexerFactory indexerFactory = 
				(IndexerFactory)Class.forName(indexerFactoryClassName).newInstance();
		Properties indexerProperties = new Properties();
		indexerProperties.setProperty(
			IndexerPropertyKey.IndexerUrl.name(),
			RepositoryPropertyKey.IndexerUrl.getProperty(properties, index));
		indexerProperties.setProperty(
			IndexerPropertyKey.IndexerUser.name(),
			RepositoryPropertyKey.RepositoryUser.getProperty(properties, index));
		indexerProperties.setProperty(
			IndexerPropertyKey.IndexerPass.name(),
			RepositoryPropertyKey.RepositoryPass.getProperty(properties, index));

		Indexer indexer = indexerFactory.createIndexer(indexerProperties);

		String serializationHandlerFactoryClazzName = 
				RepositoryPropertyKey.SerializationHandlerFactory.getProperty(properties, index);
		SerializationHandlerFactory serializationHandlerFactory = 
				(SerializationHandlerFactory) Class.forName(
						serializationHandlerFactoryClazzName, true, 
						Thread.currentThread().getContextClassLoader()).newInstance();
		serializationHandlerFactory.setTagManager(tagManager);

		return new DBRepository(
			RepositoryPropertyKey.Repository.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryFolderPath.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryAuthenticationRequired.isTrue(properties, index, false),
			tagManager,
			backgroundServiceProvider,
			indexer,
			serializationHandlerFactory,
			RepositoryPropertyKey.RepositoryUrl.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryUser.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryPass.getProperty(properties, index));
	}

}
