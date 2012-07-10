package de.catma.ui.analyzer;

import de.catma.queryengine.result.GroupedQueryResultSet;

public interface GroupedQueryResultSelectionListener {
	public void resultsSelected(
			GroupedQueryResultSet groupedQueryResultSet);
}