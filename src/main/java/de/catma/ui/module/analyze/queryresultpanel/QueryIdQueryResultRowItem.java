package de.catma.ui.module.analyze.queryresultpanel;

import java.util.ArrayList;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;
import com.vaadin.ui.UI;

import de.catma.indexer.KwicProvider;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.module.main.ErrorHandler;

public class QueryIdQueryResultRowItem implements QueryResultRowItem {
	
	private final QueryId queryId;
	protected final String identity;
	protected GroupedQueryResult groupedQueryResult;
	private QueryResultRowArray rows;

	public QueryIdQueryResultRowItem(String parentIdentity, GroupedQueryResult groupedQueryResult) {
		this.queryId = (QueryId)groupedQueryResult.getGroup();
		this.identity = parentIdentity + queryId.toSerializedString();
		this.groupedQueryResult = groupedQueryResult;
	}

	@Override
	public String getKey() {
		return queryId.getName();
	}
	
	@Override
	public String getFilterKey() {
		return getKey();
	}

	@Override
	public int getFrequency() {
		return groupedQueryResult.getTotalFrequency();
	}

	@Override
	public QueryResultRowArray getRows() {
		if (rows == null) {
			rows = new QueryResultRowArray();
			groupedQueryResult.forEach(row -> rows.add(row));
		}
		
		return rows;
	}

	@Override
	public Integer getStartOffset() {
		return null; //no startoffset on grouped entry
	}

	@Override
	public Integer getEndOffset() {
		return null; //no endoffset on grouped entry
	}
	
	@Override
	public String getDetailedKeyInContext() {
		return null; //no detaileKeyInContext on grouped entry
	}

	@Override
	public boolean isExpansionDummy() {
		return false;
	}

	@Override
	public void addChildRowItems(TreeData<QueryResultRowItem> treeData,
			LoadingCache<String, KwicProvider> kwicProviderCache) {
		try {
			for (String documentId : groupedQueryResult.getSourceDocumentIDs()) {
				String documentName = kwicProviderCache.get(documentId).getSourceDocumentName();
				DocumentQueryResultRowItem item = 
						new DocumentQueryResultRowItem(
							identity,
							documentName, documentId, groupedQueryResult.getSubResult(documentId));
				if (!treeData.contains(item)) {
					treeData.addItem(this, item);
					treeData.addItem(item, new DummyQueryResultRowItem());
				}
			}
		}
		catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("error displaying query results", e);
		}
	}
	
	@Override
	public void addQueryResultRow(QueryResultRow row, TreeData<QueryResultRowItem> treeData,
			LoadingCache<String, KwicProvider> kwicProviderCache) {
		if (this.queryId.equals(row.getQueryId())) {
			groupedQueryResult.add(row);

			if (rows != null) {
				rows.add(row);
			}
			
			// update existing
			treeData.getChildren(this).forEach(child -> {
				if (!child.isExpansionDummy()) {
					child.addQueryResultRow(row, treeData, kwicProviderCache);
				}
			});
			
			if (!treeData.getChildren(this).get(0).isExpansionDummy()) {
				// check for missing child row
				addChildRowItems(treeData, kwicProviderCache);
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identity == null) ? 0 : identity.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QueryIdQueryResultRowItem))
			return false;
		QueryIdQueryResultRowItem other = (QueryIdQueryResultRowItem) obj;
		if (identity == null) {
			if (other.identity != null)
				return false;
		} else if (!identity.equals(other.identity))
			return false;
		return true;
	}

	@Override
	public void removeQueryResultRow(QueryResultRow row, TreeData<QueryResultRowItem> treeData) {
		if (groupedQueryResult.remove(row)) {
			if (rows != null) {
				rows.remove(row);
			}

			//update existing rows
			new ArrayList<>(treeData.getChildren(this)).forEach(child -> {
				if (!child.isExpansionDummy()) {
					child.removeQueryResultRow(row, treeData);
					if (child.getRows().isEmpty()) {
						treeData.removeItem(child);
					}
				}
			});			
		}
	}	
	
	@Override
	public boolean startsWith(String searchValue) {
		for (QueryResultRow row : groupedQueryResult) {
			if (row instanceof TagQueryResultRow) {
				if (((TagQueryResultRow) row).getTagDefinitionPath().startsWith(searchValue)) {
					return true;
				}
			}
		}
		
		return false;
	}	
	
}
