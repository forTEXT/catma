package de.catma.ui.tagger;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeTable;

import de.catma.tag.TagInstance;
import de.catma.ui.tagmanager.ColorLabelColumnGenerator;
import de.catma.util.Pair;

public class TagInstanceTree extends HorizontalLayout {
	
	static interface TagIntanceActionListener {
		public void removeTagInstances(List<String> tagInstanceIDs);
	}
	
	private static enum TagInstanceTreePropertyName {
		caption,
		icon,
		color,
		;
	}
	
	private TreeTable tagInstanceTree;
	private TagIntanceActionListener tagInstanceActionListener;
	private Button btRemoveTagInstance;

	public TagInstanceTree(TagIntanceActionListener tagInstanceActionListener) {
		this.tagInstanceActionListener = tagInstanceActionListener;
		initComponents();
		initActions();
	}

	private void initActions() {

		btRemoveTagInstance.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				TagInstance selectedItem = 
					(TagInstance) tagInstanceTree.getValue();
				
				if (selectedItem == null) {
					getWindow().showNotification(
						"Information", 
						"Please select on or more Tag Instances in the list first!");
				}
				else {
					List<String> tagInstanceIDs = new ArrayList<String>();
					tagInstanceIDs.add(selectedItem.getUuid());
					tagInstanceActionListener.removeTagInstances(tagInstanceIDs);
					tagInstanceTree.removeItem(selectedItem);
				}
			}
		});
		
	}

	private void initComponents() {
		tagInstanceTree = new TreeTable();
		tagInstanceTree.setImmediate(true);
		tagInstanceTree.setSizeFull();
		tagInstanceTree.setSelectable(true);
		tagInstanceTree.setMultiSelect(false);
		
		tagInstanceTree.setContainerDataSource(new HierarchicalContainer());
		tagInstanceTree.addContainerProperty(
				TagInstanceTreePropertyName.caption, String.class, null);
		tagInstanceTree.setColumnHeader(TagInstanceTreePropertyName.caption, "Tag Instance");
		
		tagInstanceTree.addContainerProperty(
				TagInstanceTreePropertyName.icon, Resource.class, null);

		tagInstanceTree.setItemCaptionPropertyId(TagInstanceTreePropertyName.caption);
		tagInstanceTree.setItemIconPropertyId(TagInstanceTreePropertyName.icon);
		tagInstanceTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_PROPERTY);
	
		tagInstanceTree.setVisibleColumns(
				new Object[] {
						TagInstanceTreePropertyName.caption});

		tagInstanceTree.addGeneratedColumn(
			TagInstanceTreePropertyName.color,
			new ColorLabelColumnGenerator(
				new ColorLabelColumnGenerator.TagInstanceTagDefinitionProvider()));
		
		tagInstanceTree.setColumnHeader(
				TagInstanceTreePropertyName.color, "Tag Color");
		
		addComponent(tagInstanceTree);
		setExpandRatio(tagInstanceTree, 1.0f);
		
		GridLayout buttonGrid = new GridLayout(1, 1);
		buttonGrid.setMargin(true);
		buttonGrid.setSpacing(true);
		
		btRemoveTagInstance = new Button("Remove Tag Instance");
		buttonGrid.addComponent(btRemoveTagInstance);
		
		addComponent(buttonGrid);
	}
	
	public void setTagInstances(List<Pair<String,TagInstance>> tagInstances) {
		tagInstanceTree.removeAllItems();
		for (Pair<String,TagInstance> ti : tagInstances) {
			ClassResource tagIcon = 
					new ClassResource(
						"ui/tagmanager/resources/reddiamd.gif", getApplication());
			tagInstanceTree.addItem(
					new Object[] {
						ti.getFirst(),
					},
					ti.getSecond());
			tagInstanceTree.getItem(ti.getSecond()).getItemProperty(
					TagInstanceTreePropertyName.icon).setValue(tagIcon);
		}
	}
	
}
