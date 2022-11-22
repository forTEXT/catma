package de.catma.ui.module.tags;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.project.Project;
import de.catma.project.Project.ProjectEvent;
import de.catma.project.event.ProjectReadyEvent;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.IDGenerator;

public class TagResourcePanel extends VerticalLayout {
	
	private Project project;
	private Grid<TagsetDefinition> tagsetGrid;
	private PropertyChangeListener tagsetChangeListener;
	private ListDataProvider<TagsetDefinition> tagsetDataProvider;
	private ActionGridComponent<Grid<TagsetDefinition>> tagsetActionGridComponent;
	private PropertyChangeListener projectExceptionListener;
	private ErrorHandler errorHandler;
	private TagsetSelectionListener tagsetSelectionListener;
	private EventBus eventBus;
	
	public TagResourcePanel(Project project, EventBus eventBus) {
		this.eventBus = eventBus;
		this.project = project;
        this.errorHandler = (ErrorHandler)UI.getCurrent();

		initComponents();
		initActions();
		initProjectListeners();
		initData();
		this.eventBus.register(this);
	}
	
    private void initData() {
    	try {
			tagsetDataProvider = new ListDataProvider<TagsetDefinition>(project.getTagsets());
			tagsetGrid.setDataProvider(tagsetDataProvider);
			tagsetDataProvider.getItems().forEach(tagsetGrid::select);
    	}
    	catch (Exception e) {
			errorHandler.showAndLogError("Error loading data", e);
    	}
	}

	private void initActions() {
		tagsetGrid.addSelectionListener(
				selectionEvent -> handleTagsetSelectionEvent(selectionEvent));
		tagsetActionGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false); // no options so far
        tagsetActionGridComponent.getActionGridBar().addBtnAddClickListener(
            	click -> handleAddTagsetRequest());		
        tagsetActionGridComponent.setSearchFilterProvider(new SearchFilterProvider<TagsetDefinition>() {
        	@Override
        	public SerializablePredicate<TagsetDefinition> createSearchFilter(final String searchInput) {
        		return new SerializablePredicate<TagsetDefinition>() {
        			@Override
        			public boolean test(TagsetDefinition t) {
        				if (t != null) {
        					String name = t.getName();
        					if (name != null) {
        						return name.toLowerCase().contains(searchInput.toLowerCase());
        					}
        				}
        				return false;
        			}
				};
        	}
		});
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
	
	public void setTagsetChangeListener(PropertyChangeListener tagsetChangeListener) {
		this.tagsetChangeListener = tagsetChangeListener;
	}
	
	private void initProjectListeners() {
        this.projectExceptionListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Exception e = (Exception) evt.getNewValue();
				errorHandler.showAndLogError("Error handling project", e);
				
			}
		};
		project.addEventListener(
				ProjectEvent.exceptionOccurred, projectExceptionListener);
		
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
		Object oldValue = evt.getOldValue();
		Object newValue = evt.getNewValue();
		
		if (oldValue == null) { // creation
			tagsetDataProvider.refreshAll();
			tagsetGrid.select((TagsetDefinition)newValue);
		}
		else if (newValue == null) { // removal
			tagsetDataProvider.refreshAll();
		}
		else { // metadata update
			TagsetDefinition tagset = (TagsetDefinition)newValue;
			tagsetDataProvider.refreshItem(tagset);
		}
		
		Collection<TagsetDefinition> selection = getSelectedTagsets();
		tagsetGrid.deselectAll();
		selection.forEach(tagset -> {
			if (tagsetDataProvider.getItems().contains(tagset)) {
				tagsetGrid.select(tagset);
			}
		});
		
    	if (tagsetSelectionListener != null) {
    		tagsetSelectionListener.tagsetsSelected(getSelectedTagsets());
    	}
	} 
	
	public Collection<TagsetDefinition> getSelectedTagsets() {
		return tagsetGrid.getSelectedItems();
	}
	
    private void handleTagsetSelectionEvent(SelectionEvent<TagsetDefinition> selectionEvent) {
    	if (tagsetSelectionListener != null) {
    		tagsetSelectionListener.tagsetsSelected(selectionEvent.getAllSelectedItems());
    	}
    
    }

	private void initComponents() {
		setWidth("400px");
		setHeight("100%");

		Label tagsetLabel = new Label("Tagsets");
		
		tagsetGrid = new Grid<>();
		tagsetGrid.addStyleNames(
				"resource-grid", 
				"flat-undecorated-icon-buttonrenderer",
				"no-focused-before-border");
		tagsetGrid.setSelectionMode(SelectionMode.MULTI);

		tagsetGrid.setSizeFull();
		
		tagsetGrid
			.addColumn(tagset -> tagset.getName())
			.setCaption("Name")
			.setWidth(250);
		
		tagsetGrid
			.addColumn(tagset -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer())
			.setExpandRatio(1);
		
		tagsetActionGridComponent = 
				new ActionGridComponent<Grid<TagsetDefinition>>(tagsetLabel, tagsetGrid);
		tagsetActionGridComponent.setSelectionModeFixed(SelectionMode.MULTI);
		tagsetActionGridComponent.getActionGridBar().setMargin(new MarginInfo(false, false, false, true));
		addComponent(tagsetActionGridComponent);
	}

	
	public void close() {
		if (project != null) {
			project.removeEventListener(
				ProjectEvent.exceptionOccurred, projectExceptionListener);

	        project.getTagManager().removePropertyChangeListener(
        		TagManagerEvent.tagsetDefinitionChanged,
        		tagsetChangeListener);
		}
		eventBus.unregister(this);
	}
	
	@Subscribe
	public void handleProjectReadyEvent(ProjectReadyEvent projectReadyEvent) {
		TagsetSelectionListener tagsetSelectionListener = this.tagsetSelectionListener;
		this.tagsetSelectionListener = null;
		initData();
		this.tagsetSelectionListener = tagsetSelectionListener;
		this.tagsetSelectionListener.tagsetsSelected(getSelectedTagsets());
	}
	
	public void setTagsetSelectionListener(TagsetSelectionListener tagsetSelectionListener) {
		this.tagsetSelectionListener = tagsetSelectionListener;
	}

	public void setSelectedTagset(TagsetDefinition tagset) {
		this.tagsetGrid.deselectAll();
		this.tagsetGrid.select(tagset);
	}
}
