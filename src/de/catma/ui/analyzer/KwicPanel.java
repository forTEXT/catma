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
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.ui.data.util.PropertyToStringCIComparator;
import de.catma.ui.data.util.PropertyDependentItemSorter;

public class KwicPanel extends VerticalLayout {

	private enum KwicPropertyName {
		caption,
		origin,
		leftContext,
		keyword,
		rightContext,
		;
	}

	private Repository repository;
	private TreeTable kwicTable;

	public KwicPanel(Repository repository) {
		this.repository = repository;
		initComponents();
	}

	private void initComponents() {
		kwicTable = new TreeTable();
		kwicTable.setSelectable(true);
		HierarchicalContainer container = new HierarchicalContainer();
		container.setItemSorter(
				new PropertyDependentItemSorter(
						KwicPropertyName.caption, 
						new PropertyToStringCIComparator()));
		
		kwicTable.setContainerDataSource(container);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.caption, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.caption, "Phrase");
		
		kwicTable.addContainerProperty(
				KwicPropertyName.origin, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.origin, "Document/Collection");
		
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

	public void addGroupedQueryResult(GroupedQueryResult groupedQueryResult) 
			throws IOException {
		// TODO: should we put this in the  background thread?
		HashMap<String, KwicProvider> kwicProviders =
				new HashMap<String, KwicProvider>();
		
		for (QueryResultRow row : groupedQueryResult) {
			SourceDocument sourceDocument = 
					repository.getSourceDocument(row.getSourceDocumentId());
			
			if (!kwicProviders.containsKey(sourceDocument.getID())) {
				kwicProviders.put(
					sourceDocument.getID(), 
					new KwicProvider(sourceDocument));
			}
			
			KwicProvider kwicProvider = kwicProviders.get(sourceDocument.getID());
			
			KeywordInContext kwic = kwicProvider.getKwic(row.getRange(), 5);
			
			kwicTable.addItem(
				new Object[]{
					row.getPhrase(), 
					sourceDocument.toString(),
					kwic.getLeftContext(),
					kwic.getKeyword(),
					kwic.getRightContext()},
				row);
		}
	}
	
	public void removeGroupedQueryResult(GroupedQueryResult groupedQueryResult) {
		for (QueryResultRow row : groupedQueryResult) {
			kwicTable.removeItem(row);
		}
	}

	public void clear() {
		kwicTable.removeAllItems();
	}
}
