package de.catma.ui.analyzer;

import java.io.IOException;
import java.util.HashMap;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.Table;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.KeywordInContext;
import de.catma.core.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToReversedTrimmedStringCIComparator;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;

public class KwicPanel extends VerticalLayout {

	private enum KwicPropertyName {
		caption,
		leftContext,
		keyword,
		rightContext,
		;
	}

	private Repository repository;
	private TreeTable kwicTable;
	private boolean markupBased;

	public KwicPanel(Repository repository) {
		this(repository, false);
	}
	
	public KwicPanel(Repository repository, boolean markupBased) {
		this.repository = repository;
		this.markupBased = markupBased;
		initComponents();
	}

	private void initComponents() {
		kwicTable = new TreeTable();
		kwicTable.setSelectable(true);
		
		HierarchicalContainer container = new HierarchicalContainer();
		PropertyDependentItemSorter itemSorter = 
				new PropertyDependentItemSorter(
						new Object[] {
								KwicPropertyName.rightContext,
								KwicPropertyName.keyword
						},
						new PropertyToTrimmedStringCIComparator());
		//TODO: nonsense:
		itemSorter.setPropertyComparator(
			KwicPropertyName.leftContext, 
			new PropertyToReversedTrimmedStringCIComparator());
		
		container.setItemSorter(itemSorter);
		
		kwicTable.setContainerDataSource(container);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.caption, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.caption, "Document/Collection");
		
		kwicTable.addContainerProperty(
				KwicPropertyName.leftContext, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.leftContext, "Left Context");
		kwicTable.setColumnAlignment(KwicPropertyName.leftContext, Table.ALIGN_RIGHT);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.keyword, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.keyword, "Keyword");
		kwicTable.setColumnAlignment(KwicPropertyName.keyword, Table.ALIGN_CENTER);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.rightContext, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.rightContext, "Right Context");	
		kwicTable.setColumnAlignment(KwicPropertyName.rightContext, Table.ALIGN_LEFT);
		
		kwicTable.setPageLength(12); //TODO: config
		kwicTable.setSizeFull();
		addComponent(kwicTable);
	}

	public void addQueryResultRows(Iterable<QueryResultRow> queryResult) 
			throws IOException {
		// TODO: should we put this in the  background thread?
		HashMap<String, KwicProvider> kwicProviders =
				new HashMap<String, KwicProvider>();
		
		for (QueryResultRow row : queryResult) {
			SourceDocument sourceDocument = 
					repository.getSourceDocument(row.getSourceDocumentId());
			
			if (!kwicProviders.containsKey(sourceDocument.getID())) {
				kwicProviders.put(
					sourceDocument.getID(), 
					new KwicProvider(sourceDocument));
			}
			
			KwicProvider kwicProvider = kwicProviders.get(sourceDocument.getID());
			KeywordInContext kwic = kwicProvider.getKwic(row.getRange(), 5);
			String sourceDocOrMarkupCollectionDisplay = 
					sourceDocument.toString();
			
			if (markupBased && (row instanceof TagQueryResultRow)) {
				sourceDocOrMarkupCollectionDisplay =
					sourceDocument.getUserMarkupCollectionReference(
						((TagQueryResultRow)row).getMarkupCollectionId()).getName();
			}
			
			kwicTable.addItem(
				new Object[]{
					sourceDocOrMarkupCollectionDisplay,
					kwic.getLeftContext(),
					kwic.getKeyword(),
					kwic.getRightContext()},
					row);
			kwicTable.setChildrenAllowed(row, false);
		}
	}
	
	public void removeQueryResultRows(Iterable<QueryResultRow> queryResult) {
		for (QueryResultRow row : queryResult) {
			kwicTable.removeItem(row);
		}
	}

	public void clear() {
		kwicTable.removeAllItems();
	}
}
