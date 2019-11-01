package de.catma.ui.module.project;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.TagsetConflict;
import de.catma.project.conflict.Resolution;
import de.catma.project.conflict.TagConflict;
import de.catma.tag.PropertyDefinition;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.util.Cleaner;
import de.catma.util.ColorConverter;

public class TagConflictView extends VerticalLayout {

	private TagConflict tagConflict;
	private TagsetConflict tagsetConflict;
	private ResolutionListener resolutionListener;
	private TextField tagsetNameField;
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
		this.tagsetNameField.setReadOnly(false);
		this.tagsetNameField.setValue(tagsetConflict.getName());
		this.tagsetNameField.setReadOnly(true);
		
		Set<String> allPropertyDefIds = new HashSet<>();
		if (tagConflict.getDevTagDefinition() != null) {
			String devBackgroundColor = "#"+ColorConverter.toHex(tagConflict.getDevTagDefinition().getColor());
			String devColor = "#464646";
			if (!ColorConverter.isLightColor(tagConflict.getDevTagDefinition().getColor())) {
				devColor = "#FFFFFF";
			}
			this.leftTagLabel.setValue(
				"My Tag: <span style=\"background: "+devBackgroundColor+";color: "+devColor+";\">"
				+Cleaner.clean(tagConflict.getDevTagDefinition().getName())+"</span>");

			allPropertyDefIds.addAll(
					tagConflict.getDevTagDefinition().getUserDefinedPropertyDefinitions()
					.stream()
					.map(PropertyDefinition::getUuid)
					.collect(Collectors.toSet()));
		}
		else {
			this.leftTagLabel.setValue("My Tag is DELETED");
		}
		
		if (tagConflict.getMasterTagDefinition() != null) {
			String masterBackgroundColor = "#"+ColorConverter.toHex(tagConflict.getMasterTagDefinition().getColor());
			String masterColor = "#464646";
			if (!ColorConverter.isLightColor(tagConflict.getMasterTagDefinition().getColor())) {
				masterColor = "#FFFFFF";
			}
			this.rightTagLabel.setValue(
				"Their Tag: <span style=\"background: "+masterBackgroundColor+";color: "+masterColor+";\">"
				+ Cleaner.clean(tagConflict.getMasterTagDefinition().getName())+"</span>");
			
			allPropertyDefIds.addAll(
					tagConflict.getMasterTagDefinition().getUserDefinedPropertyDefinitions()
					.stream()
					.map(PropertyDefinition::getUuid)
					.collect(Collectors.toSet()));
		}
		else {
			this.rightTagLabel.setValue("Their Tag is DELETED");
		}
		
		TreeData<PropertyDefTreeItem> treeData = new TreeData<>();
		
		for (String propertyDefId : allPropertyDefIds) {
			PropertyDefinition mine = null;
			if (tagConflict.getDevTagDefinition() != null) {
				mine = tagConflict.getDevTagDefinition().getPropertyDefinitionByUuid(propertyDefId);
			}
			
			PropertyDefinition theirs = null;
			if (tagConflict.getMasterTagDefinition() != null) {
				theirs = tagConflict.getMasterTagDefinition().getPropertyDefinitionByUuid(propertyDefId);
			}
			
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
		
		btBoth.setEnabled(tagConflict.isBothPossible());
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
		setMargin(true);
		setSpacing(true);
		setSizeFull();
		
		this.tagsetNameField = new TextField("Tagset");
		this.tagsetNameField.setReadOnly(true);
		this.tagsetNameField.setWidth("100%");
		this.tagsetNameField.addStyleName("tag-conflict-view-tagset-field");
		
		addComponent(this.tagsetNameField);

		HorizontalLayout comparisonPanel = new HorizontalLayout();
		comparisonPanel.setWidth("100%");
		addComponent(comparisonPanel);
		
		this.leftTagLabel = new Label();
		this.leftTagLabel.setContentMode(ContentMode.HTML); 
		comparisonPanel.addComponent(this.leftTagLabel);
		
		
		this.rightTagLabel = new Label();
		this.rightTagLabel.setContentMode(ContentMode.HTML);
		comparisonPanel.addComponent(this.rightTagLabel);
		
		propertyGrid = TreeGridFactory.createDefaultTreeGrid();
		propertyGrid.setSizeFull();
		
		propertyGrid.addStyleNames(
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
		setExpandRatio(propertyGrid, 1f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		
		btMine = new Button("Take mine");
		buttonPanel.addComponent(btMine);
		buttonPanel.setComponentAlignment(btMine, Alignment.BOTTOM_CENTER);
		btBoth = new Button("Take both");
		buttonPanel.addComponent(btBoth);
		buttonPanel.setComponentAlignment(btBoth, Alignment.BOTTOM_CENTER);
		btTheirs = new Button("Take theirs");
		buttonPanel.addComponent(btTheirs);
		buttonPanel.setComponentAlignment(btTheirs, Alignment.BOTTOM_CENTER);
		addComponent(buttonPanel);
	}

}
