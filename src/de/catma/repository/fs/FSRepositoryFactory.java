package de.catma.repository.fs;

import java.util.Properties;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryFactory;
import de.catma.core.document.repository.RepositoryPropertyKey;
import de.catma.core.tag.TagManager;
import de.catma.serialization.SerializationHandlerFactory;

public class FSRepositoryFactory implements RepositoryFactory {
	
	public Repository createRepository(BackgroundServiceProvider backgroundServiceProvider,
			TagManager tagManager, Properties properties, int index)
			throws Exception {
		
		String serializationHandlerFactoryClazzName = 
				RepositoryPropertyKey.SerializationHandlerFactory.getProperty(properties, index);
		String repoFolderPath = RepositoryPropertyKey.RepositoryFolderPath.getProperty(properties, index);
		
		SerializationHandlerFactory serializationHandlerFactory = 
				(SerializationHandlerFactory) Class.forName(
						serializationHandlerFactoryClazzName, true, 
						Thread.currentThread().getContextClassLoader()).newInstance();
		serializationHandlerFactory.setTagManager(tagManager);
		return new FSRepository(
				RepositoryPropertyKey.Repository.getProperty(properties, index),
				repoFolderPath, 
				serializationHandlerFactory);
	}

}
