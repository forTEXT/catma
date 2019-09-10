package de.catma.ui.module.dashboard;

import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.module.main.ErrorHandler;

/**
 * Dialog that creates a Project
 * 
 * @author db
 *
 */
public abstract class AbstractProjectDialog extends AbstractOkCancelDialog<ProjectReference>{

	protected final TextField name = new TextField("Name");
	protected final TextArea description = new TextArea("Decription");
	protected final ProjectManager projectManager;
	protected Binder<ProjectData> projectBinder = new Binder<>();
	protected ErrorHandler errorLogger;
	
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
	
	public AbstractProjectDialog(String title, ProjectManager projectManager, SaveCancelListener<ProjectReference> saveCancelListener) {
		super(title, saveCancelListener);
	
	    this.errorLogger = (ErrorHandler) UI.getCurrent();
		this.projectManager = projectManager;
		projectBinder.forField(name)
	    .withValidator(new StringLengthValidator(
	        "Name must be between 2 and 50 characters long",
	        2, 20))
	    .bind(ProjectData::getName, ProjectData::setName);
		projectBinder.forField(description)
	    .withValidator(new StringLengthValidator(
	        "Description must not be empty",
	        1, 1000))
	    .bind(ProjectData::getDescription, ProjectData::setDescription);
		name.setWidth("100%");
		description.setWidth("100%");
		description.setHeight("100%");
	}


	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(name);
		content.addComponent(description);
		((AbstractOrderedLayout)content).setExpandRatio(description, 1f);
	}

	@Override
	protected void handleOkPressed() {
		ProjectData projectData = new ProjectData();
		try {
			projectBinder.writeBean(projectData);
			super.handleOkPressed();
		} catch (ValidationException e) {
			Notification.show(
					Joiner
					.on("\n")
					.join(
							e.getValidationErrors().stream()
							.map(msg -> msg.getErrorMessage())
							.collect(Collectors.toList())),Type.ERROR_MESSAGE);
		}
	}
}
