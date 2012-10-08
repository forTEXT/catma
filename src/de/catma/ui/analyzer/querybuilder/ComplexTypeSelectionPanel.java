package de.catma.ui.analyzer.querybuilder;

import java.util.Arrays;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;

import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.DynamicWizardStep;

public class ComplexTypeSelectionPanel extends VerticalLayout implements
		DynamicWizardStep {
	
	private static enum ComplexTypeOption {
		UNION(","),
		EXCLUSION("-"),
		REFINMENT("where"),
		;
		String queryElement;
		String displayString;
		
		private ComplexTypeOption(String queryElement) {
			this.queryElement = queryElement;
		}
		
		public void setDisplayString(String displayString) {
			this.displayString = displayString;
		}
		
		public String getQueryElement() {
			return queryElement;
		}
		
		@Override
		public String toString() {
			return (displayString != null)?displayString:super.toString();
		}
	}
	
	private QueryTree queryTree;
	private OptionGroup complexTypeSelect;
	private boolean typeAdded = false;

	public ComplexTypeSelectionPanel(QueryTree queryTree) {
		this.queryTree = queryTree;
		initComponents();
		initActions();
		complexTypeSelect.setValue(ComplexTypeOption.UNION);
	}

	private void initActions() {
		complexTypeSelect.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				if (typeAdded) {
					queryTree.removeLast();
				}

				queryTree.add(
					((ComplexTypeOption)complexTypeSelect.getValue()).getQueryElement());

				typeAdded = true;
			}
		});
	}

	private void initComponents() {
		
		setSpacing(true);
		setWidth("100%");
		ComplexTypeOption.UNION.setDisplayString(
				"add more results");
		ComplexTypeOption.EXCLUSION.setDisplayString(
				"exclude hits from previous results");
		ComplexTypeOption.REFINMENT.setDisplayString(
				"refine previous results");
		
		complexTypeSelect = 
				new OptionGroup("", Arrays.asList(ComplexTypeOption.values()));
		
		complexTypeSelect.setImmediate(true);
		
		addComponent(complexTypeSelect);
		setComponentAlignment(complexTypeSelect, Alignment.MIDDLE_CENTER);

	}

	public Component getContent() {
		return this;
	}
	
	@Override
	public String getCaption() {
		return "What do you want to do with the next query?";
	}

	public boolean onAdvance() {
		return true;
	}

	public boolean onBack() {
		return true;
	}

	public void stepActivated() {/* noop */}

	public boolean onFinish() {
		return false;
	}

	public boolean onFinishOnly() {
		return false;
	}

	public void stepAdded() {/* noop */}
	
	public void stepDeactivated(boolean forward) {
		if (!forward && typeAdded) {
			queryTree.removeLast();
		}
	}

}
