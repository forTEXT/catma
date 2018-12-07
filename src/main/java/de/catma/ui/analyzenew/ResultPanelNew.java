package de.catma.ui.analyzenew;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.vaadin.data.TreeData;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.shared.ui.label.ContentMode;
import com.vaadin.v7.ui.Table;

import de.catma.queryengine.parser.CatmaQueryParser.phrase_return;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.PhraseResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.tagger.annotationpanel.TagDataItem;
import de.catma.ui.tagger.annotationpanel.TagTreeItem;
import de.catma.ui.tagger.annotationpanel.TagsetDataItem;

public class ResultPanelNew extends Panel {

	private static enum TreePropertyName {
		caption, frequency, visibleInKwic,;
	}

	private VerticalLayout contentVerticalLayout;
	private Table queryResultTable;
	private TreeGrid<GroupedQueryResult> phraseTreeGrid;
	private Grid<QueryResultRow> queryResultGrid;
	private TextArea textArea;
	private Label queryInfo;
	private HorizontalLayout groupedIcons;
	private Button caretDownBt;
	private Button caretUpBt;
	private Button trashBt;
	private Button optionsBt;

	private QueryResult queryResult;
	private String queryAsString;

	public ResultPanelNew(QueryResult result, String queryAsString) {
		this.queryResult = result;
		this.queryAsString = queryAsString;

		initComponents();
		initListeners();
		// setData();
		setDataPhraseStyle();

	}

	private void initComponents() {
		contentVerticalLayout = new VerticalLayout();
		setContent(contentVerticalLayout);
		createResultInfoBar();
		createButtonBar();
	}

