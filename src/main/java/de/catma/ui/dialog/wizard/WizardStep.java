package de.catma.ui.dialog.wizard;

import com.vaadin.ui.Component;

public interface WizardStep extends Component {
	public ProgressStep getProgressStep();
	public WizardStep getNextStep();
	public boolean isValid();
	public void setStepChangeListener(StepChangeListener stepChangeListener);
	public void setFinished();
	public void setCurrent();
	
	public default String getProgressStepDescription() {
		if (getProgressStep() != null) {
			return getProgressStep().getStepDescription();
		}
		return null;
	}

}
