package de.catma.ui.tagger;

import java.util.Collection;
import java.util.List;

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
	
	private static enum MarkupCollectionTreeProperty {
		caption("Markup Collections"), 
		icon("icon"),
		visible("Visible"),
		color("Tag Color"),
		;
		
		private String displayString;

		private MarkupCollectionTreeProperty(String displayString) {
			this.displayString = displayString;
		}
		
		@Override
		public String toString() {
			return displayString;
		}
		
	}
	
	private TreeTable markupTable;
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
		
		markupTable = new TreeTable();
		markupTable.setSizeFull();
		markupTable.setContainerDataSource(new HierarchicalContainer());
		
		markupTable.addContainerProperty(
				MarkupCollectionTreeProperty.caption, 
				String.class, null);
		
		markupTable.addContainerProperty(
				MarkupCollectionTreeProperty.icon, Resource.class, null);
		
		markupTable.addContainerProperty(
				MarkupCollectionTreeProperty.visible, 
				AbstractComponent.class, null);
		
		markupTable.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_PROPERTY);
		markupTable.setItemCaptionPropertyId(
				MarkupCollectionTreeProperty.caption);
		markupTable.setItemIconPropertyId(MarkupCollectionTreeProperty.icon);
		markupTable.addGeneratedColumn(
				MarkupCollectionTreeProperty.color, new ColorLabelColumnGenerator());

		markupTable.setVisibleColumns(
				new Object[] {
						MarkupCollectionTreeProperty.caption,
						MarkupCollectionTreeProperty.color,
						MarkupCollectionTreeProperty.visible});
		
		markupTable.addItem(
			new Object[] {userMarkupItem, new Label()}, userMarkupItem);
		
		markupTable.addItem(
			new Object[] {staticMarkupItem, new Label()}, staticMarkupItem );
		

		addComponent(markupTable);
	}

	private UserMarkupCollection getUserMarkupCollection(
			Object itemId) {
		
		Object parent = markupTable.getParent(itemId);
		while((parent!=null) 
				&& !(parent instanceof UserMarkupCollection)) {
			parent = markupTable.getParent(parent);
		}
		
		return (UserMarkupCollection)parent;
	}

	public void openUserMarkupCollection(
			UserMarkupCollection userMarkupCollection) {

		markupTable.addItem(
				new Object[] {userMarkupCollection, new Label()},
				userMarkupCollection);
		markupTable.setParent(userMarkupCollection, userMarkupItem);
		
		TagLibrary tagLibrary = userMarkupCollection.getTagLibrary();
		for (TagsetDefinition tagsetDefinition : tagLibrary) {
			ClassResource tagsetIcon = 
					new ClassResource(
						"ui/tagmanager/resources/grndiamd.gif", getApplication());

			markupTable.addItem(
					new Object[]{tagsetDefinition.getName(), 
							new Label()}, tagsetDefinition);
			markupTable.getContainerProperty(
				tagsetDefinition, MarkupCollectionTreeProperty.icon).setValue(
						tagsetIcon);
			markupTable.setParent(tagsetDefinition, userMarkupCollection);
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
				
				markupTable.addItem(
						new Object[]{
								tagDefinition.getType(), 
								createCheckbox(tagDefinition)},
						tagDefinition);
				markupTable.getContainerProperty(
						tagDefinition, MarkupCollectionTreeProperty.icon).setValue(
								tagIcon);

			}
		}
		for (TagDefinition tagDefinition : tagsetDefinition) {
			String baseID = tagDefinition.getBaseID();
			TagDefinition parent = tagsetDefinition.getTagDefinition(baseID);
			if ((parent==null)
					||(parent.getID().equals(
							TagDefinition.CATMA_BASE_TAG.getID()))) {
				markupTable.setParent(tagDefinition, tagsetDefinition);
			}
			else {
				markupTable.setParent(tagDefinition, parent);
			}
		}
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			if (!markupTable.hasChildren(tagDefinition)) {
				markupTable.setChildrenAllowed(tagDefinition, false);
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

				Collection<?> children = 
						markupTable.getChildren(tagDefinition);
				
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
					}
				}							
				tagDefinitionSelectionListener.tagDefinitionSelectionChanged(
						tagReferences, enabled);
			}

		});
		return cbShowTagInstances;
	}

}
