package de.catma.ui.analyzer;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
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
		
		this.kwicPanel = new KwicPanel(repository);
		splitPanel.addComponent(kwicPanel);
		
		addComponent(splitPanel);
	}
	
	public void setQueryResult(QueryResult queryResult) throws IOException {
//		kwicPanel.clear();
		resultTable.removeAllItems();
		int totalCount = 0;
		int totalFreq = 0;
	
		HashMap<String, UserMarkupCollection> loadedUserMarkupCollections =
				new HashMap<String, UserMarkupCollection>();
		
		for (QueryResultRow row : queryResult) {
			if (row instanceof TagQueryResultRow) {
				addTagQueryResultRow(
					(TagQueryResultRow) row, loadedUserMarkupCollections);
			}
		}
	}
	
	private void addTagQueryResultRow(
		TagQueryResultRow row, 
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
						new CheckBox() //TODO: createCheckbox
				},
				tagDefinition);
		}
		Property tagDefFreqProperty = 
				resultTable.getItem(tagDefinition).getItemProperty(
						TreePropertyName.frequency);
		tagDefFreqProperty.setValue(((Integer)tagDefFreqProperty.getValue())+1);
		
		String sourceDocumentItemID = tagDefinitionId+ "@" + sourceDocument;
		
		if (!resultTable.containsId(sourceDocumentItemID)) {
			resultTable.addItem(
				new Object[] {
						sourceDocument.toString(),
						0,
						new CheckBox() //TODO: createCheckbox
				}, sourceDocumentItemID);
			resultTable.setParent(sourceDocumentItemID, tagDefinition);
		}
		
		Property sourceDocFreqProperty = 
				resultTable.getItem(sourceDocumentItemID).getItemProperty(
						TreePropertyName.frequency);
		sourceDocFreqProperty.setValue(
				((Integer)sourceDocFreqProperty.getValue())+1);
		
		String umcItemID = sourceDocumentItemID + "@" + umc.getId();
		
		if (!resultTable.containsId(umcItemID)) {
			resultTable.addItem(
					new Object[] {
							umc.getName(),
							0,
							createCheckbox(umcItemID)
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
						new CheckBox()
				}, row);
		
		resultTable.setParent(row, umcItemID);
		resultTable.setChildrenAllowed(row, false);
		
	}

	private CheckBox createCheckbox(final String itemID) {
		CheckBox cbShowInKwicView = new CheckBox();
		cbShowInKwicView.setImmediate(true);
		cbShowInKwicView.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				boolean selected = 
						event.getButton().booleanValue();

				fireShowInKwicViewSelected(itemID, selected);
			}


		});
		return cbShowInKwicView;
	}

	private void fireShowInKwicViewSelected(String itemID,
			boolean selected) {

		QueryResultRowArray queryResult = new QueryResultRowArray();
		@SuppressWarnings("unchecked")
		Collection<QueryResultRow> rows = 
				(Collection<QueryResultRow>)resultTable.getChildren(itemID);
	
		queryResult.addAll(rows);
		
		if (selected) {
			try {
				kwicPanel.addQueryResultRows(rows);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			kwicPanel.removeQueryResultRows(rows);
		}
		
	}
}
