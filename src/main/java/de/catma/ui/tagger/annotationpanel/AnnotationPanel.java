package de.catma.ui.tagger.annotationpanel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.repository.Repository;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SaveCancelListener;

public class AnnotationPanel extends VerticalLayout {
	
	
	public interface TagReferenceSelectionChangeListener {
		public void tagReferenceSelectionChanged(
				List<TagReference> tagReferences, boolean selected);
	}
	
	private ComboBox<UserMarkupCollection> currentEditableCollectionBox;
	private Button addCollectionButton;
	private TreeGrid<TagsetTreeItem> tagsetGrid;
	private Repository project;
	private Collection<TagsetDefinition> tagsets = Collections.emptyList();
	private List<UserMarkupCollection> collections = Collections.emptyList();
	private TagReferenceSelectionChangeListener selectionListener;
	private ActionGridComponent<TreeGrid<TagsetTreeItem>> tagsetGridComponent;

	public AnnotationPanel(Repository project) {
		this.project = project;
		initComponents();
		initActions();
	}

	private void initData() {
        try {
            TreeData<TagsetTreeItem> tagsetData = new TreeData<>();
            
            tagsetData.addRootItems(tagsets.stream().map(ts -> new TagsetDataItem(ts)));

            for (TagsetDefinition tagsetDefinition : tagsets) {
            	addTags(tagsetData, tagsetDefinition);
            }

            tagsetGrid.setDataProvider(new TreeDataProvider<>(tagsetData));
            for (TagsetDefinition tagset : tagsets) {
            	expandTagsetDefinition(tagset);
            }
            currentEditableCollectionBox.setValue(null);
            currentEditableCollectionBox.setDataProvider(new ListDataProvider<>(collections));
        } catch (Exception e) {
			// TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void expandTagsetDefinition(TagsetDefinition tagset) {
    	for (TagDefinition tag : tagset) {
    		TagDataItem item = new TagDataItem(tag);
    		tagsetGrid.expand(item);
    		if (!tag.getUserDefinedPropertyDefinitions().isEmpty()) {
    			tagsetGrid.setDetailsVisible(item, true);
    		}
    	}
	}

	private void addTags(TreeData<TagsetTreeItem> tagsetData, TagsetDefinition tagset) {
        for (TagDefinition tag : tagset) {
            if (tag.getParentUuid().isEmpty()) {
                tagsetData.addItem(new TagsetDataItem(tagset), new TagDataItem(tag));
                addTagSubTree(tagset, tag, tagsetData);
            }
        }
	}

	private void addTagSubTree(
    		TagsetDefinition tagset, 
    		TagDefinition tag, TreeData<TagsetTreeItem> tagsetData) {
        for (TagDefinition childDefinition : tagset.getDirectChildren(tag)) {
            tagsetData.addItem(new TagDataItem(tag), new TagDataItem(childDefinition));
            addTagSubTree(tagset, childDefinition, tagsetData);
        }
    }
	private void initActions() {
		tagsetGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getColor(), new HtmlRenderer())
			.setCaption("Name")
			.setExpandRatio(1);
		tagsetGrid.setHierarchyColumn(
			tagsetGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getName())
			
			.setCaption("Tags")
			.setExpandRatio(2));

		ButtonRenderer<TagsetTreeItem> propertySummaryRenderer = 
				new ButtonRenderer<>(rendererClickEvent -> handlePropertySummaryClickEvent(rendererClickEvent));
		propertySummaryRenderer.setHtmlContentAllowed(true); //TODO: handle property summary js and html injections!
		
		tagsetGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getPropertySummary(), 
			propertySummaryRenderer)
		.setCaption("Properties")
		.setExpandRatio(2);
		
		ButtonRenderer<TagsetTreeItem> visibilityRenderer = 
			new ButtonRenderer<TagsetTreeItem>(rendererClickEvent -> handleVisibilityClickEvent(rendererClickEvent));
		visibilityRenderer.setHtmlContentAllowed(true);
		tagsetGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getVisibilityIcon(), 
			visibilityRenderer);
		
		tagsetGrid.setStyleGenerator(new StyleGenerator<TagsetTreeItem>() {
			
			@Override
			public String apply(TagsetTreeItem item) {
				return item.generateStyle();
			}
		});
		
        ContextMenu addContextMenu = 
        		tagsetGridComponent.getActionGridBar().getBtnAddContextMenu();
        addContextMenu.addItem("Add Tag", clickEvent -> handleAddTagRequest());
        addContextMenu.addItem("Add Subtag", clickEvent -> handleAddSubtagRequest());
        addContextMenu.addItem("Add Property", clickEvent -> handleAddPropertyRequest());
		
		
		currentEditableCollectionBox.setEmptySelectionCaption("Please select or create a Collection...");
	}

	private void handleAddPropertyRequest() {
		// TODO Auto-generated method stub
	}

	private void handleAddSubtagRequest() {
		// TODO Auto-generated method stub
	}

	private void handleAddTagRequest() {
		
		Optional<TagsetDefinition> selectedTagset = tagsetGrid.getSelectedItems()
			.stream()
			.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagsetDataItem)
			.findFirst()
			.map(tagsetTreeItem -> ((TagsetDataItem)tagsetTreeItem).getTagset());
			
		AddTagDialog addTagDialog = 
				new AddTagDialog(tagsets, selectedTagset, new SaveCancelListener<TagDefinition>() {
					
					@Override
					public void savePressed(TagDefinition result) {
						
						
					}
				});
		addTagDialog.show();
		
	}

	@SuppressWarnings("unchecked")
	private void handlePropertySummaryClickEvent(RendererClickEvent<TagsetTreeItem> rendererClickEvent) {
		if (rendererClickEvent.getItem() instanceof TagDataItem) {
			TagDataItem tagDataItem = (TagDataItem) rendererClickEvent.getItem();
			
			tagDataItem.setPropertiesExpanded(!tagDataItem.isPropertiesExpanded());
			
			if (tagDataItem.isPropertiesExpanded()) {
				TagDefinition tag = tagDataItem.getTag();
				
				for (PropertyDefinition propertyDefinition : tag.getUserDefinedPropertyDefinitions()) {
					tagsetGrid.getTreeData().addItem(tagDataItem, new PropertyDataItem(propertyDefinition));
				}
				
				tagsetGrid.expand(tagDataItem);
			}
			else {
				TreeData<TagsetTreeItem> tagsetTreeData = tagsetGrid.getTreeData();
				
				for (TagsetTreeItem childTagsetTreeItem : new ArrayList<>(tagsetTreeData.getChildren(tagDataItem))) {
					childTagsetTreeItem.removePropertyDataItem(
							(TreeDataProvider<TagsetTreeItem>)tagsetGrid.getDataProvider());
				}
			}
			tagsetGrid.getDataProvider().refreshAll();
		}
	}

	@SuppressWarnings("unchecked")
	private void handleVisibilityClickEvent(RendererClickEvent<TagsetTreeItem> rendererClickEvent) {
		rendererClickEvent.getItem().setVisible(!rendererClickEvent.getItem().isVisible());
		tagsetGrid.getDataProvider().refreshItem(rendererClickEvent.getItem());
		
		TagsetTreeItem tagsetTreeItem = rendererClickEvent.getItem();
		List<TagReference> tagReferences = tagsetTreeItem.getTagReferences(collections);
		
		boolean selected = rendererClickEvent.getItem().isVisible();
		
		if (selectionListener != null) {
			selectionListener.tagReferenceSelectionChanged(tagReferences, selected);
		}
		
		tagsetTreeItem.setChildrenVisible(
			(TreeDataProvider<TagsetTreeItem>)tagsetGrid.getDataProvider(), selected, false);
	}

	private void initComponents() {
		setSizeFull();
		setSpacing(true);
		
		currentEditableCollectionBox = new ComboBox<>("Collection currently being edited");
		currentEditableCollectionBox.setWidth("100%");
		
		addCollectionButton = new IconButton(VaadinIcons.PLUS);
		
		HorizontalLayout editableCollectionPanel = 
				new HorizontalLayout(currentEditableCollectionBox, addCollectionButton);
		editableCollectionPanel.addStyleName("annotate-right-padding");
		
		editableCollectionPanel.setWidth("100%");
		editableCollectionPanel.setExpandRatio(currentEditableCollectionBox, 1.0f);
		editableCollectionPanel.setComponentAlignment(addCollectionButton, Alignment.BOTTOM_CENTER);
		
		addComponent(editableCollectionPanel);
		
		Label tagsetsLabel = new Label("Tagsets");
		
		tagsetGrid = new TreeGrid<>();
		tagsetGrid.addStyleName("annotate-tagsets-grid");
		tagsetGrid.setSizeFull();
		tagsetGrid.setSelectionMode(SelectionMode.SINGLE);
		tagsetGrid.addStyleName(MaterialTheme.GRID_BORDERLESS);

        tagsetGridComponent = new ActionGridComponent<TreeGrid<TagsetTreeItem>>(
                tagsetsLabel,
                tagsetGrid
        );
        
        addComponent(tagsetGridComponent);
        setExpandRatio(tagsetGridComponent, 1.0f);
	}
	
	public void setData(Collection<TagsetDefinition> tagsets, List<UserMarkupCollection> collections) {
		this.tagsets = tagsets;
		this.collections = collections;
		initData();
	}
	
	public void setTagReferenceSelectionChangeListener(TagReferenceSelectionChangeListener selectionListener) {
		this.selectionListener = selectionListener;
	}
	
	public UserMarkupCollection getSelectedEditableCollection() {
		return currentEditableCollectionBox.getValue();
	}

	public void addCollection(UserMarkupCollection collection) {
		this.collections.add(collection);
		currentEditableCollectionBox.getDataProvider().refreshAll();	
	}
	
	private void removeCollection(UserMarkupCollection collection) {
		if ((currentEditableCollectionBox.getValue() != null) 
				&& currentEditableCollectionBox.getValue().equals(collection)) {
			currentEditableCollectionBox.setValue(null);
		}
		collections.remove(collection);
		currentEditableCollectionBox.getDataProvider().refreshAll();	
	}

	public void removeCollection(String collectionId) {
		collections
			.stream()
			.filter(collection -> collection.getUuid().equals(collectionId))
			.findFirst()
			.ifPresent(collection -> removeCollection(collection));
	}
	
	public void setTagsets(Collection<TagsetDefinition> tagsets) {
		tagsets
		.stream()
		.filter(tagset -> !this.tagsets.contains(tagset))
		.forEach(tagset -> addTagset(tagset));
		
		this.tagsets.stream()
		.filter(tagset -> !tagsets.contains(tagset))
		.collect(Collectors.toList())
		.stream()
		.forEach(tagset -> removeTagset(tagset));
	}

	public void addTagset(TagsetDefinition tagset) {
		tagsets.add(tagset);
		@SuppressWarnings("unchecked")
		TreeDataProvider<TagsetTreeItem> treeDataProvider =
			(TreeDataProvider<TagsetTreeItem>)tagsetGrid.getDataProvider();
		treeDataProvider.getTreeData().addRootItems(new TagsetDataItem(tagset));
		addTags(treeDataProvider.getTreeData(), tagset);
		expandTagsetDefinition(tagset);
		treeDataProvider.refreshAll();
	}
	
	public void removeTagset(TagsetDefinition tagset) {
		tagsets.remove(tagset);
		@SuppressWarnings("unchecked")
		TreeDataProvider<TagsetTreeItem> treeDataProvider =
			(TreeDataProvider<TagsetTreeItem>)tagsetGrid.getDataProvider();
		treeDataProvider.getTreeData().removeItem(new TagsetDataItem(tagset));
		treeDataProvider.refreshAll();
	}
}
