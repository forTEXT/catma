package de.catma.ui.analyzenew.queryresultpanel;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;

import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRowArray;

interface QueryResultRowItem {
	public String getKey();
	public int getFrequency();
	public QueryResultRowArray getRows();
	public Integer getStartOffset();
	public Integer getEndOffset();
	public String getDetailedKeyInContext();
	public boolean isExpansionDummy();
	public void addChildRowItems(
			TreeData<QueryResultRowItem> treeData, LoadingCache<String, KwicProvider> kwicProviderCache);

	// used when result contains properties and user drills down to Kwic leafs
	public default String getPropertyName() { return null; }
	public default String getPropertyValue() { return null; }
	
	// used for 'flat table' and 'properties as columns' display
	public default String getDocumentName() { return null; }
	public default String getCollectionName() { return null; }
	public default String getTagPath() { return null; }
	
	// used for 'properties as columns' display
	public default String getPropertyValue(String propertyName) { return null; };
}