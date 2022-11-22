package de.catma.ui.module.analyze.queryresultpanel;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;
import com.vaadin.ui.UI;

import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.ui.module.main.ErrorHandler;

public class AnnotatedQueryIdQueryResultRowItem extends QueryIdQueryResultRowItem {

	private Project project;

	public AnnotatedQueryIdQueryResultRowItem(
			String parentIdentity, GroupedQueryResult groupedQueryResult, Project project) {
		super(parentIdentity, groupedQueryResult);
		this.project = project;
	}

	@Override
	public void addChildRowItems(
		TreeData<QueryResultRowItem> treeData, LoadingCache<String, KwicProvider> kwicProviderCache) {
		try {
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
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error displaying annotated query results", e);
		}
	}

}
