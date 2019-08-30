package de.catma.ui.analyzenew.annotation;

import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.repository.Repository;

public class AnnotationWizard extends VerticalLayout {
	
	private Repository project;

	public AnnotationWizard(Repository project) {
		this.project = project;
		initComponents();
	}
	
	private void initComponents() {
		setSizeFull();
		TagSelectionStep tagSelectionStep = new TagSelectionStep(project);
		
		addComponent(tagSelectionStep);
	}

	public void show() {
		Window window = new Window();
		window.setContent(this);
		window.center();
		window.setWidth("70%");
		window.setHeight("70%");
		UI.getCurrent().addWindow(window);
	}

}
