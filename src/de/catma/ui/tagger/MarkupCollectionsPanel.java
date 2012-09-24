package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.Application;
import com.vaadin.data.Item;
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

import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionManager;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
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
	
	private TreeTable markupCollectionsTree;
	private String userMarkupItem = "User Markup Collections";
	private String staticMarkupItem = "Static Markup Collections";
	private TagManager tagManager;
	
	private PropertyChangeListener tagDefChangedListener;
	private PropertyChangeListener tagsetDefChangedListener;

	private UserMarkupCollectionManager userMarkupCollectionManager;
	private UserMarkupCollection currentWritableUserMarkupColl;
	private PropertyChangeSupport propertyChangeSupport;
	private Repository repository;
	private Set<TagsetDefinition> updateableTagsetDefinitons;
	private PropertyChangeListener userMarkupCollectionChangedListener;
	private Application application;
	private PropertyChangeListener userMarkupCollectionTagLibraryChangedListener;
	
	//TODO: arg TagManager is redundant here, we could ask the repo
	public MarkupCollectionsPanel(TagManager tagManager, Repository repository) {
		propertyChangeSupport = new PropertyChangeSupport(this);
		this.tagManager = tagManager;
		this.repository = repository;
		userMarkupCollectionManager =
				new UserMarkupCollectionManager(tagManager, repository);
		updateableTagsetDefinitons = new HashSet<TagsetDefinition>();
		initComponents();
		initActions();
	}
	
	@Override
	public void attach() {
		super.attach();
		this.application = getApplication();
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
						reloadTagsetDefinition(tagsetDefinition, umc);
					}
					
				}
				
			}
		};
		
		repository.addPropertyChangeListener(
				RepositoryChangeEvent.userMarkupCollectionTagLibraryChanged, 
				userMarkupCollectionTagLibraryChangedListener);
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
		if (((CheckBox)writableProp.getValue()).booleanValue()) {
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
			
			removeWithChildrenFromTree(userMarkupCollection);
			fireWritableUserMarkupCollectionSelected(userMarkupCollection, false);
			userMarkupCollectionManager.remove(userMarkupCollection);
		}
	}

	private void updateTagsetDefinition(TagsetDefinition foreignTagsetDefinition) {
		if (updateableTagsetDefinitons.contains(foreignTagsetDefinition)) {
			List<UserMarkupCollection> outOfSynchCollections = 
					userMarkupCollectionManager.getUserMarkupCollections(
						foreignTagsetDefinition, false);

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
		
		if (updateableTagsetDefinitons.contains(foreignTagsetDefinition)) {
			List<UserMarkupCollection> outOfSynchCollections = 
					userMarkupCollectionManager.getUserMarkupCollections(
						foreignTagsetDefinition, false);

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
		

		if (updateableTagsetDefinitons.contains(foreignTagsetDefinition)) {
			List<UserMarkupCollection> outOfSynchCollections = 
					userMarkupCollectionManager.getUserMarkupCollections(
						foreignTagsetDefinition, false);

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
		
		if (updateableTagsetDefinitons.contains(foreignTagsetDefinition)) {
			
			List<UserMarkupCollection> outOfSynchCollections = 
					userMarkupCollectionManager.getUserMarkupCollections(
						foreignTagsetDefinition, false);

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
				userMarkupCollectionManager.getUserMarkupCollections(
					foreignTagsetDefinition, false);

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
				
				boolean selected = Boolean.valueOf(markupCollectionsTree.getItem(
						tagDefinition).getItemProperty(
								MarkupCollectionsTreeProperty.visible).toString());
				if (selected) {
					fireTagDefinitionSelected(tagDefinition, false);
					fireTagDefinitionSelected(tagDefinition, true);
				}
			}
		}
	}

	private TagsetDefinition getUpdateableTagsetDefinition(String tagDefUuid) {
		for (TagsetDefinition tagsetDef : updateableTagsetDefinitons) {
			if (tagsetDef.hasTagDefinition(tagDefUuid)) {
				return tagsetDef;
			}
		}
		return null;
	}

	private void initComponents() {
		setSizeFull();
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
		addUserMarkupCollectionToTree(userMarkupCollection);
		if (userMarkupCollectionManager.getUserMarkupCollections().size() == 1) {
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
				new Object[] {userMarkupCollection, new Label(), createCheckbox(userMarkupCollection)},
				userMarkupCollection);
		markupCollectionsTree.setParent(userMarkupCollection, userMarkupItem);
		
		TagLibrary tagLibrary = userMarkupCollection.getTagLibrary();
		for (TagsetDefinition tagsetDefinition : tagLibrary) {
			addTagsetDefinitionToTree(tagsetDefinition, userMarkupCollection);
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
				new ClassResource(
					"ui/tagmanager/resources/grndiamd.gif", application);

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

	private void insertTagDefinitionIntoTree(TagDefinition tagDefinition) {
		ClassResource tagIcon = 
				new ClassResource(
					"ui/tagmanager/resources/reddiamd.gif", 
				application);
		
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
		CheckBox cbShowTagInstances = new CheckBox();
		cbShowTagInstances.setImmediate(true);
		cbShowTagInstances.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				boolean selected = 
						event.getButton().booleanValue();
				for (TagDefinition tagDefinition : tagsetDefinition) {
					if (tagDefinition.getParentUuid().isEmpty()) {
						Item tagDefItem =
								markupCollectionsTree.getItem(tagDefinition);
						Property visibleProp = 
								tagDefItem.getItemProperty(
										MarkupCollectionsTreeProperty.visible);
						CheckBox cb = (CheckBox) visibleProp.getValue();
						cb.setValue(selected);
						fireTagDefinitionSelected(tagDefinition, selected);
					}
				}
			}
		});
		return cbShowTagInstances;
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
				handleUserMarkupCollectionSelectionRequest(
						selected, userMarkupCollection);
			}
		});
		return cbIsWritableUserMarkupColl;
	}
	
	private void handleUserMarkupCollectionSelectionRequest(
			boolean selected, UserMarkupCollection userMarkupCollection) {
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
		}
		else {
			currentWritableUserMarkupColl = null;
			
		}
		fireWritableUserMarkupCollectionSelected(
				userMarkupCollection, selected);
	}
	
	private void fireTagDefinitionSelected(
			TagDefinition tagDefinition, boolean selected) {
		UserMarkupCollection userMarkupCollection =
				getUserMarkupCollection(tagDefinition);
		//TODO: why can this be null?
		if (userMarkupCollection != null) {
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
		repository.removePropertyChangeListener(
				RepositoryChangeEvent.userMarkupCollectionChanged, 
				userMarkupCollectionChangedListener);
		repository.removePropertyChangeListener(
				RepositoryChangeEvent.userMarkupCollectionTagLibraryChanged, 
				userMarkupCollectionTagLibraryChangedListener);
	}
	
	public void addOrUpdateTagsetDefinition(
			final TagsetDefinition incomingTagsetDef,
			final ConfirmListener confirmListener) {
		
		final List<UserMarkupCollection> toBeUpdated = 
				userMarkupCollectionManager.getUserMarkupCollections(
						incomingTagsetDef, false);
		
		if (!toBeUpdated.isEmpty()) {
			ConfirmDialog.show(
				application.getMainWindow(), 
				"There are older versions of the Tagset '" +
					incomingTagsetDef.getName() +
					"' in the attached User Markup Collections! " +
					"Do you really want to update the attached Markup Collections?",
							
			        new ConfirmDialog.Listener() {

			            public void onClose(ConfirmDialog dialog) {
			                if (dialog.isConfirmed()) {
			                	updateableTagsetDefinitons.add(incomingTagsetDef);
			                	userMarkupCollectionManager.updateUserMarkupCollections(
			                			toBeUpdated, incomingTagsetDef);
			                	updateUserMarkupCollectionsInTree(toBeUpdated);
			            		confirmListener.confirmed();
			                }
			            }
			        });
		}
		else {
			updateableTagsetDefinitons.add(incomingTagsetDef);
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
		updateableTagsetDefinitons.remove(tagsetDefinition);
	}

	public List<Pair<String,TagInstance>> getTagInstances(List<String> instanceIDs) {
		return userMarkupCollectionManager.getTagInstances(instanceIDs);
	}

	public void removeTagInstances(List<String> tagInstanceIDs) {
		List<TagReference> tagReferences = new ArrayList<TagReference>();
		for (String tagInstanceID : tagInstanceIDs) {
			tagReferences.addAll(
				userMarkupCollectionManager.getTagReferences(tagInstanceID));
			userMarkupCollectionManager.removeTagInstance(tagInstanceID);
		}
		
		propertyChangeSupport.firePropertyChange(
				MarkupCollectionPanelEvent.tagDefinitionSelected.name(), 
				tagReferences,
				null);	
	}
}
