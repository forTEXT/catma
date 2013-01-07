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
import com.vaadin.ui.Table;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.Notification;

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
		kwicTable.addListener(new ItemClickListener() {
			
			public void itemClick(ItemClickEvent event) {
				if (event.isDoubleClick()) {
					QueryResultRow row = (QueryResultRow) event.getItemId();
					SourceDocument sd = repository.getSourceDocument(
							row.getSourceDocumentId());
					Range range = row.getRange();
					
					((CatmaApplication)getApplication()).openSourceDocument(
							sd, repository, range);
					
					List<UserMarkupCollectionReference> relatedUmcRefs = 
							relevantUserMarkupCollectionProvider.getCorpus().
								getUserMarkupCollectionRefs(sd);
					try {
						for (UserMarkupCollectionReference ref : relatedUmcRefs) {
							((CatmaApplication)getApplication()).openUserMarkupCollection(
								sd, repository.getUserMarkupCollection(ref), repository);
						}
					}
					catch (IOException e) {
						((CatmaApplication)getApplication()).showAndLogError(
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
							((CatmaApplication)getApplication()).showAndLogError(
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
					KwicPanel.this.getWindow().showNotification(
							"Info", "The search results have been tagged!", 
							Notification.TYPE_TRAY_NOTIFICATION);

				} catch (URISyntaxException e) {
					((CatmaApplication)getApplication()).showAndLogError(
							"error creating tag reference", e);
				}
			}
			
			public void cancelPressed() { /* noop */}
		}, repository);
		
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
			
			tagKwicDialog.addUserMarkCollections(
					sd, writableUmcRefs, initialTarget);
			
		}
		
		tagKwicDialog.show(getApplication().getMainWindow());
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
		
		kwicTable = new TreeTable();
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
		kwicTable.setColumnAlignment(KwicPropertyName.leftContext, Table.ALIGN_RIGHT);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.keyword, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.keyword, "Keyword");
		kwicTable.setColumnAlignment(KwicPropertyName.keyword, Table.ALIGN_CENTER);
		
		kwicTable.addContainerProperty(
				KwicPropertyName.rightContext, String.class, null);
		kwicTable.setColumnHeader(KwicPropertyName.rightContext, "Right Context");	
		kwicTable.setColumnAlignment(KwicPropertyName.rightContext, Table.ALIGN_LEFT);
		
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
					kwic.getRightContext()},
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
}
