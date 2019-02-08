package de.catma.ui.tagger.annotationpanel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.document.standoffmarkup.usermarkup.Annotation;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;

public class EditAnnotationPropertiesDialog extends AbstractOkCancelDialog<List<Property>> {
	
	private Annotation annotation;
	private TabSheet propertyTabSheet;

	public EditAnnotationPropertiesDialog(
		Repository project,
		Annotation annotation,
		SaveCancelListener<List<Property>> saveCancelListener) {
		super("Edit Annotation", saveCancelListener);
		this.annotation = annotation;
		createComponents(project);
	}

	private void createComponents(Repository project) {
		propertyTabSheet = new TabSheet();
		propertyTabSheet.setSizeFull();

		String tagId = annotation.getTagInstance().getTagDefinitionId();
		TagDefinition tag = project.getTagManager().getTagLibrary().getTagDefinition(tagId);

		for (Property property : annotation.getTagInstance().getUserDefinedProperties()) {
			PropertyDefinition propertyDef = 
					tag.getPropertyDefinitionByUuid(property.getPropertyDefinitionId());
			propertyTabSheet.addTab(
				new EditPropertyTab(property, propertyDef.getPossibleValueList()), 
				propertyDef.getName()
			);
		}
		
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(propertyTabSheet);
		((VerticalLayout)content).setExpandRatio(propertyTabSheet, 1.0f);
	}

	@Override
	protected List<Property> getResult() {
		List<Property> result = new ArrayList<>();
		Iterator<Component> componentIterator = propertyTabSheet.iterator();
		while (componentIterator.hasNext()) {
			EditPropertyTab tab = (EditPropertyTab)componentIterator.next();
			
			if (tab.isChanged()) {
				Property property = tab.getProperty();
				
				property.setPropertyValueList(tab.getPropertyValues());
				result.add(property);
			}
		}
		return result;
	}

	@Override
	protected void layoutWindow() {
		setWidth("50%");
		setHeight("70%");
	}
	
}
