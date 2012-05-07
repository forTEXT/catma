package de.catma.ui.analyzer;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;

public class PhraseResultPanel extends VerticalLayout {
	
	private static enum TreePropertyName {
		caption,
		frequency,
		;
	}
	
	private TreeTable resultTable;
	private Repository repository;

	public PhraseResultPanel(Repository repository) {
		this.repository = repository;
		initComponents();
	}

	private void initComponents() {
		resultTable = new TreeTable();
		resultTable.setSelectable(true);
		resultTable.setContainerDataSource(new HierarchicalContainer());
		resultTable.addContainerProperty(
				TreePropertyName.caption, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.caption, "Phrase");
		resultTable.addContainerProperty(
				TreePropertyName.frequency, Integer.class, null);
		resultTable.setColumnHeader(TreePropertyName.frequency, "Frequency");
		
		resultTable.setItemCaptionPropertyId(TreePropertyName.caption);
		resultTable.setPageLength(10); //TODO: config
		resultTable.setSizeFull();
		addComponent(resultTable);
	}
	
	public void setQueryResult(QueryResult queryResult) {
		resultTable.removeAllItems();
		int totalCount = 0;
		int totalFreq = 0;
		
		for (GroupedQueryResult phraseResult : 
				queryResult.asGroupedQueryResultSet()) {
			addPhraseResult(phraseResult);
			totalFreq+=phraseResult.getTotalFrequency();
			totalCount++;
		}
		
		resultTable.setFooterVisible(true);
		resultTable.setColumnFooter(
				TreePropertyName.caption, "Total count: " + totalCount);
		resultTable.setColumnFooter(
				TreePropertyName.frequency, "Total frequency: " + totalFreq);
	}

	private void addPhraseResult(GroupedQueryResult phraseResult) {
		resultTable.addItem(phraseResult.getGroup());
		resultTable.getContainerProperty(
			phraseResult.getGroup(), TreePropertyName.frequency).setValue(
					phraseResult.getTotalFrequency());

		resultTable.getContainerProperty(
			phraseResult.getGroup(), TreePropertyName.caption).setValue(
					phraseResult.getGroup());
		
		for (String sourceDocumentID : phraseResult.getSourceDocumentIDs()) {
			SourceDocument sourceDocument = 
					repository.getSourceDocument(sourceDocumentID);
			String sourceDocumentItemID = phraseResult.getGroup() + "@" + sourceDocument;
			resultTable.addItem(sourceDocumentItemID);
			resultTable.getContainerProperty(
					sourceDocumentItemID, TreePropertyName.frequency).setValue(
							phraseResult.getFrequency(sourceDocumentID));
			resultTable.getContainerProperty(
					sourceDocumentItemID, TreePropertyName.caption).setValue(
							sourceDocument.toString());
			resultTable.setParent(sourceDocumentItemID, phraseResult.getGroup());
			
			resultTable.setChildrenAllowed(sourceDocumentItemID, false);
		}
		
	}
	
}
