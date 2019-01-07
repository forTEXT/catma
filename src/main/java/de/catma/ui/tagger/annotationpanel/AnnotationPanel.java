package de.catma.ui.tagger.annotationpanel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.appreciated.material.MaterialTheme;
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

public class AnnotationPanel extends VerticalLayout {
	
	
	public interface TagReferenceSelectionChangeListener {
		public void tagReferenceSelectionChanged(
				List<TagReference> tagReferences, boolean selected);
	}
	
	private ComboBox<UserMarkupCollection> currentEditableCollectionBox;
	private Button tagsetsOptions;
	private Button addCollectionButton;
	private TreeGrid<TagsetTreeItem> tagsetsGrid;
	private Repository project;
	private Collection<TagsetDefinition> tagsets = Collections.emptyList();
	private List<UserMarkupCollection> collections = Collections.emptyList();
	private TagReferenceSelectionChangeListener selectionListener;

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

            tagsetsGrid.setDataProvider(new TreeDataProvider<>(tagsetData));
            for (TagsetDefinition tagset : tagsets) {
            	expandTagsetDefinition(tagset);
            }
            
            currentEditableCollectionBox.setDataProvider(new ListDataProvider<>(collections));
        } catch (Exception e) {
			// TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void expandTagsetDefinition(TagsetDefinition tagset) {
    	for (TagDefinition tag : tagset) {
    		TagDataItem item = new TagDataItem(tag);
    		tagsetsGrid.expand(item);
    		if (!tag.getUserDefinedPropertyDefinitions().isEmpty()) {
    			tagsetsGrid.setDetailsVisible(item, true);
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
		tagsetsGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getColor(), new HtmlRenderer())
			.setCaption("Name")
			.setExpandRatio(1);
		tagsetsGrid.setHierarchyColumn(
			tagsetsGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getName())
			
			.setCaption("Tags")
			.setExpandRatio(2));

		ButtonRenderer<TagsetTreeItem> propertySummaryRenderer = 
				new ButtonRenderer<>(rendererClickEvent -> handlePropertySummaryClickEvent(rendererClickEvent));
		propertySummaryRenderer.setHtmlContentAllowed(true); //TODO: handle property summary js and html injections!
		
		tagsetsGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getPropertySummary(), 
			propertySummaryRenderer)
		.setCaption("Properties")
		.setExpandRatio(2);
		
		ButtonRenderer<TagsetTreeItem> visibilityRenderer = 
			new ButtonRenderer<TagsetTreeItem>(rendererClickEvent -> handleVisibilityClickEvent(rendererClickEvent));
		visibilityRenderer.setHtmlContentAllowed(true);
		tagsetsGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getVisibilityIcon(), 
			visibilityRenderer);
		
		tagsetsGrid.setStyleGenerator(new StyleGenerator<TagsetTreeItem>() {
			
			@Override
			public String apply(TagsetTreeItem item) {
				return item.generateStyle();
			}
		});
		
		currentEditableCollectionBox.setEmptySelectionCaption("Please select or create a Collection...");
	}

	@SuppressWarnings("unchecked")
	private void handlePropertySummaryClickEvent(RendererClickEvent<TagsetTreeItem> rendererClickEvent) {
		if (rendererClickEvent.getItem() instanceof TagDataItem) {
			TagDataItem tagDataItem = (TagDataItem) rendererClickEvent.getItem();
			
			tagDataItem.setPropertiesExpanded(!tagDataItem.isPropertiesExpanded());
			
			if (tagDataItem.isPropertiesExpanded()) {
				TagDefinition tag = tagDataItem.getTag();
				
				for (PropertyDefinition propertyDefinition : tag.getUserDefinedPropertyDefinitions()) {
					tagsetsGrid.getTreeData().addItem(tagDataItem, new PropertyDataItem(propertyDefinition));
				}
				
				tagsetsGrid.expand(tagDataItem);
			}
			else {
				TreeData<TagsetTreeItem> tagsetTreeData = tagsetsGrid.getTreeData();
				
				for (TagsetTreeItem childTagsetTreeItem : new ArrayList<>(tagsetTreeData.getChildren(tagDataItem))) {
					childTagsetTreeItem.removePropertyDataItem(
							(TreeDataProvider<TagsetTreeItem>)tagsetsGrid.getDataProvider());
				}
			}
			tagsetsGrid.getDataProvider().refreshAll();
		}
	}

	@SuppressWarnings("unchecked")
	private void handleVisibilityClickEvent(RendererClickEvent<TagsetTreeItem> rendererClickEvent) {
		rendererClickEvent.getItem().setVisible(!rendererClickEvent.getItem().isVisible());
		tagsetsGrid.getDataProvider().refreshItem(rendererClickEvent.getItem());
		
		TagsetTreeItem tagsetTreeItem = rendererClickEvent.getItem();
		List<TagReference> tagReferences = tagsetTreeItem.getTagReferences(collections);
		
		boolean selected = rendererClickEvent.getItem().isVisible();
		
		if (selectionListener != null) {
			selectionListener.tagReferenceSelectionChanged(tagReferences, selected);
		}
		
		tagsetTreeItem.setChildrenVisible(
			(TreeDataProvider<TagsetTreeItem>)tagsetsGrid.getDataProvider(), selected, false);
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
		
		tagsetsOptions = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
//		tagsetsOptions.addStyleName(ValoTheme.BUTTON_LINK);
		
		Label tagsetsLabel = new Label("Tagsets");
//		HorizontalLayout tagsetsHeader = 
//				new HorizontalLayout(tagsetsLabel, tagsetsOptions);
//		tagsetsHeader.addStyleName("annotate-right-padding");
//		tagsetsHeader.setWidth("100%");
//		tagsetsHeader.setSpacing(true);
//		tagsetsHeader.setComponentAlignment(tagsetsOptions, Alignment.MIDDLE_RIGHT);
//		tagsetsHeader.setExpandRatio(tagsetsLabel, 1.0f);
//		addComponent(tagsetsHeader);
		
		tagsetsGrid = new TreeGrid<>();
		tagsetsGrid.addStyleName("annotate-tagsets-grid");
		tagsetsGrid.setSizeFull();
		tagsetsGrid.setSelectionMode(SelectionMode.SINGLE);
		tagsetsGrid.addStyleName(MaterialTheme.GRID_BORDERLESS);

        ActionGridComponent<TreeGrid<TagsetTreeItem>> tagsetGridComponent = new ActionGridComponent<>(
                tagsetsLabel,
                tagsetsGrid
        );
        
//		addComponent(tagsetsGrid);
//		setExpandRatio(tagsetsGrid, 1.0f);
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
			(TreeDataProvider<TagsetTreeItem>)tagsetsGrid.getDataProvider();
		treeDataProvider.getTreeData().addRootItems(new TagsetDataItem(tagset));
		addTags(treeDataProvider.getTreeData(), tagset);
		expandTagsetDefinition(tagset);
		treeDataProvider.refreshAll();
	}
	
	public void removeTagset(TagsetDefinition tagset) {
		tagsets.remove(tagset);
		@SuppressWarnings("unchecked")
		TreeDataProvider<TagsetTreeItem> treeDataProvider =
			(TreeDataProvider<TagsetTreeItem>)tagsetsGrid.getDataProvider();
		treeDataProvider.getTreeData().removeItem(new TagsetDataItem(tagset));
		treeDataProvider.refreshAll();
	}
}
