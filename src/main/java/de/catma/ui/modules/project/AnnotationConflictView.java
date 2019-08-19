package de.catma.ui.modules.project;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;

import de.catma.indexer.KwicProvider;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.project.conflict.Resolution;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagManager;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.tagger.annotationpanel.AnnotatedTextProvider;

public class AnnotationConflictView extends VerticalFlexLayout {
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
	private ResolutionListener resolutionListener;

	public AnnotationConflictView(
			AnnotationConflict annotationConflict, 
			CollectionConflict collectionConflict, 
			TagManager tagManager, KwicProvider kwicProvider,
			ResolutionListener resolutionListener) {
		this.annotationConflict = annotationConflict;
		this.collectionConflict = collectionConflict;
		this.tagManager = tagManager;
		this.kwicProvider = kwicProvider;
		this.resolutionListener = resolutionListener;
		initComponents();
		initActions();
		initData();
	}

	private void initActions() {
		btMine.addClickListener(event -> handleResolved(Resolution.MINE));
		btTheirs.addClickListener(event -> handleResolved(Resolution.THEIRS));
		btBoth.addClickListener(event -> handleResolved(Resolution.BOTH));
	}

	private void handleResolved(Resolution resolution) {
		annotationConflict.setResolution(resolution);
		this.resolutionListener.resolved();
	}

	private void initData() {
		
		TagInstance devTagInstance = this.annotationConflict.getDevTagInstance();
		TagDefinition tag = 
				tagManager.getTagLibrary().getTagDefinition(devTagInstance.getTagDefinitionId());
		String tagPath = tagManager.getTagLibrary().getTagPath(tag);
		
		String annotatedKwicText = AnnotatedTextProvider.buildAnnotatedKeywordInContext(
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
		addStyleName("annotation-conflict-view");
		this.annotatedKwic = new Label();
		this.annotatedKwic.setContentMode(ContentMode.HTML);
		addComponent(this.annotatedKwic);
		
		HorizontalFlexLayout comparisonPanel = new HorizontalFlexLayout();
		comparisonPanel.setJustifyContent(JustifyContent.SPACE_AROUND);
		comparisonPanel.addStyleName("annotation-conflict-view-comparison-panel");
		
		leftPropertyGrid = new TreeGrid<>();
		leftPropertyGrid.setWidth("100%");
		
		leftPropertyGrid.addStyleNames(
				"annotation-conflict-view-property-grid-left-margin",
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");
		leftPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getName()).setCaption("Property");
		leftPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getValue()).setCaption("Value");
		
		comparisonPanel.addComponent(leftPropertyGrid);
		rightPropertyGrid = new TreeGrid<>();
		rightPropertyGrid.setWidth("100%");
		rightPropertyGrid.addStyleNames(
				"annotation-conflict-view-property-grid-right-margin",
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");

		rightPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getName()).setCaption("Property");
		rightPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getValue()).setCaption("Value");
		comparisonPanel.addComponent(rightPropertyGrid);
		addComponent(comparisonPanel);
		HorizontalFlexLayout buttonPanel = new HorizontalFlexLayout();
		buttonPanel.addStyleName("annotation-conflict-view-button-panel");
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
