package de.catma.repository.db;

import java.util.Properties;

import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryFactory;
import de.catma.core.document.repository.RepositoryPropertyKey;
import de.catma.core.tag.TagManager;

public class DBRepositoryFactory implements RepositoryFactory {

	public Repository createRepository(TagManager tagManager,
			Properties properties, int index) throws Exception {

		return new DBRepository(
			RepositoryPropertyKey.Repository.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryFolderPath.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryAuthenticationRequired.isTrue(properties, index, false));
	}

}
