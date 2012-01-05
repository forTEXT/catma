package de.catma.core.document.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


public class RepositoryManager {
	
	private List<Repository> repositories;
	
	public RepositoryManager(Properties properties) 
			throws Exception {
		repositories = new ArrayList<Repository>();
		
		int index=1;
		while(RepositoryPropertyKey.Repository.exists(properties, index)) {
			
			RepositoryFactory repositoryFactory =  
					(RepositoryFactory)Class.forName(
							RepositoryPropertyKey.RepositoryFactory.getProperty(properties, index),
							true, Thread.currentThread().getContextClassLoader()).newInstance();
			
			Repository repository = repositoryFactory.createRepository(properties, index);
			
			repositories.add(repository);
			
			index++;
		}

	}

	public List<Repository> getRepositories() {
		return Collections.unmodifiableList(repositories);
	}
}
