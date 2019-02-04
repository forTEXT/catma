package de.catma.ui.tagger.annotationpanel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.standoffmarkup.usermarkup.Annotation;
import de.catma.tag.Property;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;

public class EditAnnotationPropertiesDialog extends AbstractOkCancelDialog<List<Property>> {
	
	private Annotation annotation;
	private TabSheet propertyTabSheet;

	public EditAnnotationPropertiesDialog(
		Annotation annotation,
		SaveCancelListener<List<Property>> saveCancelListener) {
		super("Edit Annotation", saveCancelListener);
		this.annotation = annotation;
		createComponents();
	}

	private void createComponents() {
		propertyTabSheet = new TabSheet();
		propertyTabSheet.setSizeFull();
		
		for (Property property : annotation.getTagInstance().getUserDefinedProperties()) {
			propertyTabSheet.addTab(new EditPropertyTab(property), property.getName());
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
