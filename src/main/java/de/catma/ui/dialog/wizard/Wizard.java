package de.catma.ui.dialog.wizard;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.ui.dialog.SaveCancelListener;

public class Wizard extends Window {
	
	protected ProgressPanel progressPanel;
	private VerticalLayout content;
	
	private Button btCancel;
	private Button btBack;
	private Button btCont;
	private Button btFinish;
	
	protected List<WizardStep> steps;
	private WizardStep currentStep;
	
	protected WizardContext context;
	
	private int maxVisibleSteps = 3;

	public Wizard(
			String dialogCaption,
			WizardStepFactory firstStepFactory, 
			WizardContext context, SaveCancelListener<WizardContext> saveCancelListener) {
		super(dialogCaption);
		this.steps = new ArrayList<WizardStep>();
		this.context = context;
		initComponents(firstStepFactory);
		initActions(saveCancelListener);
	}
	
	private void initActions(SaveCancelListener<WizardContext> saveCancelListener) {
		btCont.addClickListener(clickEvent -> handleContinueRequest(saveCancelListener));
		
		btCancel.addClickListener(clickEvent -> {
			UI.getCurrent().removeWindow(this);
			saveCancelListener.cancelPressed();
		});
		
		btFinish.addClickListener(clickEvent -> setNextStep(null, saveCancelListener));
		btBack.addClickListener(clickEvent -> handleBackRequest());
	}
	
	protected void handleBackRequest() {
		if (this.currentStep.isDynamic()) {
			progressPanel.removeLastProgressStep();
		}
		else {
			this.currentStep.getProgressStep().setUnfinished();
		}

		if (steps.size() > 1) {
			steps.remove(steps.size()-1);
		}
		
		if (steps.size() < 2) {
			btBack.setEnabled(false);
		}
		this.currentStep.exit(true);
		
		this.currentStep.setStepChangeListener(null);
		WizardStep nextStep = steps.get(steps.size()-1);
		content.replaceComponent(this.currentStep,nextStep);
		this.currentStep = nextStep;
		
		btCont.setEnabled(false);
		btFinish.setEnabled(false);
		
		this.currentStep.getProgressStep().setCurrent();

		this.currentStep.setStepChangeListener(changedStep -> handleStepChange(changedStep));
		this.currentStep.enter(true);
		
		btCont.setEnabled(this.currentStep.isValid() && this.currentStep.canNext());
		btFinish.setEnabled(this.currentStep.isValid() && this.currentStep.canFinish());
		
		if (this.currentStep.getProgressStep().getParent() == null) {
			progressPanel.truncateAfter(this.currentStep.getProgressStep());
			progressPanel.restoreLastProgressSteps(maxVisibleSteps);
		}
	}

	private void handleContinueRequest(SaveCancelListener<WizardContext> saveCancelListener) {
		setNextStep(currentStep.getNextStep(), saveCancelListener);
		if (progressPanel.getVisibleStepCount() > maxVisibleSteps) {
			progressPanel.clearBefore(currentStep.getProgressStep());
		}
	}
	
	private void setNextStep(WizardStep step, SaveCancelListener<WizardContext> saveCancelListener) {
		steps.add(step);
		
		this.currentStep.setStepChangeListener(null);
		this.currentStep.getProgressStep().setFinished();
		this.currentStep.exit(false);
		
		if (step != null) {
			btCont.setEnabled(false);
			btFinish.setEnabled(false);

			content.replaceComponent(this.currentStep, step);
			
			this.currentStep = step;
			this.currentStep.setStepChangeListener(changedStep -> handleStepChange(changedStep));
			this.currentStep.getProgressStep().setCurrent();
			this.currentStep.enter(false);

			btBack.setEnabled(true);
			
			if (this.currentStep.isSkipped()) {
				handleContinueRequest(saveCancelListener);
			}
		}
		else {
			UI.getCurrent().removeWindow(this);
			saveCancelListener.savePressed(context);
		}
		
	}

	private void handleStepChange(WizardStep changedStep) {
		btCont.setEnabled(changedStep.isValid() && changedStep.canNext());
		btFinish.setEnabled(changedStep.isValid() && changedStep.canFinish());
	}

	private void initComponents(WizardStepFactory firstStepFactory) {
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

		HorizontalLayout buttonPanel = new HorizontalLayout();
		
		btBack = new Button("Back");
		btBack.setEnabled(false);
		
		btCont = new Button("Continue");
		btCont.setEnabled(false);
		
		btFinish = new Button("Finish");
		btFinish.setEnabled(false);
		
		btCancel = new Button("Cancel");
		buttonPanel.addComponents(btCancel, btBack, btCont, btFinish);
		buttonPanel.setWidth("100%");
		buttonPanel.setExpandRatio(btCancel, 1f);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		
		
		this.currentStep = firstStepFactory.buildWizardStep(progressPanel);
		this.currentStep.getProgressStep().setCurrent();
		this.currentStep.setStepChangeListener(changedStep -> handleStepChange(changedStep));
		
		this.steps.add(this.currentStep);
		
		content.addComponent(currentStep);
		content.setExpandRatio(currentStep, 1f);

		content.addComponent(buttonPanel);
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}

}
