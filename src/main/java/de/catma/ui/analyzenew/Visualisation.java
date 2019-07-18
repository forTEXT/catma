package de.catma.ui.analyzenew;

import de.catma.queryengine.result.QueryResultRow;

public interface Visualisation {
	
	void addQueryResultRows(Iterable<QueryResultRow> queryResult) throws Exception;
	void removeQueryResultRows(Iterable<QueryResultRow> queryResult) throws Exception;

}
