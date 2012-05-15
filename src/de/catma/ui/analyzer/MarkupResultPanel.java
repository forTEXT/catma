package de.catma.ui.analyzer;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.core.tag.TagDefinition;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;

public class MarkupResultPanel extends VerticalLayout {
	
	private static interface TreeEntrySelectionHandler {
		public QueryResultRowArray getResultRows(boolean selected);
	}
	
	private static class UmcTreeEntrySelectionHandler implements TreeEntrySelectionHandler {
		private TreeTable resultTable;
		private String umcItemID;
		
		public UmcTreeEntrySelectionHandler(TreeTable resultTable,
				String umcItemID) {
			this.resultTable = resultTable;
			this.umcItemID = umcItemID;
		}

		public QueryResultRowArray getResultRows(boolean selected) {
			@SuppressWarnings("unchecked")
			Collection<QueryResultRow> rows = 
					(Collection<QueryResultRow>)resultTable.getChildren(
							umcItemID);
			for (QueryResultRow row : rows) {
				((CheckBox)resultTable.getItem(row).getItemProperty(
						TreePropertyName.visible).getValue()).setValue(selected);
			}
			QueryResultRowArray result = new QueryResultRowArray();
			result.addAll(rows);
			return result;
		}
	}
	
	private static class TagQueryResultRowTreeEntrySelectionHandler 
		implements TreeEntrySelectionHandler {
		
		private QueryResultRow row;
		
		public TagQueryResultRowTreeEntrySelectionHandler(QueryResultRow row) {
			this.row = row;
		}

		public QueryResultRowArray getResultRows(boolean selected) {
			QueryResultRowArray result = new QueryResultRowArray();
			result.add(row);
			return result;
		}
		
	}

	private static class SourceDocumentTreeEntrySelectionHandler implements TreeEntrySelectionHandler {
		
		private TreeTable resultTable;
		private String sourceDocumentItemID;

		public SourceDocumentTreeEntrySelectionHandler(TreeTable resultTable,
				String sourceDocumentItemID) {
			this.resultTable = resultTable;
			this.sourceDocumentItemID = sourceDocumentItemID;
		}

		public QueryResultRowArray getResultRows(boolean selected) {
			@SuppressWarnings("unchecked")
			Collection<String> umcItemIDs = 
					(Collection<String>)resultTable.getChildren(
							sourceDocumentItemID);
			QueryResultRowArray result = new QueryResultRowArray();
			
			for (String umcItemID : umcItemIDs) {
				((CheckBox)resultTable.getItem(umcItemID).getItemProperty(
						TreePropertyName.visible).getValue()).setValue(selected);
				result.addAll(
					new UmcTreeEntrySelectionHandler(
							resultTable, umcItemID).getResultRows(selected));
			}
			
			return result;
		}
	}
	
	private static class TagDefinitionTreeEntrySelectionHandler implements TreeEntrySelectionHandler {
		
		private TreeTable resultTable;
		private TagDefinition tagDefinition;
		
		public TagDefinitionTreeEntrySelectionHandler(TreeTable resultTable,
				TagDefinition tagDefinition) {
			this.resultTable = resultTable;
			this.tagDefinition = tagDefinition;
		}
		
		public QueryResultRowArray getResultRows(boolean selected) {
			@SuppressWarnings("unchecked")
			Collection<String> sourceDocItemIDs = 
					(Collection<String>)resultTable.getChildren(
							tagDefinition);
			QueryResultRowArray result = new QueryResultRowArray();
			
			for (String sourceDocItemID : sourceDocItemIDs) {
				((CheckBox)resultTable.getItem(sourceDocItemID).getItemProperty(
						TreePropertyName.visible).getValue()).setValue(selected);
				result.addAll(
					new SourceDocumentTreeEntrySelectionHandler(
							resultTable, sourceDocItemID).getResultRows(selected));
			}
			
			return result;
		}
		
	}
	
	private static enum TreePropertyName {
		caption,
		frequency, 
		visible,
		;
	}
	
	private TreeTable resultTable;
	private Repository repository;
	private KwicPanel kwicPanel;

	public MarkupResultPanel(Repository repository) {
		this.repository = repository;
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
		resultTable.setColumnHeader(TreePropertyName.caption, "Tag Definition");
		resultTable.addContainerProperty(
				TreePropertyName.frequency, Integer.class, null);
		resultTable.setColumnHeader(TreePropertyName.frequency, "Frequency");
		resultTable.addContainerProperty(
				TreePropertyName.visible, AbstractComponent.class, null);
		resultTable.setColumnHeader(TreePropertyName.visible, "Visible in Kwic");
		
		resultTable.setItemCaptionPropertyId(TreePropertyName.caption);
		resultTable.setPageLength(10); //TODO: config
		resultTable.setSizeFull();
		splitPanel.addComponent(resultTable);
		
		this.kwicPanel = new KwicPanel(repository, true);
		splitPanel.addComponent(kwicPanel);
		
		addComponent(splitPanel);
	}
	
