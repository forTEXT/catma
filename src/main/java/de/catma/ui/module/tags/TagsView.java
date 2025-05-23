package de.catma.ui.module.tags;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.sliderpanel.SliderPanel;
import org.vaadin.sliderpanel.SliderPanelBuilder;
import org.vaadin.sliderpanel.client.SliderMode;

import com.beust.jcommander.internal.Sets;
import com.github.appreciated.material.MaterialTheme;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.GridSortOrder;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.server.SerializableComparator;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Grid;
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

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionManager;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.project.Project;
import de.catma.rbac.RBACPermission;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
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
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.module.analyze.visualization.kwic.annotation.edit.BulkEditAnnotationWizard;
import de.catma.ui.module.analyze.visualization.kwic.annotation.edit.EditAnnotationWizardContextKey;
import de.catma.ui.module.analyze.visualization.kwic.annotation.edit.PropertyAction;
import de.catma.ui.module.analyze.visualization.kwic.annotation.edit.PropertyActionType;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.project.EditTagsetDialog;
import de.catma.ui.module.tags.BulkEditPropertyByNameDialog.PropertyNameItem;
import de.catma.ui.module.tags.BulkEditPropertyByNameDialog.Result;
import de.catma.user.Member;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class TagsView extends HugeCard {
	private static final Logger logger = Logger.getLogger(TagsView.class.getName());
	private final static SerializableComparator<TagsetTreeItem> TAGSET_TREE_ITEM_COMPARATOR_ASC = (t1, t2) -> t1.compareTo(t2);

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
	private PropertyChangeListener tagDefinitionChangedListener;
	private PropertyChangeListener propertyDefinitionChangedListener;
	private final Collator tagsetCollator;

	public TagsView(EventBus eventBus, Project project) {
		super("Manage Tags");
		this.eventBus = eventBus;
		this.project = project;
		this.tagsetCollator = Collator.getInstance(project.getTagManager().getTagLibrary().getLocale());
		eventBus.register(this);
		initComponents();
		initActions();
		this.tagsets = new ArrayList<>(resourcePanel.getSelectedTagsets());
		initListeners();
		initData();
	}

	private void initListeners() {
		tagDefinitionChangedListener = new PropertyChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Object newValue = evt.getNewValue();
				Object oldValue = evt.getOldValue();

				if (oldValue == null) { // tag created
					Pair<TagsetDefinition, TagDefinition> createdPair = (Pair<TagsetDefinition, TagDefinition>) newValue;
					TagsetDefinition tagsetDefinition = createdPair.getFirst();
					TagDefinition createdTagDefinition = createdPair.getSecond();

					// is this view aware of the tagset corresponding to the newly created tag?
					Optional<TagsetDataItem> optionalTagsetDataItem = tagsetData.getRootItems()
							.stream()
							.map(tagsetTreeItem -> (TagsetDataItem) tagsetTreeItem)
							.filter(tdi -> tdi.getTagset().getUuid().equals(tagsetDefinition.getUuid()))
							.findFirst();

					// no, log a warning
					if (!optionalTagsetDataItem.isPresent()) {
						logger.warning(
								String.format(
										"Failed to find tagset with ID %1$s in the TagsView TreeGrid for project \"%2$s\" with ID %3$s",
										tagsetDefinition.getUuid(),
										project.getName(),
										project.getId()
								)
						);
						return;
					}

					// yes, add the newly created tag to the corresponding tagset
					TagsetTreeItem tagsetDataItem = optionalTagsetDataItem.get();
					TagDataItem tagDataItem = new TagDataItem(createdTagDefinition, tagsetDefinition, tagsetDataItem.isEditable(), tagsetCollator);

					if (!tagsetData.contains(tagDataItem)) {
						String parentTagId = createdTagDefinition.getParentUuid();

						if (parentTagId.isEmpty()) { // the new tag is a top-level tag
							tagsetData.addItem(tagsetDataItem, tagDataItem);
							tagsetDataProvider.refreshAll();
							tagsetGrid.expand(tagsetDataItem);
						}
						else { // the new tag is a subtag
							TagDefinition parentTagDefinition = project.getTagManager().getTagLibrary().getTagDefinition(parentTagId);
							TagsetTreeItem parentTagsetTreeItem = new TagDataItem(parentTagDefinition, tagsetDefinition, tagsetDataItem.isEditable(), tagsetCollator);
							tagsetData.addItem(parentTagsetTreeItem, tagDataItem);
							tagsetDataProvider.refreshAll();
							tagsetGrid.expand(parentTagsetTreeItem);
						}
					}
				}
				else if (newValue == null) { // tag deleted
					Pair<TagsetDefinition, TagDefinition> deletedPair = (Pair<TagsetDefinition, TagDefinition>) oldValue;
					TagDefinition deletedTagDefinition = deletedPair.getSecond();

					tagsetData.removeItem(new TagDataItem(deletedTagDefinition, deletedPair.getFirst(), true, tagsetCollator));
					tagsetDataProvider.refreshAll();
				}
				else { // tag updated
					TagDefinition updatedTagDefinition = (TagDefinition) newValue;
					TagsetDefinition tagsetDefinition = (TagsetDefinition) oldValue;

					// is this view aware of the tagset corresponding to the updated tag?
					Optional<TagsetDataItem> optionalTagsetDataItem = tagsetData.getRootItems()
							.stream()
							.map(tagsetTreeItem -> (TagsetDataItem) tagsetTreeItem)
							.filter(tdi -> tdi.getTagset().getUuid().equals(tagsetDefinition.getUuid()))
							.findFirst();

					// no, log a warning
					if (!optionalTagsetDataItem.isPresent()) {
						logger.warning(
								String.format(
										"Failed to find tagset with ID %1$s in the TagsView TreeGrid for project \"%2$s\" with ID %3$s",
										tagsetDefinition.getUuid(),
										project.getName(),
										project.getId()
								)
						);
						return;
					}

					// yes, add the updated tag to the corresponding tagset
					TagsetTreeItem tagsetDataItem = optionalTagsetDataItem.get();
					TagDataItem tagDataItem = new TagDataItem(updatedTagDefinition, tagsetDefinition, tagsetDataItem.isEditable(), tagsetCollator);

					// the old tag can be removed using the newly constructed TagDataItem because TagDefinitions are compared by UUID
					tagsetData.removeItem(tagDataItem);

					TagsetTreeItem parentTagsetTreeItem = tagsetDataItem;
					String parentTagId = updatedTagDefinition.getParentUuid();
					if (!parentTagId.isEmpty()) {
						parentTagsetTreeItem = new TagDataItem(tagsetDefinition.getTagDefinition(parentTagId), tagsetDefinition, tagsetDataItem.isEditable(), tagsetCollator);
					}

					tagDataItem.setPropertiesExpanded(true);
					tagsetData.addItem(parentTagsetTreeItem, tagDataItem);

					addTagSubTree(tagsetDefinition, updatedTagDefinition, tagDataItem);

					tagsetDataProvider.refreshAll();

					showExpandedProperties(tagDataItem);
				}
			}
		};

		project.getTagManager().addPropertyChangeListener(TagManagerEvent.tagDefinitionChanged, tagDefinitionChangedListener);

		propertyDefinitionChangedListener = new PropertyChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Object newValue = evt.getNewValue();
				Object oldValue = evt.getOldValue();

				TagDefinition tagDefinition;

				if (oldValue == null) { // property created
					Pair<PropertyDefinition, TagDefinition> createdPair = (Pair<PropertyDefinition, TagDefinition>) newValue;
					tagDefinition = createdPair.getSecond();
				}
				else if (newValue == null) { // property deleted
					Pair<PropertyDefinition, Pair<TagDefinition, TagsetDefinition>> deletedPair =
							(Pair<PropertyDefinition, Pair<TagDefinition, TagsetDefinition>>) oldValue;
					tagDefinition = deletedPair.getSecond().getFirst();
				}
				else { // property updated
					tagDefinition = (TagDefinition) oldValue;
				}

				TagsetTreeItem parentTagsetTreeItem;

				if (tagDefinition.getParentUuid().isEmpty()) {
					parentTagsetTreeItem = new TagsetDataItem(
						project.getTagManager().getTagLibrary().getTagsetDefinition(tagDefinition.getTagsetDefinitionUuid()),
						tagsetCollator
					);
				}
				else {
					parentTagsetTreeItem = new TagDataItem(
						project.getTagManager().getTagLibrary().getTagDefinition(tagDefinition.getParentUuid()),
						project.getTagManager().getTagLibrary().getTagsetDefinition(tagDefinition.getTagsetDefinitionUuid()),
						tagsetCollator
					);
				}

				// is this view aware of the tag corresponding to the property?
				Optional<TagDataItem> optionalTagDataItem = tagsetData.getChildren(parentTagsetTreeItem)
						.stream()
						.map(tagsetTreeItem -> (TagDataItem) tagsetTreeItem)
						.filter(tagDataItem -> tagDataItem.getTag().getUuid().equals(tagDefinition.getUuid()))
						.findFirst();

				// no, log a warning
				if (!optionalTagDataItem.isPresent()) {
					logger.warning(
							String.format(
									"Failed to find tag with ID %1$s in the TagsView TreeGrid for project \"%2$s\" with ID %3$s",
									tagDefinition.getUuid(),
									project.getName(),
									project.getId()
							)
					);
					return;
				}

				// yes, refresh the properties being displayed
				TagDataItem tagDataItem = optionalTagDataItem.get();
				tagDataItem.setPropertiesExpanded(false);
				hideExpandedProperties(tagDataItem);
				tagDataItem.setPropertiesExpanded(true);
				showExpandedProperties(tagDataItem);

				tagsetDataProvider.refreshAll();
			}
		};

		project.getTagManager().addPropertyChangeListener(TagManagerEvent.userPropertyDefinitionChanged, propertyDefinitionChangedListener);
	}

	private void initActions() {
		Grid.Column<TagsetTreeItem, String> tagsetColumn = tagsetGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getColor(), new HtmlRenderer())
			.setCaption("Tagsets")
			.setComparator(TAGSET_TREE_ITEM_COMPARATOR_ASC)
			.setSortable(true)
			.setWidth(200);
		
		tagsetGrid.setSortOrder(List.of(new GridSortOrder<>(tagsetColumn, SortDirection.ASCENDING)));

		tagsetGrid.setHierarchyColumn(
			tagsetGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getName())
			.setCaption("Tags")
			.setComparator(TAGSET_TREE_ITEM_COMPARATOR_ASC)
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
		.setComparator(TAGSET_TREE_ITEM_COMPARATOR_ASC)
		.setHidable(true)
		.setWidth(300);
		
		tagsetGrid.addColumn(
			tagsetTreeItem -> tagsetTreeItem.getPropertyValue())
		.setCaption("Values")
		.setSortable(false)
		.setComparator(TAGSET_TREE_ITEM_COMPARATOR_ASC)
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

	    ContextMenu addContextMenu = tagsetGridComponent.getActionGridBar().getBtnAddContextMenu();
	    addContextMenu.addItem("Add Tagset", clickEvent -> handleAddTagsetRequest());
	    addContextMenu.addItem("Add Tag", clickEvent -> handleAddTagRequest());
	    addContextMenu.addItem("Add Subtag", clickEvent -> handleAddSubtagRequest());
	    addContextMenu.addItem("Add Property", clickEvent -> handleAddPropertyRequest());
		
		ContextMenu moreOptionsContextMenu = tagsetGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

		moreOptionsContextMenu.addItem("Edit Tagset", clickEvent -> handleEditTagsetRequest());
		moreOptionsContextMenu.addItem("Delete Tagset", clickEvent -> handleDeleteTagsetRequest());
		moreOptionsContextMenu.addSeparator();

		moreOptionsContextMenu.addItem("Edit Tag", clickEvent -> handleEditTagRequest());
		moreOptionsContextMenu.addItem("Delete Tag", clickEvent -> handleDeleteTagRequest());
		moreOptionsContextMenu.addSeparator();

		moreOptionsContextMenu.addItem("Edit/Delete Properties", clickEvent -> handleEditPropertiesRequest());
		moreOptionsContextMenu.addItem("Bulk Edit/Delete Properties by Name", clickEvent -> handleBulkEditPropertiesByName());

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

		project.getTagManager().removePropertyChangeListener(TagManagerEvent.tagDefinitionChanged, tagDefinitionChangedListener);
		project.getTagManager().removePropertyChangeListener(TagManagerEvent.userPropertyDefinitionChanged, propertyDefinitionChangedListener);

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
				propertyDataItem, 
				new PossibleValueDataItem(
						possibleValue, 
						propertyDataItem.getPropertyDefinition(), 
						propertyDataItem.getTag(), 
						propertyDataItem.getTagset(), 
						propertyDataItem.isEditable(), tagsetCollator));
		}
		
		tagsetGrid.expand(propertyDataItem);
	}

	private void showExpandedProperties(TagDataItem tagDataItem) {
		TagDefinition tag = tagDataItem.getTag();
		
		PropertyDataItem lastPropertyDataItem = null; 
		for (PropertyDefinition propertyDefinition : tag.getUserDefinedPropertyDefinitions()) {
			lastPropertyDataItem = new PropertyDataItem(propertyDefinition, tagDataItem.getTag(), tagDataItem.getTagset(), tagDataItem.isEditable(), tagsetCollator);
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
			.map(tagsetDataItem -> ((TagsetTreeItem)tagsetDataItem).getTagset())
			.collect(Collectors.toList());
		if (selectedTagsets.isEmpty()) {
			Notification.show(
				"Info", 
				"Please select one or more tagsets first!",
				Type.HUMANIZED_MESSAGE);
		}
		else {
			deleteTagsets(selectedTagsets);
		}
	}

	void deleteTagsets(Collection<TagsetDefinition> selectedTagsets) {
		boolean beyondUsersResponsibility = 
				selectedTagsets.stream()
				.filter(tagset -> !tagset.isResponsible(project.getCurrentUser().getIdentifier()))
				.findAny()
				.isPresent();
		try {
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
				project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.TAGSET_DELETE_OR_EDIT),
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
								"Are you sure you want to delete the tagset(s) \"%s\" and all associated tags and annotations?",
								String.join("\", \"", tagsetNames)
							),
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
		catch (IOException e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error deleting tagsets", e);
		}
	}

	private void handleEditTagsetRequest() {
		Collection<TagsetDefinition> selectedTagsets = 
				tagsetGrid.getSelectedItems().stream()
				.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagsetDataItem)
				.map(tagsetDataItem -> ((TagsetTreeItem)tagsetDataItem).getTagset())
				.collect(Collectors.toList());
		
		if (!selectedTagsets.isEmpty()) {
			final TagsetDefinition tagset = selectedTagsets.iterator().next();
			
			boolean beyondUsersResponsibility = !tagset.isResponsible(project.getCurrentUser().getIdentifier());
			try {
				BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					beyondUsersResponsibility, 
					project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.TAGSET_DELETE_OR_EDIT),
					new Action() {
						@Override
						public void execute() {
							Set<Member> projectMembers = null;
							try {
								projectMembers = TagsView.this.project.getProjectMembers();
							}
							catch (IOException e) {
								((ErrorHandler) UI.getCurrent()).showAndLogError(
										"Error loading project members", e);
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
			catch (IOException e) {
				((ErrorHandler) UI.getCurrent()).showAndLogError("Error editing tagsets", e);
			}

			
		}
		else {
			Notification.show(
				"Info", "Please select a tagset first!",
				Type.HUMANIZED_MESSAGE);
		}
	}

	private void handleEditPropertiesRequest() {
		handleAddPropertyRequest();
	}
	
	private void handleBulkEditPropertiesByName() {
		final List<String> selectedPropertyDefNames = new ArrayList<>();
		final Set<TagsetDefinition> affectedTagsets = new HashSet<>();
		final Set<TagDefinition> affectedTags = new HashSet<>();
		
		selectedPropertyDefNames.addAll(
				tagsetGrid.getSelectedItems()
				.stream()
				.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagDataItem || tagsetTreeItem instanceof PropertyDataItem || tagsetTreeItem instanceof PossibleValueDataItem)
				.flatMap(tagsetTreeItem -> {
					
					affectedTagsets.add(tagsetTreeItem.getTagset());
					affectedTags.add(tagsetTreeItem.getTag());
					
					if (tagsetTreeItem instanceof PossibleValueDataItem) {
						var pd = tagsetData.getParent(tagsetTreeItem).getPropertyDefinition(); 
						return Stream.of(pd != null ? pd.getName() : "");
					}
					else if (tagsetTreeItem instanceof TagDataItem) {
						return tagsetTreeItem.getTag().getUserDefinedPropertyDefinitions().stream().map(PropertyDefinition::getName);
					}
					var pd = tagsetTreeItem.getPropertyDefinition();
					return Stream.of(pd !=null ? pd.getName() : "");
				})
				.filter(name -> !name.isEmpty())
				.distinct()
				.collect(Collectors.toList()));


		if (selectedPropertyDefNames.isEmpty()) {
			Notification.show("Info", "Please select one ore more tags or properties first!", Type.TRAY_NOTIFICATION);	
			return;
		}
		
		if (affectedTagsets.size() > 1) {
			Notification.show("Info", "Selected properties must come from the same tagset. Please adjust your selection accordingly!", Type.TRAY_NOTIFICATION);	
			return;			
		}
			
			boolean beyondUsersResponsibility =
					affectedTagsets.stream()
					.filter(tagset -> !tagset.isResponsible(project.getCurrentUser().getIdentifier()))
					.findAny()
					.isPresent();
			
			boolean isAuthor = 
					!affectedTags.stream()
					.filter(tag -> !tag.getAuthor().equals(project.getCurrentUser().getIdentifier()))
					.findAny()
					.isPresent();
			try {
				BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					beyondUsersResponsibility, 
					isAuthor || project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.TAGSET_DELETE_OR_EDIT),
					new Action() {
						@Override
						public void execute() {
							BulkEditPropertyByNameDialog dlg = new BulkEditPropertyByNameDialog(affectedTagsets, selectedPropertyDefNames, tagsetCollator, new SaveCancelListener<>() {
								@Override
								public void savePressed(Result result) {
									handleBulkEditProperties(result, affectedTagsets);
								}
							});
							
							dlg.show();
						}
				});
			}
			catch (IOException e) {
				((ErrorHandler) UI.getCurrent()).showAndLogError("Error editing properties", e);
			}
		
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
			Notification.show("Info", "Please select one or more tags first!", Type.TRAY_NOTIFICATION);
		}
	}

	void deleteTags(List<TagDefinition> targetTags) {
		boolean beyondUsersResponsibility =
				targetTags.stream()
				.map(tag -> project.getTagManager().getTagLibrary().getTagsetDefinition(tag))
				.distinct()
				.filter(tagset -> !tagset.isResponsible(project.getCurrentUser().getIdentifier()))
				.findAny()
				.isPresent();
		
		boolean isAuthor = 
				!targetTags.stream()
				.filter(tag -> !tag.getAuthor().equals(project.getCurrentUser().getIdentifier()))
				.findAny()
				.isPresent();
		
		try {
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility,
				isAuthor || project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.TAGSET_DELETE_OR_EDIT),
				new Action() {
					@Override
					public void execute() {
						String msg = String.format(
								"Are you sure you want to delete the tag(s) \"%s\" and all associated annotations?",
								targetTags
								.stream()
								.map(TagDefinition::getName)
								.sorted()
								.collect(Collectors.joining("\", \"")));
						
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
		catch (IOException e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error deleting tags", e);
		}


	}

	private void handleEditTagRequest() {
		final List<TagDefinition> targetTags = tagsetGrid.getSelectedItems()
		.stream()
		.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagDataItem)
		.map(tagsetTreeItem -> ((TagDataItem)tagsetTreeItem).getTag())
		.collect(Collectors.toList());
		
		if (targetTags.isEmpty()) {
			Notification.show("Info", "Please select a tag first!", Type.TRAY_NOTIFICATION);
		}
		else if (targetTags.size() > 1) {
			handleAddPropertyRequest();
		}
		else {
			
			final TagDefinition targetTag = targetTags.get(0);
			
			boolean beyondUsersResponsibility =
					!project.getTagManager().getTagLibrary()
						.getTagsetDefinition(targetTag)
						.isResponsible(project.getCurrentUser().getIdentifier());
			
			boolean isAuthor = 
					targetTag.getAuthor().equals(project.getCurrentUser().getIdentifier());
			try {
				BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
						beyondUsersResponsibility, 
						isAuthor || project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.TAGSET_DELETE_OR_EDIT),
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
			catch (IOException e) {
				((ErrorHandler) UI.getCurrent()).showAndLogError("Error editing tags", e);
			}
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
			Notification.show("Info", "Please select one ore more tags first!", Type.TRAY_NOTIFICATION);
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
					.filter(tagset -> !tagset.isResponsible(project.getCurrentUser().getIdentifier()))
					.findAny()
					.isPresent();
			
			boolean isAuthor = 
					!targetTags.stream()
					.filter(tag -> !tag.getAuthor().equals(project.getCurrentUser().getIdentifier()))
					.findAny()
					.isPresent();
			try {
				BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					beyondUsersResponsibility, 
					isAuthor || project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.TAGSET_DELETE_OR_EDIT),
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
			catch (IOException e) {
				((ErrorHandler) UI.getCurrent()).showAndLogError("Error editing tags", e);
			}
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
		
		TagsetTreeItem tagsetDataItem = (TagsetTreeItem) tagsetDataItemCandidate;
		if (tagsetDataItem != null) {
			String msg = String.format(
					"Are you sure you want to delete the property \"%s\"?",
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
				"Are you sure you want to delete the value \"%s\"?",
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
	
	private void handleBulkEditProperties(Result result, Set<TagsetDefinition> affectedTagsets) {
		Set<PropertyAction> propertyActions = new HashSet<>();
		Set<String> propertyNames = new HashSet<>();
		Set<TagDefinition> tags = new HashSet<>();
		
		// handle deleted propertyDefs
		for (PropertyNameItem deletedItem : result.deletedItems()) {
			propertyNames.add(deletedItem.getOldName());
			
			for (TagDefinition tag : deletedItem.getTags()) {
				tags.add(tag);
				TagsetDefinition tagset = project.getTagManager().getTagLibrary().getTagsetDefinition(tag.getTagsetDefinitionUuid());
				PropertyDefinition propertyDef = tag.getPropertyDefinition(deletedItem.getOldName());
				project.getTagManager().removeUserDefinedPropertyDefinition(
						propertyDef, tag, tagset);
			}
			deletedItem.getPossibleValueList().forEach(value -> 
				propertyActions.add(new PropertyAction(deletedItem.getOldName(), PropertyActionType.REMOVE, value, null)));
		}
		
		// handle modified propertyDefs
		for (PropertyNameItem modifiedItem : result.modifiedItems()) {
			propertyNames.add(modifiedItem.getName());
			
			for (TagDefinition tag : modifiedItem.getTags()) {
				tags.add(tag);
				
				PropertyDefinition propertyDef = tag.getPropertyDefinition(modifiedItem.getOldName());
				
				propertyDef.setName(modifiedItem.getName());

				
				Set<String> oldValues = propertyDef.getPossibleValueList().stream().filter(value -> !modifiedItem.getPossibleValueList().contains(value)).collect(Collectors.toSet());
				Set<String> newValues = modifiedItem.getPossibleValueList().stream().filter(value -> !propertyDef.getPossibleValueList().contains(value)).collect(Collectors.toSet());
				
				boolean assumeSimpleReplacementOperation = oldValues.size() == 1 && newValues.size() == 1;
				if (assumeSimpleReplacementOperation) {
					propertyActions.add(new PropertyAction(propertyDef.getName(), PropertyActionType.REPLACE, oldValues.iterator().next(), newValues.iterator().next()));
				}
				
				boolean assumeSimpleRemoveOperation = oldValues.size() == 1 && newValues.size() == 0;
				if (assumeSimpleRemoveOperation) {
					propertyActions.add(new PropertyAction(propertyDef.getName(), PropertyActionType.REMOVE, oldValues.iterator().next(), null));
				}
				
				propertyDef.setPossibleValueList(modifiedItem.getPossibleValueList());
					
				project.getTagManager().updateUserDefinedPropertyDefinition(
						tag, propertyDef);					
			}
		}
		try {
			if (project.getSourceDocumentReferences().stream().anyMatch(doc -> !doc.getUserMarkupCollectionRefs().isEmpty())) {

				ConfirmDialog.show(UI.getCurrent(), "Edit Annotations", "Do you want to adjust the affected annotations as well?", "Yes", "No", new ConfirmDialog.Listener() {
					
					@Override
					public void onClose(ConfirmDialog dialog) {
						if (dialog.isConfirmed()) {					
							WizardContext wizardContext = new WizardContext();
							wizardContext.put(EditAnnotationWizardContextKey.TAGSETS, affectedTagsets);
							wizardContext.put(EditAnnotationWizardContextKey.TAGS, tags);
							wizardContext.put(EditAnnotationWizardContextKey.PROPERTY_NAMES, propertyNames);
							wizardContext.put(EditAnnotationWizardContextKey.PROPERTY_ACTIONS, propertyActions);
							
							BulkEditAnnotationWizard wizard = new BulkEditAnnotationWizard(
									eventBus, project, wizardContext, 
									new SaveCancelListener<WizardContext>() {
										
										@Override
										@SuppressWarnings("unchecked")
										public void savePressed(WizardContext result) {
											Collection<AnnotationCollectionReference> affectedCollections = (Collection<AnnotationCollectionReference>) result.get(EditAnnotationWizardContextKey.COLLECTIONS);
											Collection<PropertyAction> actions = (Collection<PropertyAction>) result.get(EditAnnotationWizardContextKey.PROPERTY_ACTIONS);
											Collection<TagDefinition> affectedTags =  (Collection<TagDefinition>) result.get(EditAnnotationWizardContextKey.TAGS);
											
											Collection<TagsetDefinition> affectedTagsets = (Collection<TagsetDefinition>) result.get(EditAnnotationWizardContextKey.TAGSETS);		
											updateAnnotations(affectedCollections, affectedTagsets, affectedTags, actions);
										}
									});
							wizard.show();
						}
					}
				});
			}
		}
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error accessing collections", e);
		}		
	}

	private void updateAnnotations(Collection<AnnotationCollectionReference> affectedCollections, Collection<TagsetDefinition> affectedTagsets, Collection<TagDefinition> affectedTags, Collection<PropertyAction> actions) {
		try {
			Multimap<TagInstance, Property> toBeUpdatedInstances = ArrayListMultimap.create();
			Set<AnnotationCollectionReference> toBeUpatedCollections = Sets.newHashSet();
			
			AnnotationCollectionManager collectionManager = new AnnotationCollectionManager(project);
			for (AnnotationCollectionReference collectionRef : affectedCollections) {
				AnnotationCollection collection = project.getAnnotationCollection(collectionRef);
				collectionManager.add(collection);
				for (TagDefinition tag : affectedTags) {
					List<TagReference> tagReferences = collection.getTagReferences(tag);
					
					for (PropertyAction action : actions) {
						String propertyName = action.propertyName();
						PropertyDefinition propDef = tag.getPropertyDefinition(propertyName);
						if (propDef != null) {
							tagReferences.stream()
							.map(TagReference::getTagInstance)
							.distinct()
							.map(tagInstance -> {
									Property prop = tagInstance.getUserDefinedPropetyByUuid(propDef.getUuid());
									if (prop == null) {
										prop = new Property(propDef.getUuid(), Collections.emptySet());
										tagInstance.addUserDefinedProperty(prop);
									}
									return new Pair<>(tagInstance, prop);
								}
							
							).forEach(instancePropPair -> {
								Property prop = instancePropPair.getSecond();
								List<String> existingValues = prop.getPropertyValueList();
								switch (action.type()) {
								case ADD: {
									if (!existingValues.contains(action.value())) {
										prop.setPropertyValueList(Stream.concat(existingValues.stream(), Stream.of(action.value())).toList());
										toBeUpdatedInstances.put(instancePropPair.getFirst(), prop);
										toBeUpatedCollections.add(collectionRef);
									}
									break;
								}
								case REMOVE: {
									int size = existingValues.size();
									
									prop.setPropertyValueList(existingValues.stream().filter(val -> !val.equals(action.value())).toList());
									if (size != prop.getPropertyValueList().size()) {
										toBeUpdatedInstances.put(instancePropPair.getFirst(), prop);
										toBeUpatedCollections.add(collectionRef);
									}
									break;
								}
								case REPLACE: {
									prop.setPropertyValueList(existingValues.stream().map(val -> val.equals(action.value())?action.replaceValue():val).toList());
									if (!existingValues.equals(prop.getPropertyValueList())) {
										toBeUpdatedInstances.put(instancePropPair.getFirst(), prop);
										toBeUpatedCollections.add(collectionRef);
									}
									break;
								}
								}
							});
						}
					}
				}
				toBeUpdatedInstances.asMap().forEach((tagInstance, properties) -> {				
					collectionManager.updateTagInstanceProperties(collection, tagInstance, properties);
				});
			}
	
			project.addAndCommitCollections(toBeUpatedCollections, "Auto-committing annotations that were updated due to tag modifications");
		
	    } catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error loading data", e);
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
		
		Set<TagsetDefinition> affectedTagsets = Sets.newHashSet();
		
		
		for (TagDefinition tag : targetTags) {
			TagsetDefinition tagset = 
				project.getTagManager().getTagLibrary().getTagsetDefinition(tag);

			affectedTagsets.add(tagset);
			
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
		
		try {
			if (project.getSourceDocumentReferences().stream().anyMatch(doc -> !doc.getUserMarkupCollectionRefs().isEmpty())) {
				ConfirmDialog.show(UI.getCurrent(), "Edit Annotations", "Do you want to adjust the affected annotations as well?", "Yes", "No", new ConfirmDialog.Listener() {
					
					@Override
					public void onClose(ConfirmDialog dialog) {
						if (dialog.isConfirmed()) {					
							WizardContext wizardContext = new WizardContext();
							wizardContext.put(EditAnnotationWizardContextKey.TAGSETS, affectedTagsets);
							wizardContext.put(EditAnnotationWizardContextKey.TAGS, targetTags);
							wizardContext.put(EditAnnotationWizardContextKey.PROPERTY_NAMES, availableCommonPropertyNames);
							
							BulkEditAnnotationWizard wizard = new BulkEditAnnotationWizard(
									eventBus, project, wizardContext, 
									new SaveCancelListener<WizardContext>() {
										
										@Override
										@SuppressWarnings("unchecked")
										public void savePressed(WizardContext result) {
											Collection<AnnotationCollectionReference> affectedCollections = (Collection<AnnotationCollectionReference>) result.get(EditAnnotationWizardContextKey.COLLECTIONS);
											Collection<PropertyAction> actions = (Collection<PropertyAction>) result.get(EditAnnotationWizardContextKey.PROPERTY_ACTIONS);
											Collection<TagDefinition> affectedTags =  (Collection<TagDefinition>) result.get(EditAnnotationWizardContextKey.TAGS);
											
											Collection<TagsetDefinition> affectedTagsets = (Collection<TagsetDefinition>) result.get(EditAnnotationWizardContextKey.TAGSETS);		
											updateAnnotations(affectedCollections, affectedTagsets, affectedTags, actions);
										}
									});
							wizard.show();
						}
					}
				});
			}
		}
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error accessing collections", e);
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
				.filter(tagset -> !tagset.isResponsible(project.getCurrentUser().getIdentifier()))
				.findAny()
				.isPresent();
			
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
				true,
				new Action() {
					@Override
					public void execute() {
						AddSubtagDialog addTagDialog =
							new AddSubtagDialog(new SaveCancelListener<TagDefinition>() {
								public void savePressed(TagDefinition result) {
									for (TagDefinition parent : parentTags) {
										
										TagsetDefinition tagset = 
											project.getTagManager().getTagLibrary().getTagsetDefinition(parent);
										
										TagDefinition tag = new TagDefinition(result, idGenerator.generate(), parent.getUuid(), tagset.getUuid());
										
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
			Notification.show("Info", "Please select at least one parent tag!", Type.HUMANIZED_MESSAGE);
		}
	}
	
	private void handleAddTagRequest() {
		
		final Optional<TagsetDefinition> selectedTagset = tagsetGrid.getSelectedItems()
			.stream()
			.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagsetDataItem)
			.findFirst()
			.map(tagsetTreeItem -> ((TagsetTreeItem)tagsetTreeItem).getTagset());
			
		if (tagsets.isEmpty()) {
			Notification.show(
				"Info", 
				"You do not have any tagsets to add tags to yet, please create a tagset first!",
				Type.HUMANIZED_MESSAGE);
			return;
		}
	
		List<TagsetDefinition> editableTagsets = 
				tagsets.stream()
				.collect(Collectors.toList());
		
		boolean beyondUsersResponsibility =
				editableTagsets.stream()
				.filter(tagset -> !tagset.isResponsible(project.getCurrentUser().getIdentifier()))
				.findAny()
				.isPresent();
		
		BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
				true,
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
    					!project.isReadOnly(),
    					tagsetCollator);
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
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error loading data", e);
        }
	}
	
    private void expandTagsetDefinition(TagsetDefinition tagset) {
    	for (TagDefinition tag : tagset) {
    		TagDataItem item = new TagDataItem(tag, tagset, tagsetCollator);
    		tagsetGrid.expand(item);
    	}
	}
    
	private void handleAddTagsetRequest() {
    	SingleTextInputDialog tagsetNameDlg = 
        		new SingleTextInputDialog("Create Tagset", "Please enter the tagset name:",
        				new SaveCancelListener<String>() {
    						
    						@Override
    						public void savePressed(String result) {
    							IDGenerator idGenerator = new IDGenerator();
    							TagsetDefinition tagset = new TagsetDefinition(
    									idGenerator.generateTagsetId(), result);
    							tagset.setResponsibleUser(project.getCurrentUser().getIdentifier());
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
            	TagDataItem tagItem =  new TagDataItem(tag, tagset, tagsetItem.isEditable(), tagsetCollator);
                tagsetData.addItem(tagsetItem, tagItem);
                addTagSubTree(tagset, tag, tagItem);
            }
        }
	}

	private void addTagSubTree(
    		TagsetDefinition tagset, 
    		TagDefinition tag, TagsetTreeItem parentItem) {
        for (TagDefinition childDefinition : tagset.getDirectChildren(tag)) {
        	TagDataItem childItem = new TagDataItem(childDefinition, tagset, parentItem.isEditable(), tagsetCollator);
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
        	TagsetTreeItem tagsetItem = new TagsetDataItem(tagset, tagsetCollator);
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
