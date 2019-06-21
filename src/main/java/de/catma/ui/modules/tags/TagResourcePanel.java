package de.catma.ui.modules.tags;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.rbac.RBACPermission;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.util.IDGenerator;

public class TagResourcePanel extends VerticalLayout {
	
	private Repository project;
	private Grid<TagsetDefinition> tagsetGrid;
	private PropertyChangeListener tagsetChangeListener;
	private ListDataProvider<TagsetDefinition> tagsetData;
	private ActionGridComponent<Grid<TagsetDefinition>> tagsetActionGridComponent;
	private PropertyChangeListener projectExceptionListener;
	private ErrorHandler errorHandler;
	private TagsetSelectionListener tagsetSelectionListener;
	
	public TagResourcePanel(Repository project) {
		this.project = project;
        this.errorHandler = (ErrorHandler)UI.getCurrent();

		initComponents();
		initActions();
		initProjectListeners();
		initData();
	}
	
    private void initData() {
    	try {
			tagsetData = new ListDataProvider<TagsetDefinition>(project.getTagsets());
			tagsetGrid.setDataProvider(tagsetData);
			tagsetData.getItems().forEach(tagsetGrid::select);
    	}
    	catch (Exception e) {
			errorHandler.showAndLogError("Error loading data!", e);
    	}
	}

	private void initActions() {
		tagsetGrid.addSelectionListener(
				selectionEvent -> handleTagsetSelectionEvent(selectionEvent));
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
        						return name.startsWith(searchInput);
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
	
	public void setTagsetChangeListener(PropertyChangeListener tagsetChangeListener) {
		this.tagsetChangeListener = tagsetChangeListener;
	}
	
	private void initProjectListeners() {
        this.projectExceptionListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Exception e = (Exception) evt.getNewValue();
				errorHandler.showAndLogError("Error handling Project!", e);
				
			}
		};
		project.addPropertyChangeListener(
				RepositoryChangeEvent.exceptionOccurred, projectExceptionListener);
		
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
			tagsetData.refreshAll();
		}
		else if (newValue == null) { // removal
			tagsetData.refreshAll();
		}
		else { // metadata update
			TagsetDefinition tagset = (TagsetDefinition)newValue;
			tagsetData.refreshItem(tagset);
		}
		
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
		addStyleName("tags-resource-panel");
		Label tagsetLabel = new Label("Tagsets");
		
		tagsetGrid = new Grid<>();
		tagsetGrid.addStyleNames(
				"annotate-resource-grid", 
				"flat-undecorated-icon-buttonrenderer",
				"no-focused-before-border");
		tagsetGrid.setSelectionMode(SelectionMode.MULTI);
		//TODO: shouldn't be fixed size
		tagsetGrid.setWidth("400px");
		tagsetGrid.setHeight("230px");
		tagsetGrid
			.addColumn(tagset -> tagset.getName())
			.setCaption("Name")
			.setWidth(150);
		
		tagsetGrid.addColumn(
				tagset -> project.hasPermission(
					project.getRoleForTagset(tagset.getUuid()),
					RBACPermission.TAGSET_WRITE)?VaadinIcons.UNLOCK.getHtml():VaadinIcons.LOCK.getHtml(),
				new HtmlRenderer())
		.setWidth(50);
		
		tagsetGrid
			.addColumn(tagset -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer())
			.setExpandRatio(1);
		
		tagsetActionGridComponent = 
				new ActionGridComponent<Grid<TagsetDefinition>>(tagsetLabel, tagsetGrid);
		
		addComponent(tagsetActionGridComponent);
	}

	
	public void close() {
		if (project != null) {
			project.removePropertyChangeListener(
				RepositoryChangeEvent.exceptionOccurred, projectExceptionListener);

	        project.getTagManager().removePropertyChangeListener(
        		TagManagerEvent.tagsetDefinitionChanged,
        		tagsetChangeListener);
		}
		
	}
}
