package de.catma.ui.module.analyze.visualization.kwic.annotation.edit;

import com.google.common.eventbus.EventBus;

import de.catma.project.Project;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.wizard.Wizard;
import de.catma.ui.dialog.wizard.WizardContext;

public class AnnotationWizard extends Wizard {
	
	public AnnotationWizard(
			EventBus eventBus, Project project, 
			WizardContext context, SaveCancelListener<WizardContext> saveCancelListener) {
		super("Edit Annotations", 
				progressPanel -> new CollectionSelectionStep(
						project, 
						context,
						(number, description) -> progressPanel.addStep(number, description)),
				context, saveCancelListener);
	}
}
