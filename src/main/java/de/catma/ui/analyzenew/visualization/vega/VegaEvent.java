package de.catma.ui.analyzenew.visualization.vega;

import de.catma.document.repository.Repository;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.analyzenew.QueryOptionsProvider;

public class VegaEvent {
	
	private QueryResult queryResult;
	private QueryOptionsProvider queryOptionsProvider;
	private Repository project;
	public VegaEvent(QueryResult queryResult, QueryOptionsProvider queryOptionsProvider, Repository project) {
		super();
		this.queryResult = queryResult;
		this.queryOptionsProvider = queryOptionsProvider;
		this.project = project;
	}
	public QueryResult getQueryResult() {
		return queryResult;
	}
	public QueryOptionsProvider getQueryOptionsProvider() {
		return queryOptionsProvider;
	}
	public Repository getProject() {

		return project;
	}
	
	

}
