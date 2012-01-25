package de.catma.ui.tagmanager;

import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ItemStyleGenerator;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagsetDefinition;
import de.catma.core.util.ColorConverter;

public class TagLibraryView extends VerticalLayout {
	
	private static enum TagTreePropertyName {
		caption,
		icon,
		color,
		;
	}
	
	private TagLibrary tagLibrary;
	private Tree tagTree;
	

	public TagLibraryView(TagLibrary tagLibrary, Application application) {
		super();
		this.tagLibrary = tagLibrary;
		initComponents(application);
	}
	
	private void initComponents(Application application) {
		
		tagTree = new TagTree();
		
		tagTree.setContainerDataSource(new HierarchicalContainer());
		tagTree.addContainerProperty(TagTreePropertyName.caption.name(), String.class, null);
		tagTree.addContainerProperty(TagTreePropertyName.icon.name(), Resource.class, null);
		tagTree.addContainerProperty(TagTreePropertyName.color.name(), String.class, null);
		
		tagTree.setItemCaptionPropertyId(TagTreePropertyName.caption.name());
		tagTree.setItemIconPropertyId(TagTreePropertyName.icon.name());
		tagTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_PROPERTY);
	
		addComponent(tagTree);
		
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
		
	}

	public String getTagLibraryName() {
		return tagLibrary.getName();
	}
	
}
