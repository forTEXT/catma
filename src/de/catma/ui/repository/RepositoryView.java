package de.catma.ui.repository;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.ui.tabbedview.ClosableTab;


public class RepositoryView extends VerticalLayout implements ClosableTab {
	
	// TODO: hier gehts weiter
	/**
	 * handle open/close of repo correctly in AnalyzerView
	 * error handling
	 * c3po conn pool?
	 */
	
	private Repository repository;
	private PropertyChangeListener exceptionOccurredListener;
	private SourceDocumentPanel sourceDocumentPanel;
	private CorpusPanel corpusPanel;
	private TagLibraryPanel tagLibraryPanel;
	
	public RepositoryView(Repository repository) {
		this.repository = repository;
		initComponents();

		exceptionOccurredListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				//TODO: handle
				((Throwable)evt.getNewValue()).printStackTrace();
			}
		};
		
		this.repository.addPropertyChangeListener(
			Repository.RepositoryChangeEvent.exceptionOccurred, 
			exceptionOccurredListener);
		
	}


	private void initComponents() {
		setSizeFull();
		this.setMargin(false, true, true, true);
		this.setSpacing(true);

		Component documentsLabel = createDocumentsLabel();
		addComponent(documentsLabel);
		VerticalSplitPanel splitPanel = new VerticalSplitPanel();
		splitPanel.setSplitPosition(65);
		
		Component documentsManagerPanel = createDocumentsManagerPanel();
		splitPanel.addComponent(documentsManagerPanel);
		
		tagLibraryPanel = new TagLibraryPanel(
				repository.getTagManager(), repository);
		splitPanel.addComponent(tagLibraryPanel);
		
		addComponent(splitPanel);
		setExpandRatio(splitPanel, 1f);
	}

	

	private Component createDocumentsManagerPanel() {
		
		HorizontalSplitPanel documentsManagerPanel = new HorizontalSplitPanel();
		documentsManagerPanel.setSplitPosition(25);
		documentsManagerPanel.setSizeFull();
		
		sourceDocumentPanel = new SourceDocumentPanel(repository);
		
		corpusPanel = new CorpusPanel(repository, new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				Object value = event.getProperty().getValue();
				sourceDocumentPanel.setSourceDocumentsFilter((Corpus)value);
			}		
		});

		documentsManagerPanel.addComponent(corpusPanel);
		documentsManagerPanel.addComponent(sourceDocumentPanel);
		
		return documentsManagerPanel;
	}

	private Component createDocumentsLabel() {
		Label documentsLabel = new Label("Document Manager");
		documentsLabel.addStyleName("repo-title-label");
		return documentsLabel;
	}

	public Repository getRepository() {
		return repository;
	}
	
	public void close() {
		this.repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.exceptionOccurred, 
				exceptionOccurredListener);
		
		this.corpusPanel.close();
		this.sourceDocumentPanel.close();
		this.tagLibraryPanel.close();
		
		// repository is closed by the RepositoryManager from RepositoryManagerView
	}
}


