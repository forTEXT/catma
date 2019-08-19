package de.catma.ui.analyzenew.queryresultpanel;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;

import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.ui.tagger.annotationpanel.AnnotatedTextProvider;
import de.catma.ui.util.Cleaner;

public class DocumentQueryResultRowItem implements QueryResultRowItem {

	private String documentName;
	private int freq;
	private String documentId;
	protected GroupedQueryResult groupedQueryResult;
	private QueryResultRowArray rows;
	
	public DocumentQueryResultRowItem(
			String documentName, int freq, String documentId, GroupedQueryResult groupedQueryResult) {
		this.documentName = documentName;
		this.freq = freq;
		this.documentId = documentId;
		this.groupedQueryResult = groupedQueryResult;
	}
	
	protected String getDocumentId() {
		return documentId;
	}

	@Override
	public String getKey() {
		return Cleaner.clean(documentName);
	}

	@Override
	public int getFrequency() {
		return freq;
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
				treeData.addItem(
					this, new KwicQueryResultRowItem(
						row, 
						AnnotatedTextProvider.buildKeywordInContext(
							row.getPhrase(), row.getRange(), kwicProviderCache.get(row.getSourceDocumentId())),
						AnnotatedTextProvider.buildKeywordInContextLarge(
							row.getPhrase(), row.getRange(), kwicProviderCache.get(row.getSourceDocumentId()))));
			}
		}
		catch (Exception e) {
			e.printStackTrace(); //TODO:
		}
	}

}
