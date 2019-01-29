package de.catma.ui.tagger.annotationpanel;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.standoffmarkup.usermarkup.Annotation;
import de.catma.tag.Property;

public class AnnotationDetailsCard extends VerticalLayout {
	
	private Annotation annotation;
	private TreeGrid<String> propertyTree;
	private TreeData<String> propertyTreeData;
	private TreeDataProvider<String> propertyTreeDataProvider;

	public AnnotationDetailsCard(Annotation annotation) {
		this.annotation = annotation;

		initComponents();
		initData();
	}

	private void initData() {
		for (Property property : annotation.getTagInstance().getUserDefinedProperties()) {
			propertyTreeData.addItem(null, property.getName());
			
			for (String value : property.getPropertyValueList()) {
				propertyTreeData.addItem(property.getName(), value);
			}
		}
		
		propertyTreeDataProvider.refreshAll();
		
		propertyTree.setVisible(!annotation.getTagInstance().getUserDefinedProperties().isEmpty());
	}

	private void initComponents() {
		setMargin(true);
		setSpacing(true);

		addComponent(new Label(annotation.getTagPath()));
		
		propertyTreeData= new TreeData<String>();
		propertyTreeDataProvider = new TreeDataProvider<String>(propertyTreeData);
		propertyTree = new TreeGrid<String>(propertyTreeDataProvider);
		propertyTree.addStyleName("annotation-details-card-property-tree");
		propertyTree
			.addColumn(item -> item.toString())
			.setCaption("Properties");
		propertyTree.setHeight("150px");
		propertyTree.setWidth("80%");
		addComponent(propertyTree);
		setExpandRatio(propertyTree, 1.0f);
		
		
	}

}
