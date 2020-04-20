package de.catma.ui.module.analyze.queryresultpanel;

import java.util.ArrayList;
import java.util.Set;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;
import com.vaadin.ui.UI;

import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.util.Cleaner;

public class PhraseQueryResultRowItem implements QueryResultRowItem {

	private GroupedQueryResult groupedQueryResult;
	private QueryResultRowArray rows = null;
	private final String identity;
	private boolean includeQueryId;

	public PhraseQueryResultRowItem(boolean includeQueryId, GroupedQueryResult groupedQueryResult) {
		this.includeQueryId = includeQueryId;
		this.groupedQueryResult = groupedQueryResult;
		this.identity = groupedQueryResult.getGroup().toString();
	}

	@Override
	public String getKey() {
		return Cleaner.clean(groupedQueryResult.getGroup().toString());
	}

	@Override
	public String getFilterKey() {
		return groupedQueryResult.getGroup().toString();
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
	public void addChildRowItems(TreeData<QueryResultRowItem> treeData, LoadingCache<String, KwicProvider> kwicProviderCache) {
		try {
			if (includeQueryId) {
				Set<GroupedQueryResult> groupedQueryResults = getRows().asGroupedSet(row -> {
					return row.getQueryId();
				});
				
				for (GroupedQueryResult groupedQueryResult : groupedQueryResults) {
					QueryIdQueryResultRowItem queryIdQueryResultRowItem = 
							new QueryIdQueryResultRowItem(identity, groupedQueryResult);
					if (!treeData.contains(queryIdQueryResultRowItem)) {
						treeData.addItem(this, queryIdQueryResultRowItem);
						treeData.addItem(queryIdQueryResultRowItem, new DummyQueryResultRowItem());
					}
				}			
			}
			else {
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
		}
		catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("error displaying query results", e);
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
		if (getClass() != obj.getClass())
			return false;
		PhraseQueryResultRowItem other = (PhraseQueryResultRowItem) obj;
		if (identity == null) {
			if (other.identity != null)
				return false;
		} else if (!identity.equals(other.identity))
			return false;
		return true;
	}
	
	@Override
	public void addQueryResultRow(QueryResultRow row, TreeData<QueryResultRowItem> treeData, LoadingCache<String, KwicProvider> kwicProviderCache) {
		if (groupedQueryResult.getGroup().toString().equals(row.getPhrase())) {
			groupedQueryResult.add(row);
			
			//update existing rows
			treeData.getChildren(this).forEach(child -> {
				if (!child.isExpansionDummy()) {
					child.addQueryResultRow(row, treeData, kwicProviderCache);
				}
			});
			
			if (rows != null) {
				rows.add(row);
				
				// check for missing child row
				addChildRowItems(treeData, kwicProviderCache);
			}
		}
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
		return groupedQueryResult.getGroup().toString().startsWith(searchValue);
	}
}
