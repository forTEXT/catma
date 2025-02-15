package de.catma.ui.dialog.wizard;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import java.util.ArrayList;
import java.util.List;

public class ProgressPanel extends HorizontalLayout {
	
	private List<ProgressStep> steps;
	
	public ProgressPanel() {
		steps = new ArrayList<ProgressStep>();
	}
	
	public void clear() {
		removeAllComponents();
	}
	
	public void removeLastProgressStep() {
		int componentCount = getComponentCount();
		if (componentCount > 0) {
			removeComponent(getComponent(getComponentCount()-1));
		}
		if (componentCount > 1) { // remove divider
			removeComponent(getComponent(getComponentCount()-1));
		}
		if (!steps.isEmpty()) {
			steps.remove(steps.size()-1);
		}
	}
	
	public void restoreLastProgressSteps(int count) {
		clear();
		int stepOffset = steps.size() - count;
		if (stepOffset < 0) {
			count -= Math.abs(stepOffset);
			stepOffset = steps.size() - count;
		}
		for (int i=0; i<count; i++) {
			ProgressStep step = steps.get(stepOffset+i);
			if (getComponentCount() != 0) {
				addDivider();
			}
			addComponent(step);
		}
	}
	
	public ProgressStep addStep(int number, String text) {
		if (getComponentCount() != 0) {
			addDivider();
		}
		ProgressStep step = new ProgressStep(number, text);
		addComponent(step);
		steps.add(step);
		
		return step;
	}

	private void addDivider() {
		Label divider= new Label("");
		divider.addStyleName("progress-step-divider");
		addComponent(divider);
		setComponentAlignment(divider, Alignment.MIDDLE_CENTER);
	}
	
	public int getVisibleStepCount() {
		if (getComponentCount() > 0) {
			return (getComponentCount()/2)+1;
		}
		else {
			return 0;
		}
	}

	public void truncateAfter(ProgressStep progressStep) {
		steps.removeAll(steps.subList(steps.indexOf(progressStep)+1, steps.size()));
	}

	public void clearBefore(ProgressStep progressStep) {
		
		for (ProgressStep beforeStep : steps) {
			if (beforeStep.equals(progressStep)) {
				break;
			}
			int idx = getComponentIndex(progressStep);
			if (idx > 0) {
				removeComponent(getComponent(idx-1)); //divider
			}
			removeComponent(beforeStep);
		}
		
	}
	
}
