package de.catma.ui.module.analyze.visualization;

import com.vaadin.ui.Component;

import de.catma.queryengine.result.QueryResultRow;
import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;

public interface Visualisation extends Component {
	void addQueryResultRows(Iterable<QueryResultRow> queryResult);
	void removeQueryResultRows(Iterable<QueryResultRow> queryResult);
	void setExpansionListener(ExpansionListener expansionListener);
	void close();
	void setSelectedQueryResultRow(QueryResultRow row);
	void setDisplaySetting(DisplaySetting displaySettings);
}
