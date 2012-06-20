package de.catma.ui.analyzer.querybuilder;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Validator;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;

public class PhrasePanel extends VerticalSplitPanel implements DynamicWizardStep {
	
	private Panel wordSequencePanel;
	private List<WordPanel> wordPanels;
	private Button btAddWordPanel;
	private QueryTree queryTree;
	private ResultPanel resultPanel;
	private Button btShowInPreview;
	private TextField maxTotalFrequencyField;
	private boolean onFinish;
	private String curQuery;
	private ToggleButtonStateListener toggleButtonStateListener;

	public PhrasePanel(
			ToggleButtonStateListener toggleButtonStateListener,
			QueryTree queryTree,
			QueryOptions queryOptions) {
		this.toggleButtonStateListener = toggleButtonStateListener;
		this.queryTree = queryTree;
		wordPanels = new ArrayList<WordPanel>();
		onFinish = false;
		initComponents(queryOptions);
		initActions();
	}

	private void initActions() {
		btAddWordPanel.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				WordPanel wordPanel = 
						new WordPanel(true, wordPanels);
				addWordPanel(wordPanel);
			}
		});
		btShowInPreview.addListener(new ClickListener() {
			

			public void buttonClick(ClickEvent event) {
				
				StringBuilder builder = new StringBuilder();
				String conc="";
				for (WordPanel panel : wordPanels) {
					builder.append(conc);
					builder.append(panel.getWildcardWord());
					conc = " ";
				}
				
				if (!builder.toString().trim().isEmpty()) {
					curQuery = "wild=\"" + builder.toString() + "\"";
					resultPanel.setQuery(
						curQuery, Integer.valueOf(
							(String)maxTotalFrequencyField.getValue()));
					onFinish = true;
					toggleButtonStateListener.stepChanged(PhrasePanel.this);
				}
				
			}
		});
	}

	private void initComponents(QueryOptions queryOptions) {
		Component searchPanel = createSearchPanel();
		this.addComponent(searchPanel);
		resultPanel = new ResultPanel(queryOptions);
		this.addComponent(resultPanel);
	}

	private Component createSearchPanel() {
		VerticalLayout searchPanel = new VerticalLayout();
		searchPanel.setSpacing(true);
		
		wordSequencePanel = new Panel(new HorizontalLayout());
		searchPanel.addComponent(wordSequencePanel);
		
		WordPanel firstWordPanel = new WordPanel();
		addWordPanel(firstWordPanel);
		
		HorizontalLayout buttonPanel = new HorizontalLayout(); 
		buttonPanel.setSpacing(true);
		
		btShowInPreview = new Button("Show in preview");
		buttonPanel.addComponent(btShowInPreview);
		Label maxTotalFrequencyLabel = new Label("with a maximum total frequency of");
		buttonPanel.addComponent(maxTotalFrequencyLabel);
		buttonPanel.setComponentAlignment(
				maxTotalFrequencyLabel, Alignment.MIDDLE_CENTER);
		
		maxTotalFrequencyField = new TextField();
		maxTotalFrequencyField.setValue("50");
		maxTotalFrequencyField.addValidator(new Validator() {
			public boolean isValid(Object value) {
				try {
					Integer.valueOf((String)value);
					return true;
				}
				catch (NumberFormatException nfe) {
					return false;
				}
			}
			
			public void validate(Object value) throws InvalidValueException {
				try {
					Integer.valueOf((String)value);
				}
				catch (NumberFormatException nfe) {
					throw new InvalidValueException("Value must be an integer number!");
				}
				
			}
		});
		maxTotalFrequencyField.setInvalidAllowed(false);
		buttonPanel.addComponent(maxTotalFrequencyField);
		
		Label addWordPanelLabel = 
				new Label("If your phrase contains more words you can");
		buttonPanel.addComponent(addWordPanelLabel);
		buttonPanel.setComponentAlignment(
				addWordPanelLabel, Alignment.MIDDLE_CENTER);
		
		btAddWordPanel = new Button("add another word!");
		buttonPanel.addComponent(btAddWordPanel);
		searchPanel.addComponent(buttonPanel);
		
		return searchPanel;
	}


	private void addWordPanel(WordPanel wordPanel) {
		wordSequencePanel.addComponent(wordPanel);
		wordPanels.add(wordPanel);
	}
	
	@Override
	public String getCaption() {
		return "How does your phrase look like?";
	}

	public Component getContent() {
		return this;
	}

	public boolean onAdvance() {
		return onFinish;
	}

	public boolean onBack() {
		return true;
	}

	public void stepActivated() { /*noop*/ }

	public boolean onFinish() {
		return onFinish;
	}

	public boolean onFinishOnly() {
		return onFinish;
	}

	public void stepDeactivated() {
		queryTree.add(curQuery);
	}
	
	@Override
	public void attach() {
		super.attach();
		getParent().setHeight("100%");
	}
	
	
	@Override
	public String toString() {
		return "by word or phrase";
	}
}
