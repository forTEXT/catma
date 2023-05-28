package de.catma.ui.module.analyze.queryresultpanel;

import java.util.ArrayList;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;
import com.vaadin.ui.UI;

import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.ui.module.annotate.annotationpanel.AnnotatedTextProvider;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.util.Cleaner;

public class DocumentQueryResultRowItem implements QueryResultRowItem {

	protected final String identity;
	private String documentName;
	private String documentId;
	protected GroupedQueryResult groupedQueryResult;
	private QueryResultRowArray rows;
	
	public DocumentQueryResultRowItem(
			String parentKey, String documentName, String documentId, GroupedQueryResult groupedQueryResult) {
		this.documentName = documentName;
		this.documentId = documentId;
		this.groupedQueryResult = groupedQueryResult;
		this.identity = parentKey + documentId;
		
	}
	
	protected String getDocumentId() {
		return documentId;
	}

	@Override
	public String getKey() {
		return Cleaner.clean(documentName);
	}

	@Override
	public String getFilterKey() {
		return documentName;
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
			for (QueryResultRow row : getRows()) {
				KwicQueryResultRowItem item = new KwicQueryResultRowItem(
						row, 
						AnnotatedTextProvider.buildKeywordInContext(
							row.getPhrase(), row.getRange(), kwicProviderCache.get(row.getSourceDocumentId())),
						AnnotatedTextProvider.buildKeywordInContextLarge(
							row.getPhrase(), row.getRange(), kwicProviderCache.get(row.getSourceDocumentId())),
						false);
				if (!treeData.contains(item)) {
					treeData.addItem(this, item);
				}
			}
		}
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError(
				"Error displaying annotated KWIC query results", e);
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
		if (!(obj instanceof DocumentQueryResultRowItem))
			return false;
		DocumentQueryResultRowItem other = (DocumentQueryResultRowItem) obj;
		if (identity == null) {
			if (other.identity != null)
				return false;
		} else if (!identity.equals(other.identity))
			return false;
		return true;
	}
	
	@Override
	public void addQueryResultRow(
			QueryResultRow row, TreeData<QueryResultRowItem> treeData, 
			LoadingCache<String, KwicProvider> kwicProviderCache) {
		
		if (this.documentId.equals(row.getSourceDocumentId())) {
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
			if (row.getPhrase().startsWith(searchValue)) {
				return true;
			}
		}
		return false;
	}
}
