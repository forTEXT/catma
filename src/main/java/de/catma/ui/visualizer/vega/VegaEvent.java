package de.catma.ui.visualizer.vega;

import de.catma.queryengine.result.QueryResult;
import de.catma.ui.analyzer.QueryOptionsProvider;

public class VegaEvent {
	
	private QueryResult queryResult;
	private QueryOptionsProvider queryOptionsProvider;
	public VegaEvent(QueryResult queryResult, QueryOptionsProvider queryOptionsProvider) {
		super();
		this.queryResult = queryResult;
		this.queryOptionsProvider = queryOptionsProvider;
	}
	public QueryResult getQueryResult() {
		return queryResult;
	}
	public QueryOptionsProvider getQueryOptionsProvider() {
		return queryOptionsProvider;
	}
	
	

}
