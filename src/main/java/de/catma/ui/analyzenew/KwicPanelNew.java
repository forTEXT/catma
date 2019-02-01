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
		
		
		public KwicPanelNew(Repository repository) { 
			this.repository = repository;
			
			initComponents();
			initActions();
				
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
			kwicGrid.setSelectionMode(SelectionMode.SINGLE);
			
			kwicGrid.addColumn(KwicItem::getKeyWord).setCaption("KeyWord").setId("keyWordID");
			//kwicGrid.getColumn("keyWordID").setExpandRatio(7);
			

			
			addComponent(kwicGrid);
			
		}
		
		public void addQueryResultRows(Iterable<QueryResultRow> queryResult) throws Exception {

			HashMap<String, KwicProvider> kwicProviders =	new HashMap<String, KwicProvider>();
			
			boolean showPropertyColumns = false;
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
				
				
				createKwicItemFromQueryResultRow();
				
          /*	create kwicRowItems like:
            	kwic.getBackwardContext(),
				kwic.getKeyword(),
				kwic.getForwardContext(),
				tRow.getTagDefinitionPath(),
				propertyName,
				propertyValue,
				row.getRange().getStartPoint(),
				row.getRange().getEndPoint()},
			*/
				
				
				 kwicGrid.setItems(kwicItemList);
			
		}

	}
		
		private KwicItem createKwicItemFromQueryResultRow() {
			return null;
		}
}



