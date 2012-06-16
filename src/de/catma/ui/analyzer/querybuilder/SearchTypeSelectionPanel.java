package de.catma.ui.analyzer.querybuilder;

import java.util.Arrays;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;

import de.catma.ui.dialog.wizard.DynamicWizardStep;

public class SearchTypeSelectionPanel 
	extends VerticalLayout implements DynamicWizardStep {
	
	public static enum SearchType {
		PHRASE("by word or phrase"),
		SIMIL("by grade of similarity"),
		TAG("by Tag"),
		COLLOC("by collocation"),
		FREQ("by frequency"),
		;
		private String displayString;

		private SearchType(String displayString) {
			this.displayString = displayString;
		}
		
		@Override
		public String toString() {
			return displayString;
		}
	}

	private OptionGroup searchTypeSelect;
	
	public SearchTypeSelectionPanel() {
		initComponents();
	}

	private void initComponents() {
		setSpacing(true);
		setWidth("100%");
		
		searchTypeSelect = 
			new OptionGroup(
				"", 
				Arrays.asList(SearchType.values()));
		searchTypeSelect.setValue(SearchType.PHRASE);
		
		addComponent(searchTypeSelect);
		setComponentAlignment(searchTypeSelect, Alignment.MIDDLE_CENTER);
	}

	
	@Override
	public String getCaption() {
		return "How do you want to search?";
	}
	
	public Component getContent() {
		return this;
	}
	
	public boolean onAdvance() {
		return true;
	}
	
	public boolean onBack() {
		return false;
	}
	
	public boolean onFinish() {
		return false;
	}
	
	public boolean onFinishOnly() {
		return false;
	}
	
	public void stepActivated() { /* not used */ }
	
	public void stepDeactivated() { /* not used */ }
}
