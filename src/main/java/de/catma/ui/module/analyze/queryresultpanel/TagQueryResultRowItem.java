package de.catma.ui.module.analyze.queryresultpanel;

import java.util.ArrayList;
import java.util.Set;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;
import com.vaadin.ui.UI;

import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.util.Cleaner;

public class TagQueryResultRowItem implements QueryResultRowItem {

	private final String identity;
	private GroupedQueryResult groupedQueryResult;
	private QueryResultRowArray rows;
	private Project project;
	private boolean includeQueryId;

	public TagQueryResultRowItem(boolean includeQueryId, GroupedQueryResult groupedQueryResult, Project project) {
		this.includeQueryId = includeQueryId;
		this.groupedQueryResult = groupedQueryResult;
		this.project = project;
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
					AnnotatedQueryIdQueryResultRowItem queryIdQueryResultRowItem = 
							new AnnotatedQueryIdQueryResultRowItem(identity, groupedQueryResult, project);
					if (!treeData.contains(queryIdQueryResultRowItem)) {
						treeData.addItem(this, queryIdQueryResultRowItem);
						treeData.addItem(queryIdQueryResultRowItem, new DummyQueryResultRowItem());
					}
				}			
			}
			else {
				for (String documentId : groupedQueryResult.getSourceDocumentIDs()) {
				
					String documentName = kwicProviderCache.get(documentId).getSourceDocumentName();
					AnnotatedDocumentQueryResultRowItem item = new AnnotatedDocumentQueryResultRowItem(
							identity,
							documentName, documentId, 
							groupedQueryResult.getSubResult(documentId), 
							project);
					if (!treeData.contains(item)) {
						treeData.addItem(this, item);
						treeData.addItem(item, new DummyQueryResultRowItem());
					}
				}
			}
		}
		catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("error displaying annotated query results", e);
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
		if (!(obj instanceof TagQueryResultRowItem))
			return false;
		TagQueryResultRowItem other = (TagQueryResultRowItem) obj;
		if (identity == null) {
			if (other.identity != null)
				return false;
		} else if (!identity.equals(other.identity))
			return false;
		return true;
	}
	
	@Override
	public void addQueryResultRow(QueryResultRow row, TreeData<QueryResultRowItem> treeData,
			LoadingCache<String, KwicProvider> kwicProviderCache) {
		
		if (row instanceof TagQueryResultRow) {
			if (groupedQueryResult.getGroup().toString().equals(
						((TagQueryResultRow)row).getTagDefinitionPath())) {
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
		else if (groupedQueryResult.getGroup().toString().equals(getNoTagAvailableKey())) {
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

	public static String getNoTagAvailableKey() {
		return "no Tag available / not annotated";
	}
}
