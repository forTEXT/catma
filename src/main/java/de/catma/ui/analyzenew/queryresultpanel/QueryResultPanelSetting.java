package de.catma.ui.analyzenew.queryresultpanel;

import de.catma.queryengine.result.QueryResult;

public class QueryResultPanelSetting {
	
	private String query;
	private QueryResult queryResult;
	private DisplaySetting displaySetting;
	
	public QueryResultPanelSetting(String query, QueryResult queryResult, DisplaySetting displaySetting) {
		super();
		this.query = query;
		this.queryResult = queryResult;
		this.displaySetting = displaySetting;
	}

	public String getQuery() {
		return query;
	}

	public QueryResult getQueryResult() {
		return queryResult;
	}

	public DisplaySetting getDisplaySetting() {
		return displaySetting;
	}
}
