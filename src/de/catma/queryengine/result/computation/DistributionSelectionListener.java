package de.catma.queryengine.result.computation;

import java.util.List;

import de.catma.queryengine.result.QueryResultRow;

public interface DistributionSelectionListener {
	public void queryResultRowsSelected(String label, List<QueryResultRow> rows, int x, int y);
}
