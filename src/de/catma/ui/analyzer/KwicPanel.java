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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.data.Container;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table.Align;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.CatmaApplication;
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
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.MultiSelectTreeTable;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToReversedTrimmedStringCIComparator;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.util.IDGenerator;

public class KwicPanel extends VerticalLayout {
	private enum KwicPropertyName {
		caption,
		leftContext,
		keyword,
		rightContext, 
		startPoint,
		endPoint,
		;
	}

	private Repository repository;
	private TreeTable kwicTable;
	private boolean markupBased;
	private RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider;

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
					QueryResultRow row = (QueryResultRow) event.getItemId();
					SourceDocument sd = repository.getSourceDocument(
							row.getSourceDocumentId());
					Range range = row.getRange();
					
					((CatmaApplication)UI.getCurrent()).openSourceDocument(
							sd, repository, range);
					
					List<UserMarkupCollectionReference> relatedUmcRefs = 
							relevantUserMarkupCollectionProvider.getCorpus().
								getUserMarkupCollectionRefs(sd);
					try {
						for (UserMarkupCollectionReference ref : relatedUmcRefs) {
							((CatmaApplication)UI.getCurrent()).openUserMarkupCollection(
								sd, repository.getUserMarkupCollection(ref), repository);
						}
					}
					catch (IOException e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(
							"Error opening related User Markup Collection!", e);
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
									"Error tagging search results!", e);
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
			boolean applyTag) throws IOException, URISyntaxException {    	
		@SuppressWarnings("unchecked")
		Set<QueryResultRow> selectedRows = 
				(Set<QueryResultRow>)kwicTable.getValue();
		
		if (selectedRows != null) {
			updateAllMarkupCollections(
				selectedRows, incomingTagsetDef, incomingTagDef);
		}
	}

	private void updateAllMarkupCollections(
			final Set<QueryResultRow> selectedRows, 
			final TagsetDefinition incomingTagsetDef, 
			final TagDefinition incomingTagDef) throws IOException {
		
		Set<SourceDocument> affectedDocuments = new HashSet<SourceDocument>();
		for (QueryResultRow row : selectedRows) {
			affectedDocuments.add(repository.getSourceDocument(row.getSourceDocumentId()));
		}
		
		TagKwicDialog tagKwicDialog = new TagKwicDialog(
				new SaveCancelListener<Map<String,UserMarkupCollection>>() {
			
			public void savePressed(Map<String, UserMarkupCollection> result) {
				try {
					tagKwic(result, selectedRows, incomingTagsetDef, incomingTagDef);
					Notification.show(
							"Info", "The search results have been tagged!", 
							Type.TRAY_NOTIFICATION);

				} catch (URISyntaxException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							"error creating tag reference", e);
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
				else if (relevantUserMarkupCollectionProvider.getCorpus().getUserMarkupCollectionRefs().contains(umcRef)) {
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
				"Information", "Please create a User Markup Collection first!",
				Type.TRAY_NOTIFICATION);
		}
	}

	private void tagKwic(
			Map<String, UserMarkupCollection> result, 
			Set<QueryResultRow> selectedRows, 
			TagsetDefinition incomingTagsetDef, TagDefinition incomingTagDef) throws URISyntaxException {
		
		UserMarkupCollectionManager userMarkupCollectionManager =
				new UserMarkupCollectionManager(repository);

		for (Map.Entry<String,UserMarkupCollection> entry : result.entrySet()) {
			
			UserMarkupCollection umc = entry.getValue();
			
			if (!umc.getTagLibrary().contains(incomingTagsetDef)) {
				repository.getTagManager().addTagsetDefinition(
					umc.getTagLibrary(), new TagsetDefinition(incomingTagsetDef));
			}
			userMarkupCollectionManager.add(umc);
		}

		final List<UserMarkupCollection> toBeUpdated = 
				userMarkupCollectionManager.getUserMarkupCollections(
						incomingTagsetDef, false);
		
		userMarkupCollectionManager.updateUserMarkupCollections(
    			toBeUpdated, incomingTagsetDef);
		
		Map<UserMarkupCollection, List<TagReference>> tagReferences =
				new HashMap<UserMarkupCollection, List<TagReference>>();

		IDGenerator idGenerator = new IDGenerator();
	
		for (QueryResultRow row : selectedRows) {
			UserMarkupCollection umc = 
					result.get(row.getSourceDocumentId());
			
			TagInstance ti = 
					new TagInstance(
						idGenerator.generate(), 
						umc.getTagLibrary().getTagDefinition(
								incomingTagDef.getUuid()));
			
			TagReference tr = new TagReference(
				ti, repository.getSourceDocument(
						row.getSourceDocumentId()).getID(), row.getRange());
			
			if (!tagReferences.containsKey(umc)) {
				tagReferences.put(umc, new ArrayList<TagReference>());
			}
			tagReferences.get(umc).add(tr);
		}
		
		
		for (Map.Entry<UserMarkupCollection, List<TagReference>> entry : tagReferences.entrySet()) {
			userMarkupCollectionManager.addTagReferences(entry.getValue(), entry.getKey());
		}

		
	}

	private void initComponents() {
		setSizeFull();
		
		kwicTable = new MultiSelectTreeTable();
		
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
		//TODO: nonsense:
		itemSorter.setPropertyComparator(
			KwicPropertyName.leftContext, 
			new PropertyToReversedTrimmedStringCIComparator());
		
		container.setItemSorter(itemSorter);
		
		kwicTable.setContainerDataSource(container);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.caption, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.caption, "Document/Collection");
		
		kwicTable.addContainerProperty(
				KwicPropertyName.leftContext, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.leftContext, "Left Context");
		kwicTable.setColumnAlignment(KwicPropertyName.leftContext, Align.RIGHT);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.keyword, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.keyword, "Keyword");
		kwicTable.setColumnAlignment(KwicPropertyName.keyword, Align.CENTER);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.rightContext, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.rightContext, "Right Context");	
		kwicTable.setColumnAlignment(KwicPropertyName.rightContext, Align.LEFT);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.startPoint, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.startPoint, "Start Point");
		
		kwicTable.addContainerProperty(
				KwicPropertyName.endPoint, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.endPoint, "End Point");
		
		kwicTable.setPageLength(12); //TODO: config
		kwicTable.setSizeFull();
		addComponent(kwicTable);
	}

	public void addQueryResultRows(Iterable<QueryResultRow> queryResult) 
			throws IOException {

		HashMap<String, KwicProvider> kwicProviders =
				new HashMap<String, KwicProvider>();
		
		for (QueryResultRow row : queryResult) {
			SourceDocument sourceDocument = 
					repository.getSourceDocument(row.getSourceDocumentId());
			
			if (!kwicProviders.containsKey(sourceDocument.getID())) {
				kwicProviders.put(
					sourceDocument.getID(), 
					new KwicProvider(sourceDocument));
			}
			
			KwicProvider kwicProvider = kwicProviders.get(sourceDocument.getID());
			KeywordInContext kwic = kwicProvider.getKwic(row.getRange(), 5);
			String sourceDocOrMarkupCollectionDisplay = 
					sourceDocument.toString();
			
			if (markupBased && (row instanceof TagQueryResultRow)) {
				sourceDocOrMarkupCollectionDisplay =
					sourceDocument.getUserMarkupCollectionReference(
						((TagQueryResultRow)row).getMarkupCollectionId()).getName();
			}
			
			kwicTable.addItem(
				new Object[]{
					sourceDocOrMarkupCollectionDisplay,
					kwic.getLeftContext(),
					kwic.getKeyword(),
					kwic.getRightContext(),
					row.getRange().getStartPoint(),
					row.getRange().getEndPoint()},
					row);
			kwicTable.setChildrenAllowed(row, false);
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
	
	TreeTable getKwicTable() {
		return kwicTable;
	}
}
