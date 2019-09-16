package de.catma.ui.dialog.wizard;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

public class ProgressPanel extends HorizontalLayout {
	
	
	public ProgressPanel() {
	}
	
	public ProgressStep addStep(int number, String text) {
		if (getComponentCount() != 0) {
			addDivider();
		}
		ProgressStep step = new ProgressStep(number, text);
		addComponent(step);
		return step;
	}

	private void addDivider() {
		Label divider= new Label("");
		divider.addStyleName("progress-step-divider");
		addComponent(divider);
		setComponentAlignment(divider, Alignment.MIDDLE_CENTER);
	}
	
}
