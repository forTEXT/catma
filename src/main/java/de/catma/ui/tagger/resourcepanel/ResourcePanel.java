package de.catma.ui.tagger.resourcepanel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.actiongrid.ActionGridComponent;

public class ResourcePanel extends VerticalLayout {
	
	private Repository project;
	private TreeGrid<DocumentTreeItem> documentTree;
	private TreeData<DocumentTreeItem> documentsData;
	private Grid<TagsetDefinition> tagsetGrid;
	private ResourceSelectionListener resourceSelectionListener;

	public ResourcePanel(Repository project, SourceDocument currentlySelectedSourceDocument) {
		super();
		this.project = project;
		initComponents();
		initData(currentlySelectedSourceDocument);
	}

	private void initData(SourceDocument currentlySelectedSourceDocument) {
		try {
			documentsData = new TreeData<>();
			
			Collection<SourceDocument> documents = project.getSourceDocuments(); 
			documentsData.addRootItems(
				documents
				.stream()
				.map(document -> new DocumentDataItem(document, document.equals(currentlySelectedSourceDocument))));
			
			for (DocumentTreeItem documentDataItem : documentsData.getRootItems()) {
				for (UserMarkupCollectionReference umcRef : 
					((DocumentDataItem)documentDataItem).getDocument().getUserMarkupCollectionRefs()) {
					documentsData.addItem(documentDataItem, new CollectionDataItem(umcRef));
				}
			}
			
			documentTree.setDataProvider(new TreeDataProvider<>(documentsData));
			
			ListDataProvider<TagsetDefinition> tagsetData = new ListDataProvider<>(project.getTagsets());
			tagsetGrid.setDataProvider(tagsetData);
			tagsetData.getItems().forEach(tagsetGrid::select);
			
			documentsData
				.getRootItems()
				.stream()
				.filter(documentItem -> documentItem.isSelected())
				.findAny()
				.ifPresent(documentItem -> documentTree.expand(documentItem));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<UserMarkupCollectionReference> getSelectedUserMarkupCollectionReferences() {
		
		Optional<DocumentTreeItem> optionalDocumentTreeItem = 
				documentsData.getRootItems()
				.stream()
				.filter(documentTreeItem->documentTreeItem.isSelected())
				.findFirst();
		
		if (optionalDocumentTreeItem.isPresent()) {
			return documentsData.getChildren(optionalDocumentTreeItem.get())
				.stream()
				.filter(documentTreeItem -> documentTreeItem.isSelected())
				.map(CollectionDataItem.class::cast)
				.map(collectionDataItem -> collectionDataItem.getCollectionRef())
				.collect(Collectors.toList());
		}
		
		return Collections.emptyList();
	}
	
	public Collection<TagsetDefinition> getSelectedTagsets() {
		return tagsetGrid.getSelectedItems();
	}

	private void initComponents() {
		addStyleName("annotate-resource-panel");
		Label documentTreeLabel = new Label("Documents & Annotations");
		documentTree = new TreeGrid<>();
		documentTree.addStyleName("annotate-resource-grid");
		
		ButtonRenderer<DocumentTreeItem> documentSelectionRenderer = 
				new ButtonRenderer<DocumentTreeItem>(
					documentSelectionClick -> handleVisibilityClickEvent(documentSelectionClick));
		documentSelectionRenderer.setHtmlContentAllowed(true);
		Column<DocumentTreeItem, String> selectionColumn = 
			documentTree.addColumn(
				documentTreeItem -> documentTreeItem.getSelectionIcon(),
				documentSelectionRenderer);
		
		documentTree.setHierarchyColumn(selectionColumn);
		
		documentTree
			.addColumn(documentTreeItem -> documentTreeItem.getName())
			.setCaption("Name")
			.setExpandRatio(3);
		documentTree.setWidth("400px");
		documentTree.setHeight("250px");

		
		documentTree
			.addColumn(documentTreeItem -> documentTreeItem.getIcon(), new HtmlRenderer());

		ActionGridComponent<TreeGrid<DocumentTreeItem>> documentActionGridComponent = 
				new ActionGridComponent<TreeGrid<DocumentTreeItem>>(documentTreeLabel, documentTree);
		
		addComponent(documentActionGridComponent);
		
		Label tagsetLabel = new Label("Tagsets");
		
		tagsetGrid = new Grid<>();
		tagsetGrid.addStyleName("annotate-resource-grid");
		tagsetGrid.setSelectionMode(SelectionMode.MULTI);
		tagsetGrid.setWidth("400px");
		tagsetGrid.setHeight("230px");
		tagsetGrid
			.addColumn(tagset -> tagset.getName())
			.setCaption("Name")
			.setExpandRatio(2);
		
		tagsetGrid
			.addColumn(tagset -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer());
		
		ActionGridComponent<Grid<TagsetDefinition>> tagsetActionGridComponent = 
				new ActionGridComponent<Grid<TagsetDefinition>>(tagsetLabel, tagsetGrid);
		
		addComponent(tagsetActionGridComponent);
	}

	private void handleVisibilityClickEvent(RendererClickEvent<DocumentTreeItem> documentSelectionClick) {
		DocumentTreeItem selectedItem = documentSelectionClick.getItem();
		selectedItem.setSelected(!selectedItem.isSelected());
		
		if (selectedItem.isSingleSelection()) {
			for (DocumentTreeItem item : documentsData.getRootItems()) {
				if (!item.equals(selectedItem)) {
					item.setSelected(false);
					for (DocumentTreeItem child : documentsData.getChildren(item)) {
						child.setSelected(false);
					}
				}
			}
		}		
		documentTree.getDataProvider().refreshAll();
		
		selectedItem.fireSelectedEvent(this.resourceSelectionListener);
	}

	public void setSelectionListener(
			ResourceSelectionListener resourceSelectionListener) {
		this.resourceSelectionListener = resourceSelectionListener;
	}
	
	
}
