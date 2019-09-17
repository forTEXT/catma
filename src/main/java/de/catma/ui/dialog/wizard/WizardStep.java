package de.catma.ui.dialog.wizard;

import com.vaadin.ui.Component;

public interface WizardStep extends Component {
	
	public ProgressStep getProgressStep();
	public WizardStep getNextStep();
	public boolean isValid();
	public void setStepChangeListener(StepChangeListener stepChangeListener);

	public default String getStepDescription() {
		if (getProgressStep() != null) {
			return getProgressStep().getStepDescription();
		}
		return null;
	}
	
	public default Integer getStepNumber() {
		if (getProgressStep() != null) {
			return getProgressStep().getNumber();
		}
		return null;
	}
	
	public default void enter(boolean back) {};
	public default void exit(boolean back) {};
	public default boolean isDynamic() { return false; } 
	public default boolean canNext() { return true; }
	public default boolean canFinish() { return false; }
	public default boolean isSkipped() { return false; }
}
