package de.catma.core.document;

import java.util.Properties;

public interface RepositoryFactory {

	Repository createRepository(Properties properties) throws Exception;
	
}
