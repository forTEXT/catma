package de.catma.indexer.graph;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.graphdb.GraphDatabaseService;

public enum CatmaGraphDbName {
	CATMAGRAPHDB
	;
	
	private volatile GraphDatabaseService graphDatabaseService;
	private volatile Driver driver;

	@Deprecated
	public GraphDatabaseService getGraphDatabaseService() {
		return graphDatabaseService;
	}

	@Deprecated
	public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
		this.graphDatabaseService = graphDatabaseService;
	}
	
	public void setDriver(Driver driver) {
		this.driver = driver;
	}
	
	public Session getBoltSession() {
		return driver.session();
	}	
}
