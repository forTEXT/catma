package de.catma.ui.dialog.wizard;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.ui.dialog.SaveCancelListener;

public class Wizard extends Window {
	
	protected ProgressPanel progressPanel;
	private Button btCont;
	private Button btCancel;
	private WizardStep currentStep;
	private VerticalLayout content;
	private WizardContext context;

	public Wizard(
			String dialogCaption,
			WizardStepFactory firstStepFactory, 
			WizardContext context, SaveCancelListener<WizardContext> saveCancelListener) {
		super(dialogCaption);
		this.context = context;
		initComponents(firstStepFactory);
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
		
		btCont = new Button("Continue");
		btCont.setEnabled(false);
		
		btCancel = new Button("Cancel");
		buttonPanel.addComponents(btCancel, btCont);
		buttonPanel.setWidth("100%");
		buttonPanel.setExpandRatio(btCancel, 1f);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		
		
		this.currentStep = firstStepFactory.buildWizardStep(progressPanel);
		this.currentStep.setCurrent();
		this.currentStep.setStepChangeListener(changedStep -> handleStepChange(changedStep));
		content.addComponent(currentStep);
		content.setExpandRatio(currentStep, 1f);

		content.addComponent(buttonPanel);
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}

}
