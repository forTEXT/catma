package de.catma.ui.modules.dashboard;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.modules.main.ErrorHandler;

/**
 * Dialog that creates a Project
 * 
 * @author db
 *
 */
public class CreateProjectDialog extends AbstractOkCancelDialog<ProjectReference>{

	private final TextField name = new TextField("Name");
	private final TextArea description = new TextArea("Decription");
	private final ProjectManager projectManager;
	private Binder<ProjectData> projectBinder = new Binder<>();
	private ErrorHandler errorLogger;
	
	class ProjectData {
		String name;
		String description;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		
		
	}
	
	public CreateProjectDialog(ProjectManager projectManager, SaveCancelListener<ProjectReference> saveCancelListener) {
		super("", saveCancelListener);
	    this.errorLogger = (ErrorHandler) UI.getCurrent();
		this.projectManager = projectManager;
		projectBinder.forField(name)
	    .withValidator(new StringLengthValidator(
	        "Name must be between 2 and 50 characters long",
	        2, 20))
	    .bind(ProjectData::getName, ProjectData::setName);
		projectBinder.forField(description)
	    .withValidator(new StringLengthValidator(
	        "Name must not be empty",
	        1, 1000))
	    .bind(ProjectData::getDescription, ProjectData::setDescription);
		name.setWidth("100%");
		description.setWidth("100%");
	}


	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(new Label("Creates a new Project"));
		content.addComponent(name);
		content.addComponent(description);
	}

	@Override
	protected void handleOkPressed() {
		ProjectData projectData = new ProjectData();
		try {
			projectBinder.writeBean(projectData);
			super.handleOkPressed();
		} catch (ValidationException e) {
			errorLogger.showAndLogError(e.getMessage(), e);
		}
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
