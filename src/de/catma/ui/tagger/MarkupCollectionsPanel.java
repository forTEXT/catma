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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.ClassResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.AccessMode;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.standoffmarkup.usermarkup.TagInstanceInfo;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionManager;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;
import de.catma.indexer.TagsetDefinitionUpdateLog;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.CatmaApplication;
import de.catma.ui.tagmanager.ColorLabelColumnGenerator;
import de.catma.util.Pair;

public class MarkupCollectionsPanel extends VerticalLayout {
	
	public static enum MarkupCollectionPanelEvent {
		tagDefinitionSelected,
		userMarkupCollectionSelected, 
		tagDefinitionsRemoved,
		;
	}
	
	private static enum MarkupCollectionsTreeProperty {
		caption, 
		icon,
		visible,
		color,
		writable,
		;

	}
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TreeTable markupCollectionsTree;
	private TagManager tagManager;
	
	private PropertyChangeListener tagDefChangedListener;
	private PropertyChangeListener tagsetDefChangedListener;

	private UserMarkupCollectionManager userMarkupCollectionManager;
	private UserMarkupCollection currentWritableUserMarkupColl;
	private PropertyChangeSupport propertyChangeSupport;
	private Repository repository;
	private Set<TagsetDefinition> updateableforeignTagsetDefinitions;
	private PropertyChangeListener userMarkupCollectionChangedListener;
	private PropertyChangeListener userMarkupCollectionTagLibraryChangedListener;
	private PropertyChangeListener userPropertyDefinitionChangedListener;
	private Button btnOpenMarkupCollection;
	private boolean tagDefinitionSelectedEventHandlingRunning = false;
	private Button btAllVisible;
	private Button btAllInvisible;
	private Button btCloseMarkupCollection;
	private Button btRefresh;
	
	public MarkupCollectionsPanel(Repository repository, ClickListener openMarkupCollectionsHandler) {
		propertyChangeSupport = new PropertyChangeSupport(this);
		this.tagManager = repository.getTagManager();
		this.repository = repository;
		userMarkupCollectionManager =
				new UserMarkupCollectionManager(repository);
		updateableforeignTagsetDefinitions = new HashSet<TagsetDefinition>();
		initComponents();
		initActions(openMarkupCollectionsHandler);
	}

