package de.catma.indexer.graph;

import org.neo4j.graphdb.GraphDatabaseService;

public enum CatmaGraphDbName {
	CATMAGRAPHDB
	;
	
	private volatile GraphDatabaseService graphDatabaseService;

	public GraphDatabaseService getGraphDatabaseService() {
		return graphDatabaseService;
	}

	public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
		this.graphDatabaseService = graphDatabaseService;
	}
	
	
}