	public void setQueryResult(QueryResult queryResult) throws IOException {
		kwicPanel.clear();
		resultTable.removeAllItems();
		int totalFreq = 0;
	
		HashMap<String, UserMarkupCollection> loadedUserMarkupCollections =
				new HashMap<String, UserMarkupCollection>();
		Set<String> tagDefinitions = new HashSet<String>();
		
		for (QueryResultRow row : queryResult) {
			if (row instanceof TagQueryResultRow) {
				TagQueryResultRow tRow = (TagQueryResultRow)row;
				tagDefinitions.add(tRow.getTagDefinitionId());
				addTagQueryResultRow(tRow, loadedUserMarkupCollections);
				totalFreq++;
			}
		}
		
		resultTable.setFooterVisible(true);
		resultTable.setColumnFooter(
				TreePropertyName.caption, 
				"Total count: " + tagDefinitions.size());
		resultTable.setColumnFooter(
				TreePropertyName.frequency, "Total frequency: " + totalFreq);
		
	}
	
	private void addTagQueryResultRow(
		final TagQueryResultRow row, 
		Map<String,UserMarkupCollection> loadedUserMarkupCollections) 
				throws IOException {
		
		String tagDefinitionId = row.getTagDefinitionId();
		String markupCollectionsId = row.getMarkupCollectionId();
		SourceDocument sourceDocument = 
				repository.getSourceDocument(row.getSourceDocumentId());
		
		if (!loadedUserMarkupCollections.containsKey(markupCollectionsId)) {
			UserMarkupCollectionReference userMarkupCollRef = 
				sourceDocument.getUserMarkupCollectionReference(
						markupCollectionsId);
		
			loadedUserMarkupCollections.put(
					markupCollectionsId,
					repository.getUserMarkupCollection(userMarkupCollRef));
		}
		
		UserMarkupCollection umc = 
				loadedUserMarkupCollections.get(markupCollectionsId);
		
		TagDefinition tagDefinition = 
				umc.getTagLibrary().getTagDefinition(tagDefinitionId);
		
		if (!resultTable.containsId(tagDefinition)) {
			resultTable.addItem(
				new Object[]{
						umc.getTagLibrary().getTagPath(tagDefinition),
						0,
						createCheckbox(
							new TagDefinitionTreeEntrySelectionHandler(
								resultTable, tagDefinition))
				},
				tagDefinition);
		}
		Property tagDefFreqProperty = 
				resultTable.getItem(tagDefinition).getItemProperty(
						TreePropertyName.frequency);
		tagDefFreqProperty.setValue(((Integer)tagDefFreqProperty.getValue())+1);
		
		final String sourceDocumentItemID = tagDefinitionId+ "@" + sourceDocument;
		
		if (!resultTable.containsId(sourceDocumentItemID)) {
		
			resultTable.addItem(
				new Object[] {
						sourceDocument.toString(),
						0,
						createCheckbox(
							new SourceDocumentTreeEntrySelectionHandler(
									resultTable, sourceDocumentItemID))
				}, sourceDocumentItemID);
			resultTable.setParent(sourceDocumentItemID, tagDefinition);
		}
		
		Property sourceDocFreqProperty = 
				resultTable.getItem(sourceDocumentItemID).getItemProperty(
						TreePropertyName.frequency);
		sourceDocFreqProperty.setValue(
				((Integer)sourceDocFreqProperty.getValue())+1);
		
		final String umcItemID = sourceDocumentItemID + "@" + umc.getId();
		
		if (!resultTable.containsId(umcItemID)) {
			resultTable.addItem(
				new Object[] {
					umc.getName(),
					0,
					createCheckbox(
						new UmcTreeEntrySelectionHandler(resultTable, umcItemID))
				}, umcItemID);
			resultTable.setParent(umcItemID, sourceDocumentItemID);
		}
		
		Property userMarkupCollFreqProperty = 
				resultTable.getItem(umcItemID).getItemProperty(
						TreePropertyName.frequency);
		userMarkupCollFreqProperty.setValue(
				((Integer)userMarkupCollFreqProperty.getValue())+1);
		
		resultTable.addItem(
			new Object[] {
				row.getPhrase(),
				1,
				createCheckbox(
						new TagQueryResultRowTreeEntrySelectionHandler(row))
			}, row);
		
		resultTable.setParent(row, umcItemID);
		resultTable.setChildrenAllowed(row, false);
		
	}

	private CheckBox createCheckbox(
			final TreeEntrySelectionHandler treeEntrySelectionHandler) {
		CheckBox cbShowInKwicView = new CheckBox();
		cbShowInKwicView.setImmediate(true);
		cbShowInKwicView.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				boolean selected = 
						event.getButton().booleanValue();

				fireShowInKwicViewSelected(
					treeEntrySelectionHandler, selected);
			}


		});
		return cbShowInKwicView;
	}

	private void fireShowInKwicViewSelected(
			TreeEntrySelectionHandler treeEntrySelectionHandler,
			boolean selected) {

		QueryResultRowArray queryResult = new QueryResultRowArray();
		
		queryResult.addAll(treeEntrySelectionHandler.getResultRows(selected));
		
		if (selected) {
			try {
				kwicPanel.addQueryResultRows(queryResult);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			kwicPanel.removeQueryResultRows(queryResult);
		}
		
	}
}
