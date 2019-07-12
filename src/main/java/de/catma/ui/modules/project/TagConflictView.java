package de.catma.ui.modules.project;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;

import de.catma.project.TagsetConflict;
import de.catma.project.conflict.Resolution;
import de.catma.project.conflict.TagConflict;
import de.catma.tag.PropertyDefinition;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.util.ColorConverter;

public class TagConflictView extends VerticalFlexLayout {

	private TagConflict tagConflict;
	private TagsetConflict tagsetConflict;
	private ResolutionListener resolutionListener;
	private Label tagsetLabel;
	private Label leftTagLabel;
	private Label rightTagLabel;
	private TreeGrid<PropertyDefTreeItem> propertyGrid;
	private Button btMine;
	private Button btBoth;
	private Button btTheirs;

	public TagConflictView(
		TagConflict tagConflict, 
		TagsetConflict tagsetConflict, 
		ResolutionListener resolutionListener) {
		this.tagConflict = tagConflict;
		this.tagsetConflict = tagsetConflict;
		this.resolutionListener = resolutionListener;
		initComponents();
		initActions();
		initData();
	}

	private void initData() {
		String devBackgroundColor = "#"+ColorConverter.toHex(tagConflict.getDevTagDefinition().getColor());
		String devColor = "#464646";
		if (!ColorConverter.isLightColor(tagConflict.getDevTagDefinition().getColor())) {
			devColor = "#FFFFFF";
		}
		this.leftTagLabel.setValue(
			"My Tag: <span style=\"background: "+devBackgroundColor+";color: "+devColor+";\">"
			+tagConflict.getDevTagDefinition().getName()+"</span>");

		String masterBackgroundColor = "#"+ColorConverter.toHex(tagConflict.getMasterTagDefinition().getColor());
		String masterColor = "#464646";
		if (!ColorConverter.isLightColor(tagConflict.getMasterTagDefinition().getColor())) {
			masterColor = "#FFFFFF";
		}
		this.rightTagLabel.setValue(
			"Their Tag: <span style=\"background: "+masterBackgroundColor+";color: "+masterColor+";\">"
			+tagConflict.getMasterTagDefinition().getName()+"</span>");
		
		Set<String> allPropertyDefIds = new HashSet<>();
		allPropertyDefIds.addAll(
			tagConflict.getDevTagDefinition().getUserDefinedPropertyDefinitions()
			.stream()
			.map(PropertyDefinition::getUuid)
			.collect(Collectors.toSet()));
		allPropertyDefIds.addAll(
				tagConflict.getMasterTagDefinition().getUserDefinedPropertyDefinitions()
				.stream()
				.map(PropertyDefinition::getUuid)
				.collect(Collectors.toSet()));
		TreeData<PropertyDefTreeItem> treeData = new TreeData<>();
		
		for (String propertyDefId : allPropertyDefIds) {
			PropertyDefinition mine = 
				tagConflict.getDevTagDefinition().getPropertyDefinitionByUuid(propertyDefId);
			PropertyDefinition theirs = 
				tagConflict.getMasterTagDefinition().getPropertyDefinitionByUuid(propertyDefId);
			
			PropertyDefDataItem propertyDefDataItem = new PropertyDefDataItem(mine, theirs);
			
			treeData.addItem(null, propertyDefDataItem);
			
			SortedSet<String> values = new TreeSet<>();
			
			if (mine != null) {
				values.addAll(mine.getPossibleValueList());
			}
			
			if (theirs != null) {
				values.addAll(theirs.getPossibleValueList());
			}
			
			for (String value : values) {
				PropertyDefValueDataItem propertyDefValueDataItem = new PropertyDefValueDataItem(mine, theirs, value);
				treeData.addItem(propertyDefDataItem, propertyDefValueDataItem);
			}
		}

		TreeDataProvider<PropertyDefTreeItem> propertyDefTreeDataProvider = 
				new TreeDataProvider<>(treeData);
		propertyGrid.setDataProvider(propertyDefTreeDataProvider);
		propertyGrid.expand(treeData.getRootItems());
	}


	private void initActions() {
		btMine.addClickListener(event -> handleResolved(Resolution.MINE));
		btTheirs.addClickListener(event -> handleResolved(Resolution.THEIRS));
		btBoth.addClickListener(event -> handleResolved(Resolution.BOTH));
	}

	private void handleResolved(Resolution resolution) {
		tagConflict.setResolution(resolution);
		this.resolutionListener.resolved();
	}

	private void initComponents() {
		addStyleName("tag-conflict-view");
		
		this.tagsetLabel = new Label();
		addComponent(this.tagsetLabel);

		HorizontalFlexLayout comparisonPanel = new HorizontalFlexLayout();
		addComponent(comparisonPanel);
		comparisonPanel.setJustifyContent(JustifyContent.SPACE_AROUND);
		comparisonPanel.addStyleName("tag-conflict-view-comparison-panel");
		
		this.leftTagLabel = new Label();
		this.leftTagLabel.setContentMode(ContentMode.HTML); //TODO: escape input
		comparisonPanel.addComponent(this.leftTagLabel);
		
		
		this.rightTagLabel = new Label();
		this.rightTagLabel.setContentMode(ContentMode.HTML); //TODO: escape input
		comparisonPanel.addComponent(this.rightTagLabel);
		
		propertyGrid = new TreeGrid<>();
		propertyGrid.setWidth("100%");
		
		propertyGrid.addStyleNames(
				"tag-conflict-view-property-grid-margin",
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");
		propertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getMinePropertyName())
			.setCaption("My Properties")
			.setSortable(true);
		propertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getMinePropertyValue())
			.setCaption("My Values")
			.setSortable(true);
		propertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getTheirPropertyName())
			.setCaption("Their Properties")
			.setSortable(true);
		propertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getTheirPropertyValue())
			.setCaption("Their Values")
			.setSortable(true);
		
		addComponent(propertyGrid);

		HorizontalFlexLayout buttonPanel = new HorizontalFlexLayout();
		buttonPanel.addStyleName("tag-conflict-view-button-panel");
		buttonPanel.setJustifyContent(JustifyContent.SPACE_AROUND);
		btMine = new Button("Take mine");
		buttonPanel.addComponent(btMine);
		btBoth = new Button("Take both");
		buttonPanel.addComponent(btBoth);
		btTheirs = new Button("Take theirs");
		buttonPanel.addComponent(btTheirs);
		addComponent(buttonPanel);
	}

}
