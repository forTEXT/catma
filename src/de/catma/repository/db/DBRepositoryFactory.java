package de.catma.repository.db;

import java.util.Properties;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryFactory;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.indexer.IndexerFactory;
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
			indexerFactory,
			serializationHandlerFactory,
			RepositoryPropertyKey.RepositoryUrl.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryUser.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryPass.getProperty(properties, index),
			properties.getProperty(RepositoryPropertyKey.TempDir.name()));
	}

}
