package de.catma.ui.analyzer.querybuilder;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.data.util.IntegerValueValidator;
import de.catma.ui.data.util.NonEmptySequenceValidator;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;

public class CollocPanel extends AbstractSearchPanel {

	private TextField wordInput;
	private TextField collocInput;
	private TextField spanSizeInput;
	private ResultPanel resultPanel;

	public CollocPanel(ToggleButtonStateListener toggleButtonStateListener,
			QueryTree queryTree, QueryOptions queryOptions) {
		super(toggleButtonStateListener, queryTree, queryOptions);
		initComponents();
		initActions();
	}

	private void initActions() {

		this.resultPanel.addBtShowInPreviewListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				showInPreview();
			}
		});
		
	}

	private void showInPreview() {
		if (wordInput.isValid() && collocInput.isValid() && spanSizeInput.isValid()) {
			StringBuilder builder = new StringBuilder("\"");
			builder.append(wordInput.getValue());
			builder.append("\" & \"");
			builder.append(collocInput.getValue());
			builder.append("\" ");
			builder.append(spanSizeInput.getValue());
			
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

		VerticalLayout searchPanel = new VerticalLayout();
		searchPanel.setSpacing(true);
		
		wordInput = new TextField();
		wordInput.addValidator(new NonEmptySequenceValidator("This value cannot be empty!"));
		wordInput.setRequired(true);
		wordInput.setInvalidAllowed(false);
		searchPanel.addComponent(wordInput);
		
		collocInput = new TextField("that appear near");
		collocInput.addValidator(new NonEmptySequenceValidator("This value cannot be empty!"));
		collocInput.setRequired(true);
		collocInput.setInvalidAllowed(false);
		
		searchPanel.addComponent(collocInput);
		
		spanSizeInput = new TextField("within a span of", "5");
		spanSizeInput.addValidator(new IntegerValueValidator(false, false));
		spanSizeInput.setRequired(true);
		spanSizeInput.setInvalidAllowed(false);
		
		searchPanel.addComponent(spanSizeInput);
		return searchPanel;
	}

	@Override
	public String getCaption() {
		return "Search for all occurrences of";
	}
	
	@Override
	public String toString() {
		return "by collocation";
	}
}
