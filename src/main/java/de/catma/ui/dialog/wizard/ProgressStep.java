package de.catma.ui.dialog.wizard;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

public class ProgressStep extends HorizontalLayout {

	private Label stepLabel;
	private Label stepDescription;
	private String description;
	private int number;
	
	public ProgressStep(int number, String description) {
		this.description = description;
		this.number = number;
		initComponents();
	}

	private void initComponents() {
		setMargin(false);
		stepLabel = new Label(String.valueOf(number));
		stepLabel.addStyleName("progress-step-number-faded-out");
		addComponent(stepLabel);
		stepDescription = new Label(description);
		stepDescription.addStyleName("progress-step-description");
		addComponent(stepDescription);
	}
	
	public void setCompleted(String completionText) {
		stepDescription.setValue(completionText);
		stepLabel.setValue(VaadinIcons.CHECK.getHtml());
		stepLabel.setContentMode(ContentMode.HTML);
	}
	
	public void setCurrent() {
		stepLabel.removeStyleName("progress-step-number-faded-out");
		stepLabel.addStyleName("progress-step-number-current");
	}

	public void resetCurrent() {
		stepDescription.setValue(description);
		stepLabel.setValue(String.valueOf(number));
	}
	
	public void setFinished() {
		stepLabel.removeStyleName("progress-step-number-current");
		stepLabel.addStyleName("progress-step-number-faded-out");
	}

	public void setStepDescription(String description) {
		this.description = description;
		stepDescription.setValue(description);
	}
	
	public String getStepDescription() {
		return description;
	}
	
	public int getNumber() {
		return number;
	}
}
