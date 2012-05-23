package de.catma.repository.db;

import java.util.Properties;

import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryFactory;
import de.catma.core.document.repository.RepositoryPropertyKey;
import de.catma.core.tag.TagManager;

public class DBRepositoryFactory implements RepositoryFactory {

	public Repository createRepository(TagManager tagManager,
			Properties properties, int index) throws Exception {

		boolean isLocal = 
				Boolean.valueOf(properties.getProperty(
					RepositoryPropertyKey.isLocal.name(), 
					Boolean.FALSE.toString()));
		
		return new DBRepository(
			properties.getProperty(RepositoryPropertyKey.RepositoryName.name()),
			isLocal);
	}

}
