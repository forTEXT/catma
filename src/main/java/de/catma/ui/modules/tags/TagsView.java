package de.catma.ui.modules.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.sliderpanel.SliderPanel;
import org.vaadin.sliderpanel.SliderPanelBuilder;
import org.vaadin.sliderpanel.client.SliderMode;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.repository.Repository;
import de.catma.rbac.RBACPermission;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.tagger.annotationpanel.AddEditPropertyDialog;
import de.catma.ui.tagger.annotationpanel.AddParenttagDialog;
import de.catma.ui.tagger.annotationpanel.AddSubtagDialog;
import de.catma.ui.tagger.annotationpanel.EditTagDialog;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class TagsView extends HugeCard {
	
	private EventBus eventBus;
	private Repository project;
	private TreeGrid<TagsetTreeItem> tagsetGrid;
	private ActionGridComponent<TreeGrid<TagsetTreeItem>> tagsetGridComponent;
	private TreeData<TagsetTreeItem> tagsetData;
	private TreeDataProvider<TagsetTreeItem> tagsetDataProvider;
	private IDGenerator idGenerator = new IDGenerator();
	private Collection<TagsetDefinition> tagsets;
	private TagResourcePanel resourcePanel;
	private SliderPanel drawer;

	public TagsView(EventBus eventBus, Repository project) {
		super("Manage Tags");
		this.eventBus = eventBus;
		this.project = project;
		eventBus.register(this);
		initComponents();
		initActions();
		this.tagsets = new ArrayList<>(resourcePanel.getSelectedTagsets());
		initData();
	}

	private void initActions() {
		tagsetGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getColor(), new HtmlRenderer())
			.setCaption("Tagsets")
			.setSortable(false)
			.setWidth(200);
		tagsetGrid.setHierarchyColumn(
			tagsetGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getName())
			.setCaption("Tags")
			.setSortable(false)
			.setWidth(300));
		
		ButtonRenderer<TagsetTreeItem> propertySummaryRenderer = 
				new ButtonRenderer<>(rendererClickEvent -> handlePropertySummaryClickEvent(rendererClickEvent));
		propertySummaryRenderer.setHtmlContentAllowed(true);
		
		tagsetGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getPropertySummary(), 
			propertySummaryRenderer)
		.setCaption("Properties")
		.setSortable(false)
		.setHidable(true)
		.setWidth(300);
		
		tagsetGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getPropertyValue())
		.setCaption("Values")
		.setSortable(false)
		.setHidable(true)
		.setExpandRatio(1);
		
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
	    addContextMenu.addItem("Add Tagset", clickEvent -> handleAddTagsetRequest());
	    addContextMenu.addItem("Add Tag", clickEvent -> handleAddTagRequest());
	    addContextMenu.addItem("Add Subtag", clickEvent -> handleAddSubtagRequest());
	    addContextMenu.addItem("Add Property", clickEvent -> handleAddPropertyRequest());
		
		ContextMenu moreOptionsContextMenu = 
				tagsetGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
		moreOptionsContextMenu.addItem("Edit Tag", clickEvent -> handleEditTagRequest());
		moreOptionsContextMenu.addItem("Delete Tag", clickEvent -> handleDeleteTagRequest());
		moreOptionsContextMenu.addItem("Edit/Delete Properties", clickEvent -> handleEditPropertiesRequest());
		moreOptionsContextMenu.addItem("Edit Tagset", clickEvent -> handleEditTagsetRequest());
		moreOptionsContextMenu.addItem("Delete Tagset", clickEvent -> handleDeleteTagsetRequest());
		
		resourcePanel.setTagsetSelectionListener(selectedTagsets -> {
			tagsets.clear();
			tagsets.addAll(selectedTagsets);
			initData();
		});
	}



	private void initComponents() {
        addStyleName("tags-view");
        
		HorizontalLayout content = new HorizontalLayout();
		content.addStyleName("tags-content");
		
		tagsetGrid = new TreeGrid<>();
		tagsetGrid.addStyleNames(
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");
		tagsetGrid.setSizeFull();
		tagsetGrid.setSelectionMode(SelectionMode.SINGLE);
		tagsetGrid.addStyleName(MaterialTheme.GRID_BORDERLESS);
		
		Label tagsetsLabel = new Label("Tagsets");
        tagsetGridComponent = new ActionGridComponent<TreeGrid<TagsetTreeItem>>(
                tagsetsLabel,
                tagsetGrid
        );
        tagsetGridComponent.addStyleName("tags-view-tagset-grid-component");
        
		resourcePanel = new TagResourcePanel(project); 
		drawer = new SliderPanelBuilder(resourcePanel)
				.mode(SliderMode.LEFT).expanded(false).style("tags-slider").build();
		
		addComponent(content);

        content.addComponent(drawer);
		content.addComponent(tagsetGridComponent);
	}
	
	public void close() {
		eventBus.unregister(this);
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

	private void handleDeleteTagsetRequest() {
		Collection<TagsetDefinition> selectedTagsets = 
			tagsetGrid.getSelectedItems().stream()
			.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagsetDataItem)
			.map(tagsetDataItem -> ((TagsetDataItem)tagsetDataItem).getTagset())
			.collect(Collectors.toList());
		if (selectedTagsets.isEmpty()) {
			Notification.show("Info", "Please select one or more Tagsets first!", Type.HUMANIZED_MESSAGE);
		}
		else {
			for (TagsetDefinition tagset : selectedTagsets) {
				ConfirmDialog.show(
					UI.getCurrent(), 
					"Warning", 
					String.format("Are you sure you want to delete Tagset %1$s and all related data?", tagset.getName()),
					"Yes",
					"Cancel",
					dlg -> {
						if (dlg.isConfirmed()) {
							project.getTagManager().removeTagsetDefinition(tagset);
						}
					});
			}
		}
	}

	private void handleEditTagsetRequest() {
		// TODO this should go to the Tags module, too?
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

	private void initData() {
        try {
            tagsetData = new TreeData<TagsetTreeItem>();
            
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
    
	private void handleAddTagsetRequest() {
    	SingleTextInputDialog tagsetNameDlg = 
        		new SingleTextInputDialog("Add Tagset", "Please enter the Tagset name:",
        				new SaveCancelListener<String>() {
    						
    						@Override
    						public void savePressed(String result) {
    							IDGenerator idGenerator = new IDGenerator();
    							project.getTagManager().addTagsetDefinition(
    								new TagsetDefinition(
    									null, 
    									idGenerator.generate(), result, new Version()));
    						}
    					});
            	
    	tagsetNameDlg.show();
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
	
	public void setTagsets(
			Collection<TagsetDefinition> tagsets) throws IOException {
		this.tagsets.clear();
		this.tagsets.addAll(tagsets);
        
		tagsetData.clear();
        for (TagsetDefinition tagset : tagsets) {
        	TagsetDataItem tagsetItem = new TagsetDataItem(tagset);
        	tagsetData.addItem(null, tagsetItem);
        	addTags(tagsetItem, tagset);
        }
        tagsetDataProvider.refreshAll();
        for (TagsetDefinition tagset : this.tagsets) {
        	expandTagsetDefinition(tagset);
        }
	}	
}
