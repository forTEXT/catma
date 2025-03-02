package de.catma.ui.module.annotate.annotationpanel;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.annotation.Annotation;
import de.catma.project.Project;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;

public class EditAnnotationPropertiesDialog extends AbstractOkCancelDialog<List<Property>> {
	
	private Annotation annotation;
	private TabSheet propertyTabSheet;

	public EditAnnotationPropertiesDialog(
		Project project,
		Annotation annotation,
		SaveCancelListener<List<Property>> saveCancelListener) {
		super("Edit Annotation", saveCancelListener);
		this.annotation = annotation;
		createComponents(project);
	}

	private void createComponents(Project project) {
		Locale locale = project.getTagManager().getTagLibrary().getLocale();
		Collator collator = Collator.getInstance(locale);
		Comparator<PropertyDefinition> pdComparator = (pd1, pd2) -> collator.compare(Optional.ofNullable(pd1.getName()).orElse(""), Optional.ofNullable(pd2.getName()).orElse(""));
		Comparator<String> valueComparator = (value1, value2) -> collator.compare(Optional.ofNullable(value1).orElse(""), Optional.ofNullable(value2).orElse(""));
		propertyTabSheet = new TabSheet();
		propertyTabSheet.setSizeFull();

		String tagId = annotation.getTagInstance().getTagDefinitionId();
		TagDefinition tag = project.getTagManager().getTagLibrary().getTagDefinition(tagId);
		for (PropertyDefinition propertyDef : tag.getUserDefinedPropertyDefinitions().stream().sorted(pdComparator).toList()) {
			Property property = 
				annotation.getTagInstance().getUserDefinedPropetyByUuid(propertyDef.getUuid());
			if (property == null) {
				property = new Property(propertyDef.getUuid(), Collections.emptySet());
			}
			propertyTabSheet.addTab(
				new EditPropertyTab(property, propertyDef.getPossibleValueList(), valueComparator), 
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
