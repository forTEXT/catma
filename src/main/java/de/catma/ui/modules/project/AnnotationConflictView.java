package de.catma.ui.modules.project;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;

import de.catma.indexer.KwicProvider;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagManager;
import de.catma.ui.tagger.annotationpanel.AnnotatedTextProvider;

public class AnnotationConflictView extends de.catma.ui.layout.VerticalLayout {

	private AnnotationConflict annotationConflict;
	private CollectionConflict collectionConflict;
	private TagManager tagManager;
	private TreeGrid<PropertyTreeItem> leftPropertyGrid;
	private TreeGrid<PropertyTreeItem> rightPropertyGrid;
	private Label annotatedKwic;
	private KwicProvider kwicProvider;
	private Button btMine;
	private Button btBoth;
	private Button btTheirs;

	public AnnotationConflictView(
			AnnotationConflict annotationConflict, 
			CollectionConflict collectionConflict, 
			TagManager tagManager, KwicProvider kwicProvider) {
		this.annotationConflict = annotationConflict;
		this.collectionConflict = collectionConflict;
		this.tagManager = tagManager;
		this.kwicProvider = kwicProvider;
		initComponents();
		initData();
	}

	private void initData() {
		
		TagInstance devTagInstance = this.annotationConflict.getDevTagInstance();
		TagDefinition tag = 
				tagManager.getTagLibrary().getTagDefinition(devTagInstance.getTagDefinitionId());
		String tagPath = tagManager.getTagLibrary().getTagPath(tag);
		
		String annotatedKwicText = AnnotatedTextProvider.buildKeywordInContext(
				this.annotationConflict.getDevTagReferences(), kwicProvider, tag, tagPath);
		annotatedKwic.setValue(annotatedKwicText);
		
		TreeDataProvider<PropertyTreeItem> leftPropertyTreeDataProvider = 
				createPropertyTreeDataProvider(devTagInstance, tag);
		
		leftPropertyGrid.setDataProvider(leftPropertyTreeDataProvider);
		leftPropertyGrid.expandRecursively(
				leftPropertyTreeDataProvider.getTreeData().getRootItems(), 1);
		TagInstance masterTagInstance = this.annotationConflict.getMasterTagInstance();
		
		TreeDataProvider<PropertyTreeItem> rightPropertyTreeDataProvider = 
				createPropertyTreeDataProvider(masterTagInstance, tag);
		rightPropertyGrid.setDataProvider(rightPropertyTreeDataProvider);		
		rightPropertyGrid.expandRecursively(
				rightPropertyTreeDataProvider.getTreeData().getRootItems(), 1);
 	}

	private TreeDataProvider<PropertyTreeItem> createPropertyTreeDataProvider(
			TagInstance tagInstance, TagDefinition tag) {
		TreeData<PropertyTreeItem> propertyTreeData = new TreeData<>();
		for (Property property : tagInstance.getUserDefinedProperties()) {
			PropertyDefinition propertyDef = 
				tag.getPropertyDefinitionByUuid(property.getPropertyDefinitionId());
			PropertyDataItem propertyDataItem = new PropertyDataItem(propertyDef, property);
			propertyTreeData.addItem(null, propertyDataItem);
			for (String value : property.getPropertyValueList()) {
				propertyTreeData.addItem(propertyDataItem, new PropertyValueDataItem(property, value));
			}
			
		}
		
		TreeDataProvider<PropertyTreeItem> propertyTreeDataProvider = 
				new TreeDataProvider<>(propertyTreeData);
		return propertyTreeDataProvider;
	}

	private void initComponents() {
		setWidth("100%");
		
		this.annotatedKwic = new Label();
		this.annotatedKwic.setContentMode(ContentMode.HTML);
		addComponent(this.annotatedKwic);
		
		HorizontalLayout comparisonPanel = new HorizontalLayout();
		leftPropertyGrid = new TreeGrid<>();
		leftPropertyGrid.addStyleNames(
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");
		leftPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getName()).setCaption("Property");
		leftPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getValue()).setCaption("Value");
		
		comparisonPanel.addComponent(leftPropertyGrid);
		rightPropertyGrid = new TreeGrid<>();
		rightPropertyGrid.addStyleNames(
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");

		rightPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getName()).setCaption("Property");
		rightPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getValue()).setCaption("Value");
		comparisonPanel.addComponent(rightPropertyGrid);
		addComponent(comparisonPanel);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		btMine = new Button("Take mine");
		buttonPanel.addComponent(btMine);
		btBoth = new Button("Take both");
		buttonPanel.addComponent(btBoth);
		btTheirs = new Button("Take theirs");
		buttonPanel.addComponent(btTheirs);
		addComponent(buttonPanel);
	}

	
	
}
