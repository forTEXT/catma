package de.catma.ui.analyzenew.visualization.kwic.annotation;

import com.vaadin.ui.Component;

public interface WizardStep extends Component {
	public ProgressStep getProgressStep();
	public WizardStep getNextStep();
	public boolean isValid();
	public void setStepChangeListener(StepChangeListener stepChangeListener);
	public void setFinished();
}
