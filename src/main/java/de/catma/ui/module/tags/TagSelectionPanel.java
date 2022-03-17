package de.catma.ui.module.tags;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.project.Project;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class TagSelectionPanel extends VerticalLayout {

	public interface TagSelectionChangedListener {
		public void tagSelectionChanged(TagDefinition tag);
	}

	private TreeGrid<TagsetTreeItem> tagsetGrid;
	private ActionGridComponent<TreeGrid<TagsetTreeItem>> tagsetGridComponent;
	private TreeData<TagsetTreeItem> tagsetData;
	private TreeDataProvider<TagsetTreeItem> tagsetDataProvider;
	private Project project;
	private IDGenerator idGenerator;
	private PropertyChangeListener tagChangedListener;
	private PropertyChangeListener tagsetChangeListener;

	public TagSelectionPanel(Project project) {
		this.project = project;
		this.idGenerator = new IDGenerator();
		initComponents();
		initActions();
		initData();
		initListeners();
	}
	
	private void initData() {
        try {
            tagsetData = new TreeData<TagsetTreeItem>();
            Collection<TagsetDefinition> tagsets = project.getTagManager().getTagLibrary().getTagsetDefinitions();
            for (TagsetDefinition tagset : tagsets) {
            	TagsetTreeItem tagsetItem = new TagsetDataItem(tagset);
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
    	}
	}
	private void addTags(
			TagsetTreeItem tagsetItem, 
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
	
	private void initActions() {
		tagsetGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getColor(), new HtmlRenderer())
		.setCaption("Tagsets")
		.setSortable(false)
		.setWidth(200);
		tagsetGrid.setHierarchyColumn(
			tagsetGrid.addColumn(tagsetTreeItem -> tagsetTreeItem.getName())
			.setCaption("Tags")
			.setSortable(false)
			.setExpandRatio(1));
		
		tagsetGridComponent.setSearchFilterProvider(new SearchFilterProvider<TagsetTreeItem>() {
			@Override
			public SerializablePredicate<TagsetTreeItem> createSearchFilter(String searchInput) {
				return new TagsetSearchFilterProvider(searchInput, tagsetData);
			}
		});
		
	    ContextMenu addContextMenu = 
	    		tagsetGridComponent.getActionGridBar().getBtnAddContextMenu();
	    addContextMenu.addItem("Add Tagset", clickEvent -> handleAddTagsetRequest());
	    addContextMenu.addItem("Add Tag", clickEvent -> handleAddTagRequest());
	    addContextMenu.addItem("Add Subtag", clickEvent -> handleAddSubtagRequest());
	    
		tagsetGrid.addExpandListener(expandEvent -> expandEvent.getExpandedItem().setTagsetExpanded(true));
		tagsetGrid.addCollapseListener(collapseEvent -> collapseEvent.getCollapsedItem().setTagsetExpanded(false));

	}

	private void initComponents() {
		setSizeFull();
		tagsetGrid = TreeGridFactory.createDefaultTreeGrid();
		tagsetGrid.setSizeFull();
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
        tagsetGridComponent.setSizeFull();
        addComponent(tagsetGridComponent);
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
    							tagset.setResponsableUser(project.getUser().getIdentifier());
    							project.getTagManager().addTagsetDefinition(
    								tagset);
    						}
    					});
            	
    	tagsetNameDlg.show();
	}	
	
	private void handleAddTagRequest() {
		
		final Optional<TagsetDefinition> selectedTagset = tagsetGrid.getSelectedItems()
			.stream()
			.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagsetDataItem)
			.findFirst()
			.map(tagsetTreeItem -> ((TagsetDataItem)tagsetTreeItem).getTagset());

		if (tagsetData.getRootItems().isEmpty()) {
			Notification.show(
				"Info", 
				"You do not have any Tagsets to add Tags to yet, please create a Tagset first!", 
				Type.HUMANIZED_MESSAGE);
			return;
		}
	
		List<TagsetDefinition> editableTagsets = 
				tagsetData.getRootItems().stream()
				.map(tagsetTreeItem -> ((TagsetDataItem)tagsetTreeItem).getTagset())
				.collect(Collectors.toList());

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
	
	private void handleAddSubtagRequest() {
		final List<TagDefinition> parentTags = tagsetGrid.getSelectedItems()
		.stream()
		.filter(tagsetTreeItem -> tagsetTreeItem instanceof TagDataItem)
		.map(tagsetTreeItem -> ((TagDataItem)tagsetTreeItem).getTag())
		.collect(Collectors.toList());
		
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
					
					
					tagsetDataProvider.refreshAll();
				}
				
			}
		};
		project.getTagManager().addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);
		
		this.tagsetChangeListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				handleTagsetChange(evt);
			}
		};		
		
        project.getTagManager().addPropertyChangeListener(
        		TagManagerEvent.tagsetDefinitionChanged,
        		tagsetChangeListener);		
		
	}
	
	private void handleTagsetChange(PropertyChangeEvent evt) {
		initData();
	} 	
	
	public void close() {
		project.getTagManager().removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);	
		
        project.getTagManager().removePropertyChangeListener(
    		TagManagerEvent.tagsetDefinitionChanged,
    		tagsetChangeListener);		
	
	}
	
	public void addTagSelectionChangedListener(TagSelectionChangedListener tagSelectionChangedListener) {
		tagsetGrid.addSelectionListener(new SelectionListener<TagsetTreeItem>() {
			
			@Override
			public void selectionChange(SelectionEvent<TagsetTreeItem> event) {
				
				if(event.getFirstSelectedItem().isPresent()) {
					TagsetTreeItem item = event.getFirstSelectedItem().get();
					if (item instanceof TagDataItem) {
						tagSelectionChangedListener.tagSelectionChanged(
								((TagDataItem)item).getTag());
					}
					else {
						tagSelectionChangedListener.tagSelectionChanged(null);
					}
				}
				
			}
		});
	}
}
