package de.catma.ui.module.project.documentwizard;

import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.wizard.Wizard;
import de.catma.ui.dialog.wizard.WizardContext;

public class DocumentWizard extends Wizard {
	
	public DocumentWizard(WizardContext wizardContext, SaveCancelListener<WizardContext> saveCancelListener) {
		super(
			"Upload some Documents",
			progressPanel -> new UploadPanel((number, description) -> progressPanel.addStep(number, description)),
			wizardContext,
			saveCancelListener
		);
	}

}
