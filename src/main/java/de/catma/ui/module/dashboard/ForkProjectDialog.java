package de.catma.ui.module.dashboard;

import de.catma.project.ProjectsManager;
import de.catma.project.ProjectReference;
import de.catma.ui.dialog.SaveCancelListener;

public class ForkProjectDialog extends AbstractProjectDialog {
	
	private ProjectReference sourceProjectReference;

	public ForkProjectDialog(ProjectsManager projectManager, ProjectReference sourceProjectReference, SaveCancelListener<ProjectReference> saveCancelListener) {
		super("Copy Project",projectManager, saveCancelListener);
		this.sourceProjectReference = sourceProjectReference;
		name.setPlaceholder("Please enter the name of the newly copied project");
		description.setPlaceholder("Optionally enter a description for the newly copied project");
	}

	
	@Override
	protected ProjectReference getResult() {
		try {
			return projectManager.forkProject(sourceProjectReference, name.getValue(), description.getValue());
			
		} catch (Exception e) {
			errorLogger.showAndLogError(e.getMessage(),e);
			return null;
		}
	}
}
