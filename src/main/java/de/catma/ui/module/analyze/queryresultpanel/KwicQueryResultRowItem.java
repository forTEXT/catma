package de.catma.ui.module.analyze.queryresultpanel;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;

import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;

public class KwicQueryResultRowItem implements QueryResultRowItem {

	private final QueryResultRow row;
	private String kwic;
	private String detailedKwic;
	private String documentName;
	private String collectionName;
	private boolean removed = false;
	private boolean matchTag;

	public KwicQueryResultRowItem(QueryResultRow row, String kwic, String detailedKwic, boolean matchTag) {
		this.row = row;
		this.kwic = kwic;
		this.detailedKwic = detailedKwic;
		this.matchTag = matchTag;
	}
	
	public KwicQueryResultRowItem(QueryResultRow row, String kwic, String detailedKwic, 
			String documentName, String collectionName, boolean matchTag) {
		this(row, kwic, detailedKwic, matchTag);
		this.documentName = documentName;
		this.collectionName = collectionName;
	}

	@Override
	public String getKey() {
		return this.kwic;
	}
	
	@Override
	public String getFilterKey() {
		return this.kwic;
	}

	@Override
	public int getFrequency() {
		return 1;
	}

	@Override
	public QueryResultRowArray getRows() {
		QueryResultRowArray array = new QueryResultRowArray();
		if (!removed) {
			array.add(row);
		}
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((row == null) ? 0 : row.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KwicQueryResultRowItem other = (KwicQueryResultRowItem) obj;
		if (row == null) {
			if (other.row != null)
				return false;
		} else if (!row.equals(other.row))
			return false;
		return true;
	}
	
	@Override
	public void addQueryResultRow(QueryResultRow row, TreeData<QueryResultRowItem> treeData,
			LoadingCache<String, KwicProvider> kwicProviderCache) {
		// noop
	}
	
	@Override
	public void removeQueryResultRow(QueryResultRow row, TreeData<QueryResultRowItem> treeData) {
		if ((this.row != null) && this.row.equals(row)) {
			this.removed = true;
		}
		
	}
	
	@Override
	public boolean startsWith(String searchValue) {
		return matchTag?((TagQueryResultRow)row).getTagDefinitionPath().startsWith(searchValue):row.getPhrase().startsWith(searchValue);
	}
}
