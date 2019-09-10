package de.catma.ui.module.analyze.visualization.kwic.annotation;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.project.Project;
import de.catma.ui.dialog.SaveCancelListener;

public class AnnotationWizard extends Window {
	
	private Project project;
	private ProgressPanel progressPanel;
	private Button btCont;
	private Button btCancel;
	private WizardStep currentStep;
	private VerticalLayout content;
	private WizardContext context;

	public AnnotationWizard(
			EventBus eventBus, Project project, 
			WizardContext context, SaveCancelListener<WizardContext> saveCancelListener) {
		super("Annotate selected results");
		this.project = project;
		this.context = context;
		initComponents(eventBus);
		initActions(saveCancelListener);
	}
	
	private void initActions(SaveCancelListener<WizardContext> saveCancelListener) {
		btCont.addClickListener(clickEvent -> handleContinue(saveCancelListener));
		btCancel.addClickListener(clickEvent -> {
			UI.getCurrent().removeWindow(this);
			saveCancelListener.cancelPressed();
		});
	}
	
	private void handleContinue(SaveCancelListener<WizardContext> saveCancelListener) {
		setNextStep(currentStep.getNextStep(), saveCancelListener);
	}

	private void setNextStep(WizardStep step, SaveCancelListener<WizardContext> saveCancelListener) {
		
		this.currentStep.setStepChangeListener(null);
		this.currentStep.setFinished();
		
		if (step != null) {
			btCont.setEnabled(false);
			content.replaceComponent(this.currentStep, step);
			this.currentStep = step;
			this.currentStep.setStepChangeListener(changedStep -> handleStepChange(changedStep));
			this.currentStep.setCurrent();
		}
		else {
			UI.getCurrent().removeWindow(this);
			saveCancelListener.savePressed(context);
		}
		
	}

	private void handleStepChange(WizardStep changedStep) {
		btCont.setEnabled(changedStep.isValid());
	}

	private void initComponents(EventBus eventBus) {
		setModal(true);
		setClosable(false);
		center();
		setWidth("70%");
		setHeight("70%");

		content = new VerticalLayout();
		setContent(content);
		
		content.setSizeFull();
		
		progressPanel = new ProgressPanel();
		content.addComponent(progressPanel);
		
		this.currentStep = 
			new TagSelectionStep(
				eventBus,
				project, 
				context,
				(number, description) -> progressPanel.addStep(number, description));
		this.currentStep.setStepChangeListener(changedStep -> handleStepChange(changedStep));

		content.addComponent(currentStep);
		content.setExpandRatio(currentStep, 1f);
		HorizontalLayout buttonPanel = new HorizontalLayout();
		content.addComponent(buttonPanel);
		
		btCont = new Button("Continue");
		btCont.setEnabled(false);
		
		btCancel = new Button("Cancel");
		buttonPanel.addComponents(btCancel, btCont);
		buttonPanel.setWidth("100%");
		buttonPanel.setExpandRatio(btCancel, 1f);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}

}
