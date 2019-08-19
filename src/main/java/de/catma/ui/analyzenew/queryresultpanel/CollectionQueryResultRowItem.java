package de.catma.ui.analyzenew.queryresultpanel;

import java.util.ArrayList;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;
import de.catma.ui.tagger.annotationpanel.AnnotatedTextProvider;
import de.catma.ui.util.Cleaner;

public class CollectionQueryResultRowItem implements QueryResultRowItem {

	private String collectionName;
	private int freq;
	private String documentId;
	private String collectionId;
	private QueryResultRowArray rows;
	private Repository project;

	public CollectionQueryResultRowItem(
			String collectionName, int freq, 
			String documentId, String collectionId,
			QueryResultRowArray rows, Repository project) {
		this.collectionName = collectionName;
		this.freq = freq;
		this.documentId = documentId;
		this.collectionId = collectionId;
		this.rows = rows;
		this.project = project;
	}

	@Override
	public String getKey() {
		return Cleaner.clean(collectionName);
	}

	@Override
	public int getFrequency() {
		return freq;
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
						
					treeData.addItem(
						this, new KwicQueryResultRowItem(
							tRow, 
							AnnotatedTextProvider.buildAnnotatedText(
									new ArrayList<>(tRow.getRanges()), 
									kwicProvider, 
									tagDefinition),
							AnnotatedTextProvider.buildAnnotatedKeywordInContext(
									new ArrayList<>(tRow.getRanges()), 
									kwicProvider, 
									tagDefinition, 
									tRow.getTagDefinitionPath())
						)
					);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace(); //TODO:
		}
	}

}
