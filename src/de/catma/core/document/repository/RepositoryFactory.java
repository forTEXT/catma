package de.catma.core.document.repository;

import java.util.Properties;

public interface RepositoryFactory {

	Repository createRepository(Properties properties, int index) throws Exception;
	
}
