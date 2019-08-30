package de.catma.ui.analyzenew.resourcepanel;

import java.util.Collection;
import java.util.List;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.rbac.RBACPermission;
import de.catma.ui.component.actiongrid.ActionGridComponent;

public class AnalyzeResourcePanel extends VerticalLayout {

	private Repository project;
	private Corpus corpus;
	
	private TreeGrid<DocumentTreeItem> documentTree;
	private TreeData<DocumentTreeItem> documentsData;
	
	private ActionGridComponent<TreeGrid<DocumentTreeItem>> documentActionGridComponent;

	public AnalyzeResourcePanel(Repository project, Corpus corpus, CorpusChangedListener corpusChangedListener) {
		super();
		this.project = project;
		this.corpus = corpus;
		initComponents();
		initData();
		initActions(corpusChangedListener);
	}

	private void initData() {
		documentsData = new TreeData<>();
		try {
			Collection<SourceDocument> documents = project.getSourceDocuments(); 
			
			documentsData.addRootItems(
				documents
				.stream()
				.map(document -> new DocumentDataItem(document)));
						
			for (DocumentTreeItem documentDataItem : documentsData.getRootItems()) {
				for (UserMarkupCollectionReference umcRef : 
					((DocumentDataItem)documentDataItem).getDocument().getUserMarkupCollectionRefs()) {
					documentsData.addItem(
						documentDataItem, 
						new CollectionDataItem(
							umcRef,
							project.hasPermission(
									project.getRoleForCollection(umcRef.getId()),
									RBACPermission.COLLECTION_WRITE)));
				}
			}
			
			documentTree.setDataProvider(new TreeDataProvider<>(documentsData));			
			
			Collection<SourceDocument> selectedDocuments = corpus.getSourceDocuments();
			Collection<UserMarkupCollectionReference> selectedCollections = 
					corpus.getUserMarkupCollectionRefs();
			
			documentsData.getRootItems().stream()
					.filter(documentItem -> 
						selectedDocuments.contains(((DocumentDataItem) documentItem).getDocument()))
					.forEach(documentTree::select);
	
			for (DocumentTreeItem documentDataItem : documentsData.getRootItems()) {
				List<DocumentTreeItem> collectionItems = documentsData.getChildren(documentDataItem);
				for (DocumentTreeItem oneCollection : collectionItems) {
					if (selectedCollections.contains(((CollectionDataItem) oneCollection).getCollectionRef())) {
						documentTree.getSelectionModel().select(oneCollection);
					}
				}
			}
			
			documentTree.expand(documentsData.getRootItems());
		}
		catch (Exception e) {
			//TODO:
			e.printStackTrace();
		}
	}

	private void initComponents() {
//		addStyleName("annotate-resource-panel");
		Label documentTreeLabel = new Label("Documents & Annotations");

		documentTree = new TreeGrid<>();
		documentTree.addStyleNames("resource-grid", "flat-undecorated-icon-buttonrenderer");

		documentTree
			.addColumn(documentTreeItem -> documentTreeItem.getName())
			.setCaption("Name")
			.setWidth(150);
	

		documentTree
			.addColumn(
				documentTreeItem -> documentTreeItem.getPermissionIcon(), new HtmlRenderer())
			.setWidth(50);
		
		documentTree
			.addColumn(
				documentTreeItem -> documentTreeItem.getIcon(), new HtmlRenderer())
			.setExpandRatio(1);

		documentTree.setSizeFull();

		documentActionGridComponent = new ActionGridComponent<TreeGrid<DocumentTreeItem>>(documentTreeLabel,
				documentTree);

		documentActionGridComponent.setSelectionModeFixed(Grid.SelectionMode.MULTI);

		addComponent(documentActionGridComponent);

	}

	private void initActions(CorpusChangedListener corpusChangedListener) {
		documentTree.addSelectionListener(new SelectionListener<DocumentTreeItem>() {
			@Override
			public void selectionChange(SelectionEvent<DocumentTreeItem> event) {
				if (event.isUserOriginated()) {
					event.getAllSelectedItems().forEach(item -> item.ensureSelectedParent(documentTree));
					corpusChangedListener.corpusChanged();
				}
			}
		});
	}

	public Corpus getCorpus() {
		Corpus corpus = new Corpus();
		
		documentTree.getSelectedItems().forEach(item -> item.addToCorpus(corpus));
		
		return corpus;
	}
	
	public void close() {
		//TODO
	}
}
