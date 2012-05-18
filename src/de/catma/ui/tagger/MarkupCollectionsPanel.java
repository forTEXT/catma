package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
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

import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollectionManager;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagManager;
import de.catma.core.tag.TagManager.TagManagerEvent;
import de.catma.core.tag.TagsetDefinition;
import de.catma.core.util.Pair;
import de.catma.ui.tagmanager.ColorLabelColumnGenerator;

public class MarkupCollectionsPanel extends VerticalLayout {
	
	public static enum MarkupCollectionPanelEvent {
		tagDefinitionSelected,
		userMarkupCollectionSelected,
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
	
	private TreeTable markupCollectionsTree;
	private String userMarkupItem = "User Markup Collections";
	private String staticMarkupItem = "Static Markup Collections";
	private TagManager tagManager;
	private PropertyChangeListener tagDefChangedListener;
	private UserMarkupCollectionManager userMarkupCollectionManager;
	private UserMarkupCollection currentWritableUserMarkupColl;
	private PropertyChangeSupport propertyChangeSupport;
	private Repository repository;
	
	public MarkupCollectionsPanel(TagManager tagManager, Repository repository) {
		propertyChangeSupport = new PropertyChangeSupport(this);
		this.tagManager = tagManager;
		this.repository = repository;
		userMarkupCollectionManager =
				new UserMarkupCollectionManager(tagManager);
		initComponents();
		initActions();
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

	private void initActions() {
		tagDefChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				Object oldValue = evt.getOldValue();
				Object newValue = evt.getNewValue();
				if ((oldValue == null) && (newValue == null)) {
					return;
				}
				
				if (oldValue == null) {
					//TODO: add TagsetDef, is probably not of relevance since we only add TagDefs, when corresponding markup is added
				}
				else if (newValue == null) {
					@SuppressWarnings("unchecked")
					Pair<TagsetDefinition, TagDefinition> removeOperationResult = 
							(Pair<TagsetDefinition, TagDefinition>)evt.getOldValue();
					TagDefinition td = removeOperationResult.getSecond();
					Object parentId = markupCollectionsTree.getParent(td);
					removeWithChildren(td);
					if ((parentId != null) 
							&& (!markupCollectionsTree.hasChildren(parentId))) {
						markupCollectionsTree.setChildrenAllowed(parentId, false);
					}
				}
				else {
					TagDefinition tagDefinition = 
							(TagDefinition)evt.getNewValue();
					Property captionProp = markupCollectionsTree.getContainerProperty(
							tagDefinition, 
							MarkupCollectionsTreeProperty.caption);
					
					if (captionProp != null) {
						captionProp.setValue(tagDefinition.getName());
					}
					
					boolean selected = Boolean.valueOf(markupCollectionsTree.getItem(
							tagDefinition).getItemProperty(
									MarkupCollectionsTreeProperty.visible).toString());
					if (selected) {
						fireTagDefinitionSelected(tagDefinition, false);
						fireTagDefinitionSelected(tagDefinition, true);
					}
				
				}
				
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefChangedListener);
		
	}

	private void initComponents() {
		
		markupCollectionsTree = new TreeTable();
		markupCollectionsTree.setSizeFull();
		markupCollectionsTree.setSelectable(true);
		markupCollectionsTree.setMultiSelect(false);
		markupCollectionsTree.setContainerDataSource(new HierarchicalContainer());

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
		
		markupCollectionsTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_PROPERTY);
		markupCollectionsTree.setItemCaptionPropertyId(
				MarkupCollectionsTreeProperty.caption);
		markupCollectionsTree.setItemIconPropertyId(MarkupCollectionsTreeProperty.icon);
		markupCollectionsTree.addGeneratedColumn(
				MarkupCollectionsTreeProperty.color, new ColorLabelColumnGenerator());
		markupCollectionsTree.setColumnHeader(
				MarkupCollectionsTreeProperty.color, "Tag color");
		
		markupCollectionsTree.setVisibleColumns(
				new Object[] {
						MarkupCollectionsTreeProperty.caption,
						MarkupCollectionsTreeProperty.color,
						MarkupCollectionsTreeProperty.visible,
						MarkupCollectionsTreeProperty.writable});
		
		markupCollectionsTree.addItem(
			new Object[] {userMarkupItem, new Label(), new Label()}, 
			userMarkupItem);
		
		markupCollectionsTree.addItem(
			new Object[] {staticMarkupItem, new Label(), new Label()}, 
			staticMarkupItem );

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
		addUserMarkupCollection(userMarkupCollection);
	}
	
