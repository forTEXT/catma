package de.catma.ui.tagger;

import java.util.Collection;
import java.util.List;

import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagsetDefinition;
import de.catma.ui.tagmanager.ColorLabelColumnGenerator;

public class MarkupCollectionsPanel extends VerticalLayout {
	
	public static interface TagDefinitionSelectionListener {
		public void tagDefinitionSelectionChanged(
				List<TagReference> tagReferences, boolean selected);
	} 
	
	private static enum MarkupCollectionsTreeProperty {
		caption("Markup Collections"), 
		icon("icon"),
		visible("Visible"),
		color("Tag Color"),
		;
		
		private String displayString;

		private MarkupCollectionsTreeProperty(String displayString) {
			this.displayString = displayString;
		}
		
		@Override
		public String toString() {
			return displayString;
		}
		
	}
	
	private TreeTable markupCollectionsTree;
	private String userMarkupItem = "User Markup Collections";
	private String staticMarkupItem = "Static Markup Collections";
	private TagDefinitionSelectionListener tagDefinitionSelectionListener;

	public MarkupCollectionsPanel(
			TagDefinitionSelectionListener tagDefinitionSelectionListener) {
		this.tagDefinitionSelectionListener = tagDefinitionSelectionListener;
		initComponents(tagDefinitionSelectionListener);
	}

	private void initComponents(
			final TagDefinitionSelectionListener tagDefinitionSelectionListener) {
		
		markupCollectionsTree = new TreeTable();
		markupCollectionsTree.setSizeFull();
		markupCollectionsTree.setContainerDataSource(new HierarchicalContainer());
		
		markupCollectionsTree.addContainerProperty(
				MarkupCollectionsTreeProperty.caption, 
				String.class, null);
		
		markupCollectionsTree.addContainerProperty(
				MarkupCollectionsTreeProperty.icon, Resource.class, null);
		
		markupCollectionsTree.addContainerProperty(
				MarkupCollectionsTreeProperty.visible, 
				AbstractComponent.class, null);
		
		markupCollectionsTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_PROPERTY);
		markupCollectionsTree.setItemCaptionPropertyId(
				MarkupCollectionsTreeProperty.caption);
		markupCollectionsTree.setItemIconPropertyId(MarkupCollectionsTreeProperty.icon);
		markupCollectionsTree.addGeneratedColumn(
				MarkupCollectionsTreeProperty.color, new ColorLabelColumnGenerator());

		markupCollectionsTree.setVisibleColumns(
				new Object[] {
						MarkupCollectionsTreeProperty.caption,
						MarkupCollectionsTreeProperty.color,
						MarkupCollectionsTreeProperty.visible});
		
		markupCollectionsTree.addItem(
			new Object[] {userMarkupItem, new Label()}, userMarkupItem);
		
		markupCollectionsTree.addItem(
			new Object[] {staticMarkupItem, new Label()}, staticMarkupItem );
		

		addComponent(markupCollectionsTree);
	}

	private UserMarkupCollection getUserMarkupCollection(
			Object itemId) {
		
		Object parent = markupCollectionsTree.getParent(itemId);
		while((parent!=null) 
				&& !(parent instanceof UserMarkupCollection)) {
			parent = markupCollectionsTree.getParent(parent);
		}
		
		return (UserMarkupCollection)parent;
	}

	public void openUserMarkupCollection(
			UserMarkupCollection userMarkupCollection) {

		markupCollectionsTree.addItem(
				new Object[] {userMarkupCollection, new Label()},
				userMarkupCollection);
		markupCollectionsTree.setParent(userMarkupCollection, userMarkupItem);
		
		TagLibrary tagLibrary = userMarkupCollection.getTagLibrary();
		for (TagsetDefinition tagsetDefinition : tagLibrary) {
			ClassResource tagsetIcon = 
					new ClassResource(
						"ui/tagmanager/resources/grndiamd.gif", getApplication());

			markupCollectionsTree.addItem(
					new Object[]{tagsetDefinition.getName(), 
							new Label()}, tagsetDefinition);
			markupCollectionsTree.getContainerProperty(
				tagsetDefinition, MarkupCollectionsTreeProperty.icon).setValue(
						tagsetIcon);
			markupCollectionsTree.setParent(tagsetDefinition, userMarkupCollection);
			addTagDefinitions(tagsetDefinition);
		}
	}
	
	private void addTagDefinitions(TagsetDefinition tagsetDefinition) {
		for (TagDefinition tagDefinition : tagsetDefinition) {
			if (!tagDefinition.getID().equals
					(TagDefinition.CATMA_BASE_TAG.getID())) {
				ClassResource tagIcon = 
						new ClassResource(
							"ui/tagmanager/resources/reddiamd.gif", 
						getApplication());
				
				markupCollectionsTree.addItem(
						new Object[]{
								tagDefinition.getType(), 
								createCheckbox(tagDefinition)},
						tagDefinition);
				markupCollectionsTree.getContainerProperty(
						tagDefinition, MarkupCollectionsTreeProperty.icon).setValue(
								tagIcon);

			}
		}
		for (TagDefinition tagDefinition : tagsetDefinition) {
			String baseID = tagDefinition.getBaseID();
			TagDefinition parent = tagsetDefinition.getTagDefinition(baseID);
			if ((parent==null)
					||(parent.getID().equals(
							TagDefinition.CATMA_BASE_TAG.getID()))) {
				markupCollectionsTree.setParent(tagDefinition, tagsetDefinition);
			}
			else {
				markupCollectionsTree.setParent(tagDefinition, parent);
			}
		}
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			if (!markupCollectionsTree.hasChildren(tagDefinition)) {
				markupCollectionsTree.setChildrenAllowed(tagDefinition, false);
			}
		}
	}
	
	private CheckBox createCheckbox(final TagDefinition tagDefinition) {
		CheckBox cbShowTagInstances = new CheckBox();
		cbShowTagInstances.setImmediate(true);
		cbShowTagInstances.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				boolean enabled = 
						event.getButton().booleanValue();
				System.out.println(tagDefinition + " enabled:" + enabled);
				Collection<?> children = 
						markupCollectionsTree.getChildren(tagDefinition);
				
				UserMarkupCollection userMarkupCollection =
						getUserMarkupCollection(tagDefinition);
				 
				List<TagReference> tagReferences =
						userMarkupCollection.getTagReferences(
								(TagDefinition)tagDefinition);
				
				if (children != null) {
					for (Object childId : children) {
						if (childId instanceof TagDefinition) {
							tagReferences.addAll(
								userMarkupCollection.getTagReferences(
										(TagDefinition)childId));
						}
						Object visiblePropertyValue = 
							markupCollectionsTree.getItem(
								childId).getItemProperty(
									MarkupCollectionsTreeProperty.visible).getValue();
						
						if ((visiblePropertyValue != null) 
								&& (visiblePropertyValue instanceof CheckBox)) {
							CheckBox cbVisible = (CheckBox)visiblePropertyValue;
							cbVisible.setValue(enabled);
						}
					}
				}							
				tagDefinitionSelectionListener.tagDefinitionSelectionChanged(
						tagReferences, enabled);
			}

		});
		return cbShowTagInstances;
	}

}
