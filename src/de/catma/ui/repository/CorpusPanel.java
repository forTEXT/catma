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

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.AbstractProperty;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.TreeTargetDetails;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.Notification;
import com.vaadin.ui.themes.Reindeer;

import de.catma.CatmaApplication;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.UnknownUserException;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.dialog.FormDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleValueDialog;
import de.catma.ui.repository.sharing.SharingOptions;
import de.catma.ui.repository.sharing.SharingOptionsFieldFactory;

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

	private final static String SORTCAP_PROP = "SORTCAP";
	
	private String allDocuments = "All documents";
	private Button btCreateCorpus;
	private MenuItem miMoreCorpusActions;
	private MenuItem miRemoveCorpus;
	private Tree corporaTree;

	private Repository repository;
	private PropertyChangeListener corpusChangedListener;
	private MenuItem miRenameCorpus;

	private HierarchicalContainer corporaContainer;

	private MenuItem miShareCorpus;
	
	public CorpusPanel(
			Repository repository, ValueChangeListener valueChangeListener) {
		this.repository = repository;
		
		initComponents();
		initActions(valueChangeListener);
		initListeners();
	}

	private void initListeners() {
		this.corpusChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == null) { //remove
					removeCorpusFromTree((Corpus)evt.getOldValue());
				}
				else if (evt.getOldValue() == null) { //add
					addCorpusToTree((Corpus)evt.getNewValue());
					corporaContainer.sort(new Object[] {SORTCAP_PROP}, new boolean[] { true });

					getWindow().showNotification("Information",
							"Start adding Source Documents" +
							" and Markup Collections by dragging them " +
							"from the Documents section on a Corpus.",
							Notification.TYPE_TRAY_NOTIFICATION);
				}
				else { //update name
					if (evt.getOldValue() instanceof String) {
						corporaTree.requestRepaint();
					}
					else if (evt.getOldValue() instanceof Corpus) {
						Corpus oldCorpus = (Corpus) evt.getOldValue();
						removeCorpusFromTree(oldCorpus);
						Corpus newCorpus = (Corpus) evt.getNewValue();
						addCorpusToTree(newCorpus);
						corporaTree.setValue(newCorpus);
					}
 				}
			}
		};
		
		repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.corpusChanged,
				corpusChangedListener);
	}

	private void removeCorpusFromTree(Corpus oldValue) {
		if ((corporaTree.getValue() != null) 
				&& (corporaTree.getValue().equals(oldValue))) {
			corporaTree.setValue(this.allDocuments);
		}
		corporaTree.removeItem(oldValue);
	}

	private void addCorpusToTree(Corpus corpus) {
		corporaTree.addItem(corpus);
		corporaTree.getItem(corpus).getItemProperty(SORTCAP_PROP).setValue(
				(corpus.toString()==null)?"":corpus.toString().toLowerCase());

		corporaTree.setChildrenAllowed(corpus, false);
	}

	private void initActions(final ValueChangeListener valueChangeListener) {
		corporaTree.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				Object value = event.getProperty().getValue();
				boolean corpusModificationButtonsEnabled = false;
				if (value != null) {
					if (!value.equals(allDocuments)) {
						corpusModificationButtonsEnabled = true;
						valueChangeListener.valueChange(
								new CorpusValueChangeEvent((Corpus)value));
					}
					else {
						valueChangeListener.valueChange(
								new CorpusValueChangeEvent(null));
					}
				}
				miRemoveCorpus.setEnabled(corpusModificationButtonsEnabled);
				miRenameCorpus.setEnabled(corpusModificationButtonsEnabled);
				miShareCorpus.setEnabled(corpusModificationButtonsEnabled);
			}
		});
		
		corporaTree.setDropHandler(new DropHandler() {
			
			public AcceptCriterion getAcceptCriterion() {
				return AcceptItem.ALL;
			}
			
			public void drop(DragAndDropEvent event) {
				Transferable t = event.getTransferable();
				if (t instanceof DataBoundTransferable) {
					Object sourceItemId = ((DataBoundTransferable) t).getItemId();
					
					TreeTargetDetails dropData = 
							((TreeTargetDetails) event.getTargetDetails());

					Object targetItemId = dropData.getItemIdOver();
					if (targetItemId instanceof Corpus) {
						addItemToCorpus(sourceItemId, (Corpus)targetItemId);
					}
				}
				
			}
		});
		
		miMoreCorpusActions.addItem("Analyze Corpus", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				Corpus selectedCorpus = null;
				
				
				Object selectedValue = corporaTree.getValue();
				if (selectedValue != null) {
					if (!selectedValue.equals(allDocuments)) {
						selectedCorpus = (Corpus)selectedValue;
					}
					else {
						selectedCorpus = new Corpus(allDocuments);
						for (SourceDocument sd : repository.getSourceDocuments()) {
							selectedCorpus.addSourceDocument(sd);
							for (UserMarkupCollectionReference umcRef : sd.getUserMarkupCollectionRefs()) {
								selectedCorpus.addUserMarkupCollectionReference(umcRef);
							}
						}
					}
					
					if (selectedCorpus.getSourceDocuments().isEmpty()) {
						getWindow().showNotification(
							"Information", "The corpus is empty! " +
									"Please add some documents first!",
								Notification.TYPE_TRAY_NOTIFICATION);
					}
					else {
						((AnalyzerProvider)getApplication()).analyze(
								selectedCorpus, (IndexedRepository)repository);
					}
				}
				else {
					getWindow().showNotification("Information", "Please select a corpus first!",
							Notification.TYPE_TRAY_NOTIFICATION);
				}
			}
		});
		
		miRemoveCorpus = miMoreCorpusActions.addItem("Remove Corpus", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				Object selectedValue = corporaTree.getValue();
				if ((selectedValue != null) 
						&& !selectedValue.equals(allDocuments)) {
					handleRemoveCorpusRequest((Corpus)selectedValue);
				}
				
			}
		});
		
		miRemoveCorpus.setEnabled(false);
		
		btCreateCorpus.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				handleCorpusCreationRequest();
			}
		});
		
		miRenameCorpus = miMoreCorpusActions.addItem("Rename Corpus", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				Object selectedValue = corporaTree.getValue();
				if ((selectedValue != null) 
						&& !selectedValue.equals(allDocuments)) {
					handleRenameCorpusRequest((Corpus)selectedValue);
				}
			}

		});
		
		miRenameCorpus.setEnabled(false);
		
		miShareCorpus = miMoreCorpusActions.addItem("Share Corpus", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				Object selectedValue = corporaTree.getValue();
				if ((selectedValue != null) 
						&& !selectedValue.equals(allDocuments)) {
					handleShareCorpusRequest((Corpus)selectedValue);
				}
			}

		});
		miShareCorpus.setEnabled(false);
	}
	
	protected void handleShareCorpusRequest(final Corpus corpus) {
		SharingOptions sharingOptions = new SharingOptions();
		
		FormDialog<SharingOptions> sharingOptionsDlg = new FormDialog<SharingOptions>(
			"Please enter the person you want to share with", 
			new BeanItem<SharingOptions>(sharingOptions),
			new SharingOptionsFieldFactory(), 
			new SaveCancelListener<SharingOptions>() {
				public void cancelPressed() {}
				public void savePressed(SharingOptions result) {
					try {
						repository.share(
								corpus, 
								result.getUserIdentification(), 
								result.getAccessMode());
					} catch (IOException e) {
						if (e.getCause() instanceof UnknownUserException) {
							getWindow().showNotification(
									"Sharing failed!", e.getCause().getMessage(), 
									Notification.TYPE_WARNING_MESSAGE);
						}
						else {
							((CatmaApplication)getApplication()).showAndLogError(
								"Error sharing this corpus!", e);
						}
					}
				}
			});
		sharingOptionsDlg.setVisibleItemProperties(
				new Object[] {"userIdentification", "accessMode"});
		sharingOptionsDlg.show(getApplication().getMainWindow());
	}

	private void handleRenameCorpusRequest(final Corpus corpus) {
		final String corpusNameProperty = "name";
		
		SingleValueDialog singleValueDialog = new SingleValueDialog();
		
		singleValueDialog.getSingleValue(
				getApplication().getMainWindow(),
				"Rename Corpus",
				"You have to enter a name!",
				new SaveCancelListener<PropertysetItem>() {
			public void cancelPressed() {}
			public void savePressed(
					PropertysetItem propertysetItem) {
				Property property = 
						propertysetItem.getItemProperty(
								corpusNameProperty);
				String name = (String)property.getValue();
				try {
					repository.update(corpus, name);
				} catch (IOException e) {
					((CatmaApplication)getApplication()).showAndLogError(
						"Error renaming corpus!", e);
				}
			}
		}, corpusNameProperty);
	}

	private void handleRemoveCorpusRequest(final Corpus corpus) {
		ConfirmDialog.show(
				getApplication().getMainWindow(), 
				"Do you really want to delete the Corpus '"
						+ corpus + "'? This will not delete any contents of the Corpus!",
						
		        new ConfirmDialog.Listener() {

		            public void onClose(ConfirmDialog dialog) {
		                if (dialog.isConfirmed()) {
		                	try {
								repository.delete(corpus);
							} catch (IOException e) {
								((CatmaApplication)getApplication()).showAndLogError(
									"Error deleting corpus!", e);
							}
		                }
		            }
		        });		
	}

	private void addItemToCorpus(Object item, Corpus target) {
		try {
			if (item instanceof SourceDocument) {
				SourceDocument sd = (SourceDocument)item;
				if (!target.getSourceDocuments().contains(sd)) {
					repository.update(target, sd);
				}
			}
			else if (item instanceof UserMarkupCollectionReference) {
				UserMarkupCollectionReference umcRef = 
						(UserMarkupCollectionReference)item;
				if (!target.getUserMarkupCollectionRefs().contains(umcRef)) {
					repository.update(target, umcRef);
				}
			}
			else if (item instanceof StaticMarkupCollectionReference) {
				StaticMarkupCollectionReference smcRef = 
						(StaticMarkupCollectionReference)item;
				if (!target.getStaticMarkupCollectionRefs().contains(smcRef)) {
					repository.update(target, smcRef);
				}
			}
			else {
				getWindow().showNotification(
					"Information", 
					"You can only add " +
						"Source Documents and Markup Collections to a Corpus!",
						Notification.TYPE_TRAY_NOTIFICATION);
			}
		}
		catch (IOException e) {
			((CatmaApplication)getApplication()).showAndLogError(
					"Error adding Item to Corpus!", e);
		}
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
		
		corporaContainer = new HierarchicalContainer();
		corporaTree = new Tree();
		corporaTree.setContainerDataSource(corporaContainer);

		corporaTree.addStyleName("bold-label-caption");
		corporaTree.setCaption("Corpora");
		corporaTree.addItem(allDocuments);
		corporaTree.setChildrenAllowed(allDocuments, false);
		corporaTree.setImmediate(true);

		corporaContainer.addContainerProperty(SORTCAP_PROP, String.class, null);
		
		for (Corpus c : repository.getCorpora()) {
			addCorpusToTree(c);
		}
		corporaContainer.sort(new Object[] {SORTCAP_PROP}, new boolean[] { true });

		corporaTree.setValue(allDocuments);

		corporaPanel.addComponent(corporaTree);
		
		return corporaPanel;
	}

	private void handleCorpusCreationRequest() {
		final String corpusNameProperty = "name";
		
		SingleValueDialog singleValueDialog = new SingleValueDialog();
		
		singleValueDialog.getSingleValue(
				getApplication().getMainWindow(),
				"Create a new Corpus",
				"You have to enter a name!",
				new SaveCancelListener<PropertysetItem>() {
			public void cancelPressed() {}
			public void savePressed(
					PropertysetItem propertysetItem) {
				Property property = 
						propertysetItem.getItemProperty(
								corpusNameProperty);
				String name = (String)property.getValue();
				try {
					repository.createCorpus(name);
				} catch (IOException e) {
					((CatmaApplication)getApplication()).showAndLogError(
						"Error creating corpus!", e);
				}
			}
		}, corpusNameProperty);

	}
	
	public void close() {
		repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.corpusChanged,
				corpusChangedListener);
	}

	public Corpus getSelectedCorpus() {
		Object itemId = corporaTree.getValue();
		if (itemId instanceof Corpus) {
			return (Corpus)itemId;
		}
		return null;
	}

	public void setSelectedCorpus(Corpus corpus) {
		if (corpus == null) {
			corporaTree.setValue(allDocuments);
		}
		else {
			corporaTree.setValue(corpus);
		}
	}
}

