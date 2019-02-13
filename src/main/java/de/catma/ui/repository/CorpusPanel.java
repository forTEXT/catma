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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.v7.data.Container.Sortable;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.AbstractProperty;
import com.vaadin.v7.data.util.BeanItem;
import com.vaadin.v7.data.util.IndexedContainer;
import com.vaadin.v7.data.util.ItemSorter;
import com.vaadin.v7.data.util.PropertysetItem;
import com.vaadin.v7.data.util.converter.Converter.ConversionException;
import com.vaadin.v7.event.DataBoundTransferable;
import com.vaadin.v7.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.v7.ui.AbstractSelect.AcceptItem;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.ProgressBar;
import com.vaadin.v7.ui.Table;
import com.vaadin.v7.ui.Table.ColumnHeaderMode;
import com.vaadin.v7.ui.VerticalLayout;
import com.vaadin.v7.ui.themes.Reindeer;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Corpus;
import de.catma.document.corpus.CorpusExporter;
import de.catma.document.repository.Repository;
import de.catma.document.repository.UnknownUserException;
import de.catma.document.source.SourceDocument;
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
	
	private String allDocuments = Messages.getString("CorpusPanel.allDocuments"); //$NON-NLS-1$
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
	private CorpusMarkupCollectionUploadMonitor corpusMarkupCollectionUploadMonitor;
	private ScheduledFuture<?> corpusMarkupCollectionUploadMonitorFuture;
	
	public CorpusPanel(
			Repository repository, ValueChangeListener valueChangeListener) {
		this.repository = repository;
		this.corpusMarkupCollectionUploadMonitor = 
				new CorpusMarkupCollectionUploadMonitor(repository);

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

					Notification.show(Messages.getString("CorpusPanel.infoTitle"), //$NON-NLS-1$
							Messages.getString("CorpusPanel.firstStepsInfo"), //$NON-NLS-1$
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
				corporaContainer.sort(new Object[] {TableProperty.title.name()}, new boolean[] { true });
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
		
		miMoreCorpusActions.addItem(Messages.getString("CorpusPanel.analyzeCorpus"), new Command() { //$NON-NLS-1$
			
			public void menuSelected(MenuItem selectedItem) {
				try {
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
								Messages.getString("CorpusPanel.infoTitle"), Messages.getString("CorpusPanel.corpusIsEmpty"), //$NON-NLS-1$ //$NON-NLS-2$
									Type.TRAY_NOTIFICATION);
						}
						else {
							((AnalyzerProvider)UI.getCurrent()).analyze(
									selectedCorpus, (IndexedRepository)repository);
						}
					}
					else {
						Notification.show(Messages.getString("CorpusPanel.infoTitle"), Messages.getString("CorpusPanel.selectACorpus"), //$NON-NLS-1$ //$NON-NLS-2$
								Type.TRAY_NOTIFICATION);
					}
				}
				catch (Exception e) {
					e.printStackTrace(); //TODO:
				}
			}
		});
		
		miRemoveCorpus = miMoreCorpusActions.addItem(Messages.getString("CorpusPanel.RemoveCorpus"), new Command() { //$NON-NLS-1$
			
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
		
		miRenameCorpus = miMoreCorpusActions.addItem(Messages.getString("CorpusPanel.RenameCorpus"), new Command() { //$NON-NLS-1$
			public void menuSelected(MenuItem selectedItem) {
				Object selectedValue = corporaTree.getValue();
				if ((selectedValue != null) 
						&& !selectedValue.equals(allDocuments)) {
					handleRenameCorpusRequest((Corpus)selectedValue);
				}
			}

		});
		
		miRenameCorpus.setEnabled(false);
		
		miShareCorpus = miMoreCorpusActions.addItem(Messages.getString("CorpusPanel.ShareCorpus"), new Command() { //$NON-NLS-1$
			public void menuSelected(MenuItem selectedItem) {
				Object selectedValue = corporaTree.getValue();
				if ((selectedValue != null) 
						&& !selectedValue.equals(allDocuments)) {
					handleShareCorpusRequest((Corpus)selectedValue);
				}
			}

		});
		miShareCorpus.setEnabled(false);
		
		miExportCorpus = miMoreCorpusActions.addItem(Messages.getString("CorpusPanel.ExportCorpus"), new Command() { //$NON-NLS-1$
			public void menuSelected(MenuItem selectedItem) {
				Object selectedValue = corporaTree.getValue();
				if ((selectedValue != null) 
						&& !selectedValue.equals(allDocuments)) {
					handleExportCorpusRequest((Corpus)selectedValue);
				}
			}
		});
//		miExportCorpus.setVisible(repository.getUser().hasPermission(Permission.exportcorpus));
		miExportCorpus.setEnabled(false);
		
		miGenerateCorpusAnnotations = miMoreCorpusActions.addItem(
				Messages.getString("CorpusPanel.generateAnnotations"), new Command() { //$NON-NLS-1$
			@Override
			public void menuSelected(MenuItem selectedItem) {
				Object selectedValue = corporaTree.getValue();
				if ((selectedValue != null) 
						&& !selectedValue.equals(allDocuments)) {
					Corpus corpus = (Corpus)selectedValue;
					if (!corpus.getSourceDocuments().isEmpty()) {
						handleGenerateAnnotationsRequest((Corpus)selectedValue);
					}
					else {
						Notification.show(
							Messages.getString("CorpusPanel.infoTitle"),  //$NON-NLS-1$
							Messages.getString("CorpusPanel.generationCorpusIsEmpty"),  //$NON-NLS-1$
							Type.TRAY_NOTIFICATION);
					}
					
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
				Messages.getString("CorpusPanel.annotationsType"),  //$NON-NLS-1$
				new BeanItem<GenerationOptions>(generationOptions),
				new GenerationOptionsFieldFactory(), 
				new SaveCancelListener<GenerationOptions>() {
					public void cancelPressed() {}
					public void savePressed(GenerationOptions result) {
						if (result.getTagsetIdentification() != null) {
							Notification.show(
									Messages.getString("CorpusPanel.infoTitle"),  //$NON-NLS-1$
									Messages.getString("CorpusPanel.generatingAnnotationsFeedback"),  //$NON-NLS-1$
									Type.HUMANIZED_MESSAGE);
							
							generateCorpusAnnotationsProgressBar.setCaption(Messages.getString("CorpusPanel.generatingAnnotations")); //$NON-NLS-1$
							generateCorpusAnnotationsProgressBar.setIndeterminate(true);
							generateCorpusAnnotationsProgressBar.setVisible(true);
							
							((BackgroundServiceProvider)UI.getCurrent()).submit(
								Messages.getString("CorpusPanel.generatingAnnotations"), //$NON-NLS-1$
								new AnnotationGeneratorJob(result),
								new ExecutionListener<Void>() {
									@Override
									public void done(Void result) {
										
										if (corpusMarkupCollectionUploadMonitorFuture == null) {
											corpusMarkupCollectionUploadMonitorFuture = 
													((CatmaApplication)UI.getCurrent()).accuireBackgroundService().scheduleWithFixedDelay(
														corpusMarkupCollectionUploadMonitor,
														5,
														10,
														TimeUnit.SECONDS);											
										}
										
										corpusMarkupCollectionUploadMonitor.addCorpus(selectedValue);
										generateCorpusAnnotationsProgressBar.setVisible(false);
									}
									@Override
									public void error(Throwable t) {
										generateCorpusAnnotationsProgressBar.setVisible(false);
										((CatmaApplication)UI.getCurrent()).showAndLogError(
												Messages.getString("CorpusPanel.errorReloadingRepo"), t); //$NON-NLS-1$
									}
								}
							);
						}
						else {
							Notification.show(
									Messages.getString("CorpusPanel.infoTitle"),  //$NON-NLS-1$
									Messages.getString("CorpusPanel.annotationTypeSelectionObligatory"),  //$NON-NLS-1$
									Type.TRAY_NOTIFICATION);
						}
					}
			});
			generationOptionsDlg.setVisibleItemProperties(
						new Object[] {"tagsetIdentification"}); //$NON-NLS-1$
			generationOptionsDlg.show();
		}
		catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					Messages.getString("CorpusPanel.errorGeneratingAccessToken"), e); //$NON-NLS-1$
		}
	}

	private void handleExportCorpusRequest(Corpus selectedValue) {
		final CorpusExporter corpusExporter = new CorpusExporter(repository, true);
		final String name = corpusExporter.cleanupName(selectedValue.toString());
		final String filename = name + corpusExporter.getDate() + ".tar.gz"; //$NON-NLS-1$
		
		
		DownloadDialog dlg = new DownloadDialog(new StreamSource() {
			
			@Override
			public InputStream getStream() {
				try {
					File file = new File(((CatmaApplication)UI.getCurrent()).accquirePersonalTempFolder() 
							+ "/" +filename); //$NON-NLS-1$
					try (FileOutputStream corpusOut = new FileOutputStream(file)) {
						
						corpusExporter.export(
								name,
								Collections.singletonList(selectedValue), corpusOut);
					}
					
					return new FileInputStream(file);
				}
				catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							Messages.getString("CorpusPanel.errorExportingCorpus"), e); //$NON-NLS-1$
					return null;
				}
			}
		}, filename);
		
		dlg.show();
		
	}

	protected void handleShareCorpusRequest(final Corpus corpus) {
		SharingOptions sharingOptions = new SharingOptions();
		
		FormDialog<SharingOptions> sharingOptionsDlg = new FormDialog<SharingOptions>(
			Messages.getString("CorpusPanel.enterPersonToShareWith"),  //$NON-NLS-1$
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
									Messages.getString("CorpusPanel.sharingFailed"), e.getCause().getMessage(),  //$NON-NLS-1$
									Type.ERROR_MESSAGE);
						}
						else {
							((CatmaApplication)UI.getCurrent()).showAndLogError(
								Messages.getString("CorpusPanel.errorSharing"), e); //$NON-NLS-1$
						}
					}
				}
			});
		sharingOptionsDlg.setVisibleItemProperties(
				new Object[] {"userIdentification", "accessMode"}); //$NON-NLS-1$ //$NON-NLS-2$
		sharingOptionsDlg.show();
	}

	private void handleRenameCorpusRequest(final Corpus corpus) {
		final String corpusNameProperty = "name"; //$NON-NLS-1$
		
		SingleValueDialog singleValueDialog = new SingleValueDialog();
		
		singleValueDialog.getSingleValue(
				Messages.getString("CorpusPanel.RenameCorpus"), //$NON-NLS-1$
				Messages.getString("CorpusPanel.nameObligatory"), //$NON-NLS-1$
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
						Messages.getString("CorpusPanel.errorRenamingCorpus"), e); //$NON-NLS-1$
				}
			}
		}, corpusNameProperty);
	}

	private void handleRemoveCorpusRequest(final Corpus corpus) {
		ConfirmDialog.show(
				UI.getCurrent(),
				MessageFormat.format(Messages.getString("CorpusPanel.corpusDeletionQuestion"), corpus), //$NON-NLS-1$
						
		        new ConfirmDialog.Listener() {

		            public void onClose(ConfirmDialog dialog) {
		                if (dialog.isConfirmed()) {
		                	try {
								repository.delete(corpus);
							} catch (IOException e) {
								((CatmaApplication)UI.getCurrent()).showAndLogError(
									Messages.getString("CorpusPanel.errorDeletingCorpus"), e); //$NON-NLS-1$
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
			else {
				Notification.show(
					Messages.getString("CorpusPanel.infoTitle"),  //$NON-NLS-1$
					Messages.getString("CorpusPanel.addContentInfo"), //$NON-NLS-1$
						Type.TRAY_NOTIFICATION);
			}
		}
		catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					Messages.getString("CorpusPanel.errorAddingContent"), e); //$NON-NLS-1$
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
		
		btCreateCorpus = new Button(Messages.getString("CorpusPanel.CreateCorpus")); //$NON-NLS-1$
		
		content.addComponent(btCreateCorpus);
		MenuBar menuMoreCorpusActions = new MenuBar();
		miMoreCorpusActions = 
				menuMoreCorpusActions.addItem(Messages.getString("CorpusPanel.MoreActions"), null); //$NON-NLS-1$
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
		corporaTree = new Table(Messages.getString("CorpusPanel.Corpora")); //$NON-NLS-1$
		corporaTree.setContainerDataSource(corporaContainer);
		corporaTree.addContainerProperty(TableProperty.title.name(), String.class, null);
		corporaTree.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		corporaTree.setSizeFull();
		
		corporaTree.addStyleName("bold-label-caption"); //$NON-NLS-1$

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
		final String corpusNameProperty = "name"; //$NON-NLS-1$
		
		SingleValueDialog singleValueDialog = new SingleValueDialog();
		
		singleValueDialog.getSingleValue(
				Messages.getString("CorpusPanel.createNewCorpus"), //$NON-NLS-1$
				Messages.getString("CorpusPanel.nameObligatory"), //$NON-NLS-1$
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
						Messages.getString("CorpusPanel.errorCreatingCorpus"), e); //$NON-NLS-1$
				}
			}
		}, corpusNameProperty);

	}
	
	public void close() {
		if (corpusMarkupCollectionUploadMonitorFuture != null) {
			corpusMarkupCollectionUploadMonitorFuture.cancel(true);
		}
		
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

