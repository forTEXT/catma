package de.catma.ui.modules.dashboard;

import com.vaadin.ui.ComponentContainer;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.dialog.SaveCancelListener;

public class EditProjectDialog extends AbstractProjectDialog {

	private final ProjectReference projectReference;
	
	public EditProjectDialog(ProjectReference projectReference, ProjectManager projectManager, SaveCancelListener<ProjectReference> saveCancelListener) {
		super("Updates a Project",projectManager, saveCancelListener);
		this.projectReference = projectReference;
	}
	
	@Override
	protected void addContent(ComponentContainer content) {
		super.addContent(content);
		name.setValue(projectReference.getName());
		description.setValue(projectReference.getDescription());
	}

	@Override
	protected ProjectReference getResult() {
		try {
			projectReference.setName(name.getValue());
			projectReference.setDescription(description.getValue());
			return projectReference; // TODO: currently just a fake. Not saved!
									//	TODO: marco implement		return projectManager.updateProject(projectReference);
		} catch (Exception e) {
			errorLogger.showAndLogError(e.getMessage(),e);
			return null;
		}
	}

}
