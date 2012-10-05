package de.catma.ui.analyzer;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ClassResource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Window.Notification;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import de.catma.CatmaApplication;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.queryengine.result.AccumulativeGroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;

public class MarkupResultPanel extends VerticalLayout {
	
	private static class RowWrapper {
		TagQueryResultRow row;

		public RowWrapper(TagQueryResultRow row) {
			this.row = row;
		}
		
	}
	
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
			QueryResultRowArray result = new QueryResultRowArray();
			@SuppressWarnings("unchecked")
			Collection<RowWrapper> rows = 
					(Collection<RowWrapper>)resultTable.getChildren(
							umcItemID);
			if (rows != null) {
				for (RowWrapper wrapper : rows) {
					((CheckBox)resultTable.getItem(wrapper).getItemProperty(
							TreePropertyName.visible).getValue()).setValue(selected);
					result.add(wrapper.row);
				}
			}
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
		private String tagDefinitionItemID;
		
		public TagDefinitionTreeEntrySelectionHandler(TreeTable resultTable,
				String tagDefinitionItemID) {
			this.resultTable = resultTable;
			this.tagDefinitionItemID = tagDefinitionItemID;
		}
		
		public QueryResultRowArray getResultRows(boolean selected) {
			@SuppressWarnings("unchecked")
			Collection<String> sourceDocItemIDs = 
					(Collection<String>)resultTable.getChildren(
							tagDefinitionItemID);
			QueryResultRowArray result = new QueryResultRowArray();
			
			if (sourceDocItemIDs != null) {
				for (String sourceDocItemID : sourceDocItemIDs) {
					((CheckBox)resultTable.getItem(sourceDocItemID).getItemProperty(
							TreePropertyName.visible).getValue()).setValue(selected);
					result.addAll(
						new SourceDocumentTreeEntrySelectionHandler(
								resultTable, sourceDocItemID).getResultRows(selected));
				}
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
	private Button bDist;
	private boolean init = false;
	private GroupedQueryResultSelectionListener resultSelectionListener;
	private RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider;

	public MarkupResultPanel(
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
			
			@SuppressWarnings("unchecked")
			public void buttonClick(ClickEvent event) {
				GroupedQueryResultSet set = new GroupedQueryResultSet();
				
				Set<GroupedQueryResult> selection = new HashSet<GroupedQueryResult>();
				
				selection.addAll(
						getSelectionAsGroupedQueryResults(
								(Set<Object>)resultTable.getValue()));
				
				if (selection.size() > 1) {
					AccumulativeGroupedQueryResult accResult =
							new AccumulativeGroupedQueryResult(selection);
					
					set.add(accResult);
				}
				else if (selection.size() == 1) {
					set.add(selection.iterator().next());
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
	
	private Collection<TagQueryResult> getSelectionAsGroupedQueryResults(
			Set<Object> selection) {
		
		Set<TagQueryResultRow> rows = new HashSet<TagQueryResultRow>();
		for (Object selValue : selection) {
			rows.addAll(getTagQueryResultRows(selValue));
		}
		
		Map<String, TagQueryResult> tagQueryResultsByTagDefPath = 
				new HashMap<String, TagQueryResult>();
		
		HashMap<String, UserMarkupCollection> umcCache = new HashMap<String, UserMarkupCollection>();
		
		for(TagQueryResultRow row : rows) {
			try {
				if (!umcCache.containsKey(row.getMarkupCollectionId())) {
					SourceDocument sd = repository.getSourceDocument(row.getSourceDocumentId());
					UserMarkupCollectionReference umcRef = 
							sd.getUserMarkupCollectionReference(row.getMarkupCollectionId());
					UserMarkupCollection umc = repository.getUserMarkupCollection(umcRef);
					umcCache.put(umc.getId(), umc);
				}
				UserMarkupCollection umc = umcCache.get(row.getMarkupCollectionId());
				TagDefinition td = umc.getTagLibrary().getTagDefinition(row.getTagDefinitionId());
				String tagPath = umc.getTagLibrary().getTagPath(td);
	
				if (!tagQueryResultsByTagDefPath.containsKey(tagPath)) {
					tagQueryResultsByTagDefPath.put(tagPath, new TagQueryResult(tagPath));
				}
				
				TagQueryResult tagQueryResult = tagQueryResultsByTagDefPath.get(tagPath);
				tagQueryResult.addTagQueryResultRow(row);
			}
			catch (IOException ioe) {
				((CatmaApplication)getApplication()).showAndLogError(
						"Error preparing markup results!",
						ioe);
			}
		}
		
		
		return tagQueryResultsByTagDefPath.values();
	}

	private Collection<? extends TagQueryResultRow> getTagQueryResultRows(Object selValue) {
		HashSet<TagQueryResultRow> result = new HashSet<TagQueryResultRow>();
		if (resultTable.hasChildren(selValue)) {
			for (Object child : resultTable.getChildren(selValue)) {
				if (child instanceof RowWrapper) {
					result.add(((RowWrapper)child).row);
				}
				else {
					result.addAll(getTagQueryResultRows(child));
				}
			}
		}
		return result;
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
		//TODO: a description generator that shows the version of a Tag
//		resultTable.setItemDescriptionGenerator(generator);
		
		leftComponent.addComponent(resultTable);
		leftComponent.setExpandRatio(resultTable, 1.0f);
		
		
		bDist = new Button();
		bDist.setIcon(new ClassResource(
				"ui/analyzer/resources/chart.gif", 
				getApplication()));
		leftComponent.addComponent(bDist);
		
		splitPanel.addComponent(leftComponent);
		
		this.kwicPanel = 
				new KwicPanel(
					repository, relevantUserMarkupCollectionProvider,  true);
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
		String tagDefinitionItemID = 
				tagDefinition.getUuid() + "#" + tagDefinition.getVersion();
		if (!resultTable.containsId(tagDefinitionItemID)) {
			resultTable.addItem(
				new Object[]{
						umc.getTagLibrary().getTagPath(tagDefinition),
						0,
						createCheckbox(
							new TagDefinitionTreeEntrySelectionHandler(
								resultTable, tagDefinitionItemID))
				},
				tagDefinitionItemID);
		}

		Property tagDefFreqProperty = 
				resultTable.getItem(tagDefinitionItemID).getItemProperty(
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
			resultTable.setParent(sourceDocumentItemID, tagDefinitionItemID);
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
		RowWrapper wrapper = new RowWrapper(row);
		resultTable.addItem(
			new Object[] {
				row.getPhrase(),
				1,
				createCheckbox(
						new TagQueryResultRowTreeEntrySelectionHandler(row))
			}, wrapper);
		
		resultTable.setParent(wrapper, umcItemID);
		resultTable.setChildrenAllowed(wrapper, false);
		
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
				((CatmaApplication)getApplication()).showAndLogError(
					"Error showing KWIC results!", e);
			}
		}
		else {
			kwicPanel.removeQueryResultRows(queryResult);
		}
		
	}
}
