package de.catma.ui.dialog.wizard;

public interface ProgressStepFactory {
	
	public ProgressStep create(int number, String description);

}
