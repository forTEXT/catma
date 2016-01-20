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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.Container.Sortable;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.AbstractProperty;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.ItemSorter;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Corpus;
import de.catma.document.corpus.CorpusExporter;
import de.catma.document.repository.Repository;
import de.catma.document.repository.UnknownUserException;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.heureclea.autotagger.AnnotationGeneratorJob;
import de.catma.heureclea.autotagger.GenerationOptions;
import de.catma.indexer.IndexedRepository;
import de.catma.ui.CatmaApplication;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.dialog.FormDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleValueDialog;
import de.catma.ui.repository.sharing.SharingOptions;
import de.catma.ui.repository.sharing.SharingOptionsFieldFactory;
import de.catma.user.Permission;

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

	private enum TableProperty {
		title,
		;
	}
	
	private String allDocuments = "All documents";
	private Button btCreateCorpus;
	private MenuItem miMoreCorpusActions;
	private MenuItem miRemoveCorpus;
	private Table corporaTree;

	private Repository repository;
	private PropertyChangeListener corpusChangedListener;
	private MenuItem miRenameCorpus;

	private IndexedContainer corporaContainer;

	private MenuItem miShareCorpus;

	private MenuItem miExportCorpus;

	private MenuItem miGenerateCorpusAnnotations;
	private ProgressBar generateCorpusAnnotationsProgressBar;
	
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
					corporaContainer.sort(new Object[] {TableProperty.title.name()}, new boolean[] { true });

					Notification.show("Information",
							"Start adding Source Documents" +
							" and Markup Collections by dragging them " +
							"from the Documents section on a Corpus.",
							Type.TRAY_NOTIFICATION);
				}
				else { //update name
					if (evt.getOldValue() instanceof String) {
						corporaTree.markAsDirty();
					}
					else if (evt.getOldValue() instanceof Corpus) {
						Object selectedValue = corporaTree.getValue();
						Corpus oldCorpus = (Corpus) evt.getOldValue();
						removeCorpusFromTree(oldCorpus);
						Corpus newCorpus = (Corpus) evt.getNewValue();
						addCorpusToTree(newCorpus);
						if (corporaTree.containsId(selectedValue)) {
							corporaTree.setValue(selectedValue);
						}
						else if (!corporaTree.getItemIds().isEmpty()) {
							corporaTree.setValue(corporaTree.getItemIds().iterator().next());
						}
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
		corporaTree.addItem(new Object[] {corpus.toString()}, corpus);
	}

	private void initActions(final ValueChangeListener valueChangeListener) {
		corporaTree.addValueChangeListener(new ValueChangeListener() {
			
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
				miExportCorpus.setEnabled(corpusModificationButtonsEnabled);
				miGenerateCorpusAnnotations.setEnabled(corpusModificationButtonsEnabled);
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
					
					AbstractSelectTargetDetails dropData = 
							((AbstractSelectTargetDetails) event.getTargetDetails());

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
						Notification.show(
							"Information", "The corpus is empty! " +
									"Please add some documents first!",
								Type.TRAY_NOTIFICATION);
					}
					else {
						((AnalyzerProvider)UI.getCurrent()).analyze(
								selectedCorpus, (IndexedRepository)repository);
					}
				}
				else {
					Notification.show("Information", "Please select a corpus first!",
							Type.TRAY_NOTIFICATION);
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
		
		btCreateCorpus.addClickListener(new ClickListener() {
			
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
		
		miExportCorpus = miMoreCorpusActions.addItem("Export Corpus", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				Object selectedValue = corporaTree.getValue();
				if ((selectedValue != null) 
						&& !selectedValue.equals(allDocuments)) {
					handleExportCorpusRequest((Corpus)selectedValue);
				}
			}
		});
		miExportCorpus.setVisible(repository.getUser().hasPermission(Permission.exportcorpus));
		miExportCorpus.setEnabled(false);
		
		miGenerateCorpusAnnotations = miMoreCorpusActions.addItem(
				"Generate annotations", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				Object selectedValue = corporaTree.getValue();
				if ((selectedValue != null) 
						&& !selectedValue.equals(allDocuments)) {
					handleGenerateAnnotationsRequest((Corpus)selectedValue);
				}				
			}
		});
		
		miGenerateCorpusAnnotations.setVisible(repository.getUser().hasPermission(Permission.autotagging));
		miGenerateCorpusAnnotations.setEnabled(false);
	}
	
	private void handleGenerateAnnotationsRequest(Corpus selectedValue) { 
		try {
			GenerationOptions generationOptions = new GenerationOptions(
					selectedValue.getId(), 
					repository.getUser().getIdentifier());
			FormDialog<GenerationOptions> generationOptionsDlg = new FormDialog<GenerationOptions>(
				"Please select the type of markup you want us to generate", 
				new BeanItem<GenerationOptions>(generationOptions),
				new GenerationOptionsFieldFactory(), 
				new SaveCancelListener<GenerationOptions>() {
					public void cancelPressed() {}
					public void savePressed(GenerationOptions result) {
						if (result.getTagsetIdentification() != null) {
							Notification.show(
									"Info", 
									"CATMA is generating annotations for you, this may take a while."
									+ "You will be notified once the annotions are ready.", 
									Type.HUMANIZED_MESSAGE);
							
							generateCorpusAnnotationsProgressBar.setCaption("Generating annotations...");
							generateCorpusAnnotationsProgressBar.setIndeterminate(true);
							generateCorpusAnnotationsProgressBar.setVisible(true);
							
							((BackgroundServiceProvider)UI.getCurrent()).submit(
								"Generating annotations...",
								new AnnotationGeneratorJob(result),
								new ExecutionListener<Void>() {
									@Override
									public void done(Void result) {
										try {
											repository.reload(); 
											
											Notification.show(
												"Info", 
												"Your annotations have been generated!", 
												Type.TRAY_NOTIFICATION);
										} catch (IOException e) {
											((CatmaApplication)UI.getCurrent()).showAndLogError(
													"Error reloading repository!", e);
										}
										finally {
											generateCorpusAnnotationsProgressBar.setVisible(false);
										}
									}
									@Override
									public void error(Throwable t) {
										generateCorpusAnnotationsProgressBar.setVisible(false);
										((CatmaApplication)UI.getCurrent()).showAndLogError(
												"Error reloading repository!", t);
									}
								}
							);
						}
						else {
							Notification.show(
									"Info", 
									"You need to select a markup type, no markup has been generated!", 
									Type.TRAY_NOTIFICATION);
						}
					}
			});
			generationOptionsDlg.setVisibleItemProperties(
						new Object[] {"tagsetIdentification"});
			generationOptionsDlg.show();
		}
		catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					"Error generating access token!", e);
		}
	}

	private void handleExportCorpusRequest(Corpus selectedValue) {
		final CorpusExporter corpusExporter = new CorpusExporter(repository);
		final String name = corpusExporter.cleanupName(selectedValue.toString());
		final String fileName = name + corpusExporter.getDate() + ".tar.gz";
		try {
			FileOutputStream corpusOut = new FileOutputStream(
				new File(((CatmaApplication)UI.getCurrent()).getTempDirectory() 
						+ "/" +fileName));
			
			corpusExporter.export(
				name,
				Collections.singletonList(selectedValue), corpusOut);
			
		}
		catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					"Error exporting Corpus!", e);
		}
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
							Notification.show(
									"Sharing failed!", e.getCause().getMessage(), 
									Type.WARNING_MESSAGE);
						}
						else {
							((CatmaApplication)UI.getCurrent()).showAndLogError(
								"Error sharing this corpus!", e);
						}
					}
				}
			});
		sharingOptionsDlg.setVisibleItemProperties(
				new Object[] {"userIdentification", "accessMode"});
		sharingOptionsDlg.show();
	}

	private void handleRenameCorpusRequest(final Corpus corpus) {
		final String corpusNameProperty = "name";
		
		SingleValueDialog singleValueDialog = new SingleValueDialog();
		
		singleValueDialog.getSingleValue(
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
					((CatmaApplication)UI.getCurrent()).showAndLogError(
						"Error renaming corpus!", e);
				}
			}
		}, corpusNameProperty);
	}

	private void handleRemoveCorpusRequest(final Corpus corpus) {
		ConfirmDialog.show(
				UI.getCurrent(),
				"Do you really want to delete the Corpus '"
						+ corpus + "'? This will not delete any contents of the Corpus!",
						
		        new ConfirmDialog.Listener() {

		            public void onClose(ConfirmDialog dialog) {
		                if (dialog.isConfirmed()) {
		                	try {
								repository.delete(corpus);
							} catch (IOException e) {
								((CatmaApplication)UI.getCurrent()).showAndLogError(
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
				Notification.show(
					"Information", 
					"You can only add " +
						"Source Documents and Markup Collections to a Corpus!",
						Type.TRAY_NOTIFICATION);
			}
		}
		catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					"Error adding Item to Corpus!", e);
		}
	}

	private void initComponents() {
		setSpacing(true);
		
		setSizeFull();
		Component corporaPanel = createCorporaPanel();
		addComponent(corporaPanel);
		setExpandRatio(corporaPanel, 1.0f);
		addComponent(createCorporaButtonPanel());	
	}
	
	private Component createCorporaButtonPanel() {
		HorizontalLayout content = new HorizontalLayout();
		content.setMargin(new MarginInfo(false, false, true, false));
		
		Panel corporaButtonsPanel = new Panel(content);
		corporaButtonsPanel.setStyleName(Reindeer.PANEL_LIGHT);
		((HorizontalLayout)corporaButtonsPanel.getContent()).setSpacing(true);
		
		btCreateCorpus = new Button("Create Corpus");
		
		content.addComponent(btCreateCorpus);
		MenuBar menuMoreCorpusActions = new MenuBar();
		miMoreCorpusActions = 
				menuMoreCorpusActions.addItem("More actions...", null);
		miMoreCorpusActions.setEnabled(
				repository instanceof IndexedRepository);
		
		content.addComponent(menuMoreCorpusActions);
		
		generateCorpusAnnotationsProgressBar = new ProgressBar();
		generateCorpusAnnotationsProgressBar.setVisible(false);
		content.addComponent(generateCorpusAnnotationsProgressBar);
		
		return corporaButtonsPanel;
	}

	private Component createCorporaPanel() {
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		content.setMargin(new MarginInfo(false, true, false, false));
		
		corporaContainer = new IndexedContainer();
		corporaContainer.setItemSorter(new ItemSorter() {
			private boolean asc; 
			@Override
			public void setSortProperties(Sortable container, Object[] propertyId, boolean[] ascending) {
				asc = ascending[0];
			}
			@Override
			public int compare(Object itemId1, Object itemId2) {
				if (itemId1.toString().equals(allDocuments)) {
					return 1;
				}
				if (itemId2.toString().equals(allDocuments)) {
					return 1;
				}
				
				if (asc) {
					return itemId1.toString().toLowerCase().compareTo(itemId2.toString().toLowerCase());
				}
				return itemId2.toString().toLowerCase().compareTo(itemId1.toString().toLowerCase());
			}
		});
		corporaTree = new Table("Corpora");
		corporaTree.setContainerDataSource(corporaContainer);
		corporaTree.addContainerProperty(TableProperty.title.name(), String.class, null);
		corporaTree.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		corporaTree.setSizeFull();
		
		corporaTree.addStyleName("bold-label-caption");

		corporaTree.addItem(new Object[] {allDocuments}, allDocuments);

		corporaTree.setImmediate(true);

		corporaContainer.addContainerProperty(TableProperty.title.name(), String.class, null);
		
		for (Corpus c : repository.getCorpora()) {
			addCorpusToTree(c);
		}
		corporaContainer.sort(new Object[] {TableProperty.title.name()}, new boolean[] { true });

		corporaTree.setValue(allDocuments);

		content.addComponent(corporaTree);
		
		return content;
	}

	private void handleCorpusCreationRequest() {
		final String corpusNameProperty = "name";
		
		SingleValueDialog singleValueDialog = new SingleValueDialog();
		
		singleValueDialog.getSingleValue(
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
					((CatmaApplication)UI.getCurrent()).showAndLogError(
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

