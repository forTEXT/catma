package de.catma.ui.module.annotate.annotationpanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid.ItemClick;
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

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.document.annotation.Annotation;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionManager;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.project.event.ChangeType;
import de.catma.project.event.CollectionChangeEvent;
import de.catma.serialization.intrinsic.xml.XmlMarkupCollectionSerializationHandler;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.dialog.BeyondResponsibilityConfirmDialog;
import de.catma.ui.dialog.BeyondResponsibilityConfirmDialog.Action;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.tags.AddEditPropertyDialog;
import de.catma.ui.module.tags.AddParenttagDialog;
import de.catma.ui.module.tags.AddSubtagDialog;
import de.catma.ui.module.tags.EditTagDialog;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class AnnotationPanel extends VerticalLayout {
	
	
	public interface TagReferenceSelectionChangeListener {
		public void tagReferenceSelectionChanged(
				List<TagReference> tagReferences, boolean selected);
	}
	
	private ComboBox<AnnotationCollection> currentEditableCollectionBox;
	private Button btAddCollection;
	private TreeGrid<TagsetTreeItem> tagsetGrid;
	private Project project;
	private Collection<TagsetDefinition> tagsets = new ArrayList<>();
	private List<AnnotationCollection> collections = new ArrayList<>();
	private List<AnnotationCollection> editableCollections = new ArrayList<>();
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
	private AnnotationCollectionManager collectionManager;
	private Supplier<SourceDocumentReference> currentDocumentProvider;
	private EventBus eventBus;
	private IconButton btFilterCollection;

	public AnnotationPanel(
			Project project, 
			AnnotationCollectionManager collectionManager, 
			Consumer<String> annotationSelectionListener,
			Consumer<ValueChangeEvent<AnnotationCollection>> collectionSelectionListener,
			Consumer<TagDefinition> tagSelectionListener,
			Supplier<SourceDocumentReference> currentDocumentProvider,
			EventBus eventBus) {
		this.project = project;
		this.collectionManager = collectionManager;
		this.currentDocumentProvider = currentDocumentProvider;
		this.eventBus = eventBus;
		this.eventBus.register(this);
		this.tagsetData = new TreeData<TagsetTreeItem>();
		initComponents(annotationSelectionListener);
		initActions(collectionSelectionListener, tagSelectionListener);
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
		            	Optional<TagsetTreeItem> optionalTagsetItem = findItem(tagset.getUuid());
		            	optionalTagsetItem.ifPresent(tagsetItem -> {
		            		if (tagsetData.contains(tagsetItem)) {
		            			tagsetData.addItem(
		            					tagsetItem, new TagDataItem(tag));
		            			
		            			tagsetDataProvider.refreshAll();
		            			tagsetGrid.expand(tagsetItem);
		            		}
		            	});
		            }
		            else {
		            	TagDefinition parentTag = 
		            		project.getTagManager().getTagLibrary().getTagDefinition(tag.getParentUuid());
		            	Optional<TagsetTreeItem> optionalParentTagItem = findItem(parentTag.getUuid());
		            	optionalParentTagItem.ifPresent(parentTagItem -> {
		            		if (tagsetData.contains(parentTagItem)) {
		            			tagsetData.addItem(parentTagItem, new TagDataItem(tag));
		            			
		            			tagsetDataProvider.refreshAll();
		            			tagsetGrid.expand(parentTagItem);
		            		}
		            	});
		            }
		            
		            
				}
				else if (newValue == null) { //removed
					Pair<TagsetDefinition,TagDefinition> deleted = (Pair<TagsetDefinition, TagDefinition>) oldValue;
					
					TagDefinition deletedTag = deleted.getSecond();
					Optional<TagsetTreeItem> optionalDeletedItem = findItem(deletedTag.getUuid());
					optionalDeletedItem.ifPresent(deletedItem -> {
						if (tagsetData.contains(deletedItem)) {
							tagsetData.removeItem(deletedItem);
							tagsetDataProvider.refreshAll();
							tagsetGrid.deselect(deletedItem);
						}						
					});
					
				}
				else { //update
					TagDefinition tag = (TagDefinition) newValue;
					TagsetDefinition tagset = (TagsetDefinition)oldValue;
					
	            	Optional<TagsetTreeItem> optionalTagsetItem = findItem(tagset.getUuid());
	            	optionalTagsetItem.ifPresent(tagsetItem -> {
	            		if (tagsetData.contains(tagsetItem)) {
	            			Optional<TagsetTreeItem> optionalTagDataItem = 
	            					findItem((TagsetDataItem)tagsetItem, tag.getUuid()); 
	            			optionalTagDataItem.ifPresent(tagDataItem -> {
	            				TagsetTreeItem parent = 
	            						tagsetData.getParent(tagDataItem);
		            			tagsetData.removeItem(tagDataItem);
		            			
		            			((TagDataItem)tagDataItem).setPropertiesExpanded(true);
	
		            			tagsetData.addItem(parent, tagDataItem);
		            			//TODO: sort
		            			
		            			tagsetDataProvider.refreshAll();
		            			showExpandedProperties(((TagDataItem)tagDataItem));
	            				
	            			});
	            		}
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
				
				Optional<TagsetTreeItem> optionalParentItem = Optional.empty();
				if (tag.getParentUuid().isEmpty()) {
					optionalParentItem = findItem(
						project.getTagManager().getTagLibrary()
							.getTagsetDefinition(tag.getTagsetDefinitionUuid()).getUuid());
				}
				else {
					optionalParentItem = findItem(
						project.getTagManager().getTagLibrary().getTagDefinition(tag.getParentUuid()).getUuid());
				}
				
				if (optionalParentItem.isPresent()) {
					TagsetTreeItem parentItem = optionalParentItem.get();
				
					final String tagId = tag.getUuid();
					tagsetData.getChildren(parentItem)
					.stream()
					.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagDataItem)
					.map(tagsetTreeItem -> (TagDataItem)tagsetTreeItem)
					.filter(tagDataItem -> tagDataItem.getTag().getUuid().equals(tagId))
					.findFirst()
					.ifPresent(tagDataItem -> {
						tagsetDataProvider.refreshItem(tagDataItem);
						tagDataItem.setPropertiesExpanded(false);
						hideExpandedProperties(tagDataItem);
						tagDataItem.setPropertiesExpanded(true);
						showExpandedProperties(tagDataItem);
					});
				}
				
				tagsetDataProvider.refreshAll();
				tagsetGrid.deselectAll();
			}
		};
		
		project.getTagManager().addPropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged, 
				propertyDefinitionChangedListener);	
			
	}

	private Optional<TagsetTreeItem> findItem(String uuid) {
		return findItem((TagsetDataItem)null, uuid);
	}
	
	private Optional<TagsetTreeItem> findItem(TagsetDataItem startTagsetDataItem, String uuid) {
		if (startTagsetDataItem == null) {
			return findItem(tagsetData.getRootItems(), uuid);
		}
		else if (startTagsetDataItem.getTagset().getUuid().equals(uuid)) {
			return Optional.of(startTagsetDataItem);
		}
		return findItem(tagsetData.getChildren(startTagsetDataItem), uuid);
	}

	private Optional<TagsetTreeItem> findItem(List<TagsetTreeItem> items, String uuid) {

		for (TagsetTreeItem item : items) {
			if (item.getId().equals(uuid)) {
				return Optional.of(item);
			}
		}
		
		for (TagsetTreeItem item : items) {
			Optional<TagsetTreeItem> optionalChild = findItem(tagsetData.getChildren(item), uuid);
			if (optionalChild.isPresent()) {
				return optionalChild;
			}
		}
		
		return Optional.empty();
	}

	@Subscribe
	public void handleCollectionChanged(CollectionChangeEvent collectionChangeEvent) {
		if (collectionChangeEvent.getChangeType().equals(ChangeType.CREATED)) {
			SourceDocumentReference document = currentDocumentProvider.get();
			if (document != null) {
	    		AnnotationCollectionReference collectionReference = 
	    				collectionChangeEvent.getCollectionReference();
	    		if (document.getUuid().equals(collectionReference.getSourceDocumentId())) {
					try {
						addCollection(project.getAnnotationCollection(collectionReference));
					} catch (IOException e) {
						((ErrorHandler) UI.getCurrent()).showAndLogError("Error adding new collection", e);
					}
	    		}
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
            
            initEditableCollectionData(null);
        } catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error loading data", e);
        }
    }

    private void initEditableCollectionData(AnnotationCollection preselectedCollection) {
        editableCollections.clear();
        editableCollections.addAll(
        		collections.stream()
        		.filter(collection -> !(Boolean)btFilterCollection.getData() 
        			|| collection.isResponsible(project.getCurrentUser().getIdentifier()))
        		.sorted((c1, c2) -> c1.getName().compareTo(c2.getName()))
        		.collect(Collectors.toList()));
        	
        
        ListDataProvider<AnnotationCollection> editableCollectionProvider = 
        		new ListDataProvider<>(editableCollections);
        currentEditableCollectionBox.setValue(null);
        currentEditableCollectionBox.setDataProvider(editableCollectionProvider);
        
		if (editableCollectionProvider.getItems().isEmpty()) {
			currentEditableCollectionBox.setPlaceholder(
					"Please create a collection...");
		}
		else {
			currentEditableCollectionBox.setPlaceholder(
					"Please select a collection...");
		}
		
		if (preselectedCollection != null 
				&& editableCollections.contains(preselectedCollection) 
				&& !project.isReadOnly()) {
			currentEditableCollectionBox.setValue(preselectedCollection);
		}
		else if (editableCollectionProvider.getItems().size() == 1) {
			AnnotationCollection collection = 
					editableCollectionProvider.getItems().iterator().next();
			if (collection.isResponsible(project.getCurrentUser().getIdentifier())) {
				handleCollectionChangeRequest(collection.getUuid());
			}
		}
	}

	private void expandTagsetDefinition(TagsetDefinition tagset) {
    	for (TagDefinition tag : tagset) {
    		TagDataItem item = new TagDataItem(tag);
    		tagsetGrid.expand(item);
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

	private void initActions(
			Consumer<ValueChangeEvent<AnnotationCollection>> collectionSelectionListener, 
			Consumer<TagDefinition> tagSelectionListener) {
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
						
						if (strValue != null && strValue.toLowerCase().contains(searchInput.toLowerCase())) {
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
		
		tagsetGrid.addItemClickListener(clickEvent -> handleTagSelection(clickEvent, tagSelectionListener));
		tagsetGrid.addCollapseListener(event -> event.getCollapsedItem().setTagsetExpanded(false));
		tagsetGrid.addExpandListener(event -> event.getExpandedItem().setTagsetExpanded(true));
		
        ContextMenu addContextMenu = 
        		tagsetGridComponent.getActionGridBar().getBtnAddContextMenu();
        addContextMenu.addItem("Add Tag", clickEvent -> handleAddTagRequest());
        addContextMenu.addItem("Add Subtag", clickEvent -> handleAddSubtagRequest());
        addContextMenu.addItem("Add Property", clickEvent -> handleAddPropertyRequest());
		
		ContextMenu moreOptionsContextMenu = 
				tagsetGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
		moreOptionsContextMenu.addItem("Edit Tag", clickEvent -> handleEditTagRequest());
		moreOptionsContextMenu.addItem("Edit/Delete Properties", clickEvent -> handleEditPropertiesRequest());
		
		currentEditableCollectionBox.addValueChangeListener(
			event -> collectionSelectionListener.accept(event));
		currentEditableCollectionBox.addValueChangeListener(event -> annotationDetailsPanel.refreshAnnotationDetailsProvider());
		
		annotationDetailsPanel.addMinimizeButtonClickListener(
				clickEvent -> setAnnotationDetailsPanelVisible(false));
		btMaximizeAnnotationDetailsRibbon.addClickListener(
				ClickEvent -> setAnnotationDetailsPanelVisible(true));
		
		btAddCollection.addClickListener(clickEvent -> handelAddCollectionRequest());
		btFilterCollection.addClickListener(clickEvent -> handleFilterCollectionChangeRequest());
		
        toggleEditComponentsEnabledState();
	}

	private void handleFilterCollectionChangeRequest() {
		
		BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				(Boolean)btFilterCollection.getData(), 
				true,
				new Action() {
					@Override
					public void execute() {
						// toggle filtered state
						boolean newFilteredState  = !(Boolean)btFilterCollection.getData();
						btFilterCollection.setData(newFilteredState);
						btFilterCollection.setIcon(newFilteredState?VaadinIcons.LOCK:VaadinIcons.UNLOCK);
						btFilterCollection.setDescription(newFilteredState?"Hiding collections beyond my responsibility":"Showing all available collections");
						AnnotationCollection currentSelection = 
								currentEditableCollectionBox.getValue();
						initEditableCollectionData(currentSelection);
					}
				});

	}

	private void handleTagSelection(
			ItemClick<TagsetTreeItem> clickEvent, 
			Consumer<TagDefinition> tagSelectionListener) {
		
		TagsetTreeItem selectedItem = clickEvent.getItem();
		if (selectedItem instanceof TagDataItem) {
			if (getSelectedEditableCollection() != null) {
				TagDefinition tag = ((TagDataItem)selectedItem).getTag();
				tagSelectionListener.accept(tag);
			}
			else {
				highlightCurrentEditableCollectionBox();
			}
		}
	}

	public void highlightCurrentEditableCollectionBox() {
		currentEditableCollectionBox.addStyleName("annotationpanel-current-editable-collection-box-highlight");
		final UI currentUI = UI.getCurrent(); 
		((BackgroundServiceProvider)currentUI).acquireBackgroundService().schedule(
				() -> {
					currentUI.access(() -> {
						currentEditableCollectionBox.removeStyleName(
								"annotationpanel-current-editable-collection-box-highlight");
						currentUI.push();
					});
				},
				1,
				TimeUnit.SECONDS);
	}

	private void handelAddCollectionRequest() {
		final SourceDocumentReference document = currentDocumentProvider.get();
		
		if (document != null) {
	    	SingleTextInputDialog collectionNameDlg = 
	        		new SingleTextInputDialog("Create Annotation Collection", "Please enter the collection name:",
	        				new SaveCancelListener<String>() {
	    						
	    						@Override
	    						public void savePressed(String result) {
	   								project.createAnnotationCollection(result, document);
	    						}
	    					});
	        	
        	collectionNameDlg.show();
		}
		else {
			Notification.show("Info", "Please select a document first!", Type.HUMANIZED_MESSAGE);
		}
	}

	private void handleEditPropertiesRequest() {
		handleAddPropertyRequest();
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
			
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					beyondUsersResponsibility, 
					true,
					new Action() {
						@Override
						public void execute() {
							
							EditTagDialog editTagDialog = new EditTagDialog(tagsets, project.getTagManager().getTagLibrary(), new TagDefinition(targetTag),
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
			
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				beyondUsersResponsibility, 
				true,
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
								new AddSubtagDialog(new SaveCancelListener<Collection<TagDefinition>>() {
									public void savePressed(Collection<TagDefinition> result) {
										for (TagDefinition item : result) {
											project.getTagManager().addTagDefinition(
												project.getTagManager().getTagLibrary().getTagsetDefinition(item.getTagsetDefinitionUuid()), item);
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
			.map(tagsetTreeItem -> ((TagsetDataItem)tagsetTreeItem).getTagset());
			
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
		tagsetDataProvider.refreshAll();
	}

	private void initComponents(Consumer<String> annotationSelectionListener) {
		setSizeFull();
		setSpacing(true);
		
		currentEditableCollectionBox = new ComboBox<>("Collection currently being edited");
		currentEditableCollectionBox.setWidth("100%");
		currentEditableCollectionBox.setPlaceholder(
				"Please select a document first!");
		
		btAddCollection = new IconButton(VaadinIcons.PLUS);
		btFilterCollection = new IconButton(VaadinIcons.LOCK);
		btFilterCollection.setData(Boolean.TRUE); //editable colletions are filtered
		btFilterCollection.setDescription("Hiding collections beyond my responsibility");
		HorizontalLayout editableCollectionPanel = 
				new HorizontalLayout(currentEditableCollectionBox, btAddCollection, btFilterCollection);
		editableCollectionPanel.addStyleName("annotate-right-padding");
		
		editableCollectionPanel.setWidth("100%");
		editableCollectionPanel.setExpandRatio(currentEditableCollectionBox, 1.0f);
		editableCollectionPanel.setComponentAlignment(btAddCollection, Alignment.BOTTOM_CENTER);
		editableCollectionPanel.setComponentAlignment(btFilterCollection, Alignment.BOTTOM_CENTER);
		addComponent(editableCollectionPanel);
		
		Label tagsetsLabel = new Label("Tagsets");
		
		tagsetGrid = TreeGridFactory.createDefaultTreeGrid();
		tagsetGrid.addStyleNames(
				"flat-undecorated-icon-buttonrenderer");
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
			if (!collection.isResponsible(project.getCurrentUser().getIdentifier())
					&& (Boolean)btFilterCollection.getData()) {
				handleFilterCollectionChangeRequest();
			}
			if (!editableCollections.contains(collection)) {
				editableCollections.add(collection);
			}
			if (!project.isReadOnly()) {
				currentEditableCollectionBox.setValue(collection);
				Notification.show("Info", 
					String.format(
						"The collection currently being edited has been changed to \"%s\"",
						collection.getName()),
					Type.HUMANIZED_MESSAGE);
			}
		});
	}

	public void setData(
			SourceDocumentReference sdRef,
			Collection<TagsetDefinition> tagsets, 
			List<AnnotationCollection> collections) throws Exception {
		this.tagsets = tagsets;
		this.collections = collections;
		this.annotationDetailsPanel.setDocument(sdRef);
		initData();
	}
	
	public void setTagReferenceSelectionChangeListener(TagReferenceSelectionChangeListener selectionListener) {
		this.selectionListener = selectionListener;
	}
	
	public AnnotationCollection getSelectedEditableCollection() {
		return currentEditableCollectionBox.getValue();
	}

	public void addCollection(AnnotationCollection collection) {
		if (!this.collections.contains(collection)) {
			this.collections.add(collection);
		}
		
		// if it is an imported XML collection it is not likely that the collection is going to be edited
		// and we do not automatically select it for editing
		if (!Objects.equals(collection.getName(), XmlMarkupCollectionSerializationHandler.DEFAULT_ANNOTATION_COLLECTION_TITLE)) {
			setSelectedEditableCollection(collection);
		}
		//TODO: show annotations from this collection and selected tagsets
	}

	public void setSelectedEditableCollection(AnnotationCollection collection) {
		if (!(Boolean)btFilterCollection.getData() 
				|| collection.isResponsible(project.getCurrentUser().getIdentifier())) {
			if (!this.editableCollections.contains(collection)) {
				this.editableCollections.add(collection);
				currentEditableCollectionBox.getDataProvider().refreshAll();
			}
			if ((currentEditableCollectionBox.getValue() == null) 
					&& !this.editableCollections.isEmpty()
					&& !project.isReadOnly()) {
				currentEditableCollectionBox.setValue(collection);
				Notification.show("Info", 
						String.format(
							"The collection currently being edited has been changed to \"%s\"",
							collection.getName()),
						Type.HUMANIZED_MESSAGE);				
			}
		}
	}
	
	private void removeCollection(AnnotationCollection collection) {
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
					tr -> tr.getTagInstanceId()).collect(Collectors.toSet()));
		
		toggleEditComponentsEnabledState();		
	}
	
	private void toggleEditComponentsEnabledState() {
		currentEditableCollectionBox.setEnabled(!project.isReadOnly());
		btAddCollection.setEnabled(!project.isReadOnly());
		btFilterCollection.setEnabled(!project.isReadOnly());	
        tagsetGridComponent.getActionGridBar().setAddBtnEnabled(!project.isReadOnly());
        tagsetGridComponent.getActionGridBar().setMoreOptionsBtnEnabled(!project.isReadOnly());
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
		tagsetItem.setTagsetExpanded(true);
		tagsetDataProvider.refreshAll();
	}
	
	public void removeTagset(TagsetDefinition tagset) {
		tagsets.remove(tagset);
		TagsetDataItem tagsetDataItem = new TagsetDataItem(tagset);
		if (tagsetData.contains(tagsetDataItem)) {
			tagsetData.removeItem(tagsetDataItem);
		}
		tagsetDataProvider.refreshAll();
		tagsetGrid.deselectAll();
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

	public void clearTagsets() {
		tagsets.clear();
		tagsetData.clear();
		tagsetDataProvider.refreshAll();
		tagsetGrid.deselectAll();
	}

}
