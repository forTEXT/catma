package de.catma.ui.module.analyze.visualization.kwic.annotation.edit;

import java.util.Collection;

import com.google.common.eventbus.EventBus;

import de.catma.project.Project;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.wizard.Wizard;
import de.catma.ui.dialog.wizard.WizardContext;

public class BulkEditAnnotationWizard extends Wizard {
	
	public BulkEditAnnotationWizard(
			EventBus eventBus, Project project, 
			WizardContext context, SaveCancelListener<WizardContext> saveCancelListener) {
		super("Edit Annotations", 
				progressPanel -> (context.get(EditAnnotationWizardContextKey.COLLECTIONS) != null && !((Collection<?>)context.get(EditAnnotationWizardContextKey.COLLECTIONS)).isEmpty())? 
					new PropertyActionSelectionStep(project, context, (number, description) -> progressPanel.addStep(number, description), 1):
					new CollectionSelectionStep(
						project, 
						context,
						(number, description) -> progressPanel.addStep(number, description)),
				context, saveCancelListener);
		setWidth("80%");
		setHeight("80%");
	}
}
