package de.catma.ui.modules.tags;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;

import de.catma.document.repository.Repository;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.hugecard.HugeCard;

public class TagsView extends HugeCard {
	
	private EventBus eventBus;
	private Repository project;
	private TreeGrid tagsetGrid;
	private ActionGridComponent<TreeGrid<TagsetTreeItem>> tagsetGridComponent;

	public TagsView(EventBus eventBus, Repository project) {
		super("Manage Tags");
		this.eventBus = eventBus;
		this.project = project;
		eventBus.register(this);
		initComponents();
		initActions();
	}

	private void initActions() {
		// TODO Auto-generated method stub
		
	}

	private void initComponents() {
		
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
		
	}
	
	public void close() {
		eventBus.unregister(this);
	}

}
