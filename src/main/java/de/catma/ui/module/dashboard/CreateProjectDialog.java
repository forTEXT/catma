package de.catma.ui.module.dashboard;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.dialog.SaveCancelListener;

public class CreateProjectDialog extends AbstractProjectDialog{

	public CreateProjectDialog(ProjectManager projectManager, SaveCancelListener<ProjectReference> saveCancelListener) {
		super("Creates a new Project",projectManager, saveCancelListener);
	}

	
	@Override
	protected ProjectReference getResult() {
		try {
			return projectManager.createProject(name.getValue(), description.getValue());
			
		} catch (Exception e) {
			errorLogger.showAndLogError(e.getMessage(),e);
			return null;
		}
	}
}
