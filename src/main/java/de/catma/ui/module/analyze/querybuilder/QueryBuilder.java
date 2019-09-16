package de.catma.ui.module.analyze.querybuilder;

import de.catma.project.Project;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.wizard.Wizard;
import de.catma.ui.dialog.wizard.WizardContext;

public class QueryBuilder extends Wizard {
	public enum ContextKey {
		QUERY_TREE,
		;
	}
	
	public QueryBuilder(Project project, 
			WizardContext context, SaveCancelListener<WizardContext> saveCancelListener) {
		super("Build your Query", 
				progressPanel -> new SearchTypeSelectionStep(
						project, 
						context,
						(number, description) -> progressPanel.addStep(number, description)),
				context, saveCancelListener);
	}

}
