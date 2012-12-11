package de.catma.ui.analyzer.querybuilder;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.data.util.NonEmptySequenceValidator;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;

public class SimilPanel extends AbstractSearchPanel implements DynamicWizardStep {

	
	private ResultPanel resultPanel;
	private TextField inputField;
	private Slider gradeSlider;

	public SimilPanel(
			ToggleButtonStateListener toggleButtonStateListener,
			QueryTree queryTree,
			QueryOptions queryOptions,
			TagsetDefinitionDictionary tagsetDefinitionDictionary) {
		super(toggleButtonStateListener, 
				queryTree, queryOptions, tagsetDefinitionDictionary);
		initComponents();
		initActions();
	}
	
	private void initActions() {		
		resultPanel.addBtShowInPreviewListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				showInPreview();
			}
		});
	}

	private void showInPreview() {
		if (inputField.isValid()){
			StringBuilder builder = new StringBuilder("simil=\"");
			builder.append(inputField.getValue());
			builder.append("\" ");
			builder.append(((Double)gradeSlider.getValue()).intValue());
			builder.append("%");
			if (curQuery != null) {
				queryTree.removeLast();
			}
			curQuery = builder.toString();
			resultPanel.setQuery(curQuery);
			
			queryTree.add(curQuery);
			onFinish = !isComplexQuery();
			onAdvance = true;
			toggleButtonStateListener.stepChanged(this);
		}
		else {
			onFinish = false;
			onAdvance = false;
		}
	}

	private void initComponents() {
		VerticalSplitPanel splitPanel = new VerticalSplitPanel();
		
		Component searchPanel = createSearchPanel();
		splitPanel.addComponent(searchPanel);
		resultPanel = new ResultPanel(queryOptions);
		splitPanel.addComponent(resultPanel);
		addComponent(splitPanel);
		
		super.initSearchPanelComponents(splitPanel);
	}

	private Component createSearchPanel() {
		HorizontalLayout searchPanel = new HorizontalLayout();
		searchPanel.setWidth("100%");
		searchPanel.setSpacing(true);
		
		inputField = new TextField();
		inputField.setWidth("100%");
		searchPanel.addComponent(inputField);
		searchPanel.setExpandRatio(inputField, 0.7f);
		inputField.setImmediate(true);
		inputField.setRequired(true);
		inputField.setInvalidAllowed(false);
		inputField.addValidator(new NonEmptySequenceValidator("This value may not be empty!"));
		
		gradeSlider = new Slider("Grade of similarity", 0, 100);
		gradeSlider.setResolution(0);
		gradeSlider.setSizeFull();
		try {
			gradeSlider.setValue(80.0);
		} catch (ValueOutOfBoundsException toBeIgnored) {}
		
		searchPanel.addComponent(gradeSlider);
		searchPanel.setExpandRatio(gradeSlider, 0.3f);
		
		
		return searchPanel;
	}

	@Override
	public String getCaption() {
		return "The word is similar to";
	}
	
	@Override
	public String toString() {
		return "by grade of similarity";
	}

}
