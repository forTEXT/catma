package de.catma.ui.module.project.documentwizard;

import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.wizard.Wizard;
import de.catma.ui.dialog.wizard.WizardContext;

public class DocumentWizard extends Wizard {
	public enum WizardContextKey {
		COLLECTION_NAME_PATTERN,
		UPLOAD_FILE_LIST,
		PROJECT,
		APOSTROPHE_AS_SEPARATOR,
		SIMPLE_XML,
		TAGSET_IMPORT_LIST,
		;
	}
	
	public DocumentWizard(WizardContext wizardContext, SaveCancelListener<WizardContext> saveCancelListener) {
		super(
			"Add Documents to Your Project",
			progressPanel -> new UploadStep(wizardContext, (number, description) -> progressPanel.addStep(number, description)),
			wizardContext,
			saveCancelListener
		);
	}

}
