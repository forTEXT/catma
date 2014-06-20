/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.repository;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.ClassResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.CatmaApplication;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.ui.admin.AdminWindow;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.user.Role;


public class RepositoryView extends VerticalLayout implements ClosableTab {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Repository repository;
	private PropertyChangeListener exceptionOccurredListener;
	private SourceDocumentPanel sourceDocumentPanel;
	private CorpusPanel corpusPanel;
	private TagLibraryPanel tagLibraryPanel;
	private boolean init = false;
	private Button btReload;
	private Button btAdmin;
	
	public RepositoryView(Repository repository) {
		this.repository = repository;

		exceptionOccurredListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (UI.getCurrent() !=null) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
						"Repository Error!", (Throwable)evt.getNewValue());
				}
				else {
					logger.log(
						Level.SEVERE, "repository error", 
						(Throwable)evt.getNewValue());
				}
			}
		};
		
		
	}

	@Override
	public void attach() {
		super.attach();
		if (!init) {
			initComponents();
			initActions();
			this.repository.addPropertyChangeListener(
					Repository.RepositoryChangeEvent.exceptionOccurred, 
					exceptionOccurredListener);
			init = true;
		}
		
	}

	private void initActions() {
		btReload.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				try {
					Corpus corpus = corpusPanel.getSelectedCorpus();
					corpusPanel.setSelectedCorpus(null);
					repository.reload();
					corpusPanel.setSelectedCorpus(corpus);
					
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							"Error reloading repository!", e);
				}
			}
		});
		
		btAdmin.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				AdminWindow adminWindow = new AdminWindow();
				
				UI.getCurrent().addWindow(adminWindow);
			}
		});
		
	}

	private void initComponents() {
		setSizeFull();
		this.setMargin(new MarginInfo(false, true, true, true));
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
		HorizontalLayout labelLayout = new HorizontalLayout();
		labelLayout.setWidth("100%");
		labelLayout.setSpacing(true);
		
		Label documentsLabel = new Label("Document Manager");
		documentsLabel.addStyleName("bold-label");
		
		labelLayout.addComponent(documentsLabel);
		labelLayout.setExpandRatio(documentsLabel, 1.0f);
		btAdmin = new Button("Admin");
		btAdmin.addStyleName("icon-button"); // for top-margin
		btAdmin.setVisible(repository.getUser().getRole().equals(Role.ADMIN));
		
		labelLayout.addComponent(btAdmin);
		labelLayout.setComponentAlignment(btAdmin, Alignment.MIDDLE_RIGHT);
		
		btReload = new Button(""); 
		btReload.setIcon(new ClassResource("ui/resources/icon-reload.gif"));
		btReload.addStyleName("icon-button");
		labelLayout.addComponent(btReload);
		labelLayout.setComponentAlignment(btReload, Alignment.MIDDLE_RIGHT);
		
		Label helpLabel = new Label();
		helpLabel.setIcon(new ClassResource("ui/resources/icon-help.gif"));
		helpLabel.setWidth("20px");
		helpLabel.setDescription(
				"<h3>Hints</h3>" +
				"<h4>First steps</h4>" +
				"<h5>Adding a Source Document</h5>" +
				"You can add a Source Document by clicking the \"Add Source Document\"-button. " +
				"A Source Document can be a web resource pointed to by the URL or you can upload a document from your computer. " +
				"<h5>Tagging a Source Document</h5>" +
				"When you add your first Source Document, CATMA generates a set of example items to get you going: " +
				"<ul><li>A User Markup Collection to hold your markup</li><li>A Tag Library with an example Tagset that contains an example Tag</li></ul> "+
				"To start tagging a Source Document, just select the example User Markup Collection from the tree and click the \"Open User Markup Collection\"-button. " +
				"Then follow the instructions given to you by the Tagger component." +
				"<h5>Analyze a Source Document</h5>" +
				"To analyze a Source Document, just select that document from the tree and click \"Analyze Source Document\" in the \"More Actions\"-menu." +
				"Then follow the instructions given to you by the Analyzer component.");

		labelLayout.addComponent(helpLabel);
		labelLayout.setComponentAlignment(helpLabel, Alignment.MIDDLE_RIGHT);
		
		return labelLayout;
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
	
	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }

}


