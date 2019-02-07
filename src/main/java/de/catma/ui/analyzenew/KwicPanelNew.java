package de.catma.ui.analyzenew;

import com.vaadin.ui.VerticalLayout;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;

import com.vaadin.ui.UI;
import com.vaadin.v7.data.Container;
import com.vaadin.v7.data.util.HierarchicalContainer;
import com.vaadin.v7.event.DataBoundTransferable;
import com.vaadin.v7.event.ItemClickEvent;
import com.vaadin.v7.event.ItemClickEvent.ItemClickListener;
import com.vaadin.v7.ui.AbstractSelect.AcceptItem;
import com.vaadin.v7.ui.Table;
import com.vaadin.v7.ui.Table.Align;
import com.vaadin.v7.ui.Table.CellStyleGenerator;




import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.source.KeywordInContext;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionManager;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.CatmaApplication;
import de.catma.ui.analyzer.Messages;
import de.catma.ui.analyzer.RelevantUserMarkupCollectionProvider;
import de.catma.ui.analyzer.TagKwicDialog;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class KwicPanelNew extends VerticalLayout{
	
		private enum KwicPropertyName {
			caption,
			leftContext,
			keyword,
			rightContext, 
			startPoint,
			endPoint,
			tagtype,
			propertyname,
			propertyvalue,
			;
		}

		private Repository repository;
		private Grid<KwicItem> kwicGrid;
		private RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider;
		private WeakHashMap<Object, Boolean> itemDirCache = new WeakHashMap<>();
		private int kwicSize = 5;
		private List<KwicItem> kwicItemList;
		private boolean showPropertyColumns;
		
		
		public KwicPanelNew(Repository repository) { 
			this.repository = repository;
			
			initComponents();
			initActions();
			setHeight("100%");
				
		}
		public KwicPanelNew(Repository repository, 
				RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider) {
			this(repository, relevantUserMarkupCollectionProvider,  false);
		}
		
		public KwicPanelNew(
				Repository repository, 
				RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider, 
				boolean markupBased) {
			this.repository = repository;
			this.relevantUserMarkupCollectionProvider = relevantUserMarkupCollectionProvider;
	
			
			initComponents();
			initActions();

		}
		
		private void initActions() {
		
			
		}
		
		private void initComponents() {
			kwicGrid= new Grid<KwicItem>();
	    	kwicItemList= new ArrayList<>(); 
			kwicGrid.setSelectionMode(SelectionMode.MULTI);
			//kwicGrid.setSelectionMode(SelectionMode.SINGLE);
			kwicGrid.setWidth("100%");
			kwicGrid.setHeight("100%");
			
			kwicGrid.addColumn(KwicItem::getDocCollection).setCaption("Document/Collection").setId("docCollectionID");
			kwicGrid.addColumn(KwicItem::getBackwardContext).setCaption("Left Context").setId("backwardID");
			kwicGrid.addColumn(KwicItem::getKeyWord).setCaption("KeyWord").setId("keyWordID");
			kwicGrid.addColumn(KwicItem::getForewardContext).setCaption("Right Context").setId("forewardID");
			kwicGrid.addColumn(KwicItem::getRangeStartPoint).setCaption("Start Point").setId("startPointID");
			kwicGrid.addColumn(KwicItem::getRangeEndPoint).setCaption("End Point").setId("endPointID");
			
			//kwicGrid.getColumn("keyWordID").setExpandRatio(7);
			

			
			addComponent(kwicGrid);
			
		}
		
		public void addQueryResultRows(Iterable<QueryResultRow> queryResult) throws Exception {

		if(kwicGrid.getColumn("propValueID") != null){
					
			kwicGrid.removeColumn("propValueID");
			kwicGrid.removeColumn("propNameID");
			
		}
			
			HashMap<String, KwicProvider> kwicProviders =	new HashMap<String, KwicProvider>();
			kwicItemList.removeAll(kwicItemList);
			
			showPropertyColumns = false;
			boolean markupBased=true;
			
			for (QueryResultRow row : queryResult) {
				
	
				SourceDocument sourceDocument = repository.getSourceDocument(row.getSourceDocumentId());
				
				if (!kwicProviders.containsKey(sourceDocument.getID())) {
					kwicProviders.put(sourceDocument.getID(), new KwicProvider(sourceDocument));
					}
				
				KwicProvider kwicProvider = kwicProviders.get(sourceDocument.getID());
				KeywordInContext kwic = kwicProvider.getKwic(row.getRange(), kwicSize);
				String sourceDocOrMarkupCollectionDisplay = sourceDocument.toString();
				
				if (markupBased && (row instanceof TagQueryResultRow)) {
					sourceDocOrMarkupCollectionDisplay =sourceDocument.getUserMarkupCollectionReference(((TagQueryResultRow)row).getMarkupCollectionId()).getName();
				}
				
				itemDirCache.put(row, kwic.isRightToLeft());
					
				KwicItem kwicItem=createKwicItemFromQueryResultRow(row , kwic, showPropertyColumns);
	
				kwicItemList.add(kwicItem);
			
	 	}
			if(showPropertyColumns) {
				kwicGrid.addColumn(KwicItem::getPropertyName).setCaption("Property Name").setId("propNameID");
				kwicGrid.addColumn(KwicItem::getPropertyValue).setCaption("Property Value").setId("propValueID");
			
				
			}
			
			 kwicGrid.setItems(kwicItemList);
			 
	}
		

		
	private KwicItem createKwicItemFromQueryResultRow(QueryResultRow queryResultRow, KeywordInContext kwic, boolean showPropertyColumns ) {
		KwicItem kwicItem = new KwicItem();
		
		if(queryResultRow instanceof TagQueryResultRow) {
			
			TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;
			kwicItem.setDocCollection(tagQueryResultRow.getTagDefinitionPath());
			kwicItem.setKeyWord(tagQueryResultRow.getPhrase());
			kwicItem.setBackwardContext(kwic.getBackwardContext());
			kwicItem.setForewardContext(kwic.getForwardContext());
			kwicItem.setRangeStartPoint(queryResultRow.getRange().getStartPoint());
			kwicItem.setRangeEndPoint(queryResultRow.getRange().getEndPoint());
			
			if(tagQueryResultRow.getPropertyName()!=null) {
				
			this.showPropertyColumns = true;
				kwicItem.setPropertyName(tagQueryResultRow.getPropertyName());
				kwicItem.setPropertyValue(tagQueryResultRow.getPropertyValue());
						
			}

			return kwicItem;
			
		}else {
			kwicItem.setKeyWord(queryResultRow.getPhrase());
			kwicItem.setBackwardContext(kwic.getBackwardContext());
			kwicItem.setForewardContext(kwic.getForwardContext());
			kwicItem.setRangeStartPoint(queryResultRow.getRange().getStartPoint());
			kwicItem.setRangeEndPoint(queryResultRow.getRange().getEndPoint());

			return kwicItem;
			
		}

	}
}



