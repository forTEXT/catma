package de.catma.ui.analyzenew.queryresultpanel;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;

import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class DummyQueryResultRowItem implements QueryResultRowItem {

	@Override
	public String getKey() {
		return null; //no key
	}

	@Override
	public int getFrequency() {
		return 0;
	}

	@Override
	public QueryResultRowArray getRows() {
		return null; // no rows
	}

	@Override
	public Integer getStartOffset() {
		return null; //no startoffset 
	}

	@Override
	public Integer getEndOffset() {
		return null; //no endoffset 
	}

	@Override
	public String getDetailedKeyInContext() {
		return null; // no detailedKeyInContext
	}

	@Override
	public boolean isExpansionDummy() {
		return true;
	}

	@Override
	public void addChildRowItems(TreeData<QueryResultRowItem> treeData, LoadingCache<String, KwicProvider> kwicProviderCache) {
		// no children to add
	}
	
	@Override
	public void addQueryResultRow(QueryResultRow row, TreeData<QueryResultRowItem> treeData,
			LoadingCache<String, KwicProvider> kwicProviderCache) {
		// noop
	}
	
	@Override
	public void removeQueryResultRow(QueryResultRow row, TreeData<QueryResultRowItem> treeData) {
		// noop
	}
	
	@Override
	public boolean startsWith(String searchValue) {
		return true;
	}
}