	public void addPropertyChangeListener(MarkupCollectionPanelEvent propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName.name(), listener);
	}

	public void removePropertyChangeListener(MarkupCollectionPanelEvent propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName.name(),
				listener);
	}

	private void initActions(ClickListener openMarkupCollectionsHandler) {
		btnOpenMarkupCollection.addClickListener(openMarkupCollectionsHandler);

		tagDefChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				Object oldValue = evt.getOldValue();
				Object newValue = evt.getNewValue();
				if ((oldValue == null) && (newValue == null)) {
					return;
				}
				
				if (oldValue == null) { // add
					@SuppressWarnings("unchecked")
					Pair<TagsetDefinition, TagDefinition> addOperationResult = 
							(Pair<TagsetDefinition, TagDefinition>)evt.getNewValue();
					
					addTagDefinition(
							addOperationResult.getSecond(), 
							addOperationResult.getFirst());
					
				}
				else if (newValue == null) { // removal
					@SuppressWarnings("unchecked")
					Pair<TagsetDefinition, TagDefinition> removeOperationResult = 
							(Pair<TagsetDefinition, TagDefinition>)evt.getOldValue();
					
					removeTagDefinition(
						removeOperationResult.getSecond(), 
						removeOperationResult.getFirst());
				}
				else { // update
					TagDefinition tagDefinition = 
							(TagDefinition)evt.getNewValue();
					updateTagDefinition(tagDefinition);
				}
				
			}

		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefChangedListener);
		
		
		userPropertyDefinitionChangedListener = 
				new PropertyChangeListener() {
					
					public void propertyChange(PropertyChangeEvent evt) {
						if (evt.getOldValue() == null) { //insert
							@SuppressWarnings("unchecked")
							Pair<PropertyDefinition, TagDefinition> pair = 
								(Pair<PropertyDefinition, TagDefinition>) evt.getNewValue();
							updateTagDefinition(pair.getSecond());
						}
						else if (evt.getNewValue() == null) {//delete
							@SuppressWarnings("unchecked")
							Pair<PropertyDefinition, TagDefinition> pair = 
									(Pair<PropertyDefinition, TagDefinition>) evt.getOldValue();
								updateTagDefinition(pair.getSecond());
						}
						else { //update
							updateTagDefinition((TagDefinition) evt.getOldValue());
						}
					}
				};
		
		this.tagManager.addPropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged,
				userPropertyDefinitionChangedListener);
		
		tagsetDefChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				Object oldValue = evt.getOldValue();
				Object newValue = evt.getNewValue();
				if ((oldValue == null) && (newValue == null)) {
					return;
				}

				if (oldValue == null) { //addition
					@SuppressWarnings("unchecked")
					Pair<TagLibrary, TagsetDefinition> addOpResult = 
							 (Pair<TagLibrary, TagsetDefinition>) newValue;	
					if ((currentWritableUserMarkupColl != null) &&
							currentWritableUserMarkupColl.getTagLibrary().equals(
									addOpResult.getFirst())) {
						addTagsetDefinitionToTree(
							addOpResult.getSecond(), currentWritableUserMarkupColl);
					}
				}
				else if (newValue == null) { //removal
					@SuppressWarnings("unchecked")
					Pair<TagLibrary, TagsetDefinition> removeOpResult = 
							 (Pair<TagLibrary, TagsetDefinition>) oldValue;
					
					removeTagsetDefinition(removeOpResult.getSecond());
					
				}
				else { // update
					updateTagsetDefinition((TagsetDefinition)evt.getNewValue());
				}
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagsetDefinitionChanged,
				tagsetDefChangedListener);
		
		userMarkupCollectionChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == null) { // remove
					UserMarkupCollectionReference userMarkupCollectionReference =
							(UserMarkupCollectionReference) evt.getOldValue();
					if (userMarkupCollectionManager.contains(userMarkupCollectionReference)) {
						removeUserMarkupCollection(userMarkupCollectionReference);
					}
				}
				else if (evt.getOldValue() != null) { // update
					UserMarkupCollectionReference umcRef = 
							(UserMarkupCollectionReference)evt.getNewValue();
					if (userMarkupCollectionManager.contains(umcRef)) {
						UserMarkupCollection userMarkupCollection = 
							userMarkupCollectionManager.updateUserMarkupCollection(
								umcRef);
						updateUserMarkupCollectionInTree(userMarkupCollection);
					}
				}
			}
		};
		
		repository.addPropertyChangeListener(
				RepositoryChangeEvent.userMarkupCollectionChanged, 
				userMarkupCollectionChangedListener);
		
		userMarkupCollectionTagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if ((evt.getNewValue() != null) && (evt.getOldValue() != null)) {
					
					TagsetDefinition tagsetDefinition = 
							(TagsetDefinition) evt.getOldValue();
					
					@SuppressWarnings("unchecked")
					List<UserMarkupCollection> userMarkupCollcCollections =
							(List<UserMarkupCollection>) evt.getNewValue();
					
					for (UserMarkupCollection umc : userMarkupCollcCollections) {
						if (userMarkupCollectionManager.contains(umc.getId())) {
							reloadTagsetDefinition(
								tagsetDefinition, 
								userMarkupCollectionManager.getUserMarkupCollection(
										umc.getId()));
						}
					}
					
				}
				
			}
		};
		
		repository.addPropertyChangeListener(
				RepositoryChangeEvent.userMarkupCollectionTagLibraryChanged, 
				userMarkupCollectionTagLibraryChangedListener);
		
		btCloseMarkupCollection.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				Object value = markupCollectionsTree.getValue();
				if (value instanceof UserMarkupCollection) {
					UserMarkupCollection umc = (UserMarkupCollection)value;
					removeUserMarkupCollection(
							new UserMarkupCollectionReference(umc.getId(), umc.getContentInfoSet()));
				}
				else {
					Notification.show("Info", "Please select a Markup Collection first!", Type.TRAY_NOTIFICATION);
				}
			}
		});
		
		btRefresh.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				Object value = markupCollectionsTree.getValue();
				if (value instanceof UserMarkupCollection) {
					UserMarkupCollection umc = (UserMarkupCollection)value;
					//TODO: close all active tagsets
					UserMarkupCollectionReference umcRef =
							new UserMarkupCollectionReference(umc.getId(), umc.getContentInfoSet());
					removeUserMarkupCollection(umcRef);
					UserMarkupCollection refreshedUmc;
					try {
						refreshedUmc = repository.getUserMarkupCollection(umcRef, true);
						openUserMarkupCollection(refreshedUmc);
					} catch (IOException e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(
								"error refreshing Markup Collection!", e);
					}
				}
				else {
					Notification.show("Info", "Please select a Markup Collection first!", Type.TRAY_NOTIFICATION);
				}
			}
		});
		
		btAllVisible.addClickListener(event->selectAllVisible(true));
		btAllInvisible.addClickListener(event->selectAllVisible(false));
	}
	
	private void reindex(TagsetDefinition tagsetDef) {
		try {
			UserMarkupCollection umc = 
					(UserMarkupCollection) this.markupCollectionsTree.getParent(
							tagsetDef);
			Indexer indexer = ((IndexedRepository)repository).getIndexer();
			indexer.reindex(
					tagsetDef, 
					new TagsetDefinitionUpdateLog(), 
					umc);
			Notification.show(
				"Information", "Reindexing finished!", 
				Type.TRAY_NOTIFICATION);

		}
		catch (IOException ioe) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					"error reindexing Markup Collection!", ioe);
		}
		
	}

	private void reindex(UserMarkupCollection umc) {
		try {
			Indexer indexer = ((IndexedRepository)repository).getIndexer();
			
			for (TagsetDefinition tagsetDef : umc.getTagLibrary()) {
				indexer.reindex(
					tagsetDef, 
					new TagsetDefinitionUpdateLog(), 
					umc);
			}
			Notification.show(
					"Information", "Reindexing finished!", 
					Type.TRAY_NOTIFICATION);
		}
		catch (IOException ioe) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					"error reindexing Markup Collection!", ioe);
		}
	}

	private void reloadTagsetDefinition(TagsetDefinition foreignTagsetDefinition,
			UserMarkupCollection umc) {

		removeWithChildrenFromTree(
			umc.getTagLibrary().getTagsetDefinition(foreignTagsetDefinition.getUuid()));
		
		addTagsetDefinitionToTree(
			umc.getTagLibrary().getTagsetDefinition(foreignTagsetDefinition.getUuid()), 
			umc);
		
		for (TagDefinition td : umc.getTagLibrary().getTagsetDefinition(foreignTagsetDefinition.getUuid())) {
			fireTagDefinitionSelected(td, false);
			fireTagDefinitionSelected(td, true);
		}
	}

	private void updateUserMarkupCollectionInTree(
			UserMarkupCollection userMarkupCollection) {
		Property captionProp = markupCollectionsTree.getContainerProperty(
				userMarkupCollection, MarkupCollectionsTreeProperty.caption);
		captionProp.setValue(userMarkupCollection.toString());
		Property writableProp = markupCollectionsTree.getContainerProperty(
				userMarkupCollection, MarkupCollectionsTreeProperty.writable);
		if (((CheckBox)writableProp.getValue()).getValue()) {
			fireWritableUserMarkupCollectionSelected(userMarkupCollection,true);
		}
	}

	private void removeUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		UserMarkupCollection userMarkupCollection = 
				userMarkupCollectionManager.getUserMarkupCollection(
						userMarkupCollectionReference);
		if (userMarkupCollection != null) {
			
			for (TagsetDefinition tagsetDefinition : 
				userMarkupCollection.getTagLibrary()) {
				
				Item tagsetDefItem = 
						markupCollectionsTree.getItem(tagsetDefinition); 
				if (tagsetDefItem != null) {
					for (TagDefinition tagDefinition : tagsetDefinition) {
						if (tagDefinition.getParentUuid().isEmpty()) {
							fireTagDefinitionSelected(tagDefinition, false);
						}
					}
				}
			}
			
			logger.info(
					"removing UserMarkupCollection " 
							+ userMarkupCollection + "#" 
							+ userMarkupCollection.getId()
							+ " from MarkupCollectionsPanel.Tree");
			removeWithChildrenFromTree(userMarkupCollection);
			fireWritableUserMarkupCollectionSelected(userMarkupCollection, false);
			userMarkupCollectionManager.remove(userMarkupCollection);
		}
	}

	private void updateTagsetDefinition(TagsetDefinition foreignTagsetDefinition) {
		if (updateableforeignTagsetDefinitions.contains(foreignTagsetDefinition)) {
			List<UserMarkupCollection> outOfSynchCollections = 
					userMarkupCollectionManager.getOutOfSyncUserMarkupCollections(
						foreignTagsetDefinition);

			userMarkupCollectionManager.updateUserMarkupCollections(
					outOfSynchCollections, foreignTagsetDefinition);
			
			
			for (UserMarkupCollection umc : outOfSynchCollections) {
				TagsetDefinition tagsetDefinition = 
						umc.getTagLibrary().getTagsetDefinition(
								foreignTagsetDefinition.getUuid());
				Property captionProp = markupCollectionsTree.getContainerProperty(
						tagsetDefinition, 
						MarkupCollectionsTreeProperty.caption);
				
				if (captionProp != null) {
					captionProp.setValue(tagsetDefinition.getName());
				}
				
			}
		}
		
	}

	private void removeTagsetDefinition(TagsetDefinition foreignTagsetDefinition) {
		
		if (updateableforeignTagsetDefinitions.contains(foreignTagsetDefinition)) {
			List<UserMarkupCollection> outOfSynchCollections = 
					userMarkupCollectionManager.getOutOfSyncUserMarkupCollections(
						foreignTagsetDefinition);

			//FIXME: update does not remove Tagsets just Tags, removal should probably occur by a button of this panel only!
			userMarkupCollectionManager.updateUserMarkupCollections(
					outOfSynchCollections, foreignTagsetDefinition);
			
			
			for (UserMarkupCollection umc : outOfSynchCollections) {
				TagsetDefinition tagsetDefinition = 
						umc.getTagLibrary().getTagsetDefinition(
								foreignTagsetDefinition.getUuid());
				
				Object parentId = markupCollectionsTree.getParent(tagsetDefinition);
				removeWithChildrenFromTree(tagsetDefinition);
				if ((parentId != null) 
						&& (!markupCollectionsTree.hasChildren(parentId))) {
					markupCollectionsTree.setChildrenAllowed(parentId, false);
				}
			}
		}
		
	}

	private void addTagDefinition(
			TagDefinition foreignTagDefinition, 
			TagsetDefinition foreignTagsetDefinition) {
		

		if (updateableforeignTagsetDefinitions.contains(foreignTagsetDefinition)) {
			List<UserMarkupCollection> outOfSynchCollections = 
					userMarkupCollectionManager.getOutOfSyncUserMarkupCollections(
						foreignTagsetDefinition);

			userMarkupCollectionManager.updateUserMarkupCollections(
					outOfSynchCollections, foreignTagsetDefinition);
			
			for (UserMarkupCollection umc : outOfSynchCollections) {
				TagDefinition tagDefinition = umc.getTagLibrary().getTagDefinition(
						foreignTagDefinition.getUuid());
				TagsetDefinition tagsetDefinition = 
						umc.getTagLibrary().getTagsetDefinition(tagDefinition);
				
				addTagDefinitionToTree(tagDefinition, tagsetDefinition);
			}
		}
	}

	private void removeTagDefinition(
			TagDefinition foreignTagDefinition, 
			TagsetDefinition foreignTagsetDefinition) {
		
		if (updateableforeignTagsetDefinitions.contains(foreignTagsetDefinition)) {
			
			List<UserMarkupCollection> outOfSynchCollections = 
					userMarkupCollectionManager.getOutOfSyncUserMarkupCollections(
						foreignTagsetDefinition);

			for (UserMarkupCollection umc : outOfSynchCollections) {
				TagDefinition tagDefinition = umc.getTagLibrary().getTagDefinition(
						foreignTagDefinition.getUuid());
				
				Set<TagDefinition> allDeleted = new HashSet<TagDefinition>();
				allDeleted.add(tagDefinition);
				
				collectItemsOfType(
					TagDefinition.class, 
					markupCollectionsTree.getChildren(tagDefinition), allDeleted);
				
				propertyChangeSupport.firePropertyChange(
						MarkupCollectionPanelEvent.tagDefinitionsRemoved.name(), 
						allDeleted, null);
				
				Object parentId = markupCollectionsTree.getParent(tagDefinition);
				removeWithChildrenFromTree(tagDefinition);
				if ((parentId != null) 
						&& (!markupCollectionsTree.hasChildren(parentId))) {
					markupCollectionsTree.setChildrenAllowed(parentId, false);
				}
			}

			userMarkupCollectionManager.updateUserMarkupCollections(
					outOfSynchCollections, foreignTagsetDefinition);
		}

	}
	
	private void updateTagDefinition(TagDefinition foreignTagDefinition) {
		
		TagsetDefinition foreignTagsetDefinition = 
				getUpdateableTagsetDefinition(foreignTagDefinition.getUuid());
		
		if (foreignTagsetDefinition != null) {
			
			List<UserMarkupCollection> outOfSynchCollections = 
				userMarkupCollectionManager.getOutOfSyncUserMarkupCollections(
					foreignTagsetDefinition);
			
			userMarkupCollectionManager.updateUserMarkupCollections(
					outOfSynchCollections, foreignTagsetDefinition);
			
			
			for (UserMarkupCollection umc : outOfSynchCollections) {
				TagDefinition tagDefinition = umc.getTagLibrary().getTagDefinition(
						foreignTagDefinition.getUuid());
				
				Property captionProp = markupCollectionsTree.getContainerProperty(
						tagDefinition, 
						MarkupCollectionsTreeProperty.caption);
				
				if (captionProp != null) {
					captionProp.setValue(tagDefinition.getName());
				}
				CheckBox box = (CheckBox)markupCollectionsTree.getItem(
						tagDefinition).getItemProperty(
								MarkupCollectionsTreeProperty.visible).getValue();
				boolean selected = box.getValue();
				if (selected) {
					fireTagDefinitionSelected(tagDefinition, false);
					fireTagDefinitionSelected(tagDefinition, true);
				}
			}
		}
	}

	private TagsetDefinition getUpdateableTagsetDefinition(String tagDefUuid) {
		for (TagsetDefinition tagsetDef : updateableforeignTagsetDefinitions) {
			if (tagsetDef.hasTagDefinition(tagDefUuid)) {
				return tagsetDef;
			}
		}
		return null;
	}
	
	private void initComponents() {
		setMargin(new MarginInfo(false, true, true, false));
		HorizontalLayout buttonHeaderPanel = new HorizontalLayout();

		buttonHeaderPanel.setSpacing(true);
		buttonHeaderPanel.setMargin(new MarginInfo(true, false, true, false));
		buttonHeaderPanel.addStyleName("catma-tagger-markup-panels");
		
		btnOpenMarkupCollection = new Button("Open Markup Collection");
		btnOpenMarkupCollection.addStyleName("primary-button");
		buttonHeaderPanel.addComponent(btnOpenMarkupCollection);
		
		btCloseMarkupCollection = new Button("Close");
		buttonHeaderPanel.addComponent(btCloseMarkupCollection);
		
		btRefresh = new Button(FontAwesome.REFRESH);
		buttonHeaderPanel.addComponent(btRefresh);
		
		
		btAllVisible = new Button("Select all visible");
		buttonHeaderPanel.addComponent(btAllVisible);
		btAllInvisible = new Button("Deselect all visible");
		buttonHeaderPanel.addComponent(btAllInvisible);
		
		addComponent(buttonHeaderPanel);
		
		markupCollectionsTree = new TreeTable();
		markupCollectionsTree.setWidth("100%");
		markupCollectionsTree.setSelectable(true);
		markupCollectionsTree.setMultiSelect(false);
		markupCollectionsTree.setContainerDataSource(new HierarchicalContainer());
		markupCollectionsTree.addStyleName("catma-tagger-markup-panels");

		markupCollectionsTree.addContainerProperty(
				MarkupCollectionsTreeProperty.caption, 
				String.class, null);
		markupCollectionsTree.setColumnHeader(
				MarkupCollectionsTreeProperty.caption, "Markup Collections");
		
		markupCollectionsTree.addContainerProperty(
				MarkupCollectionsTreeProperty.icon, Resource.class, null);
		
		markupCollectionsTree.addContainerProperty(
				MarkupCollectionsTreeProperty.visible, 
				AbstractComponent.class, null);
		markupCollectionsTree.setColumnHeader(
				MarkupCollectionsTreeProperty.visible, "Visible");
		
		markupCollectionsTree.addContainerProperty(
				MarkupCollectionsTreeProperty.writable, 
				AbstractComponent.class, null);
		markupCollectionsTree.setColumnHeader(
				MarkupCollectionsTreeProperty.writable, "Writable");
		
		markupCollectionsTree.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		markupCollectionsTree.setItemCaptionPropertyId(
				MarkupCollectionsTreeProperty.caption);
		markupCollectionsTree.setItemIconPropertyId(MarkupCollectionsTreeProperty.icon);
		markupCollectionsTree.addGeneratedColumn(
				MarkupCollectionsTreeProperty.color, new ColorLabelColumnGenerator());
		markupCollectionsTree.setColumnHeader(
				MarkupCollectionsTreeProperty.color, "Tag Type color");
		
		markupCollectionsTree.setVisibleColumns(
				new Object[] {
						MarkupCollectionsTreeProperty.caption,
						MarkupCollectionsTreeProperty.color,
						MarkupCollectionsTreeProperty.visible,
						MarkupCollectionsTreeProperty.writable});

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
		userMarkupCollectionManager.add(userMarkupCollection);
		addUserMarkupCollectionToTree(userMarkupCollection);
		if ((userMarkupCollectionManager.getUserMarkupCollections().size() == 1) 
				&& userMarkupCollection.getAccessMode().equals(AccessMode.WRITE)) {
			Property property =
				markupCollectionsTree.getItem(userMarkupCollection).getItemProperty(
					MarkupCollectionsTreeProperty.writable);
			((CheckBox)property.getValue()).setValue(true);
			handleUserMarkupCollectionSelectionRequest(
					true, userMarkupCollection);
		}
	}
	
	private void addUserMarkupCollectionToTree(
			UserMarkupCollection userMarkupCollection) {
		markupCollectionsTree.addItem(
				new Object[] {
						userMarkupCollection.toString(), 
						createVisibilityCheckbox(userMarkupCollection), 
						createWritableCheckbox(userMarkupCollection)},
				userMarkupCollection);
		markupCollectionsTree.setCollapsed(userMarkupCollection, false);
		
		TagLibrary tagLibrary = userMarkupCollection.getTagLibrary();
		for (TagsetDefinition tagsetDefinition : tagLibrary) {
			addTagsetDefinitionToTree(tagsetDefinition, userMarkupCollection);
		}
		
		if (markupCollectionsTree.hasChildren(userMarkupCollection) && 
				markupCollectionsTree.getChildren(userMarkupCollection).size() == 1) {
			markupCollectionsTree.setCollapsed(
				markupCollectionsTree.getChildren(
						userMarkupCollection).iterator().next(), false);
		}
	}
	
	private void removeWithChildrenFromTree(Object itemId) {
		@SuppressWarnings("rawtypes")
		Collection children = markupCollectionsTree.getChildren(itemId);
		
		if (children != null) {
			Object[] childArray = children.toArray();
			for (Object childId : childArray) {
				removeWithChildrenFromTree(childId);
			}
		}
		markupCollectionsTree.removeItem(itemId);
	}

	private void addTagsetDefinitionToTree(
			TagsetDefinition tagsetDefinition, 
			UserMarkupCollection userMarkupCollection) {
		
		ClassResource tagsetIcon = 
				new ClassResource("tagmanager/resources/grndiamd.gif");

		markupCollectionsTree.addItem(
				new Object[]{tagsetDefinition.getName(), 
						createCheckbox(tagsetDefinition), 
						new Label()}, tagsetDefinition);
		markupCollectionsTree.getContainerProperty(
			tagsetDefinition, MarkupCollectionsTreeProperty.icon).setValue(
					tagsetIcon);
		markupCollectionsTree.setParent(tagsetDefinition, userMarkupCollection);
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			insertTagDefinitionIntoTree(tagDefinition);
		}
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			maintainTagDefinitionHierarchy(tagDefinition, tagsetDefinition);
		}
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			maintainTagDefinitionChildrenState(tagDefinition);
		}
	}
	
	private void addTagDefinitionToTree(
			TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) {
		insertTagDefinitionIntoTree(tagDefinition);
		maintainTagDefinitionHierarchy(tagDefinition, tagsetDefinition);
		maintainTagDefinitionChildrenState(tagDefinition);
	}
	
	private void maintainTagDefinitionChildrenState(TagDefinition tagDefinition) {
		if (!markupCollectionsTree.hasChildren(tagDefinition)) {
			markupCollectionsTree.setChildrenAllowed(tagDefinition, false);
		}
	}

	@SuppressWarnings("unchecked")
	private void insertTagDefinitionIntoTree(TagDefinition tagDefinition) {
		ClassResource tagIcon = 
				new ClassResource("tagmanager/resources/reddiamd.gif");
		
		markupCollectionsTree.addItem(
				new Object[]{
						tagDefinition.getName(), 
						createCheckbox(tagDefinition),
						new Label()},
				tagDefinition);
		markupCollectionsTree.getContainerProperty(
				tagDefinition, MarkupCollectionsTreeProperty.icon).setValue(
						tagIcon);
	}

	private void maintainTagDefinitionHierarchy(
			TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) {
		String baseID = tagDefinition.getParentUuid();
		if (baseID.isEmpty()) {
			markupCollectionsTree.setParent(tagDefinition, tagsetDefinition);
		}
		else {
			TagDefinition parent = tagsetDefinition.getTagDefinition(baseID);
			markupCollectionsTree.setParent(tagDefinition, parent);
		}
	}
	
	private CheckBox createCheckbox(final TagsetDefinition tagsetDefinition) {
		final CheckBox cbShowTagInstances = new CheckBox();
		cbShowTagInstances.setImmediate(true);
		cbShowTagInstances.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				boolean selected = cbShowTagInstances.getValue();
				
				UserMarkupCollection userMarkupCollection =
						getUserMarkupCollection(tagsetDefinition);
				
				if (!tagDefinitionSelectedEventHandlingRunning) {
					
					List<TagReference> tagReferences = new ArrayList<>();
					for (TagDefinition tagDefinition : tagsetDefinition) {
						tagReferences.addAll(
								userMarkupCollection.getTagReferences(
										tagDefinition, true));
					}
					propertyChangeSupport.firePropertyChange(
							MarkupCollectionPanelEvent.tagDefinitionSelected.name(), 
							selected?null:tagReferences,
									selected?tagReferences:null);
				}
				
				for (TagDefinition tagDefinition : tagsetDefinition) {
					if (tagDefinition.getParentUuid().isEmpty()) {
						Item tagDefItem =
								markupCollectionsTree.getItem(tagDefinition);
						Property<?> visibleProp = 
								tagDefItem.getItemProperty(
										MarkupCollectionsTreeProperty.visible);
						CheckBox cb = (CheckBox) visibleProp.getValue();
						tagDefinitionSelectedEventHandlingRunning = true;
						try {
							cb.setValue(selected);
						}
						finally {
							tagDefinitionSelectedEventHandlingRunning = false;
						}

					}
				}
				
			}
		});
		return cbShowTagInstances;
	}

	private CheckBox createVisibilityCheckbox(final UserMarkupCollection umc) {
		final CheckBox cbShowTagInstances = new CheckBox();
		cbShowTagInstances.setImmediate(true);
		cbShowTagInstances.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				boolean selected = cbShowTagInstances.getValue();
				
				if (!tagDefinitionSelectedEventHandlingRunning) {
					fireMarkupCollectionSelected(umc, selected);
				}

				for (TagsetDefinition tagsetDefinition : umc.getTagLibrary()) {
					Item tagsetDefItem =
							markupCollectionsTree.getItem(tagsetDefinition);
					Property<?> visibleProp = 
							tagsetDefItem.getItemProperty(
									MarkupCollectionsTreeProperty.visible);
					CheckBox cb = (CheckBox) visibleProp.getValue();
					tagDefinitionSelectedEventHandlingRunning = true;
					try {
						cb.setValue(selected);
					}
					finally {
						tagDefinitionSelectedEventHandlingRunning  = false;
					}
				}
			}
		});
		return cbShowTagInstances;
	}
	
	private void selectAllVisible(boolean visible) {
		ArrayList<TagReference> tagReferences = new ArrayList<>();
		
		for (UserMarkupCollection userMarkupCollection : userMarkupCollectionManager.getUserMarkupCollections()) {
			tagReferences.addAll(userMarkupCollection.getTagReferences());
			Item userMarkupCollectionItem =
					markupCollectionsTree.getItem(userMarkupCollection);
			Property<?> visibleProp = 
					userMarkupCollectionItem.getItemProperty(
							MarkupCollectionsTreeProperty.visible);
			CheckBox cb = (CheckBox) visibleProp.getValue();
			tagDefinitionSelectedEventHandlingRunning = true;
			try {
				cb.setValue(visible);
			}
			finally {
				tagDefinitionSelectedEventHandlingRunning = false;
			}
		}
		
		propertyChangeSupport.firePropertyChange(
				MarkupCollectionPanelEvent.tagDefinitionSelected.name(), 
				visible?null:tagReferences,
						visible?tagReferences:null);

	}

	private CheckBox createCheckbox(final TagDefinition tagDefinition) {
		final CheckBox cbShowTagInstances = new CheckBox();
		cbShowTagInstances.setImmediate(true);
		cbShowTagInstances.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				boolean selected = cbShowTagInstances.getValue();

				fireTagDefinitionSelected(tagDefinition, selected);
			}


		});
		return cbShowTagInstances;
	}
	
	private CheckBox createWritableCheckbox(
			final UserMarkupCollection userMarkupCollection) {
		
		final CheckBox cbIsWritableUserMarkupColl = new CheckBox();
		cbIsWritableUserMarkupColl.setImmediate(true);
		cbIsWritableUserMarkupColl.addValueChangeListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				
				boolean selected = cbIsWritableUserMarkupColl.getValue();

				handleUserMarkupCollectionSelectionRequest(
						selected, userMarkupCollection);
			}
		});
		cbIsWritableUserMarkupColl.setEnabled(
			userMarkupCollection.getAccessMode().equals(AccessMode.WRITE));
		return cbIsWritableUserMarkupColl;
	}
	
	private void handleUserMarkupCollectionSelectionRequest(
			boolean selected, UserMarkupCollection userMarkupCollection) {
		if (selected) {
			for (UserMarkupCollection umc : userMarkupCollectionManager) {
				if (!umc.equals(userMarkupCollection)) {
					Item umcItem = 
						markupCollectionsTree.getItem(
							umc);
					if (umcItem != null) {
						Object writeablePropertyValue = 
							umcItem.getItemProperty(
								MarkupCollectionsTreeProperty.writable).getValue();
						
						if ((writeablePropertyValue != null) 
								&& (writeablePropertyValue instanceof CheckBox)) {
							CheckBox cbWritable = (CheckBox)writeablePropertyValue;
							cbWritable.setValue(false);
						}
					}
					else {
						logger.warning(
							"[" + ((CatmaApplication)UI.getCurrent()).getUser() 
							+ "] could not find UserMarkupCollection " 
							+ umc + "#" + umc.getId() 
							+ " in the MarkupCollectionsPanel.Tree!");
					}
				}
			}
			currentWritableUserMarkupColl = userMarkupCollection;
		}
		else {
			currentWritableUserMarkupColl = null;
			
		}
		fireWritableUserMarkupCollectionSelected(
				userMarkupCollection, selected);
	}
	
	private void fireMarkupCollectionSelected(
			UserMarkupCollection markupCollection, boolean selected) {

		List<TagReference> tagReferences =
				markupCollection.getTagReferences();
		
		propertyChangeSupport.firePropertyChange(
				MarkupCollectionPanelEvent.tagDefinitionSelected.name(), 
				selected?null:tagReferences,
				selected?tagReferences:null);
	}

	
	private void fireTagDefinitionSelected(
			TagDefinition tagDefinition, boolean selected) {

		UserMarkupCollection userMarkupCollection =
				getUserMarkupCollection(tagDefinition);
		
		if (!tagDefinitionSelectedEventHandlingRunning) {
			List<TagReference> tagReferences =
					userMarkupCollection.getTagReferences(
							tagDefinition, true);
			
			propertyChangeSupport.firePropertyChange(
					MarkupCollectionPanelEvent.tagDefinitionSelected.name(), 
					selected?null:tagReferences,
					selected?tagReferences:null);
		}
		
		List<TagDefinition> children = 
				userMarkupCollection.getChildren(tagDefinition);

		if (children != null) {
			for (Object childId : children) {
				Object visiblePropertyValue = 
					markupCollectionsTree.getItem(
						childId).getItemProperty(
							MarkupCollectionsTreeProperty.visible).getValue();
				
				if ((visiblePropertyValue != null) 
						&& (visiblePropertyValue instanceof CheckBox)) {
					CheckBox cbVisible = (CheckBox)visiblePropertyValue;
					tagDefinitionSelectedEventHandlingRunning = true;
					try {
						cbVisible.setValue(selected);
					}
					finally {
						tagDefinitionSelectedEventHandlingRunning = false;
					}
				}
			}
		}		

	}
	
	private void fireWritableUserMarkupCollectionSelected(
			UserMarkupCollection userMarkupCollection, boolean selected) {
		propertyChangeSupport.firePropertyChange(
			MarkupCollectionPanelEvent.userMarkupCollectionSelected.name(), 
			(!selected? userMarkupCollection : null), 
			(selected? userMarkupCollection : null));
		
	}
	
	public void close() {
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefChangedListener);
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagsetDefinitionChanged,
				tagsetDefChangedListener);
		tagManager.removePropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged,
				userPropertyDefinitionChangedListener);
		repository.removePropertyChangeListener(
				RepositoryChangeEvent.userMarkupCollectionChanged, 
				userMarkupCollectionChangedListener);
		repository.removePropertyChangeListener(
				RepositoryChangeEvent.userMarkupCollectionTagLibraryChanged, 
				userMarkupCollectionTagLibraryChangedListener);
	}
	
	public void addOrUpdateTagsetDefinition(
			UI ui,
			final TagsetDefinition incomingTagsetDef,
			final ConfirmListener confirmListener) {
		
		final List<UserMarkupCollection> toBeUpdated = 
				userMarkupCollectionManager.getOutOfSyncUserMarkupCollections(
						incomingTagsetDef);
		
		if (!toBeUpdated.isEmpty()) {
			ConfirmDialog.show(
				ui, 
				"There are older versions of the Tagset '" +
					incomingTagsetDef.getName() +
					"' in the attached Markup Collections! " +
					"Do you really want to update the attached Markup Collections?",
							
			        new ConfirmDialog.Listener() {

			            public void onClose(ConfirmDialog dialog) {
			                if (dialog.isConfirmed()) {

			                	// clear all TagInstance displaying because
			                	// the instances might get deleted during TagsetDefintion update
			                	for (UserMarkupCollection umc : toBeUpdated) {
			                		for (TagsetDefinition tagsetDefinition : umc.getTagLibrary()) {
			                			for (TagDefinition td : tagsetDefinition) {
			                				fireTagDefinitionSelected(td, false);
			                			}
			                		}
			                	}
			                	
			                	updateableforeignTagsetDefinitions.add(incomingTagsetDef);
			                	
			                	userMarkupCollectionManager.updateUserMarkupCollections(
			                			toBeUpdated, incomingTagsetDef);
			                	updateUserMarkupCollectionsInTree(toBeUpdated);
			            		confirmListener.confirmed();
			                }
			            }
			        });
		}
		else {
			updateableforeignTagsetDefinitions.add(incomingTagsetDef);
			confirmListener.confirmed();
		}
	}
	
	private void updateUserMarkupCollectionsInTree(
			List<UserMarkupCollection> toBeUpdated) {
		for (UserMarkupCollection c : toBeUpdated) {
			Set<TagDefinition> currentlySelectedValid = 
					getCurrentlySelectedValidTagDefinitions(c);
			
			Collection<?> children = 
					markupCollectionsTree.getChildren(c);
			
			Set<TagDefinition> allSelected = new HashSet<TagDefinition>();
			collectItemsOfType(TagDefinition.class, children, allSelected);
			
			allSelected.removeAll(currentlySelectedValid);
			
			propertyChangeSupport.firePropertyChange(
					MarkupCollectionPanelEvent.tagDefinitionsRemoved.name(), 
					allSelected, null);
			
			removeWithChildrenFromTree(c);
			addUserMarkupCollectionToTree(c);
			
			for (TagDefinition td : currentlySelectedValid) {
				fireTagDefinitionSelected(td, false);
				Item item = markupCollectionsTree.getItem(td);
				if (item != null) {
					((CheckBox)item.getItemProperty(	
							MarkupCollectionsTreeProperty.visible).getValue()).setValue(true);
					fireTagDefinitionSelected(td, true);
				}
			}
			
		}
	}
	
	public void addTagReferences(List<TagReference> tagReferences) {
		userMarkupCollectionManager.addTagReferences(
				tagReferences, currentWritableUserMarkupColl);

	}
	
	public UserMarkupCollection getCurrentWritableUserMarkupCollection() {
		return currentWritableUserMarkupColl;
	}
	
	public List<UserMarkupCollection> getUserMarkupCollections() {
		return this.userMarkupCollectionManager.getUserMarkupCollections();
	}

	public Repository getRepository() {
		return repository;
	}
	
	private Set<TagDefinition> getCurrentlySelectedValidTagDefinitions(
			UserMarkupCollection userMarkupCollection) {
		HashSet<TagDefinition> result = new HashSet<TagDefinition>();

		
		for (TagsetDefinition tagsetDefinition : userMarkupCollection.getTagLibrary()) {
			for (TagDefinition td : tagsetDefinition) {
				Item item = markupCollectionsTree.getItem(td); 
				if (item != null) {
					Property property = 
							(Property)item.getItemProperty(
									MarkupCollectionsTreeProperty.visible);
					CheckBox checkBox = (CheckBox) property.getValue();
					
					boolean selected = (Boolean) checkBox.getValue();
					
					if (selected) {
						result.add(td);
					}
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private <T> void collectItemsOfType(Class<T> clazz, Collection<?> items,
			Set<T> result) {
		if (items != null) {
			for (Object o : items) {
				if (o.getClass().equals(clazz)) {
					result.add((T)o);
				}
				
				Collection<?> children = markupCollectionsTree.getChildren(o);
				if ((children != null) && !children.isEmpty()) {
					collectItemsOfType(clazz, children, result);
				}
			}
		}		
	}

	public void removeUpdateableTagsetDefinition(TagsetDefinition tagsetDefinition) {
		updateableforeignTagsetDefinitions.remove(tagsetDefinition);
	}

	public List<TagInstanceInfo> getTagInstances(Collection<String> instanceIDs) {
		return userMarkupCollectionManager.getTagInstanceInfos(instanceIDs);
	}

	public void removeTagInstances(List<String> tagInstanceIDs) {
		List<TagReference> tagReferences = new ArrayList<TagReference>();
		for (String tagInstanceID : tagInstanceIDs) {
			tagReferences.addAll(
				userMarkupCollectionManager.getTagReferences(tagInstanceID));
			userMarkupCollectionManager.removeTagInstance(tagInstanceID);
		}
	}

	public void updateProperty(TagInstance tagInstance, Collection<de.catma.tag.Property> properties) {
		try {
			userMarkupCollectionManager.updateProperty(tagInstance, properties);
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError("Error updating the Property", e);
		}
		
	}

	public TagInstanceInfo getTagInstanceInfo(String tagInstanceId) {
		return userMarkupCollectionManager.getTagInstanceInfo(tagInstanceId);
	}
}
