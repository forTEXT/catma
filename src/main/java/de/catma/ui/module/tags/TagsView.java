package de.catma.ui.module.tags;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.project.Project;
import de.catma.project.Project.RepositoryChangeEvent;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.TagsetMetadata;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.dialog.BeyondResponsibilityConfirmDialog;
import de.catma.ui.dialog.BeyondResponsibilityConfirmDialog.Action;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.project.EditTagsetDialog;
import de.catma.user.Member;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class TagsView extends HugeCard {
	
	private EventBus eventBus;
	private Project project;
	private TreeGrid<TagsetTreeItem> tagsetGrid;
	private ActionGridComponent<TreeGrid<TagsetTreeItem>> tagsetGridComponent;
	private TreeData<TagsetTreeItem> tagsetData;
	private TreeDataProvider<TagsetTreeItem> tagsetDataProvider;
	private IDGenerator idGenerator = new IDGenerator();
	private Collection<TagsetDefinition> tagsets;
	private TagResourcePanel resourcePanel;
	private SliderPanel drawer;
	private PropertyChangeListener tagChangedListener;
	private PropertyChangeListener propertyDefinitionChangedListener;
	private PropertyChangeListener projectExceptionListener;

	public TagsView(EventBus eventBus, Project project) {
		super("Manage Tags");
		this.eventBus = eventBus;
		this.project = project;
		eventBus.register(this);
		initComponents();
		initActions();
		this.tagsets = new ArrayList<>(resourcePanel.getSelectedTagsets());
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
		            	tagsetData.getRootItems()
		            		.stream()
		            		.map(tagsetTreeItem -> (TagsetDataItem)tagsetTreeItem)
		            		.filter(tagsetDataItem -> tagsetDataItem.getTagset().getUuid().equals(tagset.getUuid()))
		            		.findFirst()
		            		.ifPresent(tagsetDataItem -> {
		            			TagDataItem tagDataItem = 
		            					new TagDataItem(tag, tagsetDataItem.isEditable());
		            			if (!tagsetData.contains(tagDataItem)) {
			            			tagsetData.addItem(
			            					tagsetDataItem, tagDataItem);
			            			tagsetDataProvider.refreshAll();
			            			
			            			tagsetGrid.expand(tagsetDataItem);
		            			}
		            		});
		            }
		            else {
		            	tagsetData.getRootItems()
	            		.stream()
	            		.map(tagsetTreeItem -> (TagsetDataItem)tagsetTreeItem)
	            		.filter(tagsetDataItem -> tagsetDataItem.getTagset().getUuid().equals(tagset.getUuid()))
	            		.findFirst()
	            		.ifPresent(tagsetDataItem -> {		            
	            			TagDataItem tagDataItem = new TagDataItem(tag, tagsetDataItem.isEditable());
	            			if (!tagsetData.contains(tagDataItem)) {
		            			TagDefinition parentTag = 
			            		project.getTagManager().getTagLibrary().getTagDefinition(tag.getParentUuid());
			            	
				            	TagsetTreeItem parentTagItem = new TagDataItem(parentTag, tagsetDataItem.isEditable());
				            	tagsetData.addItem(parentTagItem, tagDataItem);
		
				            	tagsetDataProvider.refreshAll();
		
				            	tagsetGrid.expand(parentTagItem);
	            			}
	            		});
		            }
		            
				}
				else if (newValue == null) { //removed
					Pair<TagsetDefinition,TagDefinition> deleted = (Pair<TagsetDefinition, TagDefinition>) oldValue;
					
					TagDefinition deletedTag = deleted.getSecond();
					
					tagsetData.removeItem(new TagDataItem(deletedTag, true));
					tagsetDataProvider.refreshAll();
					tagsetGrid.deselectAll();
					
				}
				else { //update
					TagDefinition tag = (TagDefinition) newValue;
					TagsetDefinition tagset = (TagsetDefinition)oldValue;
	            	tagsetData.getRootItems()
            		.stream()
            		.map(tagsetTreeItem -> (TagsetDataItem)tagsetTreeItem)
            		.filter(tagsetDataItem -> tagsetDataItem.getTagset().getUuid().equals(tagset.getUuid()))
            		.findFirst()
            		.ifPresent(tagsetDataItem -> {
            			TagDataItem oldItem = new TagDataItem(tag, tagsetDataItem.isEditable());
            			String parentTagId = tag.getParentUuid();
            			TagsetTreeItem parent = tagsetDataItem;
            			
            			if (!parentTagId.isEmpty()) {
            				parent = new TagDataItem(
            					tagset.getTagDefinition(parentTagId), tagsetDataItem.isEditable());
            			}

            			tagsetData.removeItem(oldItem);

            			TagDataItem tagDataItem = new TagDataItem(tag, tagsetDataItem.isEditable());
            			tagDataItem.setPropertiesExpanded(true);
            			tagsetData.addItem(parent, tagDataItem);
            			
            			addTagSubTree(tagset, tag, tagDataItem);
            			
            			//TODO: sort

            			tagsetDataProvider.refreshAll();
            			
            			showExpandedProperties(tagDataItem);
            		});
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
				tagsetGrid.deselectAll();
			}
		};
		
		project.getTagManager().addPropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged, 
				propertyDefinitionChangedListener);	
		
        this.projectExceptionListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				initData();
			}
		};
		project.addPropertyChangeListener(
				RepositoryChangeEvent.exceptionOccurred, projectExceptionListener);
			
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
		.setWidth(300);
		
		ButtonRenderer<TagsetTreeItem> btRemovalRenderer = 
			new ButtonRenderer<>(rendererClickEvent -> handleTagsetTreeItemRemovalRequest(rendererClickEvent));
		btRemovalRenderer.setHtmlContentAllowed(true);
		
		tagsetGrid.addColumn(
				tagsetTreeItem -> tagsetTreeItem.getResponsibleUser())
		.setCaption("Responsible")
		.setSortable(false)
		.setHidable(true)
		.setWidth(150);
		
		
		tagsetGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getRemoveIcon())
		.setRenderer(btRemovalRenderer)
		.setSortable(false)
		.setHidable(false)
		.setWidth(60);
		
		tagsetGrid.setStyleGenerator(new StyleGenerator<TagsetTreeItem>() {
			
			@Override
			public String apply(TagsetTreeItem item) {
				return item.generateStyle();
			}
		});
		
		tagsetGridComponent.setSearchFilterProvider(new SearchFilterProvider<TagsetTreeItem>() {
			@Override
			public SerializablePredicate<TagsetTreeItem> createSearchFilter(String searchInput) {
				return new TagsetSearchFilterProvider(searchInput, tagsetData);
			}
		});
		
		tagsetGrid.addExpandListener(expandEvent -> handleExpandCollapseTagset(expandEvent.getExpandedItem(), true));
		tagsetGrid.addCollapseListener(collapseEvent -> handleExpandCollapseTagset(collapseEvent.getCollapsedItem(), false));

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



	private void handleExpandCollapseTagset(TagsetTreeItem tagsetTreeItem, boolean expanded) {
		tagsetTreeItem.setTagsetExpanded(expanded);
	}

	private void handleTagsetTreeItemRemovalRequest(RendererClickEvent<TagsetTreeItem> rendererClickEvent) {
		TagsetTreeItem item = rendererClickEvent.getItem();
		if (item.isEditable()) {
			item.handleRemovalRequest(this);
		}
	}

	private void initComponents() {
        getHugeCardBar().setMoreOptionsButtonVisible(false);
        
		HorizontalLayout content = new HorizontalLayout();
		content.setSizeFull();
		
		tagsetGrid = TreeGridFactory.createDefaultTreeGrid();
		tagsetGrid.addStyleNames(
				"flat-undecorated-icon-buttonrenderer");
		tagsetGrid.setSizeFull();
		tagsetGrid.setSelectionMode(SelectionMode.SINGLE);
		tagsetGrid.addStyleName(MaterialTheme.GRID_BORDERLESS);
		
		Label tagsetsLabel = new Label("Tagsets");
        tagsetGridComponent = new ActionGridComponent<TreeGrid<TagsetTreeItem>>(
                tagsetsLabel,
                tagsetGrid
        );
        
		resourcePanel = new TagResourcePanel(project, eventBus); 
		drawer = new SliderPanelBuilder(resourcePanel)
				.mode(SliderMode.LEFT).expanded(false).build();
		
		addComponent(content);
		setExpandRatio(content, 1f);
        content.addComponent(drawer);
		content.addComponent(tagsetGridComponent);
		content.setExpandRatio(tagsetGridComponent, 1f);
	}
	
	public void close() {
		resourcePanel.close();
		eventBus.unregister(this);
		project.getTagManager().removePropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged, 
				propertyDefinitionChangedListener);	
		project.getTagManager().removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);	
		project.removePropertyChangeListener(
			RepositoryChangeEvent.exceptionOccurred, projectExceptionListener);

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
				propertyDataItem, 
				new PossibleValueDataItem(possibleValue, propertyDataItem.isEditable()));
		}
		
		tagsetGrid.expand(propertyDataItem);
	}

	private void showExpandedProperties(TagDataItem tagDataItem) {
		TagDefinition tag = tagDataItem.getTag();
		
		PropertyDataItem lastPropertyDataItem = null; 
		for (PropertyDefinition propertyDefinition : tag.getUserDefinedPropertyDefinitions()) {
			lastPropertyDataItem = new PropertyDataItem(propertyDefinition, tagDataItem.isEditable());
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
			Notification.show(
				"Info", 
				"Please select one or more Tagsets first!",
				Type.HUMANIZED_MESSAGE);
		}
		else {
			deleteTagsets(selectedTagsets);
		}
	}

	void deleteTagsets(Collection<TagsetDefinition> selectedTagsets) {
		boolean beyondUsersResponsibility = 
				selectedTagsets.stream()
				.filter(tagset -> !tagset.isResponsible(project.getUser().getIdentifier()))
				.findAny()
				.isPresent();
		
		BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
			beyondUsersResponsibility, 
			new Action() {
				@Override
				public void execute() {

					List<String> tagsetNames = 
							selectedTagsets.stream()
							.map(TagsetDefinition::getName)
							.sorted()
							.collect(Collectors.toList());
					ConfirmDialog.show(
						UI.getCurrent(), 
						"Warning", 
						String.format(
							"Are you sure you want to delete Tagset(s) %1$s and all related data?", 
							String.join(",", tagsetNames)),
						"Delete",
						"Cancel",
						dlg -> {
							if (dlg.isConfirmed()) {
								for (TagsetDefinition tagset : selectedTagsets) {
									project.getTagManager().removeTagsetDefinition(tagset);
								}
							}
						});
				}
			});
	}

	private void handleEditTagsetRequest() {
		Collection<TagsetDefinition> selectedTagsets = 
				tagsetGrid.getSelectedItems().stream()
				.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagsetDataItem)
				.map(tagsetDataItem -> ((TagsetDataItem)tagsetDataItem).getTagset())
				.collect(Collectors.toList());
		
		if (!selectedTagsets.isEmpty()) {
			final TagsetDefinition tagset = selectedTagsets.iterator().next();
			
			boolean beyondUsersResponsibility = !tagset.isResponsible(project.getUser().getIdentifier());
				
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
				new Action() {
					@Override
					public void execute() {
						Set<Member> projectMembers = null;
						try {
							projectMembers = TagsView.this.project.getProjectMembers();
						}
						catch (IOException e) {
							((ErrorHandler)UI.getCurrent()).showAndLogError(
									"Error loading project members!", e);
						}
						if (projectMembers != null) {
							EditTagsetDialog editTagsetDlg = new EditTagsetDialog(
									new TagsetMetadata(
											tagset.getName(), 
											tagset.getDescription(), 
											tagset.getResponsibleUser()),
									projectMembers,
									new SaveCancelListener<TagsetMetadata>() {
										@Override
										public void savePressed(TagsetMetadata result) {
											project.getTagManager().setTagsetMetadata(
													tagset, result);
										}
									});
							editTagsetDlg.show();
						}
					}
				});
		}
		else {
			Notification.show(
				"Info", "Please select a Tagset first!", 
				Type.HUMANIZED_MESSAGE);
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
			deleteTags(targetTags);
		}
		else {
			Notification.show("Info", "Please select one or more Tags first!", Type.TRAY_NOTIFICATION);
		}
	}

	void deleteTags(List<TagDefinition> targetTags) {
		boolean beyondUsersResponsibility =
				targetTags.stream()
				.map(tag -> project.getTagManager().getTagLibrary().getTagsetDefinition(tag))
				.distinct()
				.filter(tagset -> !tagset.isResponsible(project.getUser().getIdentifier()))
				.findAny()
				.isPresent();

		BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
			new Action() {
				@Override
				public void execute() {
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
		});


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
			
			boolean beyondUsersResponsibility =
					!project.getTagManager().getTagLibrary()
						.getTagsetDefinition(targetTag)
						.isResponsible(project.getUser().getIdentifier());
			
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					beyondUsersResponsibility, 
				new Action() {
					@Override
					public void execute() {
						EditTagDialog editTagDialog = 
							new EditTagDialog(new TagDefinition(targetTag), 
								new SaveCancelListener<TagDefinition>() {
							public void savePressed(TagDefinition result) {
								project.getTagManager().updateTagDefinition(targetTag, result);
							};
						});
						editTagDialog.show();
					}
			});

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
			
			
			boolean beyondUsersResponsibility =
					targetTags.stream()
					.map(tag -> project.getTagManager().getTagLibrary().getTagsetDefinition(tag))
					.distinct()
					.filter(tagset -> !tagset.isResponsible(project.getUser().getIdentifier()))
					.findAny()
					.isPresent();
			
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
				new Action() {
					@Override
					public void execute() {
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
			});
		}
	}
	
	void deletePropertyDataItem(PropertyDataItem propertyDataItem) {
		TagDataItem tagDataItem = (TagDataItem) tagsetData.getParent(propertyDataItem);
		TagsetTreeItem tagsetDataItemCandidate = tagsetData.getParent(tagDataItem);
		while (!(tagsetDataItemCandidate instanceof TagsetDataItem)) {
			tagsetDataItemCandidate = tagsetData.getParent(tagsetDataItemCandidate);
			if (tagsetDataItemCandidate == null) {
				break;
			}
		}
		
		TagsetDataItem tagsetDataItem = (TagsetDataItem) tagsetDataItemCandidate;
		if (tagsetDataItem != null) {
			String msg = String.format(
					"Are you sure you want to delete the following Property: %1$s?", 
					propertyDataItem.getPropertyDefinition().getName());
				
			ConfirmDialog.show(UI.getCurrent(), "Warning", msg, "Delete", "Cancel", dlg -> {
				if (dlg.isConfirmed()) {
					project.getTagManager().removeUserDefinedPropertyDefinition(
							propertyDataItem.getPropertyDefinition(), tagDataItem.getTag(), tagsetDataItem.getTagset());
				}
			});
		}
	}

	void deletePossibleValueDataItem(PossibleValueDataItem possibleValueDataItem) {
		PropertyDataItem propertyDataItem = (PropertyDataItem) tagsetData.getParent(possibleValueDataItem);
		TagDataItem tagDataItem = (TagDataItem) tagsetData.getParent(propertyDataItem);
		String msg = String.format(
				"Are you sure you want to delete the following Value: %1$s?", 
				possibleValueDataItem.getPropertyValue());
			
		ConfirmDialog.show(UI.getCurrent(), "Warning", msg, "Delete", "Cancel", dlg -> {
			if (dlg.isConfirmed()) {
				ArrayList<String> values = 
					new ArrayList<>(propertyDataItem.getPropertyDefinition().getPossibleValueList());
				values.remove(possibleValueDataItem.getPropertyValue());
				propertyDataItem.getPropertyDefinition().setPossibleValueList(values);
				project.getTagManager().updateUserDefinedPropertyDefinition(
						tagDataItem.getTag(), propertyDataItem.getPropertyDefinition());
			}
		});
		
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
						if (!existingPropertyDef.getName().equals(editedPropertyDef.getName())
								|| !existingPropertyDef.getPossibleValueList().equals(editedPropertyDef.getPossibleValueList())) {
							existingPropertyDef.setName(editedPropertyDef.getName());
							existingPropertyDef.setPossibleValueList(
								editedPropertyDef.getPossibleValueList());
							project.getTagManager().updateUserDefinedPropertyDefinition(
									tag, existingPropertyDef);
						}
					});
				
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

		
		if (!parentTags.isEmpty()) {
			
			boolean beyondUsersResponsibility =
				parentTags.stream()
				.map(tag -> project.getTagManager().getTagLibrary().getTagsetDefinition(tag))
				.distinct()
				.filter(tagset -> !tagset.isResponsible(project.getUser().getIdentifier()))
				.findAny()
				.isPresent();
			
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
				new Action() {
					@Override
					public void execute() {
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
				});
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
			
		if (tagsets.isEmpty()) {
			Notification.show(
				"Info", 
				"You do not have any Tagsets to add Tags to yet, please create a Tagset first!", 
				Type.HUMANIZED_MESSAGE);
			return;
		}
	
		List<TagsetDefinition> editableTagsets = 
				tagsets.stream()
				.collect(Collectors.toList());
		
		boolean beyondUsersResponsibility =
				editableTagsets.stream()
				.filter(tagset -> !tagset.isResponsible(project.getUser().getIdentifier()))
				.findAny()
				.isPresent();
		
		BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
				new Action() {
					@Override
					public void execute() {
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
				});
	}
	
	private void initData() {
        try {
            tagsetData = new TreeData<TagsetTreeItem>();
            Map<String, Member> projectMembersByIdentifier = 
            		project.getProjectMembers().stream()
        			.collect(Collectors.toMap(
        					Member::getIdentifier, 
        					Function.identity()));
            for (TagsetDefinition tagset : tagsets) {
            	String responsibleUser = null;
            	

        		if (tagset.getResponsibleUser() != null) {
        			Member member = projectMembersByIdentifier.get(tagset.getResponsibleUser());
        			if (member != null) {
        				responsibleUser = member.getName();
        			}
        		}
            	
            	TagsetTreeItem tagsetItem = 
        			new TagsetDataItem(
    					tagset, 
    					responsibleUser,
    					!project.isReadOnly());
            	tagsetData.addItem(null, tagsetItem);
            	addTags(tagsetItem, tagset);
            }
            tagsetDataProvider = new TreeDataProvider<TagsetTreeItem>(tagsetData);
            tagsetGrid.setDataProvider(tagsetDataProvider);
            for (TagsetDefinition tagset : tagsets) {
            	expandTagsetDefinition(tagset);
            }
            
            tagsetGridComponent.getActionGridBar().setAddBtnEnabled(!project.isReadOnly());
            tagsetGridComponent.getActionGridBar().setMoreOptionsBtnEnabled(!project.isReadOnly());
            
        } catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("Error loading data!", e);
        }
	}
	
    private void expandTagsetDefinition(TagsetDefinition tagset) {
    	for (TagDefinition tag : tagset) {
    		TagDataItem item = new TagDataItem(tag);
    		tagsetGrid.expand(item);
    	}
	}
    
	private void handleAddTagsetRequest() {
    	SingleTextInputDialog tagsetNameDlg = 
        		new SingleTextInputDialog("Add Tagset", "Please enter the Tagset name:",
        				new SaveCancelListener<String>() {
    						
    						@Override
    						public void savePressed(String result) {
    							IDGenerator idGenerator = new IDGenerator();
    							TagsetDefinition tagset = new TagsetDefinition(
    									idGenerator.generateTagsetId(), result);
    							tagset.setResponsibleUser(project.getUser().getIdentifier());
    							project.getTagManager().addTagsetDefinition(
    								tagset);
    						}
    					});
            	
    	tagsetNameDlg.show();
	}

	private void addTags(
			TagsetTreeItem tagsetItem, 
			TagsetDefinition tagset) {
		
        for (TagDefinition tag : tagset) {
            if (tag.getParentUuid().isEmpty()) {
            	TagDataItem tagItem =  new TagDataItem(tag, tagsetItem.isEditable());
                tagsetData.addItem(tagsetItem, tagItem);
                addTagSubTree(tagset, tag, tagItem);
            }
        }
	}

	private void addTagSubTree(
    		TagsetDefinition tagset, 
    		TagDefinition tag, TagsetTreeItem parentItem) {
        for (TagDefinition childDefinition : tagset.getDirectChildren(tag)) {
        	TagDataItem childItem = new TagDataItem(childDefinition, parentItem.isEditable());
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
        	TagsetTreeItem tagsetItem = new TagsetDataItem(tagset);
        	tagsetData.addItem(null, tagsetItem);
        	addTags(tagsetItem, tagset);
        }
        tagsetDataProvider.refreshAll();
        for (TagsetDefinition tagset : this.tagsets) {
        	expandTagsetDefinition(tagset);
        }
	}

	public void setSelectedTagset(TagsetDefinition tagset) {
		this.resourcePanel.setSelectedTagset(tagset);
	}	
}
