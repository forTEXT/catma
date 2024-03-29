package de.catma.ui.module.dashboard;

import com.vaadin.ui.ComponentContainer;

import de.catma.project.ProjectsManager;
import de.catma.project.ProjectReference;
import de.catma.ui.dialog.SaveCancelListener;

public class EditProjectDialog extends AbstractProjectDialog {

	private final ProjectReference projectReference;
	
	public EditProjectDialog(ProjectReference projectReference, ProjectsManager projectManager, SaveCancelListener<ProjectReference> saveCancelListener) {
		super("Edit Project",projectManager, saveCancelListener);
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
		projectReference.setName(name.getValue());
		projectReference.setDescription(description.getValue());
		return projectReference; 
	}

}
