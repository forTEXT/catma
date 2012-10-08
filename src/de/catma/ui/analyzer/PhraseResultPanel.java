package de.catma.ui.analyzer;

import java.io.IOException;
import java.util.Set;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ClassResource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.Notification;

import de.catma.CatmaApplication;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.result.AccumulativeGroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;

public class PhraseResultPanel extends VerticalLayout {
	
	private static enum TreePropertyName {
		caption,
		frequency, 
		visibleInKwic,
		;
	}
	
	private TreeTable resultTable;
	private Repository repository;
	private KwicPanel kwicPanel;
	private GroupedQueryResultSelectionListener resultSelectionListener;
	private Button bDist;
	private boolean init = false;
	private RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider;

	public PhraseResultPanel(
			Repository repository, 
			GroupedQueryResultSelectionListener resultSelectionListener, 
			RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider) {
		this.repository = repository;
		this.resultSelectionListener = resultSelectionListener;
		this.relevantUserMarkupCollectionProvider = relevantUserMarkupCollectionProvider;
	}
	
	@Override
	public void attach() {
		super.attach();
		if (!init) {
			initComponents();
			initActions();
			init = true;
		}
	}

	private void initActions() {
		bDist.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				GroupedQueryResultSet set = new GroupedQueryResultSet();

				Set<?> selection = (Set<?>) resultTable.getValue();
				
				if (selection.size() > 1) {
					AccumulativeGroupedQueryResult accResult =
							new AccumulativeGroupedQueryResult();
					
					for (Object value : selection) {
						accResult.addGroupedQueryResult(extractGroupedQueryResult(value));
					}
					
					set.add(accResult);
				}
				else if (selection.size() == 1) {
					Object value = selection.iterator().next();
					set.add(extractGroupedQueryResult(value));
				}
				
				if (selection.size() > 0) {
					resultSelectionListener.resultsSelected(set);
				}
				else {
					getWindow().showNotification(
							"Information", "Please select one or more result rows!",
							Notification.TYPE_TRAY_NOTIFICATION);
				}
			}


		});
	}

	private GroupedQueryResult extractGroupedQueryResult(Object value) {
		if (value instanceof SourceDocumentItemID) {
			GroupedQueryResult parentResult = (GroupedQueryResult)resultTable.getParent(value);
			return parentResult.getSubResult(
					((SourceDocumentItemID)value).getSourceDocumentID());
		}
		else {
			return (GroupedQueryResult)value;
		}
	}

	private void initComponents() {
		setSizeFull();
		
		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		splitPanel.setSizeFull();
		VerticalLayout leftComponent = new VerticalLayout();
		leftComponent.setSpacing(true);
		leftComponent.setSizeFull();
		
		resultTable = new TreeTable();
		resultTable.setSelectable(true);
		resultTable.setMultiSelect(true);
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
		
		resultTable.setItemCaptionPropertyId(TreePropertyName.caption);
		resultTable.setPageLength(10); //TODO: config
		resultTable.setSizeFull();
		
		leftComponent.addComponent(resultTable);
		leftComponent.setExpandRatio(resultTable, 1.0f);
		
		bDist = new Button();
		bDist.setIcon(new ClassResource(
				"ui/analyzer/resources/chart.gif", 
				getApplication()));
		leftComponent.addComponent(bDist);
		
		splitPanel.addComponent(leftComponent);
		
		this.kwicPanel = new KwicPanel(repository, relevantUserMarkupCollectionProvider);
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
		resultTable.addItem( 
				new Object[] {
					phraseResult.getGroup(), 
					phraseResult.getTotalFrequency(),
					createKwicCheckbox(phraseResult) 
				},
				phraseResult);

		resultTable.getContainerProperty(
			phraseResult, TreePropertyName.caption).setValue(
					phraseResult.getGroup());

		for (String sourceDocumentID : phraseResult.getSourceDocumentIDs()) {
			SourceDocument sourceDocument = 
					repository.getSourceDocument(sourceDocumentID);
			SourceDocumentItemID sourceDocumentItemID = 
					new SourceDocumentItemID(
							phraseResult.getGroup() 
								+ "@" + sourceDocument, 
							sourceDocumentID);
			
			resultTable.addItem(sourceDocumentItemID);
			resultTable.getContainerProperty(
					sourceDocumentItemID, TreePropertyName.frequency).setValue(
							phraseResult.getFrequency(sourceDocumentID));
			resultTable.getContainerProperty(
					sourceDocumentItemID, TreePropertyName.caption).setValue(
							sourceDocument.toString());
			resultTable.setParent(sourceDocumentItemID, phraseResult);
			
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
	
	private void fireShowInKwicViewSelected(GroupedQueryResult phraseResult,
			boolean selected) {

		if (selected) {
			try {
				kwicPanel.addQueryResultRows(phraseResult);
			} catch (IOException e) {
				((CatmaApplication)getApplication()).showAndLogError(
					"Error showing KWIC results!", e);
			}
		}
		else {
			kwicPanel.removeQueryResultRows(phraseResult);
		}
		
	}
	
}
