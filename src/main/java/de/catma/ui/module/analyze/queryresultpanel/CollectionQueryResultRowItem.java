package de.catma.ui.module.analyze.queryresultpanel;

import java.util.ArrayList;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;
import com.vaadin.ui.UI;

import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;
import de.catma.ui.module.annotate.annotationpanel.AnnotatedTextProvider;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.util.Cleaner;

public class CollectionQueryResultRowItem implements QueryResultRowItem {

	private final String collectionName;
	private final String documentId;
	private final String collectionId;
	private final QueryResultRowArray rows;
	private final Project project;
	private final String identity;
	private final int contextSize;

	public CollectionQueryResultRowItem(
			String parentIdentity,
			String collectionName,
			String documentId, String collectionId,
			QueryResultRowArray rows, Project project,
			int contextSize) {
		this.collectionName = collectionName;
		this.documentId = documentId;
		this.collectionId = collectionId;
		this.rows = rows;
		this.project = project;
		this.contextSize = contextSize;
		this.identity = parentIdentity + collectionId;
	}

	@Override
	public String getKey() {
		return Cleaner.clean(collectionName);
	}
	
	@Override
	public String getFilterKey() {
		return collectionName;
	}

	@Override
	public int getFrequency() {
		return rows.getTotalFrequency();
	}

	@Override
	public QueryResultRowArray getRows() {
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
			for (QueryResultRow row : getRows()) {
				if (row instanceof TagQueryResultRow) {
					TagQueryResultRow tRow = (TagQueryResultRow)row;
					
					KwicProvider kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
					TagDefinition tagDefinition = 
						project.getTagManager().getTagLibrary().getTagDefinition(tRow.getTagDefinitionId());
					KwicQueryResultRowItem item = 
							new KwicQueryResultRowItem(
									tRow, 
									AnnotatedTextProvider.buildAnnotatedText(
											new ArrayList<>(tRow.getRanges()), 
											kwicProvider, 
											tagDefinition,
											contextSize),
									AnnotatedTextProvider.buildAnnotatedKeywordInContext(
											new ArrayList<>(tRow.getRanges()), 
											kwicProvider, 
											tagDefinition, 
											tRow.getTagDefinitionPath(),
											contextSize),
									true
								);
					if (!treeData.contains(item)) {
						treeData.addItem(this, item);
					}
				}
			}
		}
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error displaying annotated KWIC query results", e);
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
		if (!(obj instanceof CollectionQueryResultRowItem))
			return false;
		CollectionQueryResultRowItem other = (CollectionQueryResultRowItem) obj;
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
		
		if ((row instanceof TagQueryResultRow) 
				&& this.collectionId.equals(((TagQueryResultRow)row).getMarkupCollectionId())) {
			rows.add(row);

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
		if (rows.remove(row)) {
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
		for (QueryResultRow row : rows) {
			if (row instanceof TagQueryResultRow) {
				if (((TagQueryResultRow) row).getTagDefinitionPath().startsWith(searchValue)) {
					return true;
				}
			}
		}
		return false;
	}
}
