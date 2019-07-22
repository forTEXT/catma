package de.catma.ui.tagger.annotationpanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.vaadin.dialogs.ConfirmDialog;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.repository.Repository;
import de.catma.document.repository.event.ChangeType;
import de.catma.document.repository.event.CollectionChangeEvent;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.Annotation;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionManager;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.rbac.RBACPermission;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.modules.tags.AddEditPropertyDialog;
import de.catma.ui.modules.tags.AddParenttagDialog;
import de.catma.ui.modules.tags.AddSubtagDialog;
import de.catma.ui.modules.tags.EditTagDialog;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class AnnotationPanel extends VerticalLayout {
	
	
	public interface TagReferenceSelectionChangeListener {
		public void tagReferenceSelectionChanged(
				List<TagReference> tagReferences, boolean selected);
	}
	
	private ComboBox<UserMarkupCollection> currentEditableCollectionBox;
	private Button btAddCollection;
	private TreeGrid<TagsetTreeItem> tagsetGrid;
	private Repository project;
	private Collection<TagsetDefinition> tagsets = new ArrayList<>();
	private List<UserMarkupCollection> collections = new ArrayList<>();
	private List<UserMarkupCollection> editableCollections = new ArrayList<>();
	private TagReferenceSelectionChangeListener selectionListener;
	private ActionGridComponent<TreeGrid<TagsetTreeItem>> tagsetGridComponent;
	private TreeData<TagsetTreeItem> tagsetData;
	private TreeDataProvider<TagsetTreeItem> tagsetDataProvider;
	private IDGenerator idGenerator = new IDGenerator();
	private PropertyChangeListener tagChangedListener;
	private PropertyChangeListener propertyDefinitionChangedListener;
	private AnnotationDetailsPanel annotationDetailsPanel;
	private Button btMaximizeAnnotationDetailsRibbon;
	private VerticalSplitPanel rightSplitPanel;
	private UserMarkupCollectionManager collectionManager;
	private Supplier<SourceDocument> currentDocumentProvider;
	private EventBus eventBus;

	public AnnotationPanel(
			Repository project, 
			UserMarkupCollectionManager collectionManager, 
			Consumer<String> annotationSelectionListener,
			Consumer<UserMarkupCollection> collectionSelectionListener,
			Supplier<SourceDocument> currentDocumentProvider,
			EventBus eventBus) {
		this.project = project;
		this.collectionManager = collectionManager;
		this.currentDocumentProvider = currentDocumentProvider;
		this.eventBus = eventBus;
		this.eventBus.register(this);
		this.tagsetData = new TreeData<TagsetTreeItem>();
		initComponents(annotationSelectionListener);
		initActions(collectionSelectionListener);
		initListeners();
		initData();
	}

	private void initListeners() {
		tagChangedListener = new PropertyChangeListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				
				Object newValue = evt.getNewValue();
				Object oldValue = evt.getOldValue();
				
				if (oldValue == null) { //created
					Pair<TagsetDefinition, TagDefinition> value = 
							(Pair<TagsetDefinition, TagDefinition>)newValue;
					
					TagsetDefinition tagset = value.getFirst();
					TagDefinition tag = value.getSecond();
		            if (tag.getParentUuid().isEmpty()) {
		            	TagsetTreeItem tagsetItem = new TagsetDataItem(tagset);
		            	tagsetData.addItem(
		            		tagsetItem, new TagDataItem(tag));
		            	
		            	tagsetGrid.expand(tagsetItem);
		            }
		            else {
		            	TagDefinition parentTag = 
		            		project.getTagManager().getTagLibrary().getTagDefinition(tag.getParentUuid());
		            	TagsetTreeItem parentTagItem = new TagDataItem(parentTag);
		            	tagsetData.addItem(parentTagItem, new TagDataItem(tag));
		            	
		            	tagsetGrid.expand(parentTagItem);
		            }
		            
					tagsetDataProvider.refreshAll();
		            
				}
				else if (newValue == null) { //removed
					Pair<TagsetDefinition,TagDefinition> deleted = (Pair<TagsetDefinition, TagDefinition>) oldValue;
					
					TagDefinition deletedTag = deleted.getSecond();
					
					tagsetData.removeItem(new TagDataItem(deletedTag));
					tagsetDataProvider.refreshAll();
					
				}
				else { //update
					TagDefinition tag = (TagDefinition) newValue;
					TagsetDefinition tagset = (TagsetDefinition)oldValue;
	            	TagsetTreeItem tagsetItem = new TagsetDataItem(tagset);

					tagsetData.removeItem(new TagDataItem(tag));
					TagDataItem tagDataItem = new TagDataItem(tag);
					tagDataItem.setPropertiesExpanded(true);
					tagsetData.addItem(tagsetItem, tagDataItem);
					//TODO: sort
					
					showExpandedProperties(tagDataItem);
					
					tagsetDataProvider.refreshAll();
				}
				
			}
		};
		project.getTagManager().addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);
		
		propertyDefinitionChangedListener = new PropertyChangeListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Object newValue = evt.getNewValue();
				Object oldValue = evt.getOldValue();
				
				TagDefinition tag = null;
				
				if (oldValue == null) { //created
					Pair<PropertyDefinition, TagDefinition> newData =
							(Pair<PropertyDefinition, TagDefinition>) newValue;
					
					tag = newData.getSecond();
					
				}
				else if (newValue == null) { // removed
					Pair<PropertyDefinition, Pair<TagDefinition, TagsetDefinition>> oldData =
							(Pair<PropertyDefinition, Pair<TagDefinition, TagsetDefinition>>) oldValue;
					
					tag = oldData.getSecond().getFirst();
				}
				else { //update
					tag = (TagDefinition) oldValue;
				}
				
				TagsetTreeItem parentItem = null;
				if (tag.getParentUuid().isEmpty()) {
					parentItem = new TagsetDataItem(
						project.getTagManager().getTagLibrary()
							.getTagsetDefinition(tag.getTagsetDefinitionUuid()));
				}
				else {
					parentItem = new TagDataItem(
						project.getTagManager().getTagLibrary().getTagDefinition(tag.getParentUuid()));
				}
				
				final String tagId = tag.getUuid();
				tagsetData.getChildren(parentItem)
				.stream()
				.map(tagsetTreeItem -> (TagDataItem)tagsetTreeItem)
				.filter(tagDataItem -> tagDataItem.getTag().getUuid().equals(tagId))
				.findFirst()
				.ifPresent(tagDataItem -> {
					tagDataItem.setPropertiesExpanded(false);
					hideExpandedProperties(tagDataItem);
					tagDataItem.setPropertiesExpanded(true);
					showExpandedProperties(tagDataItem);
				});
				
				tagsetDataProvider.refreshAll();
			}
		};
		
		project.getTagManager().addPropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged, 
				propertyDefinitionChangedListener);	
			
	}

	@Subscribe
	public void handleCollectionChanged(CollectionChangeEvent collectionChangeEvent) {
		if (collectionChangeEvent.getChangeType().equals(ChangeType.CREATED)) {
    		UserMarkupCollectionReference collectionReference = 
    				collectionChangeEvent.getCollectionReference();
			try {
				addCollection(project.getUserMarkupCollection(collectionReference));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (collectionChangeEvent.getChangeType().equals(ChangeType.DELETED)) {
			removeCollection(collectionChangeEvent.getCollectionReference().getId());
		}
		else {
			currentEditableCollectionBox.getDataProvider().refreshAll();	
		}
	}


	private void initData() {
        try {
            tagsetData.clear();
            
            for (TagsetDefinition tagset : tagsets) {
            	TagsetDataItem tagsetItem = new TagsetDataItem(tagset);
            	tagsetData.addItem(null, tagsetItem);
            	addTags(tagsetItem, tagset);
            }
            tagsetDataProvider = new TreeDataProvider<TagsetTreeItem>(tagsetData);
            tagsetGrid.setDataProvider(tagsetDataProvider);
            for (TagsetDefinition tagset : tagsets) {
            	expandTagsetDefinition(tagset);
            }
            
            editableCollections.clear();
            editableCollections.addAll(
            		collections
            		.stream()
            		.filter(
            				collection -> project.hasPermission(
            						project.getRoleForCollection(collection.getId()), 
            						RBACPermission.COLLECTION_WRITE))
            		.collect(Collectors.toList()));
            	
            
            ListDataProvider<UserMarkupCollection> editableCollectionProvider = 
            		new ListDataProvider<>(editableCollections);
            currentEditableCollectionBox.setValue(null);
            currentEditableCollectionBox.setDataProvider(editableCollectionProvider);
            
            
    		if (!project.isAuthorizedOnProject(RBACPermission.COLLECTION_CREATE)) {
    			if (editableCollectionProvider.getItems().isEmpty()) {
    				currentEditableCollectionBox.setEmptySelectionCaption(
    						"Please contact the Project maintainer to get an editable Collection!");
    			}
    			else {
    				currentEditableCollectionBox.setEmptySelectionCaption(
    						"Please select a Collection...");
    			}
    		}
    		else if (editableCollectionProvider.getItems().isEmpty()) {
				currentEditableCollectionBox.setEmptySelectionCaption(
						"Please create a Collection...");
    		}
    		else {
				currentEditableCollectionBox.setEmptySelectionCaption(
						"Please select a Collection...");    			
    		}
    		
    		if (editableCollectionProvider.getItems().size() == 1) {
    			handleCollectionChangeRequest(editableCollectionProvider.getItems().iterator().next().getUuid());
    		}

    		btAddCollection.setVisible(project.isAuthorizedOnProject(RBACPermission.COLLECTION_CREATE));

        } catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("Error loading data!", e);
        }
    }

    private void expandTagsetDefinition(TagsetDefinition tagset) {
    	for (TagDefinition tag : tagset) {
    		TagDataItem item = new TagDataItem(tag);
    		tagsetGrid.expand(item);
    		if (!tag.getUserDefinedPropertyDefinitions().isEmpty()) {
    			tagsetGrid.setDetailsVisible(item, true);
    		}
    	}
	}

	private void addTags(
			TagsetDataItem tagsetItem, 
			TagsetDefinition tagset) {
		
        for (TagDefinition tag : tagset) {
            if (tag.getParentUuid().isEmpty()) {
            	TagDataItem tagItem =  new TagDataItem(tag);
                tagsetData.addItem(tagsetItem, tagItem);
                addTagSubTree(tagset, tag, tagItem);
            }
        }
	}

	private void addTagSubTree(
    		TagsetDefinition tagset, 
    		TagDefinition tag, TagDataItem parentItem) {
        for (TagDefinition childDefinition : tagset.getDirectChildren(tag)) {
        	TagDataItem childItem = new TagDataItem(childDefinition);
            tagsetData.addItem(parentItem, childItem);
            addTagSubTree(tagset, childDefinition, childItem);
        }
    }

	private void initActions(Consumer<UserMarkupCollection> collectionSelectionListener) {
		tagsetGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getColor(), new HtmlRenderer())
			.setCaption("Tagsets")
			.setSortable(false)
			.setWidth(100);
		tagsetGrid.setHierarchyColumn(
			tagsetGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getName())
			.setCaption("Tags")
			.setSortable(false)
			.setWidth(200));
		
		ButtonRenderer<TagsetTreeItem> propertySummaryRenderer = 
				new ButtonRenderer<>(rendererClickEvent -> handlePropertySummaryClickEvent(rendererClickEvent));
		propertySummaryRenderer.setHtmlContentAllowed(true);
		
		tagsetGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getPropertySummary(), 
			propertySummaryRenderer)
		.setCaption("Properties")
		.setSortable(false)
		.setHidable(true)
		.setWidth(100);
		
		tagsetGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getPropertyValue())
		.setCaption("Values")
		.setSortable(false)
		.setHidable(true)
		.setWidth(100);
			
		
		ButtonRenderer<TagsetTreeItem> visibilityRenderer = 
			new ButtonRenderer<TagsetTreeItem>(rendererClickEvent -> handleVisibilityClickEvent(rendererClickEvent));
		visibilityRenderer.setHtmlContentAllowed(true);
		tagsetGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getVisibilityIcon(), 
			visibilityRenderer)
		.setWidth(80)
		.setSortable(false);
		
		tagsetGrid.setStyleGenerator(new StyleGenerator<TagsetTreeItem>() {
			
			@Override
			public String apply(TagsetTreeItem item) {
				return item.generateStyle();
			}
		});
		
		tagsetGridComponent.setSearchFilterProvider(new SearchFilterProvider<TagsetTreeItem>() {
			@Override
			public SerializablePredicate<TagsetTreeItem> createSearchFilter(String searchInput) {
				return new SerializablePredicate<TagsetTreeItem>() {
					@Override
					public boolean test(TagsetTreeItem t) {
						return testWithChildren(t);
					}
					
					private boolean testTagsetTreeItem(TagsetTreeItem t) {
						String strValue = t.toString();
						
						if (strValue != null && strValue.startsWith(searchInput)) {
							return true;
						}

						
						return false;
					}
					
					public boolean testWithChildren(TagsetTreeItem t) {
						if (t == null) {
							return false;
						}
						
						if (testTagsetTreeItem(t)) {
							return true;
						}
						
						for (TagsetTreeItem child : tagsetData.getChildren(t)) {
							if (testWithChildren(child)) {
								return true;
							}
						}
						
						return false;
					}
				};
			}
		});
		
        ContextMenu addContextMenu = 
        		tagsetGridComponent.getActionGridBar().getBtnAddContextMenu();
        addContextMenu.addItem("Add Tag", clickEvent -> handleAddTagRequest());
        addContextMenu.addItem("Add Subtag", clickEvent -> handleAddSubtagRequest());
        addContextMenu.addItem("Add Property", clickEvent -> handleAddPropertyRequest());
		
		ContextMenu moreOptionsContextMenu = 
				tagsetGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
		moreOptionsContextMenu.addItem("Edit Tag", clickEvent -> handleEditTagRequest());
		moreOptionsContextMenu.addItem("Delete Tag", clickEvent -> handleDeleteTagRequest());
		moreOptionsContextMenu.addItem("Edit/Delete Properties", clickEvent -> handleEditPropertiesRequest());
		
		currentEditableCollectionBox.addValueChangeListener(
			event -> collectionSelectionListener.accept(event.getValue()));
		annotationDetailsPanel.addMinimizeButtonClickListener(
				clickEvent -> setAnnotationDetailsPanelVisible(false));
		btMaximizeAnnotationDetailsRibbon.addClickListener(
				ClickEvent -> setAnnotationDetailsPanelVisible(true));
		
		btAddCollection.addClickListener(clickEvent -> handelAddCollectionRequest());
		
	}

	private void handelAddCollectionRequest() {
		final SourceDocument document = currentDocumentProvider.get();
		
		if (document != null) {
	    	SingleTextInputDialog collectionNameDlg = 
	        		new SingleTextInputDialog("Add Annotation Collection", "Please enter the Collection name:",
	        				new SaveCancelListener<String>() {
	    						
	    						@Override
	    						public void savePressed(String result) {
	   								project.createUserMarkupCollection(result, document);
	    						}
	    					});
	        	
        	collectionNameDlg.show();
		}
		else {
			Notification.show("Info", "Please select a Document first!", Type.HUMANIZED_MESSAGE);
		}
	}

	private void handleEditPropertiesRequest() {
		handleAddPropertyRequest();
	}

	private void handleDeleteTagRequest() {
		final List<TagDefinition> targetTags = tagsetGrid.getSelectedItems()
		.stream()
		.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagDataItem)
		.map(tagsetTreeItem -> ((TagDataItem)tagsetTreeItem).getTag())
		.collect(Collectors.toList());
		if (!targetTags.isEmpty()) {
			for (TagDefinition targetTag : targetTags) {
				if (!project.hasPermission(
						project.getRoleForTagset(targetTag.getTagsetDefinitionUuid()), 
						RBACPermission.TAGSET_WRITE)) {
					Notification.show(
						"Info", 
						String.format(
							"You do not have the permission to make changes to the Tagset of Tag %1$s, "
							+ "Please contact the Project maintainer!", 
							targetTag.getName()), 
						Type.HUMANIZED_MESSAGE);
					return;
				}			
			}
			String msg = String.format(
				"Are you sure you want to delete the following Tags: %1$s?", 
				targetTags
				.stream()
				.map(TagDefinition::getName)
				.collect(Collectors.joining(",")));
			
			ConfirmDialog.show(UI.getCurrent(), "Warning", msg, "Delete", "Cancel", dlg -> {
				if (dlg.isConfirmed()) {
					for (TagDefinition tag : targetTags) {
						TagsetDefinition tagset =
								project.getTagManager().getTagLibrary().getTagsetDefinition(tag);
						project.getTagManager().removeTagDefinition(tagset, tag);
					}
				}
			});
		}
		else {
			Notification.show("Info", "Please select one or more Tags first!", Type.TRAY_NOTIFICATION);
		}
	}

	private void handleEditTagRequest() {
		final List<TagDefinition> targetTags = tagsetGrid.getSelectedItems()
		.stream()
		.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagDataItem)
		.map(tagsetTreeItem -> ((TagDataItem)tagsetTreeItem).getTag())
		.collect(Collectors.toList());
		
		if (targetTags.isEmpty()) {
			Notification.show("Info", "Please select a Tag first!", Type.TRAY_NOTIFICATION);
		}
		else if (targetTags.size() > 1) {
			handleAddPropertyRequest();
		}
		else {
			
			final TagDefinition targetTag = targetTags.get(0);
			
			if (!project.hasPermission(
					project.getRoleForTagset(targetTag.getTagsetDefinitionUuid()), 
					RBACPermission.TAGSET_WRITE)) {
				Notification.show(
					"Info", 
					String.format(
						"You do not have the permission to make changes to the Tagset of Tag %1$s, "
						+ "Please contact the Project maintainer!", 
						targetTag.getName()), 
					Type.HUMANIZED_MESSAGE);
				return;
			}			
			
			EditTagDialog editTagDialog = new EditTagDialog(new TagDefinition(targetTag), 
					new SaveCancelListener<TagDefinition>() {
						public void savePressed(TagDefinition result) {
							
							project.getTagManager().updateTagDefinition(targetTag, result);
							
							//TODO: reload on error
						};
					});
			editTagDialog.show();
		}
		
	}

	private void handleAddPropertyRequest() {
		final List<TagDefinition> targetTags = new ArrayList<>();
		if (tagsetGrid.getSelectedItems().size() == 1) {
			TagsetTreeItem selectedItem = 
				tagsetGrid.getSelectedItems().iterator().next();
			
			while (!(selectedItem instanceof TagDataItem) && (selectedItem != null)) {
				selectedItem = tagsetData.getParent(selectedItem);
			}
			
			if (selectedItem != null) {
				targetTags.add(((TagDataItem)selectedItem).getTag());
			}
		}
		else {
			targetTags.addAll(
				tagsetGrid.getSelectedItems()
				.stream()
				.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagDataItem)
				.map(tagsetTreeItem -> ((TagDataItem)tagsetTreeItem).getTag())
				.collect(Collectors.toList()));
		}
		
		if (targetTags.isEmpty()) {
			Notification.show("Info", "Please select one ore more Tags first!", Type.TRAY_NOTIFICATION);
		}
		else {
			
			for (TagDefinition targetTag : targetTags) {
				if (!project.hasPermission(
						project.getRoleForTagset(targetTag.getTagsetDefinitionUuid()), 
						RBACPermission.TAGSET_WRITE)) {
					Notification.show(
						"Info", 
						String.format(
							"You do not have the permission to make changes to the Tagset of Tag %1$s, "
							+ "Please contact the Project maintainer!", 
							targetTag.getName()), 
						Type.HUMANIZED_MESSAGE);
					return;
				}
			}			
			
			
			Multimap<String, PropertyDefinition> propertiesByName = 
					ArrayListMultimap.create();
			
			for (TagDefinition tag : targetTags) {
				for (PropertyDefinition propertyDef : tag.getUserDefinedPropertyDefinitions()) {
					if (!propertiesByName.containsKey(propertyDef.getName()) || 
							propertiesByName.get(propertyDef.getName()).iterator().next().getPossibleValueList()
								.equals(propertyDef.getPossibleValueList())) {
						propertiesByName.put(propertyDef.getName(), propertyDef);
					}
				}
			}
			
			List<PropertyDefinition> commonProperties = 
				propertiesByName.asMap().entrySet()
				.stream()
				.filter(entry -> entry.getValue().size() == targetTags.size())
				.map(entry -> new PropertyDefinition(entry.getValue().iterator().next()))
				.collect(Collectors.toList());
			final boolean bulkEdit = targetTags.size() > 1; // just a single tag's properties or is it a bulk(>1) edit?
					
			AddEditPropertyDialog addPropertyDialog = new AddEditPropertyDialog(
				bulkEdit,
				commonProperties,
				new SaveCancelListener<List<PropertyDefinition>>() {
					@Override
					public void savePressed(List<PropertyDefinition> result) {
						if (bulkEdit) {
							handleBulkEditProperties(result,
									commonProperties, targetTags);
						}
						else {
							handleSingleEditProperties(result, targetTags.iterator().next());
						}
						
					}
			});
			
			addPropertyDialog.show();
		}
	}

	private void handleSingleEditProperties(List<PropertyDefinition> editedPropertyDefs, TagDefinition tag) {
		TagsetDefinition tagset = 
				project.getTagManager().getTagLibrary().getTagsetDefinition(tag);
		
		for (PropertyDefinition existingPropertyDef : 
			new ArrayList<>(tag.getUserDefinedPropertyDefinitions())) {
			
			//handle deleted PropertyDefs
			if (!editedPropertyDefs.contains(existingPropertyDef)) {
				project.getTagManager().removeUserDefinedPropertyDefinition(
						existingPropertyDef, tag, tagset);
			}
			//handle updated PropertyDefs
			else {
				editedPropertyDefs
					.stream()
					.filter(possiblyChangedPd -> 
						possiblyChangedPd.getUuid().equals(existingPropertyDef.getUuid()))
					.findFirst()
					.ifPresent(editedPropertyDef -> {
						existingPropertyDef.setName(editedPropertyDef.getName());
						existingPropertyDef.setPossibleValueList(
							editedPropertyDef.getPossibleValueList());
					});
				
				project.getTagManager().updateUserDefinedPropertyDefinition(
					tag, existingPropertyDef);
			}
		}
		
		//handle created PropertyDefs
		for (PropertyDefinition pd : editedPropertyDefs) {
			if (tag.getPropertyDefinitionByUuid(pd.getUuid()) == null) {
				PropertyDefinition createdPropertyDefinition = 
						new PropertyDefinition(pd);
				pd.setUuid(idGenerator.generate());
				
				project.getTagManager().addUserDefinedPropertyDefinition(
					tag, createdPropertyDefinition);
			}
		}
	}

	private void handleBulkEditProperties(
		List<PropertyDefinition> editedProperties, 
		List<PropertyDefinition> commonProperties,
		List<TagDefinition> targetTags) {
		final Set<String> availableCommonPropertyNames = 
				editedProperties.stream().map(propertyDef -> propertyDef.getName())
				.collect(Collectors.toSet());
		
		final Set<String> deletedCommonProperyNames = commonProperties
		.stream()
		.map(propertyDef -> propertyDef.getName())
		.filter(name -> !availableCommonPropertyNames.contains(name))
		.collect(Collectors.toSet());
		
		for (TagDefinition tag : targetTags) {
			TagsetDefinition tagset = 
				project.getTagManager().getTagLibrary().getTagsetDefinition(tag);

			for (PropertyDefinition existingPropertyDef : 
				new ArrayList<>(tag.getUserDefinedPropertyDefinitions())) {
				
				//handle deleted PropertyDefs
				if (deletedCommonProperyNames.contains(existingPropertyDef.getName())) {
					project.getTagManager().removeUserDefinedPropertyDefinition(
							existingPropertyDef, tag, tagset);
				}
				//handle updated PropertyDefs
				else if (availableCommonPropertyNames.contains(existingPropertyDef.getName())) {
					editedProperties
					.stream()
					.filter(possiblyChangedPd -> 
						possiblyChangedPd.getName().equals(existingPropertyDef.getName()))
					.findFirst()
					.ifPresent(possiblyChangedPd -> 
						existingPropertyDef.setPossibleValueList(
							possiblyChangedPd.getPossibleValueList()));
					
					project.getTagManager().updateUserDefinedPropertyDefinition(
						tag, existingPropertyDef);
				}
			}
			
			//handle created PropertyDefs
			for (PropertyDefinition pd : editedProperties) {
				if (tag.getPropertyDefinition(pd.getName()) == null) {
					PropertyDefinition createdPropertyDefinition = 
							new PropertyDefinition(pd);
					pd.setUuid(idGenerator.generate());
					
					project.getTagManager().addUserDefinedPropertyDefinition(
						tag, createdPropertyDefinition);
				}
			}
		}
	}

	private void handleAddSubtagRequest() {
		final List<TagDefinition> parentTags = tagsetGrid.getSelectedItems()
		.stream()
		.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagDataItem)
		.map(tagsetTreeItem -> ((TagDataItem)tagsetTreeItem).getTag())
		.collect(Collectors.toList());
		
		for (TagDefinition parentTag : parentTags) {
			if (!project.hasPermission(project.getRoleForTagset(
					parentTag.getTagsetDefinitionUuid()), 
					RBACPermission.TAGSET_WRITE)) {
				
				Notification.show(
					"Info", 
					String.format(
						"You do not have the permission to make changes to the Tagset of Tag %1$s, "
						+ "Please contact the Project maintainer!", 
						parentTag.getName()), 
					Type.HUMANIZED_MESSAGE);
				return;
			}
		}
		
		if (!parentTags.isEmpty()) {
			AddSubtagDialog addTagDialog =
				new AddSubtagDialog(new SaveCancelListener<TagDefinition>() {
					public void savePressed(TagDefinition result) {
						for (TagDefinition parent : parentTags) {
							
							TagsetDefinition tagset = 
								project.getTagManager().getTagLibrary().getTagsetDefinition(parent);
							
							TagDefinition tag = new TagDefinition(result);
							tag.setUuid(idGenerator.generate());
							tag.setParentUuid(parent.getUuid());
							tag.setTagsetDefinitionUuid(tagset.getUuid());
							
							project.getTagManager().addTagDefinition(
									tagset, tag);
						}
					};
				});
			addTagDialog.show();
		}
		else {
			Notification.show("Info", "Please select at least one parent Tag!", Type.HUMANIZED_MESSAGE);
		}
	}

	private void handleAddTagRequest() {
		
		final Optional<TagsetDefinition> selectedTagset = tagsetGrid.getSelectedItems()
			.stream()
			.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagsetDataItem)
			.findFirst()
			.map(tagsetTreeItem -> ((TagsetDataItem)tagsetTreeItem).getTagset());
			
		if (selectedTagset.isPresent() 
				&& !project.hasPermission(
						project.getRoleForTagset(selectedTagset.get().getUuid()), 
						RBACPermission.TAGSET_WRITE)) {
			Notification.show(
				"Info", 
				String.format(
					"You do not have the permission to make changes to Tagset %1$s, "
					+ "Please contact the Project maintainer!", 
					selectedTagset.get().getName()), 
				Type.HUMANIZED_MESSAGE);
			return;
		}
		
		
		if (tagsets.isEmpty()) {
			if (project.isAuthorizedOnProject(RBACPermission.TAGSET_CREATE_OR_UPLOAD)) {
				Notification.show(
					"Info", 
					"You do not have any Tagsets to add Tags to yet, please create a Tagset first!", 
					Type.HUMANIZED_MESSAGE);
			}
			else {
				Notification.show(
					"Info", 
					"You do not have any Tagsets to add Tags to yet, please contact the Project maintainer!", 
					Type.HUMANIZED_MESSAGE);
			}
			return;
		}
	
		List<TagsetDefinition> editableTagsets = 
				tagsets.stream()
				.filter(tagset -> project.hasPermission(project.getRoleForTagset(tagset.getUuid()), RBACPermission.TAGSET_WRITE))
				.collect(Collectors.toList());
		if (editableTagsets.isEmpty()) {
			Notification.show(
				"Info",
				"You do not have the permission to make changes to any of the available Tagsets! "
				+ "Please contact the Project maintainer for changes!",
				Type.HUMANIZED_MESSAGE);
		}
		AddParenttagDialog addTagDialog = 
			new AddParenttagDialog(
				editableTagsets, 
				selectedTagset, 
				new SaveCancelListener<Pair<TagsetDefinition, TagDefinition>>() {
				
				@Override
				public void savePressed(Pair<TagsetDefinition, TagDefinition> result) {
					project.getTagManager().addTagDefinition(
							result.getFirst(), result.getSecond());
				}
			});
		addTagDialog.show();
		
	}

	private void handlePropertySummaryClickEvent(RendererClickEvent<TagsetTreeItem> rendererClickEvent) {
		if (rendererClickEvent.getItem() instanceof TagDataItem) {
			TagDataItem tagDataItem = (TagDataItem) rendererClickEvent.getItem();
			
			tagDataItem.setPropertiesExpanded(!tagDataItem.isPropertiesExpanded());
			
			if (tagDataItem.isPropertiesExpanded()) {
				showExpandedProperties(tagDataItem);
			}
			else {
				hideExpandedProperties(tagDataItem);
			}
			tagsetDataProvider.refreshAll();
		}
		else if (rendererClickEvent.getItem() instanceof PropertyDataItem) {
			PropertyDataItem propertyDataItem= (PropertyDataItem)rendererClickEvent.getItem();
			
			propertyDataItem.setValuesExpanded(!propertyDataItem.isValuesExpanded());
			
			if (propertyDataItem.isValuesExpanded()) {
				showExpandedPossibleValues(propertyDataItem);
			}
			else {
				hideExpandedPossibleValues(propertyDataItem);
			}
			tagsetDataProvider.refreshAll();
		}
	}

	private void hideExpandedPossibleValues(PropertyDataItem propertyDataItem) {
		TreeData<TagsetTreeItem> tagsetTreeData = tagsetGrid.getTreeData();
		
		for (TagsetTreeItem childTagsetTreeItem : new ArrayList<>(tagsetTreeData.getChildren(propertyDataItem))) {
			childTagsetTreeItem.removePropertyDataItem(tagsetDataProvider);
		}
	}

	private void showExpandedPossibleValues(PropertyDataItem propertyDataItem) {
		PropertyDefinition propertyDefinition = propertyDataItem.getPropertyDefinition();
		
		for (String possibleValue : propertyDefinition.getPossibleValueList()) {
			tagsetGrid.getTreeData().addItem(
				new PropertyDataItem(propertyDefinition), 
				new PossibleValueDataItem(possibleValue));
		}
		
		tagsetGrid.expand(propertyDataItem);
	}

	private void showExpandedProperties(TagDataItem tagDataItem) {
		TagDefinition tag = tagDataItem.getTag();
		
		PropertyDataItem lastPropertyDataItem = null; 
		for (PropertyDefinition propertyDefinition : tag.getUserDefinedPropertyDefinitions()) {
			lastPropertyDataItem = new PropertyDataItem(propertyDefinition);
			tagsetGrid.getTreeData().addItem(tagDataItem, lastPropertyDataItem);
		}
		
		List<TagsetTreeItem> children = 
			tagsetData.getChildren(tagDataItem).stream()
			.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagDataItem)
			.collect(Collectors.toList());
		
		for (int i = children.size()-1; i>=0; i--) {
			tagsetData.moveAfterSibling(children.get(i), lastPropertyDataItem);
		}
		
		tagsetGrid.expand(tagDataItem);
	}

	private void hideExpandedProperties(TagDataItem tagDataItem) {
		TreeData<TagsetTreeItem> tagsetTreeData = tagsetGrid.getTreeData();
		
		for (TagsetTreeItem childTagsetTreeItem : new ArrayList<>(tagsetTreeData.getChildren(tagDataItem))) {
			childTagsetTreeItem.removePropertyDataItem(tagsetDataProvider);
		}
	}

	private void handleVisibilityClickEvent(RendererClickEvent<TagsetTreeItem> rendererClickEvent) {
		rendererClickEvent.getItem().setVisible(!rendererClickEvent.getItem().isVisible());
		tagsetDataProvider.refreshItem(rendererClickEvent.getItem());
		
		TagsetTreeItem tagsetTreeItem = rendererClickEvent.getItem();
		List<TagReference> tagReferences = tagsetTreeItem.getTagReferences(collections);
		
		boolean selected = rendererClickEvent.getItem().isVisible();
		
		if (selectionListener != null) {
			selectionListener.tagReferenceSelectionChanged(tagReferences, selected);
		}
		
		tagsetTreeItem.setChildrenVisible(
				tagsetDataProvider, selected, false);
	}

	private void initComponents(Consumer<String> annotationSelectionListener) {
		setSizeFull();
		setSpacing(true);
		
		currentEditableCollectionBox = new ComboBox<>("Collection currently being edited");
		currentEditableCollectionBox.setWidth("100%");
		currentEditableCollectionBox.setEmptySelectionCaption(
				"Please select a Document first!");

		btAddCollection = new IconButton(VaadinIcons.PLUS);
		btAddCollection.setVisible(project.isAuthorizedOnProject(RBACPermission.COLLECTION_CREATE));
		
		HorizontalLayout editableCollectionPanel = 
				new HorizontalLayout(currentEditableCollectionBox, btAddCollection);
		editableCollectionPanel.addStyleName("annotate-right-padding");
		
		editableCollectionPanel.setWidth("100%");
		editableCollectionPanel.setExpandRatio(currentEditableCollectionBox, 1.0f);
		editableCollectionPanel.setComponentAlignment(btAddCollection, Alignment.BOTTOM_CENTER);
		
		addComponent(editableCollectionPanel);
		
		Label tagsetsLabel = new Label("Tagsets");
		
		tagsetGrid = new TreeGrid<>();
		tagsetGrid.addStyleNames(
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");
		tagsetGrid.setSizeFull();
		tagsetGrid.setSelectionMode(SelectionMode.SINGLE);
		tagsetGrid.addStyleName(MaterialTheme.GRID_BORDERLESS);
		
        tagsetGridComponent = new ActionGridComponent<TreeGrid<TagsetTreeItem>>(
                tagsetsLabel,
                tagsetGrid
        );
        tagsetGridComponent.setMargin(false);
        
        rightSplitPanel = new VerticalSplitPanel();
        rightSplitPanel.setSizeFull();
        rightSplitPanel.setSplitPosition(90);
        rightSplitPanel.setLocked(true);
        
        addComponent(rightSplitPanel);
        setExpandRatio(rightSplitPanel, 1.0f);
        
        rightSplitPanel.addComponent(tagsetGridComponent);
        
        btMaximizeAnnotationDetailsRibbon = new IconButton(VaadinIcons.ANGLE_DOUBLE_UP);
        btMaximizeAnnotationDetailsRibbon.addStyleName("annotation-panel-button-right-align");
        rightSplitPanel.addComponent(btMaximizeAnnotationDetailsRibbon);
        
        annotationDetailsPanel = new AnnotationDetailsPanel(
        		project, 
        		collectionManager,
        		annotationSelectionListener,
        		collectionId -> currentEditableCollectionBox.getValue() != null 
        			&& currentEditableCollectionBox.getValue().getUuid().contentEquals(collectionId),
        		collectionId -> handleCollectionChangeRequest(collectionId));
	}

	private void handleCollectionChangeRequest(String collectionId) {
		collections.stream()
		.filter(collection -> collection.getUuid().equals(collectionId))
		.findFirst()
		.ifPresent(collection -> {
			currentEditableCollectionBox.setValue(collection);
			Notification.show("Info", 
				String.format(
					"The Collection currently being edited has been changed to '%1$s'!",  
					collection.getName()),
				Type.HUMANIZED_MESSAGE);
		});
	}

	public void setData(
			SourceDocument document, 
			Collection<TagsetDefinition> tagsets, 
			List<UserMarkupCollection> collections) throws IOException {
		this.tagsets = tagsets;
		this.collections = collections;
		this.annotationDetailsPanel.setDocument(document);
		initData();
	}
	
	public void setTagReferenceSelectionChangeListener(TagReferenceSelectionChangeListener selectionListener) {
		this.selectionListener = selectionListener;
	}
	
	public UserMarkupCollection getSelectedEditableCollection() {
		return currentEditableCollectionBox.getValue();
	}

	public void addCollection(UserMarkupCollection collection) {
		this.collections.add(collection);
		if (project.hasPermission(
			project.getRoleForCollection(collection.getId()), 
			RBACPermission.COLLECTION_WRITE)) {
			this.editableCollections.add(collection);
			currentEditableCollectionBox.getDataProvider().refreshAll();	
			if ((currentEditableCollectionBox.getValue() == null) 
					&& !this.editableCollections.isEmpty()) {
				currentEditableCollectionBox.setValue(collection);
				
			}
		}			
		//TODO: show Annotations from this collection and selected Tagsets
	}
	
	private void removeCollection(UserMarkupCollection collection) {
		if ((currentEditableCollectionBox.getValue() != null) 
				&& currentEditableCollectionBox.getValue().equals(collection)) {
			currentEditableCollectionBox.setValue(null);
		}
		collections.remove(collection);
		editableCollections.remove(collection);
		
		currentEditableCollectionBox.getDataProvider().refreshAll();	
		//TODO: hide annotations from selected tagsets and this collection
		annotationDetailsPanel.removeAnnotations(
			collection.getTagReferences().stream().map(
					tr -> tr.getTagInstanceID()).collect(Collectors.toSet()));
	}

	public void removeCollection(String collectionId) {
		collections
			.stream()
			.filter(collection -> collection.getUuid().equals(collectionId))
			.findFirst()
			.ifPresent(collection -> removeCollection(collection));
	}
	
	public void setTagsets(Collection<TagsetDefinition> tagsets) {
		tagsets
		.stream()
		.filter(tagset -> !this.tagsets.contains(tagset))
		.forEach(tagset -> addTagset(tagset));
		
		this.tagsets.stream()
		.filter(tagset -> !tagsets.contains(tagset))
		.collect(Collectors.toList())
		.stream()
		.forEach(tagset -> removeTagset(tagset));
	}

	public void addTagset(TagsetDefinition tagset) {
		tagsets.add(tagset);
		TagsetDataItem tagsetItem = new TagsetDataItem(tagset);
		tagsetData.addRootItems(tagsetItem);
		addTags(tagsetItem, tagset);
		expandTagsetDefinition(tagset);
		tagsetDataProvider.refreshAll();
	}
	
	public void removeTagset(TagsetDefinition tagset) {
		tagsets.remove(tagset);
		tagsetData.removeItem(new TagsetDataItem(tagset));
		tagsetDataProvider.refreshAll();
	}
	
	public void close() {
		eventBus.unregister(this);
		annotationDetailsPanel.close();
		project.getTagManager().removePropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged, 
				propertyDefinitionChangedListener);	
		project.getTagManager().removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);			
	}
	
	private void setAnnotationDetailsPanelVisible(boolean visible) {
		if (visible && (annotationDetailsPanel.getParent() == null)){
			rightSplitPanel.removeComponent(btMaximizeAnnotationDetailsRibbon);
			rightSplitPanel.addComponent(annotationDetailsPanel);
			rightSplitPanel.setSplitPosition(50);
			rightSplitPanel.setMinSplitPosition(1, Unit.PERCENTAGE);
			rightSplitPanel.setLocked(false);
		}
		else if (btMaximizeAnnotationDetailsRibbon.getParent() == null){
			rightSplitPanel.removeComponent(annotationDetailsPanel);
			rightSplitPanel.addComponent(btMaximizeAnnotationDetailsRibbon);
			rightSplitPanel.setSplitPosition(90);		
			rightSplitPanel.setLocked(true);
		}
	}

	public void showAnnotationDetails(Collection<Annotation> annotations) throws IOException {
		if (annotationDetailsPanel.getParent() == null) {
			setAnnotationDetailsPanelVisible(true);
		}
		annotationDetailsPanel.addAnnotations(annotations);
		
	}
	
	public List<TagReference> getVisibleTagReferences(Collection<TagReference> tagReferences) {
		return tagReferences.stream().filter(tagRef -> isVisible(tagRef)).collect(Collectors.toList());
	}

	private boolean isVisible(TagReference tagRef) {

		String tagId = tagRef.getTagDefinitionId();
		
		for (TagsetTreeItem tagsetTreeItem : tagsetData.getRootItems()) {
			if (tagsetTreeItem instanceof TagsetDataItem) {
				boolean visible = tagsetTreeItem.isVisible();
				TagsetDefinition tagset = ((TagsetDataItem)tagsetTreeItem).getTagset();
				if (tagset.hasTagDefinition(tagId)) {
					if (visible) {
						return true;
					}
					else {
						TagsetTreeItem tagItem = findTagItem(tagsetTreeItem, tagId);
						if (tagItem != null) {
							return tagItem.isVisible();
						}
					}
				}
			}
		}
		
		return false;
	}

	private TagsetTreeItem findTagItem(TagsetTreeItem parentItem, String tagId) {
		for (TagsetTreeItem item : tagsetData.getChildren(parentItem)) {
			if ((item instanceof TagDataItem) && ((TagDataItem)item).getTag().getUuid().equals(tagId)) {
				return item;
			}
			
			findTagItem(item, tagId);
		}
		
		
		return null;
	}

	public void removeAnnotations(Collection<String> annotationIds) {
		annotationDetailsPanel.removeAnnotations(annotationIds);
	}
}
