package de.catma.ui.analyzenew.queryresultpanel;

import de.catma.queryengine.QueryId;
import de.catma.queryengine.result.QueryResult;

public class QueryResultPanelSetting {
	
	private QueryId queryId;
	private QueryResult queryResult;
	private DisplaySetting displaySetting;
	
	public QueryResultPanelSetting(QueryId queryId, QueryResult queryResult, DisplaySetting displaySetting) {
		super();
		this.queryId = queryId;
		this.queryResult = queryResult;
		this.displaySetting = displaySetting;
	}

	public QueryId getQueryId() {
		return queryId;
	}

	public QueryResult getQueryResult() {
		return queryResult;
	}

	public DisplaySetting getDisplaySetting() {
		return displaySetting;
	}
}
