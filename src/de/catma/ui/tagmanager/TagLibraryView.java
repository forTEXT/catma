package de.catma.ui.tagmanager;

import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ItemStyleGenerator;

import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagsetDefinition;
import de.catma.core.util.ColorConverter;

public class TagLibraryView extends HorizontalLayout {
	
	private static enum TagTreePropertyName {
		caption,
		icon,
		color,
		;
	}
	
	private TagLibrary tagLibrary;
	private Tree tagTree;
	private Button btInsertTagset;
	private Button btRemoveTagset;
	private Button btEditTagset;
	private Button btInsertTag;
	private Button btRemoveTag;
	private Button btEditTag;
	private Button btInsertProperty;
	private Button btRemoveProperty;
	private Button btEditProperty;
	

	public TagLibraryView(TagLibrary tagLibrary, Application application) {
		super();
		this.tagLibrary = tagLibrary;
		initComponents(application);
	}
	
	private void initComponents(Application application) {
		setWidth("100%");
		Panel tagTreePanel = new Panel();
		float height = application.getMainWindow().getHeight()*70/100;
		tagTreePanel.setHeight(height, application.getMainWindow().getHeightUnits());
		
		addComponent(tagTreePanel);
		setExpandRatio(tagTreePanel, 2);
		
		tagTree = new TagTree();
		
		tagTree.setContainerDataSource(new HierarchicalContainer());
		tagTree.addContainerProperty(TagTreePropertyName.caption.name(), String.class, null);
		tagTree.addContainerProperty(TagTreePropertyName.icon.name(), Resource.class, null);
		tagTree.addContainerProperty(TagTreePropertyName.color.name(), String.class, null);
		
		tagTree.setItemCaptionPropertyId(TagTreePropertyName.caption.name());
		tagTree.setItemIconPropertyId(TagTreePropertyName.icon.name());
		tagTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_PROPERTY);
	
		tagTreePanel.addComponent(tagTree);
		
		for (TagsetDefinition tagsetDefinition : tagLibrary) {
			ClassResource tagsetIcon = 
					new ClassResource("ui/tagmanager/resources/grndiamd.gif", application);
			tagTree.addItem(tagsetDefinition);
			tagTree.getContainerProperty(
					tagsetDefinition, TagTreePropertyName.caption.name()).setValue(
							tagsetDefinition.getName());
			tagTree.getContainerProperty(
					tagsetDefinition, TagTreePropertyName.icon.name()).setValue(tagsetIcon);
			
			for (TagDefinition tagDefinition : tagsetDefinition) {
				if (!tagDefinition.getID().equals(TagDefinition.CATMA_BASE_TAG.getID())) {
					ClassResource tagIcon = 
							new ClassResource("ui/tagmanager/resources/reddiamd.gif", application);
	
					tagTree.addItem(tagDefinition);
					tagTree.getContainerProperty(
							tagDefinition, TagTreePropertyName.caption.name()).setValue(
									tagDefinition.getType());
					tagTree.getContainerProperty(
							tagDefinition, TagTreePropertyName.icon.name()).setValue(tagIcon);
					tagTree.getContainerProperty(
							tagDefinition, TagTreePropertyName.color.name()).setValue(tagDefinition.getColor());
					
					for (PropertyDefinition propertyDefinition : 
							tagDefinition.getUserDefinedPropertyDefinitions()) {
						
						ClassResource propertyIcon = 
								new ClassResource("ui/tagmanager/resources/ylwdiamd.gif", application);
						
						tagTree.addItem(propertyDefinition);
						tagTree.setParent(propertyDefinition, tagDefinition);
						tagTree.getContainerProperty(
								propertyDefinition, 
								TagTreePropertyName.caption.name()).setValue(
										propertyDefinition.getName());
						tagTree.getContainerProperty(
								propertyDefinition, 
								TagTreePropertyName.icon.name()).setValue(
										propertyIcon);
						tagTree.setChildrenAllowed(propertyDefinition, false);
					}
				}
			}
			
			tagTree.setItemStyleGenerator(new ItemStyleGenerator() {
				
				public String getStyle(Object itemId) {
					Property colorProperty = tagTree.getContainerProperty(
							itemId, TagTreePropertyName.color.name());
					
					if (colorProperty != null) {
						Object colorValue = colorProperty.getValue();
						if (colorValue != null) {
							ColorConverter colorConverter =
									new ColorConverter(Integer.valueOf(colorValue.toString()));
							
							return "catma-tag-color-" + colorConverter.toHex();
						}
					}
					return null;
				}
			});
			
			for (TagDefinition tagDefinition : tagsetDefinition) {
				String baseID = tagDefinition.getBaseID();
				TagDefinition parent = tagLibrary.getTagDefintion(baseID);
				if ((parent==null)||(parent.getID().equals(TagDefinition.CATMA_BASE_TAG.getID()))) {
					tagTree.setParent(tagDefinition, tagsetDefinition);
				}
				else {
					tagTree.setParent(tagDefinition, parent);
				}
			}
			
			for (TagDefinition tagDefinition : tagsetDefinition) {
				if (!tagTree.hasChildren(tagDefinition)) {
					tagTree.setChildrenAllowed(tagDefinition, false);
				}
			}
		}
		
		GridLayout buttonGrid = new GridLayout(1, 19);
		buttonGrid.setMargin(true);
		buttonGrid.setSpacing(true);
		buttonGrid.addStyleName("taglibrary-action-grid");
		
		Label tagsetLabel = new Label();
		tagsetLabel.setIcon(
				new ClassResource("ui/tagmanager/resources/grndiamd.gif", application));
		tagsetLabel.setCaption("Tagset");
		
		buttonGrid.addComponent(tagsetLabel);
		
		btInsertTagset = new Button("Insert Tagset");
		buttonGrid.addComponent(btInsertTagset);
		
		btRemoveTagset = new Button("Remove Tagset");
		buttonGrid.addComponent(btRemoveTagset);
		
		btEditTagset = new Button("Edit Tagset");
		buttonGrid.addComponent(btEditTagset);
		
		Label tagLabel = new Label();
		tagLabel.setIcon(
				new ClassResource("ui/tagmanager/resources/reddiamd.gif", application));
		tagLabel.setCaption("Tag");
		
		buttonGrid.addComponent(tagLabel, 0, 4, 0, 8 );
		buttonGrid.setComponentAlignment(tagLabel, Alignment.BOTTOM_LEFT);
		
		btInsertTag = new Button("Insert Tag");
		buttonGrid.addComponent(btInsertTag);
		
		btRemoveTag = new Button("Remove Tag");
		buttonGrid.addComponent(btRemoveTag);
		
		btEditTag = new Button("Edit Tag");
		buttonGrid.addComponent(btEditTag);
		
		Label propertyLabel = new Label();
		propertyLabel.setIcon(
				new ClassResource("ui/tagmanager/resources/ylwdiamd.gif", application));
		propertyLabel.setCaption("Property");
		
		
		buttonGrid.addComponent(propertyLabel, 0, 12, 0, 16);
		buttonGrid.setComponentAlignment(propertyLabel, Alignment.BOTTOM_LEFT);
		
		btInsertProperty = new Button("Insert Property");
		buttonGrid.addComponent(btInsertProperty);
		
		btRemoveProperty = new Button("Remove Property");
		buttonGrid.addComponent(btRemoveProperty);
		
		btEditProperty = new Button("Edit Property");
		buttonGrid.addComponent(btEditProperty);
		
		addComponent(buttonGrid);
		setExpandRatio(buttonGrid, 0);
	}

	public String getTagLibraryName() {
		return tagLibrary.getName();
	}
	
}
