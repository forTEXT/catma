package de.catma.ui.analyzer;

import java.io.IOException;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;

public class PhraseResultPanel extends VerticalLayout {
	
	static interface VisualizeGroupedQueryResultSelectionListener {
		public void setSelected(
				GroupedQueryResultSet groupedQueryResultSet, boolean selected);
	}
	
	private static enum TreePropertyName {
		caption,
		frequency, 
		visibleInKwic,
		visibleInVisualizations,
		;
	}
	
	private TreeTable resultTable;
	private Repository repository;
	private KwicPanel kwicPanel;
	private VisualizeGroupedQueryResultSelectionListener resultSelectionListener;

	public PhraseResultPanel(
			Repository repository, 
			VisualizeGroupedQueryResultSelectionListener resultSelectionListener) {
		this.repository = repository;
		this.resultSelectionListener = resultSelectionListener;
		initComponents();
	}

	private void initComponents() {
		setSizeFull();
		
		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		splitPanel.setSizeFull();
		
		resultTable = new TreeTable();
		resultTable.setSelectable(true);
		HierarchicalContainer container = new HierarchicalContainer();
		container.setItemSorter(
				new PropertyDependentItemSorter(
						TreePropertyName.caption, 
						new PropertyToTrimmedStringCIComparator()));
		
		resultTable.setContainerDataSource(container);

		resultTable.addContainerProperty(
				TreePropertyName.caption, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.caption, "Phrase");
		resultTable.addContainerProperty(
				TreePropertyName.frequency, Integer.class, null);
		resultTable.setColumnHeader(TreePropertyName.frequency, "Frequency");
		resultTable.addContainerProperty(
				TreePropertyName.visibleInKwic, AbstractComponent.class, null);
		resultTable.setColumnHeader(TreePropertyName.visibleInKwic, "Visible in Kwic");
		resultTable.addContainerProperty(
				TreePropertyName.visibleInVisualizations, AbstractComponent.class,
				null);
		resultTable.setColumnHeader(
				TreePropertyName.visibleInVisualizations, "Visualize");
		
		resultTable.setItemCaptionPropertyId(TreePropertyName.caption);
		resultTable.setPageLength(10); //TODO: config
		resultTable.setSizeFull();
		splitPanel.addComponent(resultTable);
		
		
		this.kwicPanel = new KwicPanel(repository);
		splitPanel.addComponent(kwicPanel);
		
		addComponent(splitPanel);
	}
	
	public void setQueryResult(QueryResult queryResult) {
		kwicPanel.clear();
		resultTable.removeAllItems();
		int totalCount = 0;
		int totalFreq = 0;
		
		for (GroupedQueryResult phraseResult : 
				queryResult.asGroupedSet()) {
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
		resultTable.addItem(new Object[]{
				phraseResult.getGroup(), 
				phraseResult.getTotalFrequency(),
				createKwicCheckbox(phraseResult),
				createVisualizeCheckbox(phraseResult)},
				phraseResult.getGroup());

		resultTable.getContainerProperty(
			phraseResult.getGroup(), TreePropertyName.caption).setValue(
					phraseResult.getGroup());
		
		for (String sourceDocumentID : phraseResult.getSourceDocumentIDs()) {
			SourceDocument sourceDocument = 
					repository.getSourceDocument(sourceDocumentID);
			String sourceDocumentItemID = 
					phraseResult.getGroup() + "@" + sourceDocument;
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

	private CheckBox createKwicCheckbox(final GroupedQueryResult phraseResult) {
		CheckBox cbShowInKwicView = new CheckBox();
		cbShowInKwicView.setImmediate(true);
		cbShowInKwicView.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				boolean selected = 
						event.getButton().booleanValue();

				fireShowInKwicViewSelected(phraseResult, selected);
			}


		});
		return cbShowInKwicView;
	}

	private CheckBox createVisualizeCheckbox(final GroupedQueryResult phraseResult) {
		CheckBox cbShowInKwicView = new CheckBox();
		cbShowInKwicView.setImmediate(true);
		cbShowInKwicView.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				boolean selected = 
						event.getButton().booleanValue();

				GroupedQueryResultSet set = new GroupedQueryResultSet();
				set.add(phraseResult);
				resultSelectionListener.setSelected(set, selected);
			}


		});
		return cbShowInKwicView;
	}
	
	private void fireShowInKwicViewSelected(GroupedQueryResult phraseResult,
			boolean selected) {

		if (selected) {
			try {
				kwicPanel.addQueryResultRows(phraseResult);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			kwicPanel.removeQueryResultRows(phraseResult);
		}
		
	}
	
}
