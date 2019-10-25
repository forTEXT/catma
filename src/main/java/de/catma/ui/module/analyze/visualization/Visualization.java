package de.catma.ui.module.analyze.visualization;

import com.vaadin.ui.Component;

import de.catma.queryengine.result.QueryResultRow;
import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;

public interface Visualization extends Component {
	void addQueryResultRows(Iterable<QueryResultRow> queryResult);
	void removeQueryResultRows(Iterable<QueryResultRow> queryResult);
	void setExpansionListener(ExpansionListener expansionListener);
	void close();
	void setSelectedQueryResultRows(Iterable<QueryResultRow> selectedRows);
	void setDisplaySetting(DisplaySetting displaySettings);
}
