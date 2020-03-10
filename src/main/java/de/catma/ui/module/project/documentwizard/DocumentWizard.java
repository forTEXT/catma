package de.catma.ui.module.project.documentwizard;

import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.wizard.Wizard;
import de.catma.ui.dialog.wizard.WizardContext;

public class DocumentWizard extends Wizard {
	public enum WizardContextKey {
		UPLOAD_FILE_LIST,
		PROJECT,
		;
	}
	
	public DocumentWizard(WizardContext wizardContext, SaveCancelListener<WizardContext> saveCancelListener) {
		super(
			"Add Documents to your Project",
			progressPanel -> new UploadStep(wizardContext, (number, description) -> progressPanel.addStep(number, description)),
			wizardContext,
			saveCancelListener
		);
	}

}
