package de.catma.ui.module.analyze.visualization.kwic.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.Project;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;
import de.catma.ui.module.annotate.annotationpanel.EditPropertyTab;

public class PropertySelectionStep extends VerticalLayout implements WizardStep {
	
	private ProgressStep progressStep;
	private TabSheet propertyTabSheet;
	private CollectionSelectionStep nextStep;
	private WizardContext context;
	private boolean skipped;

	public PropertySelectionStep(EventBus eventBus, Project project, WizardContext context, ProgressStepFactory progressStepFactory) {
		this.context = context;
		this.context.put(AnnotationWizardContextKey.PROPERTIES, Collections.emptyList());
		this.progressStep = progressStepFactory.create(2, "Set Property values");
		this.nextStep = new CollectionSelectionStep(eventBus, project, context, progressStepFactory);
		initComponents();
	}

	private void initComponents() {
		setSizeFull();
		setMargin(false);
		propertyTabSheet = new TabSheet();
		propertyTabSheet.setSizeFull();
		addComponent(propertyTabSheet);
	}

	@Override
	public ProgressStep getProgressStep() {
		return progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		return nextStep;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		if (stepChangeListener != null) {
			stepChangeListener.stepChanged(this);
		}
	}

	public void setTag(TagDefinition tag) {
		propertyTabSheet.removeAllComponents();
		
		for (PropertyDefinition propertyDef : tag.getUserDefinedPropertyDefinitions()) {
			Property property = new Property(propertyDef.getUuid(), Collections.emptySet());
			EditPropertyTab editPropertyTab = 
					new EditPropertyTab(property, propertyDef.getPossibleValueList());
			editPropertyTab.addSelectionListener(event -> handleSelection());
			propertyTabSheet.addTab(editPropertyTab, propertyDef.getName());
		}	
	}
	
	private void handleSelection() {
		int propCount = 0;
		int valueCount = 0;
		Iterator<Component> componentIterator = propertyTabSheet.iterator();
		while (componentIterator.hasNext()) {
			EditPropertyTab tab = (EditPropertyTab)componentIterator.next();
			propCount++;
			valueCount += tab.getPropertyValues().size();
		}
		
		String propertyCaption = propCount > 1?"Properties":"Property";
		String valueCaption = valueCount > 1?"values":"value";
		
		progressStep.setCompleted(
			String.format(
				"%1$d %2$s on %3$d %4$s ", 
				valueCount, valueCaption, propCount, propertyCaption));
	}

	
	@Override
	public void exit(boolean back) {

		if (back) {
			context.put(AnnotationWizardContextKey.PROPERTIES, Collections.emptyList());
		}
		else {
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
			context.put(AnnotationWizardContextKey.PROPERTIES, result);
			if (result.isEmpty()) {
				progressStep.setCompleted("no values");
			}			
		}
	}

	public void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}
	
	@Override
	public boolean isSkipped() {
		return skipped;
	}
}
