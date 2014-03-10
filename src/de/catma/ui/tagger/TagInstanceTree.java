/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.tagger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.Window.Notification;

import de.catma.document.standoffmarkup.usermarkup.TagInstanceInfo;
import de.catma.tag.Property;
import de.catma.tag.PropertyValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.ui.dialog.FormDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.StringListProperty;
import de.catma.ui.tagmanager.ColorLabelColumnGenerator;

public class TagInstanceTree extends HorizontalLayout {
	
	static interface TagIntanceActionListener {
		public void removeTagInstances(List<String> tagInstanceIDs);
		public void updateProperty(TagInstance tagInstance, Property property);		
	}
	
	private static enum TagInstanceTreePropertyName {
		caption,
		icon,
		color,
		path,
		instanceId, 
		umc,
		;
	}
	
	private TreeTable tagInstanceTree;
	private TagIntanceActionListener tagInstanceActionListener;
	private Button btRemoveTagInstance;
	private Button btEditPropertyValues;

	public TagInstanceTree(TagIntanceActionListener tagInstanceActionListener) {
		this.tagInstanceActionListener = tagInstanceActionListener;
		initComponents();
		initActions();
	}

	private void initActions() {
		
		btRemoveTagInstance.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Object selItem = tagInstanceTree.getValue();
				
				final List<TagInstance> selectedItems = 
						getTagInstance(selItem);
				
				if (selectedItems.isEmpty()) {
					getWindow().showNotification(
						"Information", 
						"Please select one or more Tag Instances in the list first!",
						Notification.TYPE_TRAY_NOTIFICATION);
				}
				else {
					ConfirmDialog.show(getApplication().getMainWindow(), 
							"Remove Tag Instances", 
							"Do you want to remove the selected Tag Instances?", 
							"Yes", "No", new ConfirmDialog.Listener() {
						public void onClose(ConfirmDialog dialog) {
							if (dialog.isConfirmed()) {
								List<String> tagInstanceIDs = new ArrayList<String>();
								for (TagInstance ti : selectedItems) {
									tagInstanceIDs.add(ti.getUuid());
									removeTagInstanceFromTree(ti);
								}
								tagInstanceActionListener.removeTagInstances(
										tagInstanceIDs);
							}
						}

					});
				}
			}
		});
		
		btEditPropertyValues.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Object selection = tagInstanceTree.getValue();
				final Property property = getProperty((Set<?>)selection);
				final TagInstance tagInstance = (TagInstance) tagInstanceTree.getParent(property);
				
				if ((((Set<?>)selection).size()>1) || (property == null)) {
					getWindow().showNotification(
						"Information", 
						"Please select excactly one Property from the list first!",
						Notification.TYPE_TRAY_NOTIFICATION);
				}
				else {
					final String valuesProp = "values";
					PropertysetItem propertyCollection = new PropertysetItem();
					propertyCollection.addItemProperty(valuesProp, new StringListProperty());
					Set<String> initialValues = new HashSet<String>();
					initialValues.addAll(
						property.getPropertyDefinition().getPossibleValueList().getPropertyValueList().getValues());
					initialValues.addAll(
						property.getPropertyValueList().getValues());
					propertyCollection.getItemProperty(valuesProp).setValue(
							property.getPropertyValueList().getValues());
					FormDialog<PropertysetItem> editValueDlg = new FormDialog<PropertysetItem>(
							"Edit Property values", 
							"New property values created here exist only for this tag instance! " +
							"For the creation of new systematic values use the Tag Manager.",
							propertyCollection, 
							new PropertyValueEditorFormFieldFactory(
								initialValues),
							new SaveCancelListener<PropertysetItem>() {
								public void cancelPressed() {}
								public void savePressed(PropertysetItem result) {
									
									StringListProperty stringList =
											(StringListProperty) result.getItemProperty(valuesProp);
									
									property.setPropertyValueList(
										new PropertyValueList(stringList.getList()));
									// update prop values
									// update prop value index
									// (handle deletion of prop defs, update should be fine, needs testing) 
									
									tagInstanceActionListener.updateProperty(tagInstance, property);
								}
							});
					editValueDlg.show(getApplication().getMainWindow());
				}	
			}
		});
		
		tagInstanceTree.addListener(new ItemClickEvent.ItemClickListener() {
			
			public void itemClick(ItemClickEvent event) {
				Object selection = tagInstanceTree.getValue();
//				final Property property = getProperty((Set<?>)selection);
				final TagInstance tagInstance = getTagInstance((Set<?>)selection);
				
				if ((event.isDoubleClick()) && (tagInstance != null)){
					PropertyEditDialog dialog = 
							new PropertyEditDialog("Edit Properties for Tag "+tagInstance.getTagDefinition().getName(),
									tagInstance,
									new SaveCancelListener<Set<Property>>() {
										public void cancelPressed() {}
										public void savePressed(Set<Property> list) {
											
										}
									});
					dialog.show(getApplication().getMainWindow());
				}
				
			}
		});

	}	

	private TagInstance getTagInstance(Set<?> selection) {
		if (selection.iterator().hasNext()) {
			Object selVal = selection.iterator().next();
			while ((selVal != null) && !(selVal instanceof TagInstance)) {
				selVal = tagInstanceTree.getParent(selVal);
			}
			return (TagInstance)selVal;
		}
		return null;
	}
	
	private Property getProperty(Set<?> selection) {
		if (selection.iterator().hasNext()) {
			Object selVal = selection.iterator().next();
			while ((selVal != null) && !(selVal instanceof Property)) {
				selVal = tagInstanceTree.getParent(selVal);
			}
			return (Property)selVal;
		}
		return null;
	}

	private void removeTagInstanceFromTree(TagInstance ti) {
		for (Property p : ti.getUserDefinedProperties()) {
			for (String value : p.getPropertyValueList().getValues()) {
				tagInstanceTree.removeItem(String.valueOf(p.hashCode())+value);
			}
			tagInstanceTree.removeItem(p);
		}
		tagInstanceTree.removeItem(ti);
	}
	
	
	
	private List<TagInstance> getTagInstance(Object selection) {
		List<TagInstance> selectedTagInstances = new ArrayList<TagInstance>();
		if (selection != null) {
			Set<?> selectedValues = (Set<?>)selection;
			for (Object selValue : selectedValues) {
				while (tagInstanceTree.getParent(selValue) != null) {
					selValue = tagInstanceTree.getParent(selValue);
				}
				selectedTagInstances.add((TagInstance)selValue);
			}
		}
		return selectedTagInstances;
	}

	private void initComponents() {
		tagInstanceTree = new TreeTable();
		tagInstanceTree.setImmediate(true);
		tagInstanceTree.setSizeFull();
		tagInstanceTree.setSelectable(true);
		tagInstanceTree.setMultiSelect(true);
		tagInstanceTree.setColumnReorderingAllowed(true);
		tagInstanceTree.setColumnCollapsingAllowed(true);
		
		tagInstanceTree.setContainerDataSource(new HierarchicalContainer());
		tagInstanceTree.addContainerProperty(
				TagInstanceTreePropertyName.caption, String.class, null);
		tagInstanceTree.setColumnHeader(TagInstanceTreePropertyName.caption, "Tag Instance");
		
		tagInstanceTree.addContainerProperty(
				TagInstanceTreePropertyName.icon, Resource.class, null);

		tagInstanceTree.addContainerProperty(
				TagInstanceTreePropertyName.path, String.class, null);

		tagInstanceTree.addContainerProperty(
				TagInstanceTreePropertyName.instanceId, String.class, null);
		
		tagInstanceTree.addContainerProperty(
				TagInstanceTreePropertyName.umc, String.class, null);

		tagInstanceTree.setItemCaptionPropertyId(TagInstanceTreePropertyName.caption);
		tagInstanceTree.setItemIconPropertyId(TagInstanceTreePropertyName.icon);

		tagInstanceTree.addGeneratedColumn(
			TagInstanceTreePropertyName.color,
			new ColorLabelColumnGenerator(
				new ColorLabelColumnGenerator.TagInstanceTagDefinitionProvider()));
		
		tagInstanceTree.setVisibleColumns(
				new Object[] {
						TagInstanceTreePropertyName.caption, 
						TagInstanceTreePropertyName.color,
						TagInstanceTreePropertyName.path,
						TagInstanceTreePropertyName.instanceId,
						TagInstanceTreePropertyName.umc});
		tagInstanceTree.setColumnHeader(
				TagInstanceTreePropertyName.color, "Tag Color");
		tagInstanceTree.setColumnHeader(
				TagInstanceTreePropertyName.path, "Tag Path");
		tagInstanceTree.setColumnHeader(
				TagInstanceTreePropertyName.instanceId, "Tag Instance ID");
		tagInstanceTree.setColumnHeader(
				TagInstanceTreePropertyName.umc, "User Markup Collection");
		addComponent(tagInstanceTree);
		setExpandRatio(tagInstanceTree, 1.0f);
		
		GridLayout buttonGrid = new GridLayout(1, 2);
		buttonGrid.setMargin(false, true, true, true);
		buttonGrid.setSpacing(true);
		
		btRemoveTagInstance = new Button("Remove Tag Instance");
		buttonGrid.addComponent(btRemoveTagInstance);
		
		btEditPropertyValues = new Button("Edit Property values");
		buttonGrid.addComponent(btEditPropertyValues);
		
		addComponent(buttonGrid);
	}
	
	public void setTagInstances(List<TagInstanceInfo> tagInstances) {
		tagInstanceTree.removeAllItems();
		for (TagInstanceInfo ti : tagInstances) {
			ClassResource tagIcon = 
					new ClassResource(
						"ui/tagmanager/resources/reddiamd.gif", getApplication());
			
			tagInstanceTree.addItem(ti.getTagInstance());
			Item item = tagInstanceTree.getItem(ti.getTagInstance());
			
			item.getItemProperty(
					TagInstanceTreePropertyName.caption).setValue(
							ti.getTagInstance().getTagDefinition().getName());
			item.getItemProperty(
					TagInstanceTreePropertyName.path).setValue(
							ti.getTagPath());
			item.getItemProperty(
					TagInstanceTreePropertyName.instanceId).setValue(
							ti.getTagInstance().getUuid());
			item.getItemProperty(
					TagInstanceTreePropertyName.umc).setValue(
							ti.getUserMarkupCollection().getName());
			item.getItemProperty(
					TagInstanceTreePropertyName.icon).setValue(tagIcon);
			
			tagInstanceTree.setChildrenAllowed(
					ti.getTagInstance(), 
					!ti.getTagInstance().getUserDefinedProperties().isEmpty());
			
			for (Property property : ti.getTagInstance().getUserDefinedProperties()) {
				ClassResource propIcon = 
						new ClassResource(
							"ui/tagmanager/resources/ylwdiamd.gif", getApplication());
				List<String> values = property.getPropertyValueList().getValues();
				String caption = property.getName();
				if (values.isEmpty()) {
					caption += " (not set)";
				}
				tagInstanceTree.addItem(property);
				item = tagInstanceTree.getItem(property);
				
				item.getItemProperty(
						TagInstanceTreePropertyName.caption).setValue(
								caption);
				item.getItemProperty(
							TagInstanceTreePropertyName.icon).setValue(propIcon);
				tagInstanceTree.setParent(property, ti.getTagInstance());
				tagInstanceTree.setChildrenAllowed(property, !values.isEmpty());
				
				for (String value : values) {
					String itemId = String.valueOf(property.hashCode()) + value; 
					tagInstanceTree.addItem(itemId);
					item = tagInstanceTree.getItem(itemId);
					
					item.getItemProperty(
							TagInstanceTreePropertyName.caption).setValue(
									value);

					tagInstanceTree.setParent(itemId, property);
					tagInstanceTree.setChildrenAllowed(itemId, false);
				}
				tagInstanceTree.setCollapsed(property, false);
			}
			if (tagInstanceTree.hasChildren(ti.getTagInstance())) {
				tagInstanceTree.setCollapsed(ti.getTagInstance(), false);
			}
		}
		tagInstanceTree.sort(
			new Object[] {TagInstanceTreePropertyName.caption}, new boolean[] {true});
	}
	
	

	public List<String> getTagInstanceIDs(Set<TagDefinition> excludeFilter) {
		ArrayList<String> idList = new ArrayList<String>();
		for (Object itemId : tagInstanceTree.getItemIds()) {
			if (tagInstanceTree.getParent(itemId)==null) {
				TagInstance ti = (TagInstance)itemId;
				if (!excludeFilter.contains(ti.getTagDefinition())) {
					idList.add(ti.getUuid());
				}
			}
		}
		return idList;
	}
}
