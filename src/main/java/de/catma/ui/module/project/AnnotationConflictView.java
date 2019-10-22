package de.catma.ui.module.project;

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

import de.catma.indexer.KwicProvider;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.project.conflict.Resolution;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagManager;
import de.catma.ui.module.annotate.annotationpanel.AnnotatedTextProvider;

public class AnnotationConflictView extends VerticalLayout {
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
	private TextField documentNameField;
	private TextField collectionNameField;

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
		
		documentNameField.setReadOnly(false);
		documentNameField.setValue(kwicProvider.getSourceDocumentName());
		documentNameField.setReadOnly(true);
		
		collectionNameField.setReadOnly(false);
		collectionNameField.setValue(this.collectionConflict.getContentInfoSet().getTitle());
		collectionNameField.setReadOnly(true);
		
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
		setMargin(true);
		setSpacing(true);
		setSizeFull();
		
		HorizontalLayout resourceInfoPanel = new HorizontalLayout();
		resourceInfoPanel.setWidth("100%");
		addComponent(resourceInfoPanel);
		
		this.documentNameField = new TextField("Document");
		this.documentNameField.setReadOnly(true);
		this.documentNameField.setWidth("100%");
		this.documentNameField.addStyleName("annotation-conflict-view-resource-field");
		resourceInfoPanel.addComponent(documentNameField);
		
		this.collectionNameField = new TextField("Collection");
		this.collectionNameField.setReadOnly(true);
		this.collectionNameField.setWidth("100%");
		this.collectionNameField.addStyleName("annotation-conflict-view-resource-field");
		resourceInfoPanel.addComponent(collectionNameField);
		
		this.annotatedKwic = new Label();
		this.annotatedKwic.setWidth("100%");
		this.annotatedKwic.setContentMode(ContentMode.HTML);
		addComponent(this.annotatedKwic);
		
		HorizontalLayout comparisonPanel = new HorizontalLayout();
		comparisonPanel.setSizeFull();
		
		leftPropertyGrid = new TreeGrid<>();
		leftPropertyGrid.setSizeFull();
		
		leftPropertyGrid.addStyleNames(
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");
		leftPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getName()).setCaption("Property");
		leftPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getValue()).setCaption("Value");
		
		comparisonPanel.addComponent(leftPropertyGrid);
		
		rightPropertyGrid = new TreeGrid<>();
		rightPropertyGrid.setSizeFull();
		rightPropertyGrid.addStyleNames(
				"no-focused-before-border", "flat-undecorated-icon-buttonrenderer");

		rightPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getName()).setCaption("Property");
		rightPropertyGrid.addColumn(propertyTreeItem -> propertyTreeItem.getValue()).setCaption("Value");
		comparisonPanel.addComponent(rightPropertyGrid);
		addComponent(comparisonPanel);
		setExpandRatio(comparisonPanel, 1f);
		
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
