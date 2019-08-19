package de.catma.ui.analyzenew.queryresultpanel;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;

import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;

public class KwicQueryResultRowItem implements QueryResultRowItem {

	private QueryResultRow row;
	private String kwic;
	private String detailedKwic;
	private String documentName;
	private String collectionName;

	public KwicQueryResultRowItem(QueryResultRow row, String kwic, String detailedKwic) {
		this.row = row;
		this.kwic = kwic;
		this.detailedKwic = detailedKwic;
	}
	
	public KwicQueryResultRowItem(QueryResultRow row, String kwic, String detailedKwic, 
			String documentName, String collectionName) {
		this(row, kwic, detailedKwic);
		this.documentName = documentName;
		this.collectionName = collectionName;
	}

	@Override
	public String getKey() {
		return this.kwic;
	}

	@Override
	public int getFrequency() {
		return 1;
	}

	@Override
	public QueryResultRowArray getRows() {
		QueryResultRowArray array = new QueryResultRowArray();
		array.add(row);
		return array;
	}

	@Override
	public Integer getStartOffset() {
		return row.getRange().getStartPoint();
	}
	
	@Override
	public Integer getEndOffset() {
		return row.getRange().getEndPoint();
	}

	@Override
	public String getDetailedKeyInContext() {
		return detailedKwic;
	}

	@Override
	public boolean isExpansionDummy() {
		return false;
	}
	
	@Override
	public String getPropertyName() {
		if (row instanceof TagQueryResultRow) {
			return ((TagQueryResultRow) row).getPropertyName();
		}
		return QueryResultRowItem.super.getPropertyName();
	}
	
	@Override
	public String getPropertyValue() {
		if (row instanceof TagQueryResultRow) {
			return ((TagQueryResultRow) row).getPropertyValue();
		}
		return QueryResultRowItem.super.getPropertyValue();
	}

	@Override
	public void addChildRowItems(TreeData<QueryResultRowItem> treeData,
			LoadingCache<String, KwicProvider> kwicProviderCache) {
		// no children
	}
	
	@Override
	public String getCollectionName() {
		return collectionName;
	}
	
	@Override
	public String getDocumentName() {
		return documentName;
	}
	
	@Override
	public String getTagPath() {
		if (row instanceof TagQueryResultRow) {
			return ((TagQueryResultRow) row).getTagDefinitionPath();
		}
		return QueryResultRowItem.super.getTagPath();
	}
}
