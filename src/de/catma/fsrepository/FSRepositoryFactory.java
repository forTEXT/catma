package de.catma.fsrepository;

import java.util.Properties;

import de.catma.core.document.Repository;
import de.catma.core.document.RepositoryFactory;
import de.catma.serialization.SerializationHandlerFactory;

public class FSRepositoryFactory implements RepositoryFactory {
	private enum Key {
		SerializationHandlerFactory,
		RepoFolderPath,
		;
	}

	public Repository createRepository(Properties properties) throws Exception {
		String serializationHandlerFactoryClazzName = 
				properties.getProperty(Key.SerializationHandlerFactory.name());
		String repoFolderPath = properties.getProperty(Key.RepoFolderPath.name());
		
		SerializationHandlerFactory serializationHandlerFactory = 
				(SerializationHandlerFactory) Thread.currentThread().getContextClassLoader().loadClass(
				serializationHandlerFactoryClazzName).newInstance();
		
		return new FSRepository(repoFolderPath, serializationHandlerFactory);
	}

}
