package de.catma.ui.module.analyze.queryresultpanel;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;

import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public interface QueryResultRowItem {
	public String getKey();
	public String getFilterKey();
	public int getFrequency();
	public QueryResultRowArray getRows();
	public Integer getStartOffset();
	public Integer getEndOffset();
	public String getDetailedKeyInContext();
	public boolean isExpansionDummy();
	public void addChildRowItems(
			TreeData<QueryResultRowItem> treeData, LoadingCache<String, KwicProvider> kwicProviderCache);
	public void addQueryResultRow(
		QueryResultRow row, 
		TreeData<QueryResultRowItem> treeData, 
		LoadingCache<String, KwicProvider> kwicProviderCache);
	
	public boolean startsWith(String searchValue);

	// used when result contains properties and user drills down to Kwic leafs
	public default String getPropertyName() { return null; }
	public default String getPropertyValue() { return null; } //TODO: split up in short value and description, see KwicItemHandler
	
	// used for 'flat table' and 'properties as columns' display
	public default String getDocumentName() { return null; }
	public default String getCollectionName() { return null; }
	public default String getTagPath() { return null; }
	
	// used for 'properties as columns' display
	public default String getPropertyValue(String propertyName) { return null; } //TODO: split up in short value and description, see KwicItemHandler
	public void removeQueryResultRow(QueryResultRow row, TreeData<QueryResultRowItem> treeData);
}