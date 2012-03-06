package de.catma.ui.tagger;

import java.util.List;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagsetDefinition;

public class MarkupCollectionsPanel extends VerticalLayout {
	
	public static interface TagDefinitionSelectionListener {
		public void tagDefinitionSelectionChanged(
				List<TagReference> tagReferences, boolean selected);
	} 
	
	private static enum MarkupCollectionTreeProperty {
		caption("Markup Collections")
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

	public MarkupCollectionsPanel(
			TagDefinitionSelectionListener tagDefinitionSelectionListener) {
		initComponents(tagDefinitionSelectionListener);
	}

	private void initComponents(
			final TagDefinitionSelectionListener tagDefinitionSelectionListener) {
		
		markupTable = new TreeTable();
		markupTable.setContainerDataSource(new HierarchicalContainer());
		
		markupTable.addContainerProperty(
				MarkupCollectionTreeProperty.caption, 
				String.class, null);
		markupTable.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_PROPERTY);
		markupTable.setItemCaptionPropertyId(
				MarkupCollectionTreeProperty.caption);
		markupTable.setVisibleColumns(
				new Object[] {MarkupCollectionTreeProperty.caption});
		
		markupTable.addItem(userMarkupItem).getItemProperty(
				MarkupCollectionTreeProperty.caption).setValue(userMarkupItem);
		markupTable.addItem(staticMarkupItem).getItemProperty(
				MarkupCollectionTreeProperty.caption).setValue(staticMarkupItem);
		
		markupTable.addGeneratedColumn("Show", new ColumnGenerator() {
			
			public Object generateCell(
					final Table source, final Object itemId, Object columnId) {
				
				if (itemId instanceof TagDefinition) {
					CheckBox cbShowTagInstances = new CheckBox();
					cbShowTagInstances.addListener(new ClickListener() {
						
						public void buttonClick(ClickEvent event) {
							boolean enabled = 
									event.getButton().booleanValue();

							
							UserMarkupCollection userMarkupCollection =
									getUserMarkupCollection(itemId);
							 
							List<TagReference> tagReferences =
									userMarkupCollection.getTagReferences(
											(TagDefinition)itemId);
							
							tagDefinitionSelectionListener.tagDefinitionSelectionChanged(
									tagReferences, enabled);
						}

					});
					return cbShowTagInstances;
				}
				
				
				return new Label();
			}
		});
		
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

		markupTable.addItem(userMarkupCollection).getItemProperty(
				MarkupCollectionTreeProperty.caption).setValue(
						userMarkupCollection.toString());
		markupTable.setParent(userMarkupCollection, userMarkupItem);
		
		TagLibrary tagLibrary = userMarkupCollection.getTagLibrary();
		for (TagsetDefinition tagsetDefinition : tagLibrary) {
			markupTable.addItem(tagsetDefinition).getItemProperty(
					MarkupCollectionTreeProperty.caption).setValue(
							tagsetDefinition.getName());
			markupTable.setParent(tagsetDefinition, userMarkupCollection);
			addTagDefinitions(tagsetDefinition);
		}
	}

	private void addTagDefinitions(TagsetDefinition tagsetDefinition) {
		for (TagDefinition tagDefinition : tagsetDefinition) {
			if (!tagDefinition.getID().equals
					(TagDefinition.CATMA_BASE_TAG.getID())) {
				markupTable.addItem(tagDefinition).getItemProperty(
						MarkupCollectionTreeProperty.caption).setValue(
								tagDefinition.getType());
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
	
	
}
