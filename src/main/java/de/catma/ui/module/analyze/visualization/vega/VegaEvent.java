package de.catma.ui.module.analyze.visualization.vega;

import de.catma.project.Project;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.module.analyze.QueryOptionsProvider;

public class VegaEvent {
	
	private QueryResult queryResult;
	private QueryOptionsProvider queryOptionsProvider;
	private Project project;
	public VegaEvent(QueryResult queryResult, QueryOptionsProvider queryOptionsProvider, Project project) {
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
	public Project getProject() {

		return project;
	}
	
	

}
