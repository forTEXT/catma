package de.catma.fsrepository;

import java.util.Properties;

import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryFactory;
import de.catma.core.document.repository.RepositoryPropertyKey;
import de.catma.serialization.SerializationHandlerFactory;

public class FSRepositoryFactory implements RepositoryFactory {
	
	public Repository createRepository(Properties properties, int index) throws Exception {
		String serializationHandlerFactoryClazzName = 
				RepositoryPropertyKey.SerializationHandlerFactory.getProperty(properties, index);
		String repoFolderPath = RepositoryPropertyKey.RepositoryFolderPath.getProperty(properties, index);
		
		SerializationHandlerFactory serializationHandlerFactory = 
				(SerializationHandlerFactory) Class.forName(
						serializationHandlerFactoryClazzName, true, 
						Thread.currentThread().getContextClassLoader()).newInstance();
		
		return new FSRepository(
				RepositoryPropertyKey.Repository.getProperty(properties, index),
				repoFolderPath, 
				serializationHandlerFactory);
	}

}
