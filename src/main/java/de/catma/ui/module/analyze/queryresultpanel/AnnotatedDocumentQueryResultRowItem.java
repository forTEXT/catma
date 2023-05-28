package de.catma.ui.module.analyze.queryresultpanel;

import java.util.HashMap;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;
import com.vaadin.ui.UI;

import de.catma.document.source.SourceDocumentReference;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.module.main.ErrorHandler;

public class AnnotatedDocumentQueryResultRowItem extends DocumentQueryResultRowItem {

	private Project project;

	public AnnotatedDocumentQueryResultRowItem(String parentIdentity, String documentName, String documentId,
			GroupedQueryResult groupedQueryResult, Project project) {
		super(parentIdentity, documentName, documentId, groupedQueryResult);
		this.project = project;
	}

	@Override
	public void addChildRowItems(TreeData<QueryResultRowItem> treeData,
			LoadingCache<String, KwicProvider> kwicProviderCache) {
		try {
			HashMap<String, QueryResultRowArray> rowsByCollectionId = new HashMap<String, QueryResultRowArray>();
	
			for (QueryResultRow row : groupedQueryResult) {
				
				if (row instanceof TagQueryResultRow) {
					TagQueryResultRow tRow = (TagQueryResultRow) row;
		
					String collectionId = tRow.getMarkupCollectionId();
					
					QueryResultRowArray rows = null;
					if (!rowsByCollectionId.containsKey(collectionId)) {
						rows = new QueryResultRowArray();
						rowsByCollectionId.put(collectionId, rows);
						
					} else {
						rows = rowsByCollectionId.get(collectionId);
					}
					
					rows.add(row);
				}
			}
			
			for (String collectionId : rowsByCollectionId.keySet()) {
				SourceDocumentReference documentRef = kwicProviderCache.get(getDocumentId()).getSourceDocumentReference();
				String collectionName = documentRef.getUserMarkupCollectionReference(collectionId).getName();
				QueryResultRowArray rows = rowsByCollectionId.get(collectionId);
				CollectionQueryResultRowItem item = 
					new CollectionQueryResultRowItem(
						identity,
						collectionName,
						getDocumentId(), collectionId, 
						rows, project);
				
				if (!treeData.contains(item)) {
					treeData.addItem(this, item);
					treeData.addItem(item, new DummyQueryResultRowItem());
				}
			}
		}
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error displaying annotated query results", e);
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
