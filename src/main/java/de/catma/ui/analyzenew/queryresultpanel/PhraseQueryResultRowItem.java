package de.catma.ui.analyzenew.queryresultpanel;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;

import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.ui.util.Cleaner;

public class PhraseQueryResultRowItem implements QueryResultRowItem {

	private GroupedQueryResult groupedQueryResult;
	private QueryResultRowArray rows = null;

	public PhraseQueryResultRowItem(GroupedQueryResult groupedQueryResult) {
		this.groupedQueryResult = groupedQueryResult;
	}

	@Override
	public String getKey() {
		return Cleaner.clean(groupedQueryResult.getGroup().toString());
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
			for (String documentId : groupedQueryResult.getSourceDocumentIDs()) {
				String documentName = kwicProviderCache.get(documentId).getSourceDocumentName();
				int freq = groupedQueryResult.getFrequency(documentId);
				DocumentQueryResultRowItem item = 
						new DocumentQueryResultRowItem(
							documentName, freq, documentId, groupedQueryResult.getSubResult(documentId));
				treeData.addItem(this, item);
				treeData.addItem(item, new DummyQueryResultRowItem());
				
			}
		}
		catch (Exception e) {
			e.printStackTrace(); //TODO:
		}
	}
}
