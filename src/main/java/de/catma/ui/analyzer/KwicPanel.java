/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.analyzer;

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
import com.vaadin.v7.ui.VerticalLayout;

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
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class KwicPanel extends VerticalLayout {
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
	private Table kwicTable;
	private boolean markupBased;
	private RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider;
	private WeakHashMap<Object, Boolean> itemDirCache = new WeakHashMap<>();
	private int kwicSize = 5;
	
	public KwicPanel(Repository repository, 
			RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider) {
		this(repository, relevantUserMarkupCollectionProvider,  false);
	}
	
	public KwicPanel(
			Repository repository, 
			RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider, 
			boolean markupBased) {
		this.repository = repository;
		this.relevantUserMarkupCollectionProvider = relevantUserMarkupCollectionProvider;
		this.markupBased = markupBased;
		
		initComponents();
		initActions();

	}

	private void initActions() {
		kwicTable.addItemClickListener(new ItemClickListener() {
			
			public void itemClick(ItemClickEvent event) {
				if (event.isDoubleClick()) {
					try {
						final QueryResultRow row = (QueryResultRow) event.getItemId();
						final SourceDocument sd = repository.getSourceDocument(
								row.getSourceDocumentId());
						final Range range = row.getRange();
						
						((CatmaApplication)UI.getCurrent()).openSourceDocument(
								sd, repository, range);
						
						if (row instanceof TagQueryResultRow) {
							final String umcId = 
									((TagQueryResultRow)row).getMarkupCollectionId();
							for (UserMarkupCollectionReference ref :
								relevantUserMarkupCollectionProvider
									.getCorpus().getUserMarkupCollectionRefs(sd)) {
								
								if (ref.getId().equals(umcId)) {
									((CatmaApplication)UI.getCurrent()).openUserMarkupCollection(
											sd, repository.getUserMarkupCollection(ref), repository);
									break;
								}
								
							}
						}
					}
					catch (Exception e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(
								Messages.getString("KwicPanel.errorOpeningSearchResults"), e); //$NON-NLS-1$
					}			
				}
				
			}
		});
		kwicTable.setDropHandler(new DropHandler() {
			
			public AcceptCriterion getAcceptCriterion() {
				
				return AcceptItem.ALL;
			}
			
			public void drop(DragAndDropEvent event) {
				DataBoundTransferable transferable = 
						(DataBoundTransferable)event.getTransferable();
				
                if (!(transferable.getSourceContainer() 
                		instanceof Container.Hierarchical)) {
                    return;
                }

                final Object sourceItemId = transferable.getItemId();
                if (sourceItemId instanceof TagDefinition) {
                	TagDefinition incomingTagDef = (TagDefinition)sourceItemId;
                	
                	TagsetDefinition incomingTagsetDef = 
                			getTagsetDef(
                				(HierarchicalContainer)transferable.getSourceContainer(), 
                				incomingTagDef);
                	
                	if (incomingTagsetDef != null) {
	                	try {
							applyTagOperationToAllSelectedResults(
									incomingTagsetDef, incomingTagDef, true);
						} catch (Exception e) {
							((CatmaApplication)UI.getCurrent()).showAndLogError(
									Messages.getString("KwicPanel.errorTaggingSearchResults"), e); //$NON-NLS-1$
						}
                	}
                }
			}
		});
	}

	private TagsetDefinition getTagsetDef(HierarchicalContainer sourceContainer,
			TagDefinition incomingTagDef) {
		Object parent = sourceContainer.getParent(incomingTagDef);
		while ((parent != null && !(parent instanceof TagsetDefinition))) {
			parent = sourceContainer.getParent(parent);
		}
		if (parent == null) {
			return null;
		}
		else {
			return (TagsetDefinition)parent;
		}
	}

	private void applyTagOperationToAllSelectedResults(
			TagsetDefinition incomingTagsetDef, TagDefinition incomingTagDef,
			boolean applyTag) throws Exception {    	
		@SuppressWarnings("unchecked")
		Set<QueryResultRow> selectedRows = 
				(Set<QueryResultRow>)kwicTable.getValue();
		
		if (selectedRows.isEmpty()) {
			Notification.show(
					Messages.getString("KwicPanel.infoTitle"), Messages.getString("KwicPanel.selectResultsFirstHint"), //$NON-NLS-1$ //$NON-NLS-2$
					Type.TRAY_NOTIFICATION);
			return;
		}
		
		updateAllMarkupCollections(
			selectedRows, incomingTagsetDef, incomingTagDef);
	}

	private void updateAllMarkupCollections(
			final Set<QueryResultRow> selectedRows, 
			final TagsetDefinition incomingTagsetDef, 
			final TagDefinition incomingTagDef) throws Exception {
		
		Set<SourceDocument> affectedDocuments = new HashSet<SourceDocument>();
		for (QueryResultRow row : selectedRows) {
			affectedDocuments.add(repository.getSourceDocument(row.getSourceDocumentId()));
		}
		
		TagKwicDialog tagKwicDialog = new TagKwicDialog(
				new SaveCancelListener<Map<String,UserMarkupCollection>>() {
			
			public void savePressed(Map<String, UserMarkupCollection> result) {
				try {
					Pair<Integer,Integer> countStats = 
						tagKwic(result, selectedRows, incomingTagsetDef, incomingTagDef);
					
					int umcCount = countStats.getFirst();
					int tagRefCount = countStats.getSecond();
					
					if (umcCount > 0) {
						if (tagRefCount > 1) {
							Notification.show(
								Messages.getString("KwicPanel.infoTitle"),  //$NON-NLS-1$
								MessageFormat.format(
								Messages.getString("KwicPanel.taggedSearchResultsFeedback"), tagRefCount, umcCount,    //$NON-NLS-1$
									((umcCount>1)?Messages.getString("KwicPanel.collections"):Messages.getString("KwicPanel.collection"))), //$NON-NLS-1$ //$NON-NLS-2$
								Type.TRAY_NOTIFICATION);
						}
						else {
							Notification.show(
								Messages.getString("KwicPanel.infoTitle"),  //$NON-NLS-1$
								MessageFormat.format(
								Messages.getString("KwicPanel.taggedSearchResultFeedback"), tagRefCount, umcCount,    //$NON-NLS-1$
									((umcCount>1)?Messages.getString("KwicPanel.collections"):Messages.getString("KwicPanel.collection"))), //$NON-NLS-1$ //$NON-NLS-2$
								Type.TRAY_NOTIFICATION);
						}
					}
					else {
						Notification.show(
								Messages.getString("KwicPanel.infoTitle"), Messages.getString("KwicPanel.nothingHasBeenTaggedFeedback"), //$NON-NLS-1$ //$NON-NLS-2$
								Type.TRAY_NOTIFICATION);
					}
				} catch (Exception e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							Messages.getString("KwicPanel.errorTaggingSearchResults"), e); //$NON-NLS-1$
				}
			}
			
			public void cancelPressed() { /* noop */}
		}, repository);
		
		boolean umcFound = false;
		for (SourceDocument sd : affectedDocuments) {
			List<UserMarkupCollectionReference> writableUmcRefs = 
					repository.getWritableUserMarkupCollectionRefs(sd);
			
			UserMarkupCollectionReference initialTarget = null;
			
			for (UserMarkupCollectionReference umcRef : writableUmcRefs) {
				if (initialTarget == null) {
					initialTarget = umcRef;
				}
				else if (relevantUserMarkupCollectionProvider.getCorpus()
						.getUserMarkupCollectionRefs().contains(umcRef)) {
					initialTarget = umcRef;
					break;
				}
			}
			if (!writableUmcRefs.isEmpty()) {
				tagKwicDialog.addUserMarkCollections(
						sd, writableUmcRefs, initialTarget);
				umcFound = true;
			}
		}
		if (umcFound) {
			tagKwicDialog.show();
		}
		else {
			Notification.show(
				Messages.getString("KwicPanel.infoTitle"), Messages.getString("KwicPanel.selectCollectionHint"), //$NON-NLS-1$ //$NON-NLS-2$
				Type.TRAY_NOTIFICATION);
		}
	}

	private Pair<Integer,Integer> tagKwic(
			Map<String, UserMarkupCollection> result, 
			Set<QueryResultRow> selectedRows, 
			TagsetDefinition incomingTagsetDef, TagDefinition incomingTagDef) throws Exception {
		
		UserMarkupCollectionManager userMarkupCollectionManager =
				new UserMarkupCollectionManager(repository);

		for (Map.Entry<String,UserMarkupCollection> entry : result.entrySet()) {
			
			UserMarkupCollection umc = entry.getValue();
			
			if (!umc.getTagLibrary().contains(incomingTagsetDef)) {
				repository.getTagManager().addTagsetDefinition(new TagsetDefinition(incomingTagsetDef));
			}
			userMarkupCollectionManager.add(umc);
		}

		final List<UserMarkupCollection> toBeUpdated = 
				userMarkupCollectionManager.getOutOfSyncUserMarkupCollections(
						incomingTagsetDef);
		
		userMarkupCollectionManager.updateUserMarkupCollections(
    			toBeUpdated, incomingTagsetDef);
		
		Map<UserMarkupCollection, List<TagReference>> tagReferences =
				new HashMap<UserMarkupCollection, List<TagReference>>();

		IDGenerator idGenerator = new IDGenerator();
	
		for (QueryResultRow row : selectedRows) {
			UserMarkupCollection umc = 
					result.get(row.getSourceDocumentId());
			if (umc != null) {
				TagInstance ti = 
						new TagInstance(
							idGenerator.generate(), 
							umc.getTagLibrary().getTagDefinition(
									incomingTagDef.getUuid()));
				
				TagReference tr = new TagReference(
					ti, repository.getSourceDocument(
							row.getSourceDocumentId()).getID(), row.getRange(), umc.getId());
				
				if (!tagReferences.containsKey(umc)) {
					tagReferences.put(umc, new ArrayList<TagReference>());
				}
				tagReferences.get(umc).add(tr);
			}
		}
		
		int refCount = 0;
		for (Map.Entry<UserMarkupCollection, List<TagReference>> entry : tagReferences.entrySet()) {
			userMarkupCollectionManager.addTagReferences(entry.getValue(), entry.getKey());
			refCount += entry.getValue().size();
		}

		return new Pair<Integer, Integer>(tagReferences.size(), refCount); //collection count, tag ref count
	}

	private void initComponents() {
		setSizeFull();
		
		kwicTable = new Table();
		
		kwicTable.setSizeFull();
		kwicTable.setSelectable(true);
		kwicTable.setMultiSelect(true);
		
		HierarchicalContainer container = new HierarchicalContainer();
		PropertyDependentItemSorter itemSorter = 
				new PropertyDependentItemSorter(
						new Object[] {
								KwicPropertyName.rightContext,
								KwicPropertyName.keyword
						},
						new PropertyToTrimmedStringCIComparator());
		//TODO: split up context in separate columns to achieve true sortability
		itemSorter.setPropertyComparator(
			KwicPropertyName.leftContext, 
			new PropertyToTrimmedStringCIComparator());
		
		container.setItemSorter(itemSorter);
		
		kwicTable.setContainerDataSource(container);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.caption, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.caption, Messages.getString("KwicPanel.docCollection")); //$NON-NLS-1$
		
		kwicTable.addContainerProperty(
				KwicPropertyName.leftContext, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.leftContext, Messages.getString("KwicPanel.leftContext")); //$NON-NLS-1$
		kwicTable.setColumnAlignment(KwicPropertyName.leftContext, Align.RIGHT);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.keyword, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.keyword, Messages.getString("KwicPanel.keyword")); //$NON-NLS-1$
		kwicTable.setColumnAlignment(KwicPropertyName.keyword, Align.CENTER);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.rightContext, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.rightContext, Messages.getString("KwicPanel.rightContext"));	 //$NON-NLS-1$
		kwicTable.setColumnAlignment(KwicPropertyName.rightContext, Align.LEFT);
		if (markupBased) {
			kwicTable.addContainerProperty(KwicPropertyName.tagtype, String.class, null);
			kwicTable.setColumnHeader(KwicPropertyName.tagtype, Messages.getString("KwicPanel.tag")); //$NON-NLS-1$
			kwicTable.addContainerProperty(KwicPropertyName.propertyname, String.class, null);
			kwicTable.setColumnHeader(KwicPropertyName.propertyname, Messages.getString("KwicPanel.propertyName")); //$NON-NLS-1$
			kwicTable.addContainerProperty(KwicPropertyName.propertyvalue, String.class, null);
			kwicTable.setColumnHeader(KwicPropertyName.propertyvalue, Messages.getString("KwicPanel.propertyValue")); //$NON-NLS-1$
			kwicTable.setColumnCollapsingAllowed(true);
			kwicTable.setColumnCollapsible(KwicPropertyName.propertyname, true);
			kwicTable.setColumnCollapsible(KwicPropertyName.propertyvalue, true);
			kwicTable.setColumnCollapsed( 
					KwicPropertyName.propertyname, true);
			kwicTable.setColumnCollapsed( 
					KwicPropertyName.propertyvalue, true);
		}
		kwicTable.addContainerProperty(
				KwicPropertyName.startPoint, Integer.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.startPoint, Messages.getString("KwicPanel.startPoint")); //$NON-NLS-1$
		
		kwicTable.addContainerProperty(
				KwicPropertyName.endPoint, Integer.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.endPoint, Messages.getString("KwicPanel.endPoint")); //$NON-NLS-1$

		kwicTable.setSizeFull();
		
		kwicTable.setCellStyleGenerator(new CellStyleGenerator() {
			
			@Override
			public String getStyle(Table source, Object itemId, Object propertyId) {
				Boolean isRtl = itemDirCache.get(itemId);
				if ((isRtl != null) && isRtl.booleanValue()) {
					return "rtl-field"; //$NON-NLS-1$
				}
				return null;
			}
		});
		
		addComponent(kwicTable);
	}

	public void addQueryResultRows(Iterable<QueryResultRow> queryResult) 
			throws Exception {

		HashMap<String, KwicProvider> kwicProviders =
				new HashMap<String, KwicProvider>();
		boolean showPropertyColumns = false;
		for (QueryResultRow row : queryResult) {
			SourceDocument sourceDocument = 
					repository.getSourceDocument(row.getSourceDocumentId());
			
			if (!kwicProviders.containsKey(sourceDocument.getID())) {
				kwicProviders.put(
					sourceDocument.getID(), 
					new KwicProvider(sourceDocument));
			}
			
			KwicProvider kwicProvider = kwicProviders.get(sourceDocument.getID());
			KeywordInContext kwic = kwicProvider.getKwic(row.getRange(), kwicSize);
			String sourceDocOrMarkupCollectionDisplay = 
					sourceDocument.toString();
			
			if (markupBased && (row instanceof TagQueryResultRow)) {
				sourceDocOrMarkupCollectionDisplay =
					sourceDocument.getUserMarkupCollectionReference(
						((TagQueryResultRow)row).getMarkupCollectionId()).getName();
			}
			itemDirCache.put(row, kwic.isRightToLeft());
			
			if (markupBased) {
				TagQueryResultRow tRow = (TagQueryResultRow) row;
				String propertyName = tRow.getPropertyName();
				String propertyValue = tRow.getPropertyValue();
				showPropertyColumns = (propertyName != null)||showPropertyColumns;
				kwicTable.addItem(
					new Object[]{
						sourceDocOrMarkupCollectionDisplay,
						kwic.getBackwardContext(),
						kwic.getKeyword(),
						kwic.getForwardContext(),
						tRow.getTagDefinitionPath(),
						propertyName,
						propertyValue,
						row.getRange().getStartPoint(),
						row.getRange().getEndPoint()},
					row);
			}
			else {
				kwicTable.addItem(
					new Object[]{
						sourceDocOrMarkupCollectionDisplay,
						kwic.getBackwardContext(),
						kwic.getKeyword(),
						kwic.getForwardContext(),
						row.getRange().getStartPoint(),
						row.getRange().getEndPoint()},
					row);
			}
		}
		
		if (showPropertyColumns) {
			kwicTable.setColumnCollapsed( 
					KwicPropertyName.propertyname, false);
			kwicTable.setColumnCollapsed( 
					KwicPropertyName.propertyvalue, false);
		}

	}
	
	public void removeQueryResultRows(Iterable<QueryResultRow> queryResult) {
		for (QueryResultRow row : queryResult) {
			kwicTable.removeItem(row);
		}
	}

	public void clear() {
		kwicTable.removeAllItems();
	}
	
	@SuppressWarnings("unchecked")
	public Set<QueryResultRow> getSelection() {
		return (Set<QueryResultRow>) kwicTable.getValue();
	}
	
	Table getKwicTable() {
		return kwicTable;
	}

	public void selectAll() {
		kwicTable.setValue(kwicTable.getItemIds());
	}

	@SuppressWarnings("unchecked")
	public void setKwicSize(int kwicSize) throws Exception {
		this.kwicSize = kwicSize;
		QueryResultRowArray rows = new QueryResultRowArray();
		rows.addAll((Collection<QueryResultRow>) kwicTable.getItemIds());
		kwicTable.removeAllItems();
		addQueryResultRows(rows);
	}
}
