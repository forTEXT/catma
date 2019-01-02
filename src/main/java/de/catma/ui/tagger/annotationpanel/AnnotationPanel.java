package de.catma.ui.tagger.annotationpanel;

import java.util.Collection;
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
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.DetailsGenerator;
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

public class AnnotationPanel extends VerticalLayout {
	
	public interface TagReferenceSelectionChangeListener {
		public void tagReferenceSelectionChanged(
				List<TagReference> tagReferences, boolean selected);
	}
	
	private class PropertyDisplayItem {
		private String displayValue;
		private PropertyDefinition propertyDefinition;
		
		public PropertyDisplayItem(String displayValue, PropertyDefinition propertyDefinition) {
			super();
			this.displayValue = displayValue;
			this.propertyDefinition = propertyDefinition;
		}
		
		public void setDisplayValue(String displayValue) {
			this.displayValue = displayValue;
		}
		
		@Override
		public String toString() {
			return displayValue;
		}
	}
	
	private ComboBox<UserMarkupCollection> currentEditableCollectionBox;
	private Button tagsetsOptions;
	private Button addCollectionButton;
	private TreeGrid<TagTreeItem> tagsetsGrid;
	private Repository project;
	private Collection<TagsetDefinition> tagsets;
	private List<UserMarkupCollection> collections;
	private TagReferenceSelectionChangeListener selectionListener;

	public AnnotationPanel(Repository project) {
		this.project = project;
		initComponents();
		initActions();
	}

	private void initData() {
        try {
            TreeData<TagTreeItem> tagsetData = new TreeData<>();
            
            tagsetData.addRootItems(tagsets.stream().map(ts -> new TagsetDataItem(ts)));

            for (TagsetDefinition tagsetDefinition : tagsets) {
                for (TagDefinition tagDefinition : tagsetDefinition) {
                    if (tagDefinition.getParentUuid().isEmpty()) {
                        tagsetData.addItem(new TagsetDataItem(tagsetDefinition), new TagDataItem(tagDefinition));
                        addTagDefinitionSubTree(tagsetDefinition, tagDefinition, tagsetData);
                    }
                }
            }

            tagsetsGrid.setDataProvider(new TreeDataProvider<>(tagsetData));
            for (TagsetDefinition tagset : tagsets) {
            	for (TagDefinition tag : tagset) {
            		TagDataItem item = new TagDataItem(tag);
            		tagsetsGrid.expand(item);
            		if (!tag.getUserDefinedPropertyDefinitions().isEmpty()) {
            			tagsetsGrid.setDetailsVisible(item, true);
            		}
            	}
            }
            
            currentEditableCollectionBox.setDataProvider(new ListDataProvider<>(collections));
        } catch (Exception e) {
			// TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void addTagDefinitionSubTree(
    		TagsetDefinition tagsetDefinition, 
    		TagDefinition tagDefinition, TreeData<TagTreeItem> tagsetData) {
        for (TagDefinition childDefinition : tagsetDefinition.getDirectChildren(tagDefinition)) {
            tagsetData.addItem(new TagDataItem(tagDefinition), new TagDataItem(childDefinition));
            addTagDefinitionSubTree(tagsetDefinition, childDefinition, tagsetData);
        }
    }
	private void initActions() {
		tagsetsGrid.addColumn(tagTreeItem -> tagTreeItem.getColor(), new HtmlRenderer())
			.setCaption("Tagsets")
			.setExpandRatio(1);
		tagsetsGrid.setHierarchyColumn(
			tagsetsGrid.addColumn(tagTreeItem -> tagTreeItem.getName())
			.setCaption("Tags")
			.setExpandRatio(3));

		ButtonRenderer<TagTreeItem> visibilityRenderer = 
			new ButtonRenderer<TagTreeItem>(rendererClickEvent -> handleVisibilityClickEvent(rendererClickEvent));
		visibilityRenderer.setHtmlContentAllowed(true);
		tagsetsGrid.addColumn(
			tagTreeItem -> tagTreeItem.getVisibilityIcon(), 
			visibilityRenderer);
		
		currentEditableCollectionBox.setEmptySelectionCaption("Please select or create a Collection...");
	}

	@SuppressWarnings("unchecked")
	private void handleVisibilityClickEvent(RendererClickEvent<TagTreeItem> rendererClickEvent) {
		rendererClickEvent.getItem().setVisible(!rendererClickEvent.getItem().isVisible());
		tagsetsGrid.getDataProvider().refreshItem(rendererClickEvent.getItem());
		
		TagTreeItem tagTreeItem = rendererClickEvent.getItem();
		List<TagReference> tagReferences = tagTreeItem.getTagReferences(collections);
		
		boolean selected = rendererClickEvent.getItem().isVisible();
		
		if (selectionListener != null) {
			selectionListener.tagReferenceSelectionChanged(tagReferences, selected);
		}
		
		tagTreeItem.setChildrenVisible(
			(TreeDataProvider<TagTreeItem>)tagsetsGrid.getDataProvider(), selected, false);
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
		
//        ActionGridComponent<TreeGrid<TagTreeItem>> tagsetGridComponent = new ActionGridComponent<>(
//                tagsetsLabel,
//                tagsetsGrid
//        );
        
		addComponent(tagsetsGrid);
		setExpandRatio(tagsetsGrid, 1.0f);
//        addComponent(tagsetGridComponent);
//        setExpandRatio(tagsetGridComponent, 1.0f);
		tagsetsGrid.setDetailsGenerator(new DetailsGenerator<TagTreeItem>() {
			
			@Override
			public Component apply(TagTreeItem t) {
				if (t instanceof TagDataItem) {
					TagDefinition tag = ((TagDataItem)t).getTag();
					
					TreeData<PropertyDisplayItem> propertyData = new TreeData<>();
					
					final String parentItemDisplayString = tag.getName() + " Properties: " + tag.getUserDefinedPropertyDefinitions().stream()
					.limit(3)
					.map(property -> property.getName())
					.collect(Collectors.joining(","))
					+ ((tag.getUserDefinedPropertyDefinitions().size() > 3)?"...":"");
					
					final PropertyDisplayItem parent = new PropertyDisplayItem(parentItemDisplayString, null);
					propertyData.addRootItems(parent);
					
					for (PropertyDefinition propertyDefinition : tag.getUserDefinedPropertyDefinitions()) {
						propertyData.addItem(
							parent, 
							new PropertyDisplayItem(propertyDefinition.getName(), propertyDefinition));
					}
					
					Tree<PropertyDisplayItem> properties = new Tree<>();
					properties.addExpandListener(expandEvent -> {
						parent.setDisplayValue(tag.getName() + " Properties");
					});
					
					
					properties.addCollapseListener(collapseEvent -> {
						parent.setDisplayValue(parentItemDisplayString);
					});
					
					properties.setSelectionMode(SelectionMode.NONE);
					properties.addStyleName("annotate-properties-details-tree");
					properties.setDataProvider(new TreeDataProvider<>(propertyData));
					return properties;
				}
				return null;
			}
		});		
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
	
}