	private void initListeners() {
		caretDownBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.addComponent(phraseTreeGrid);
				groupedIcons.replaceComponent(caretDownBt, caretUpBt);

			}
		});

		caretUpBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.removeComponent(phraseTreeGrid);
				groupedIcons.replaceComponent(caretUpBt, caretDownBt);

			}
		});
	}

	private void setData() {
		QueryResultRowArray resultRowArrayArrayList = queryResult.asQueryResultRowArray();
		queryResultTable = new Table("Results");
		queryResultTable.addContainerProperty("Phrase", String.class, null);
		queryResultTable.addContainerProperty("Range", String.class, null);

		for (QueryResultRow queryResultRow : resultRowArrayArrayList) {
			Object newItemId = queryResultTable.addItem();
			Item row1 = queryResultTable.getItem(newItemId);

			row1.getItemProperty("Phrase").setValue(queryResultRow.getPhrase());
			row1.getItemProperty("Range").setValue(queryResultRow.getRange().toString());
		}
		queryResultTable.setWidth("100%");

	}

	private void setDataPhraseStyle() {

		Set<GroupedQueryResult> groupedQueryResults = queryResult.asGroupedSet();
		TreeData<GroupedQueryResult> data = new TreeData<>();
		phraseTreeGrid = new TreeGrid<>();
		data.addItems(null, groupedQueryResults);

		for (GroupedQueryResult groupedQueryResult : groupedQueryResults) {

			Set<String> sourceDocs = groupedQueryResult.getSourceDocumentIDs();
			// Iterator<QueryResultRow> rowIterator=	groupedQueryResult.iterator();

			Iterator<String>docIterator = sourceDocs.iterator();
			//int docsize = sourceDocs.size();

		
				
				while(docIterator.hasNext()) {
					String surceDocID= docIterator.next().toString();
					GroupedQueryResult subResults=	groupedQueryResult.getSubResult(surceDocID);
					 Iterator<QueryResultRow> rowIterator=subResults.iterator();
					 while(rowIterator.hasNext()){
						 data.addItem(groupedQueryResult, (GroupedQueryResult) rowIterator.next());
						 
					 }
					
					
				}
			
			
			phraseTreeGrid.setDetailsVisible(groupedQueryResult, true);
		}

		TreeDataProvider<GroupedQueryResult> dataProvider = new TreeDataProvider<>(data);
		phraseTreeGrid.addColumn(GroupedQueryResult::getGroup).setCaption("Phrase");
		phraseTreeGrid.addColumn(GroupedQueryResult::getSourceDocumentIDs).setCaption("SourceDocs");
		phraseTreeGrid.addColumn((GroupedQueryResult::getSubResult)).setCaption("Subresults");
		//phraseTreeGrid.addColumn((GroupedQueryResult::getFrequency)).setCaption("Subresults");
		//phraseTreeGrid.addColumn(getCheckbox).setCaption("select")

		phraseTreeGrid.setDataProvider(dataProvider);
		phraseTreeGrid.setWidth("100%");

		/*
		 * for (GroupedQueryResult phraseResult : groupedQueryResults) {
		 * addPhraseResult(phraseResult, container);
		 * totalFreq+=phraseResult.getTotalFrequency(); totalCount++; }
		 * 
		 * resultTable.setContainerDataSource(container);
		 * 
		 * '''''''''''''''''''''''''''''''''''''''''''''''''''' private void initData()
		 * { try { Collection<TagsetDefinition> tagsets = project.getTagsets();
		 * TreeData<TagTreeItem> tagsetData = new TreeData<>();
		 * 
		 * tagsetData.addRootItems(tagsets.stream().map(ts -> new TagsetDataItem(ts)));
		 * 
		 * for (TagsetDefinition tagsetDefinition : tagsets) { for (TagDefinition
		 * tagDefinition : tagsetDefinition) { if
		 * (tagDefinition.getParentUuid().isEmpty()) { tagsetData.addItem(new
		 * TagsetDataItem(tagsetDefinition), new TagDataItem(tagDefinition));
		 * addTagDefinitionSubTree(tagsetDefinition, tagDefinition, tagsetData); } } }
		 * 
		 * tagsetsGrid.setDataProvider(new TreeDataProvider<>(tagsetData)); for
		 * (TagsetDefinition tagset : tagsets) { for (TagDefinition tag : tagset) {
		 * TagDataItem item = new TagDataItem(tag); tagsetsGrid.expand(item); if
		 * (!tag.getUserDefinedPropertyDefinitions().isEmpty()) {
		 * tagsetsGrid.setDetailsVisible(item, true); } } } } catch (Exception e) {
		 * e.printStackTrace(); } }
		 */
	}
	private CheckBox createCheckbox(final GroupedQueryResult phraseResult) {
		final CheckBox checkBox = new CheckBox();
		//checkBox.setValue(false);

		checkBox.addValueChangeListener(event ->{
	  boolean checked=  checkBox.getValue();
	  Notification.show("Is checked",
              "is : "+ checked,
              Notification.Type.HUMANIZED_MESSAGE);
			
		});
			
	 

				//synchronizeWithSelectedView(phraseResult, selected);
	
		return checkBox;
	}

	private void createResultInfoBar() {
		QueryResultRowArray resultRowArrayArrayList = queryResult.asQueryResultRowArray();
		int resultSize = resultRowArrayArrayList.size();
		queryInfo = new Label(queryAsString + "(" + resultSize + ")");
		queryInfo.setStyleName("body");
		contentVerticalLayout.addComponent(queryInfo);
	}

	private void createButtonBar() {
		groupedIcons = new HorizontalLayout();
		groupedIcons.setMargin(false);

		caretDownBt = new Button(VaadinIcons.CARET_DOWN);
		caretDownBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		caretUpBt = new Button(VaadinIcons.CARET_UP);
		caretUpBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		optionsBt = new Button(VaadinIcons.ELLIPSIS_V);
		optionsBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		trashBt = new Button(VaadinIcons.TRASH);
		trashBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);

		groupedIcons.addComponents(trashBt, optionsBt, caretDownBt);
		groupedIcons.setWidthUndefined();

		contentVerticalLayout.addComponent(groupedIcons);
		contentVerticalLayout.setComponentAlignment(groupedIcons, Alignment.MIDDLE_RIGHT);
	}

}
