package de.catma.ui.analyzenew.resourcepanel;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.modules.main.ErrorHandler;

public class AnalyzeResourcePanel extends VerticalLayout {

	private Repository project;
	private Corpus corpus;
	private TreeGrid<DocumentTreeItem> documentTree;
	private TreeData<DocumentTreeItem> documentsData;
	private AnalyzeResourceSelectionListener analyzeResourceSelectionListener;
	private ActionGridComponent<TreeGrid<DocumentTreeItem>> documentActionGridComponent;
	private PropertyChangeListener collectionChangeListener;
	private PropertyChangeListener projectExceptionListener;
	private ErrorHandler errorHandler;
	private PropertyChangeListener tagsetChangeListener;

	public AnalyzeResourcePanel(Repository project, Corpus corpus) {
		super();
		this.project = project;
		this.corpus = corpus;
		this.errorHandler = (ErrorHandler) UI.getCurrent();

		initComponents();
		initData();
		initListeners();
	}

	private void initData() {
		try {
			documentsData = new TreeData<>();

			Collection<SourceDocument> documents = project.getSourceDocuments();

			Collection<SourceDocument> selectedDocuments = corpus.getSourceDocuments();
			Collection<UserMarkupCollectionReference> selectedCollections = corpus.getUserMarkupCollectionRefs();

			documentsData.addRootItems(documents.stream()
					.map(document -> new DocumentDataItem(document, selectedDocuments.contains(document))));

			for (DocumentTreeItem documentDataItem : documentsData.getRootItems()) {
				for (UserMarkupCollectionReference umcRef : ((DocumentDataItem) documentDataItem).getDocument()
						.getUserMarkupCollectionRefs()) {

					documentsData.addItem(documentDataItem,
							new CollectionDataItem(umcRef, selectedCollections.contains(umcRef)));

				}
			}

			documentTree.setDataProvider(new TreeDataProvider<>(documentsData));
			documentsData.getRootItems().stream()
					.filter(documentItem -> selectedDocuments.contains(((DocumentDataItem) documentItem).getDocument()))
					.forEach(documentTree::select);

			for (DocumentTreeItem documentDataItem : documentsData.getRootItems()) {
				List<DocumentTreeItem> collectionItems = documentsData.getChildren(documentDataItem);
				for (DocumentTreeItem oneCollection : collectionItems) {
					if (selectedCollections.contains(((CollectionDataItem) oneCollection).getCollectionRef())) {
						documentTree.getSelectionModel().select(oneCollection);
					}

				}

			}

		} catch (Exception e) {
			errorHandler.showAndLogError("Error loading data!", e);
		}
	}

	private void initComponents() {
		addStyleName("annotate-resource-panel");
		Label documentTreeLabel = new Label("Documents & Annotations");

		documentTree = new TreeGrid<>();
		documentTree.addStyleNames("annotate-resource-grid", "flat-undecorated-icon-buttonrenderer");

		documentTree.addColumn(documentTreeItem -> documentTreeItem.getName()).setCaption("Name").setExpandRatio(3);

		// TODO: shouldn't be fixed size
		documentTree.setWidth("400px");
		documentTree.setHeight("450px");

		documentTree.addColumn(documentTreeItem -> documentTreeItem.getIcon(), new HtmlRenderer());

		documentActionGridComponent = new ActionGridComponent<TreeGrid<DocumentTreeItem>>(documentTreeLabel,
				documentTree);

		documentActionGridComponent.setSelectionModeFixed(Grid.SelectionMode.MULTI);

		addComponent(documentActionGridComponent);

	}

	private void initListeners() {
		documentTree.addSelectionListener(new SelectionListener<DocumentTreeItem>() {
			@Override
			public void selectionChange(SelectionEvent<DocumentTreeItem> event) {

				List<DocumentTreeItem> formerselectedItems = collectFormerSelectedDocuments();
				setBookIconsClosed();
				List<DocumentTreeItem> clearifiedList = deselectCollectionsForDeselectedDocs(event,
						formerselectedItems);

				selectDocsForSelectedCollections(clearifiedList);
				documentTree.getDataProvider().refreshAll();

				analyzeResourceSelectionListener.updateQueryOptions(documentTree);

			}
		});

	}

	private void setBookIconsClosed() {
		documentTree.getTreeData().getRootItems().stream()
				.forEach(item -> ((DocumentDataItem) item).setSelected(false));
	}

	private List<DocumentTreeItem> collectFormerSelectedDocuments() {
		return documentTree.getTreeData().getRootItems().stream()
				.filter(documentTreeItem -> documentTreeItem.isSelected()).collect(Collectors.toList());
	}

	private void selectDocsForSelectedCollections(List<DocumentTreeItem> selectedItemsClearified) {

		List<DocumentTreeItem> selected = selectedItemsClearified;

		for (DocumentTreeItem selectedItem : selected) {

			if (selectedItem.getClass() == CollectionDataItem.class) {

				DocumentTreeItem docItem = documentsData.getParent(selectedItem);
				documentTree.select(docItem);
			}

			if (selectedItem.getClass() == DocumentDataItem.class) {
				((DocumentDataItem) selectedItem).setSelected(true);

			}
		}

	}

	private List<DocumentTreeItem> deselectCollectionsForDeselectedDocs(SelectionEvent<DocumentTreeItem> selectedItems,
			List<DocumentTreeItem> oldListSelectedRootItems) {

		Set<DocumentTreeItem> selected = selectedItems.getAllSelectedItems();

		List<DocumentTreeItem> clearifiedList = selected.stream().collect(Collectors.toList());

		for (DocumentTreeItem oldItem : oldListSelectedRootItems) {

			if (!selected.contains(oldItem)) {
				List<DocumentTreeItem> toUnselect = documentTree.getTreeData().getChildren(oldItem);

				toUnselect.stream().forEach(item -> clearifiedList.remove(item));

				toUnselect.stream().forEach(item -> ((CollectionDataItem) item).setSelected(false));
				toUnselect.stream().forEach(item -> documentTree.deselect(item));
				documentTree.deselect(oldItem);

			} else {

			}
		}

		selected.stream().filter(item -> item.getClass().isInstance(DocumentDataItem.class))
				.forEach(item -> ((DocumentDataItem) item).setSelected(true));

		return clearifiedList;

	}

	/*
	 * public void close() { if (project != null) {
	 * project.removePropertyChangeListener(
	 * RepositoryChangeEvent.exceptionOccurred, projectExceptionListener);
	 * 
	 * project.removePropertyChangeListener(
	 * RepositoryChangeEvent.userMarkupCollectionChanged, collectionChangeListener);
	 * 
	 * project.getTagManager().removePropertyChangeListener(
	 * TagManagerEvent.tagsetDefinitionChanged, tagsetChangeListener); } }
	 */

	public void setSelectionListener(AnalyzeResourceSelectionListener analyzeResourceSelectionListener) {
		this.analyzeResourceSelectionListener = analyzeResourceSelectionListener;

	}
}
