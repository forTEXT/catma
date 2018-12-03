package de.catma.ui.tagger.annotationpanel;

import java.util.Collection;
import java.util.stream.Collectors;

import com.vaadin.data.TreeData;
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
import com.vaadin.ui.themes.ValoTheme;

import de.catma.document.repository.Repository;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

public class AnnotationPanel extends VerticalLayout {
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

	public AnnotationPanel(Repository project) {
		this.project = project;
		initComponents();
		initActions();
		initData();
	}

	private void initData() {
        try {
            Collection<TagsetDefinition> tagsets = project.getTagsets();
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
        } catch (Exception e) {
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
		tagsetsGrid.addColumn(tagTreeItem -> tagTreeItem.getColor(), new HtmlRenderer()).setExpandRatio(1);
		tagsetsGrid.setHierarchyColumn(
			tagsetsGrid.addColumn(tagTreeItem -> tagTreeItem.getName())
			.setExpandRatio(3));
		
//		tagsetsGrid.addColumn(tagTreeItem -> tagTreeItem.getName())
//		.setExpandRatio(3)
//		.setCaption("Name");
		ButtonRenderer<TagTreeItem> visibilityRenderer = 
			new ButtonRenderer<TagTreeItem>(rendererClickEvent -> handleVisibilityClickEvent(rendererClickEvent));
		visibilityRenderer.setHtmlContentAllowed(true);
		tagsetsGrid.addColumn(
			tagTreeItem -> tagTreeItem.getVisibilityIcon(), 
			visibilityRenderer);
		
		currentEditableCollectionBox.setEmptySelectionCaption("Please select or create a Collection...");
	}

	private void handleVisibilityClickEvent(RendererClickEvent<TagTreeItem> rendererClickEvent) {
		System.out.println(rendererClickEvent.getItem());
	}

	private void initComponents() {
		setSizeFull();
		setSpacing(true);
		
		currentEditableCollectionBox = new ComboBox<>("Collection currently being edited");
		currentEditableCollectionBox.setWidth("100%");
		
		addCollectionButton = new Button(VaadinIcons.PLUS);
		addCollectionButton.addStyleName(ValoTheme.BUTTON_LINK);
		
		HorizontalLayout editableCollectionPanel = 
				new HorizontalLayout(currentEditableCollectionBox, addCollectionButton);
		editableCollectionPanel.addStyleName("annotate-right-padding");
		
		editableCollectionPanel.setWidth("100%");
		editableCollectionPanel.setExpandRatio(currentEditableCollectionBox, 1.0f);
		editableCollectionPanel.setComponentAlignment(addCollectionButton, Alignment.BOTTOM_CENTER);
		
		addComponent(editableCollectionPanel);
		
		
		tagsetsOptions = new Button(VaadinIcons.OPTIONS);
		tagsetsOptions.addStyleName(ValoTheme.BUTTON_LINK);
		
		Label tagsetsLabel = new Label("Tagsets");
		HorizontalLayout tagsetsHeader = 
				new HorizontalLayout(tagsetsLabel, tagsetsOptions);
		tagsetsHeader.addStyleName("annotate-right-padding");
		tagsetsHeader.setWidth("100%");
		tagsetsHeader.setSpacing(true);
		tagsetsHeader.setComponentAlignment(tagsetsOptions, Alignment.MIDDLE_RIGHT);
		tagsetsHeader.setExpandRatio(tagsetsLabel, 1.0f);
		addComponent(tagsetsHeader);
		
		tagsetsGrid = new TreeGrid<>();
		tagsetsGrid.addStyleName("annotate-tagsets-grid");
		tagsetsGrid.setSizeFull();
		tagsetsGrid.setSelectionMode(SelectionMode.SINGLE);
		
		addComponent(tagsetsGrid);
		setExpandRatio(tagsetsGrid, 1.0f);
		tagsetsGrid.setDetailsGenerator(new DetailsGenerator<TagTreeItem>() {
			
			@Override
			public Component apply(TagTreeItem t) {
				if (t instanceof TagDataItem) {
					TagDefinition tag = ((TagDataItem)t).getTag();
					
					TreeData<PropertyDisplayItem> propertyData = new TreeData<>();
					
					final String parentItemDisplayString = "Properties: " + tag.getUserDefinedPropertyDefinitions().stream()
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
						parent.setDisplayValue("Properties");
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
	
}