	private void addUserMarkupCollection(
			UserMarkupCollection userMarkupCollection) {
		markupCollectionsTree.addItem(
				new Object[] {userMarkupCollection, new Label(), createCheckbox(userMarkupCollection)},
				userMarkupCollection);
		markupCollectionsTree.setParent(userMarkupCollection, userMarkupItem);
		
		TagLibrary tagLibrary = userMarkupCollection.getTagLibrary();
		for (TagsetDefinition tagsetDefinition : tagLibrary) {
			ClassResource tagsetIcon = 
					new ClassResource(
						"ui/tagmanager/resources/grndiamd.gif", getApplication());

			markupCollectionsTree.addItem(
					new Object[]{tagsetDefinition.getName(), 
							new Label(), new Label()}, tagsetDefinition);
			markupCollectionsTree.getContainerProperty(
				tagsetDefinition, MarkupCollectionsTreeProperty.icon).setValue(
						tagsetIcon);
			markupCollectionsTree.setParent(tagsetDefinition, userMarkupCollection);
			addTagDefinitions(tagsetDefinition);
		}
	}
	
	private void removeWithChildren(Object itemId) {
		@SuppressWarnings("rawtypes")
		Collection children = markupCollectionsTree.getChildren(itemId);
		
		if (children != null) {
			Object[] childArray = children.toArray();
			for (Object childId : childArray) {
				removeWithChildren(childId);
			}
		}
		markupCollectionsTree.removeItem(itemId);
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
								tagDefinition.getName(), 
								createCheckbox(tagDefinition),
								new Label()},
						tagDefinition);
				markupCollectionsTree.getContainerProperty(
						tagDefinition, MarkupCollectionsTreeProperty.icon).setValue(
								tagIcon);

			}
		}
		for (TagDefinition tagDefinition : tagsetDefinition) {
			String baseID = tagDefinition.getParentID();
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
				boolean selected = 
						event.getButton().booleanValue();

				fireTagDefinitionSelected(tagDefinition, selected);
			}


		});
		return cbShowTagInstances;
	}
	
	private CheckBox createCheckbox(
			final UserMarkupCollection userMarkupCollection) {
		
		CheckBox cbIsWritableUserMarkupColl = new CheckBox();
		cbIsWritableUserMarkupColl.setImmediate(true);
		cbIsWritableUserMarkupColl.addListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				
				boolean selected = 
						event.getButton().booleanValue();
				if (selected) {
					for (UserMarkupCollection umc : userMarkupCollectionManager) {
						if (!umc.equals(userMarkupCollection)) {
							Object writeablePropertyValue = 
								markupCollectionsTree.getItem(
									umc).getItemProperty(
										MarkupCollectionsTreeProperty.writable).getValue();
							
							if ((writeablePropertyValue != null) 
									&& (writeablePropertyValue instanceof CheckBox)) {
								CheckBox cbWritable = (CheckBox)writeablePropertyValue;
								cbWritable.setValue(false);
							}
						}
					}
					currentWritableUserMarkupColl = userMarkupCollection;
					fireUserMarkupCollectionWriteable(
							userMarkupCollection);
				}
			}
		});
		return cbIsWritableUserMarkupColl;
	}
	
	private void fireTagDefinitionSelected(
			TagDefinition tagDefinition, boolean selected) {
		UserMarkupCollection userMarkupCollection =
				getUserMarkupCollection(tagDefinition);
		
		List<TagReference> tagReferences =
				userMarkupCollection.getTagReferences(
						tagDefinition, true);
		
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
					cbVisible.setValue(selected);
				}
			}
		}		
		
		propertyChangeSupport.firePropertyChange(
				MarkupCollectionPanelEvent.tagDefinitionSelected.name(), 
				selected?null:tagReferences,
				selected?tagReferences:null);
	}
	
	private void fireUserMarkupCollectionWriteable(
			UserMarkupCollection userMarkupCollection) {
		propertyChangeSupport.firePropertyChange(
			MarkupCollectionPanelEvent.userMarkupCollectionSelected.name(), 
			null, userMarkupCollection);
		
	}
	
	public void close() {
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefChangedListener);
	}
	
	public void updateTagsetDefinition(TagsetDefinition incomingTagsetDef) {
		List<UserMarkupCollection> modified = 
				userMarkupCollectionManager.updateUserMarkupCollections(
			incomingTagsetDef);
		for (UserMarkupCollection c : modified) {
			removeWithChildren(c);
			addUserMarkupCollection(c);
		}
	}
	
	public void addTagReferences(
			List<TagReference> tagReferences, SourceDocument sourceDocument) {
		userMarkupCollectionManager.addTagReferences(
				tagReferences, currentWritableUserMarkupColl);
		
		try {
			repository.update(currentWritableUserMarkupColl, sourceDocument);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//FIXME: add TagsetDefs to tree!!!
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
}
