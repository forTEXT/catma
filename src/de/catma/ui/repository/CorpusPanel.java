package de.catma.ui.repository;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.AbstractProperty;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.indexer.IndexedRepository;
import de.catma.ui.analyzer.AnalyzerProvider;

public class CorpusPanel extends VerticalLayout {
	private static class CorpusProperty extends AbstractProperty {
		private Corpus corpus;
		
		public CorpusProperty(Corpus corpus) {
			this.corpus = corpus;
		}

		public Class<?> getType() {
			return Corpus.class;
		}
		
		public Object getValue() {
			return corpus;
		}
		
		public void setValue(Object newValue) throws ReadOnlyException,
				ConversionException {
			throw new ReadOnlyException();
		}
	}
	
	private static class CorpusValueChangeEvent implements Property.ValueChangeEvent {
		private CorpusProperty corpusProperty;
		
		public CorpusValueChangeEvent(Corpus corpus) {
			this.corpusProperty = new CorpusProperty(corpus);
		}

		public Property getProperty() {
			return corpusProperty;
		}
	}
	
	private String allDocuments = "All documents";
	private Button btCreateCorpus;
	private MenuItem miMoreCorpusActions;
	private MenuItem miRemoveCorpus;
	private Tree corporaTree;

	private Repository repository;
	
	public CorpusPanel(Repository repository, ValueChangeListener valueChangeListener) {
		this.repository = repository;
		
		initComponents();
		initActions(valueChangeListener);
	}

	private void initActions(final ValueChangeListener valueChangeListener) {
		corporaTree.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				Object value = event.getProperty().getValue();
				boolean corpusRemoveButtonEnabled = false;
				if (value != null) {
					if (!value.equals(allDocuments)) {
						corpusRemoveButtonEnabled = true;
						valueChangeListener.valueChange(
								new CorpusValueChangeEvent((Corpus)value));
					}
					else {
						valueChangeListener.valueChange(
								new CorpusValueChangeEvent(null));
					}
				}
				miRemoveCorpus.setEnabled(corpusRemoveButtonEnabled);
			}
		});
		
		miMoreCorpusActions.addItem("Analyze Corpus", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				Corpus selectedCorpus = null;
				
				
				Object selectedValue = corporaTree.getValue();
				if (!selectedValue.equals(allDocuments)) {
					selectedCorpus = (Corpus)selectedValue;
				}
				((AnalyzerProvider)getApplication()).analyze(
						selectedCorpus, (IndexedRepository)repository);
			}
		});
		
		miRemoveCorpus = miMoreCorpusActions.addItem("Remove Corpus", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				// TODO Auto-generated method stub
				
			}
		});
		
		miRemoveCorpus.setEnabled(false);
	}

	private void initComponents() {
		setSpacing(true);
		setMargin(false, true, true, false);
		
		setSizeFull();
		Component corporaPanel = createCorporaPanel();
		addComponent(corporaPanel);
		setExpandRatio(corporaPanel, 1.0f);
		addComponent(createCorporaButtonPanel());	
	}
	
	private Component createCorporaButtonPanel() {
		
		Panel corporaButtonsPanel = new Panel(new HorizontalLayout());
		corporaButtonsPanel.setStyleName(Reindeer.PANEL_LIGHT);
		((HorizontalLayout)corporaButtonsPanel.getContent()).setSpacing(true);
		
		btCreateCorpus = new Button("Create Corpus");
		
		corporaButtonsPanel.addComponent(btCreateCorpus);
		MenuBar menuMoreCorpusActions = new MenuBar();
		miMoreCorpusActions = 
				menuMoreCorpusActions.addItem("More actions...", null);
		miMoreCorpusActions.setEnabled(
				repository instanceof IndexedRepository);
		corporaButtonsPanel.addComponent(menuMoreCorpusActions);
		
		return corporaButtonsPanel;
	}

	private Component createCorporaPanel() {
		Panel corporaPanel = new Panel();
		corporaPanel.getContent().setSizeUndefined();
		corporaPanel.setSizeFull();
		
		corporaTree = new Tree();
		corporaTree.addStyleName("repo-tree");
		corporaTree.setCaption("Corpora");
		corporaTree.addItem(allDocuments);
		corporaTree.setChildrenAllowed(allDocuments, false);
		corporaTree.setImmediate(true);
		
		for (Corpus c : repository.getCorpora()) {
			corporaTree.addItem(c);
			corporaTree.setChildrenAllowed(c, false);
		}
		corporaTree.setValue(allDocuments);

		corporaPanel.addComponent(corporaTree);
		
		return corporaPanel;
	}

	public void close() {
		// TODO Auto-generated method stub
		
	}


}
