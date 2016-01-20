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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.Collection;

import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.data.Property;
import com.vaadin.data.Container.Sortable;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.ItemSorter;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.Table.TableDragMode;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.repository.UnknownUserException;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.document.source.contenthandler.XMLContentHandler;
import de.catma.document.standoffmarkup.MarkupCollectionReference;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;
import de.catma.indexer.TagsetDefinitionUpdateLog;
import de.catma.serialization.intrinsic.xml.XmlMarkupCollectionSerializationHandler;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.CatmaApplication;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.dialog.FormDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleValueDialog;
import de.catma.ui.dialog.UploadDialog;
import de.catma.ui.repository.sharing.SharingOptions;
import de.catma.ui.repository.sharing.SharingOptionsFieldFactory;
import de.catma.ui.repository.wizard.AddSourceDocWizardFactory;
import de.catma.ui.repository.wizard.AddSourceDocWizardResult;
import de.catma.ui.repository.wizard.SourceDocumentResult;
import de.catma.util.CloseSafe;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

@SuppressWarnings("deprecation")
public class SourceDocumentPanel extends HorizontalSplitPanel
	implements ValueChangeListener {
	
	private enum TableProperty {
		title,
		;
	}
	private final static Object[] DEFAULT_VISIBIL_PROP = new Object[] {
		"title", "author", "description", "publisher"
	};
	
	private final ContentInfoSet emptyContentInfoSet = new ContentInfoSet();
	private HierarchicalContainer documentsContainer;
	private TreeTable documentsTree;
	private Repository repository;
	private String userMarkupItemDisplayString = "Markup Collections";
	private Button btOpenDocument;
	private Button btAddDocument;
	private MenuItem miMoreDocumentActions;
	private Form contentInfoForm;
	private Button btEditContentInfo;
	private Button btSaveContentInfoChanges;
	private Button btDiscardContentInfoChanges;
	
	private PropertyChangeListener sourceDocumentChangedListener;
	private PropertyChangeListener userMarkupDocumentChangedListener;

	private Corpus currentCorpus;
	private boolean init = false;

	public SourceDocumentPanel(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public void attach() {
		super.attach();
		if (!init) {
			initComponents();
			initActions();
			initListeners();
			init = true;
		}
	}

	private void initListeners() {
		sourceDocumentChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) { //insert
					SourceDocument sd = repository.getSourceDocument(
							(String)evt.getNewValue());
					addSourceDocumentToTree(sd);
					documentsContainer.sort(new Object[] {TableProperty.title.name()}, new boolean[] { true });
				}
				else if (evt.getNewValue() == null) { //remove
					removeSourceDocumentFromTree(
						(SourceDocument)evt.getOldValue());
				}
				else { //update
					removeSourceDocumentFromTree((SourceDocument) evt.getNewValue()); //newValue intended
					addSourceDocumentToTree((SourceDocument) evt.getNewValue());
					documentsContainer.sort(new Object[] {TableProperty.title.name()}, new boolean[] { true });
				}
			}
		};
		this.repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.sourceDocumentChanged,
				sourceDocumentChangedListener);
		
		userMarkupDocumentChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) { // insert
					@SuppressWarnings("unchecked")
					Pair<UserMarkupCollectionReference, SourceDocument>
						result = 
							(Pair<UserMarkupCollectionReference, SourceDocument>)
								evt.getNewValue();
					
					addUserMarkupCollectionReferenceToTree(
							result.getFirst(), result.getSecond());
				}
				else if (evt.getNewValue() == null) { // remove
					UserMarkupCollectionReference userMarkupCollectionReference =
							(UserMarkupCollectionReference) evt.getOldValue();
					removUserMarkupCollectionReferenceFromTree(
							userMarkupCollectionReference);
				}
				else { // update
					documentsTree.markAsDirty();
				}
			}
		};
		this.repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.userMarkupCollectionChanged,
				userMarkupDocumentChangedListener);	

	}

	private void removeSourceDocumentFromTree(SourceDocument sourceDocument) {
		for (UserMarkupCollectionReference umcRef : 
			sourceDocument.getUserMarkupCollectionRefs()) {
			documentsTree.removeItem(umcRef);
		}
		
		for (StaticMarkupCollectionReference smcRef : 
			sourceDocument.getStaticMarkupCollectionRefs()) {
			documentsTree.removeItem(smcRef);
		}
		
		while ((documentsTree.getChildren(sourceDocument) != null)
				&& !documentsTree.getChildren(sourceDocument).isEmpty()){
			documentsTree.removeItem(
					documentsTree.getChildren(sourceDocument).iterator().next());
		}
		documentsTree.removeItem(sourceDocument);
	}

	private void initActions() {
		btAddDocument.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
				final AddSourceDocWizardResult wizardResult = 
						new AddSourceDocWizardResult();
				
				AddSourceDocWizardFactory factory = 
					new AddSourceDocWizardFactory(
							new WizardProgressListener() {
						
						public void wizardCompleted(WizardCompletedEvent event) {
							event.getWizard().removeListener(this);
							final boolean generateStarterKit = repository.getSourceDocuments().isEmpty();
							try {
								for(SourceDocumentResult sdr : wizardResult.getSourceDocumentResults()){
									final SourceDocument sourceDocument = sdr.getSourceDocument();
									
									repository.addPropertyChangeListener(
										RepositoryChangeEvent.sourceDocumentChanged,
										new PropertyChangeListener() {

											@Override
											public void propertyChange(PropertyChangeEvent evt) {
												
												if ((evt.getNewValue() == null)	|| (evt.getOldValue() != null)) {
													return; // no insert
												}
												
												String newSdId = (String) evt.getNewValue();
												if (!sourceDocument.getID().equals(newSdId)) {
													return;
												}
												
													
												repository.removePropertyChangeListener(
													RepositoryChangeEvent.sourceDocumentChanged, 
													this);
												
												if (currentCorpus != null) {
													try {
														repository.update(currentCorpus, sourceDocument);
														setSourceDocumentsFilter(currentCorpus);
														
													} catch (IOException e) {
														((CatmaApplication)UI.getCurrent()).showAndLogError(
															"Error adding Source Document to Corpus! " +
															"The Source Document has been added to 'All Documents's", e);
													}
													
												}
												
												if (sourceDocument
														.getSourceContentHandler()
														.hasIntrinsicMarkupCollection()) {
													try {
														handleIntrinsicMarkupCollection(sourceDocument);
													} catch (IOException e) {
														((CatmaApplication)UI.getCurrent()).showAndLogError(
															"Error extracting intrinsic markup collection!", e);
													}
												}
												
												if (generateStarterKit) {
													generateStarterKit(sourceDocument);
												}
											}
										});

									repository.insert(sourceDocument);
								}
								
							} catch (IOException e) {
								((CatmaApplication)UI.getCurrent()).showAndLogError(
									"Error adding the Source Document!", e);
							}
						}

						public void wizardCancelled(WizardCancelledEvent event) {
							event.getWizard().removeListener(this);
						}
						
						public void stepSetChanged(WizardStepSetChangedEvent event) {/*not needed*/}
						
						public void activeStepChanged(WizardStepActivationEvent event) {/*not needed*/}
					}, 
					wizardResult,
					repository);
				
				Window sourceDocCreationWizardWindow = 
						factory.createWizardWindow(
								"Add new Source Document", "85%",  "98%");
				
				UI.getCurrent().addWindow(
						sourceDocCreationWizardWindow);
				
				sourceDocCreationWizardWindow.center();
			}
		});
		
		btOpenDocument.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				final Object value = documentsTree.getValue();
				handleOpenDocumentRequest(value);
			}
		});
		
		documentsTree.addValueChangeListener(this);
		
		miMoreDocumentActions.addItem("Analyze Document", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleAnalyzeDocumentRequest();
			}
		});
		
		miMoreDocumentActions.addItem("Remove Document", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleSourceDocumentRemovalRequest();
			}
		});
		
		miMoreDocumentActions.addItem("Export Document...", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleSourceDocumentExportRequest();
			}
		});

		
		miMoreDocumentActions.addItem("Share Document", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				handleShareSourceDocumentRequest();
			}
		});

//		miMoreDocumentActions.addItem("Clear Graph DB", new Command() {
//			public void menuSelected(MenuItem selectedItem) {
//				try {
//					GraphDatabaseService graphDb = (GraphDatabaseService) new InitialContext().lookup(
//							CatmaGraphDbName.CATMAGRAPHDB.name());
//					Transaction transaction = graphDb.beginTx();
//					GlobalGraphOperations globalGraphOperations = GlobalGraphOperations.at(graphDb);
//					for (Node n : globalGraphOperations.getAllNodes()) {
//						for (Relationship r : n.getRelationships()) {
//							r.delete();
//						}
//						n.delete();
//					}
//					transaction.success();
//					transaction.close();
//				}
//				catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
		miMoreDocumentActions.addSeparator();
		
		miMoreDocumentActions.addItem("Create Markup Collection", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleUserMarkupCollectionCreation();
			}

		});
		
		miMoreDocumentActions.addItem("Reindex Markup Collection", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				Object value = documentsTree.getValue();

				handleUserMarkupCollectionReindexRequest(value);
			}
		});
		
		miMoreDocumentActions.addItem("Import Markup Collection", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleUserMarkupCollectionImportRequest();
			}
		});
		
		miMoreDocumentActions.addItem("Export Markup Collection...", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				
				Object value = documentsTree.getValue();
				handleUserMarkupCollectionExportRequest(value);
			}
		});

		miMoreDocumentActions.addItem("Remove Markup Collection", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				handleUserMarkupRemoval();
			}

		});
		miMoreDocumentActions.addItem("Share Markup Collection", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				handleShareUmcRequest();
			}

		});
		MenuItem generateAnnotations = miMoreDocumentActions.addItem("Generate annotations", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				handleGenerateAnnotations();
			}
		});
		generateAnnotations.setVisible(false);//TODO: implement

		btEditContentInfo.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(false);
				btSaveContentInfoChanges.setVisible(true);
				btDiscardContentInfoChanges.setVisible(true);
				contentInfoForm.setReadOnly(false);
			}
		});
		
		btSaveContentInfoChanges.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(true);
				btSaveContentInfoChanges.setVisible(false);
				btDiscardContentInfoChanges.setVisible(false);
				contentInfoForm.commit();
				contentInfoForm.setReadOnly(true);				
				Object value = documentsTree.getValue();
				@SuppressWarnings("unchecked")
				BeanItem<ContentInfoSet> item = 
						(BeanItem<ContentInfoSet>)contentInfoForm.getItemDataSource();
				ContentInfoSet contentInfoSet = item.getBean();

				if (value instanceof UserMarkupCollectionReference) {
					repository.update((UserMarkupCollectionReference)value, contentInfoSet);
				}
				else if (value instanceof SourceDocument) {
					repository.update((SourceDocument)value, contentInfoSet);
				}
			}
		});
		
		btDiscardContentInfoChanges.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(true);
				btSaveContentInfoChanges.setVisible(false);
				btDiscardContentInfoChanges.setVisible(false);
				contentInfoForm.discard();
				contentInfoForm.setReadOnly(true);				
			}
		});

		documentsTree.addItemClickListener(new ItemClickListener() {
			
			public void itemClick(ItemClickEvent event) {
				//FIXME: documentsTree needs to be converted to TreeTable because Tree is buggy with double clicks
				if (event.isDoubleClick()) {
					Object item = event.getItemId();
					handleOpenDocumentRequest(item);
				}
			}
		});
	}
	
	
	private void handleGenerateAnnotations() {
		Object value = documentsTree.getValue();
		if ((value == null) || !(value instanceof SourceDocument)) {
			 Notification.show(
                    "Information",
                    "Please select a Source Document first",
                    Type.TRAY_NOTIFICATION);
		}
		else{
			
			final SourceDocument sourceDocument = (SourceDocument)value;
			Corpus corpus = currentCorpus;
			if (corpus == null) {
				corpus = createAutoCorpus(); 
			}
			
			
			
		}
	}

	private Corpus createAutoCorpus() {
		try {
			//HIER GEHTS WEITER
			repository.createCorpus("");
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				"Error creating corpus!", e);
		}

		return null;
	}

	private void handleSourceDocumentExportRequest() {
		Object value = documentsTree.getValue();
		if ((value == null) || !(value instanceof SourceDocument)) {
			 Notification.show(
                    "Information",
                    "Please select a Source Document first",
                    Type.TRAY_NOTIFICATION);
		}
		else{
			
			final SourceDocument sourceDocument = (SourceDocument)value;

			SourceDocumentExportOptionsDialog sdExportOptionsDialog = 
					new SourceDocumentExportOptionsDialog(repository, sourceDocument);
			UI.getCurrent().addWindow(sdExportOptionsDialog);
		}
	}

	private void handleShareUmcRequest() {
		Object selValue = documentsTree.getValue();
		if ((selValue != null) 
				&& (selValue instanceof UserMarkupCollectionReference)) {
			final UserMarkupCollectionReference userMarkupCollectionReference =
					(UserMarkupCollectionReference) selValue;

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
									userMarkupCollectionReference, 
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
		else {
			Notification.show(
					"Information", "Please select a Markup Collection first!",
					Type.TRAY_NOTIFICATION);
		}
	}

	private void handleShareSourceDocumentRequest() {
		Object value = documentsTree.getValue();
		if ((value == null) || !(value instanceof SourceDocument)) {
			 Notification.show(
                    "Information",
                    "Please select a Source Document first",
                    Type.TRAY_NOTIFICATION);
		}
		else{
			final SourceDocument sourceDocument = (SourceDocument)value;
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
									sourceDocument, 
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
		
	}

	private void generateStarterKit(
			final SourceDocument sourceDocument) {
		String name = "Example Markup Collection";
		try {
			repository.addPropertyChangeListener(
					RepositoryChangeEvent.userMarkupCollectionChanged, 
				new PropertyChangeListener() {
				
				public void propertyChange(PropertyChangeEvent evt) {
					
					if (evt.getOldValue() != null) {
						return;
					}
					
					@SuppressWarnings("unchecked")
					Pair<UserMarkupCollectionReference, SourceDocument> umcResultPair = 
							(Pair<UserMarkupCollectionReference, SourceDocument>) evt.getNewValue();
					
					if (umcResultPair != null) {
						
						if (sourceDocument.equals(umcResultPair.getSecond())) {
							
							documentsTree.setCollapsed(umcResultPair.getSecond(), false);
							documentsTree.setValue(umcResultPair.getFirst());
							
							try {
								repository.addPropertyChangeListener(
										RepositoryChangeEvent.tagLibraryChanged, 
										new PropertyChangeListener() {
									
									public void propertyChange(PropertyChangeEvent evt) {
										TagLibraryReference tagLibRef = 
												(TagLibraryReference)evt.getNewValue();
										if ((tagLibRef != null)
												&& (evt.getOldValue() == null)) {
											IDGenerator idGenerator = new IDGenerator();
											try {
												TagLibrary tagLibrary = 
														repository.getTagLibrary(tagLibRef);
												
												TagsetDefinition tsd = 
														new TagsetDefinition(
															null,
															idGenerator.generate(), 
															"Example Tagset", 
															new Version());
												
												repository.getTagManager().addTagsetDefinition(
														tagLibrary, tsd);

												TagDefinition td = 
														new TagDefinition(
																null,
																idGenerator.generate(),
																"Example Tag",
																new Version(), 
																null, "");
												PropertyDefinition colorPropertyDef =
														new PropertyDefinition(
															null,
															idGenerator.generate(),
															PropertyDefinition.SystemPropertyName.
																catma_displaycolor.name(),
															new PropertyPossibleValueList(
																ColorConverter.toRGBIntAsString(
																	ColorConverter.randomHex())));
												td.addSystemPropertyDefinition(
														colorPropertyDef);
												repository.getTagManager().addTagDefinition(
													tsd, td);
												
											} catch (IOException e) {
												((CatmaApplication)UI.getCurrent()).showAndLogError(
													"Error creating the Example Tagset and Tag Type Definitions!", e);
											}
											
											repository.removePropertyChangeListener(
													RepositoryChangeEvent.tagLibraryChanged, 
													this);
										}
									}
								});
								repository.createTagLibrary("Example Tag Type Library");
								repository.removePropertyChangeListener(
									RepositoryChangeEvent.userMarkupCollectionChanged, this);
							} catch (IOException e) {
								((CatmaApplication)UI.getCurrent()).showAndLogError(
										"Error creating Example Tag Type Library!", e);
							}
						}
					}
				}
			});
			repository.createUserMarkupCollection(
					name, sourceDocument);
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				"Error creating Example Markup Collection!", e);
		}
		
		
	}

	private void handleUserMarkupCollectionReindexRequest(Object value) {
		if ((value != null) && (value instanceof UserMarkupCollectionReference)) {
			final UserMarkupCollectionReference umcRef = 
					(UserMarkupCollectionReference)value;
			try {
				UserMarkupCollection umc = repository.getUserMarkupCollection(umcRef);
				Indexer indexer = ((IndexedRepository)repository).getIndexer();
				
				for (TagsetDefinition tagsetDef : umc.getTagLibrary()) {
					indexer.reindex(
						tagsetDef, 
						new TagsetDefinitionUpdateLog(), 
						umc);
				}
				Notification.show(
						"Information", "Reindexing finished!", 
						Type.TRAY_NOTIFICATION);
			}
			catch (IOException ioe) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
						"error reindexing Markup Collection!", ioe);
			}
		}
	}

	private void handleUserMarkupCollectionExportRequest(Object value) {
		if ((value != null) && (value instanceof UserMarkupCollectionReference)) {
			final UserMarkupCollectionReference umcRef = 
					(UserMarkupCollectionReference)value;
			final SourceDocument sd = 
					(SourceDocument)documentsTree.getParent(
							documentsTree.getParent(value));
			MarkupCollectionExportOptionsDialog mcExportOptionsDialog = 
					new MarkupCollectionExportOptionsDialog(repository, sd, umcRef);
			UI.getCurrent().addWindow(mcExportOptionsDialog);
			
		}
		else {
			Notification.show(
					"Information", "Please select a Markup Collection first!",
					Type.TRAY_NOTIFICATION);
		}
		
	}

	private void handleOpenDocumentRequest(final Object value) {
		try {
			if (value==null) {
				Notification.show(
						"Information", "Please select a document first!",
						Type.TRAY_NOTIFICATION);
			}
			if (value instanceof SourceDocument) {
				((CatmaApplication)UI.getCurrent()).openSourceDocument(
						(SourceDocument)value, repository);
			}
			else if (value instanceof StaticMarkupCollectionReference) {
					//TODO: remove
				
			}
			else if (value instanceof UserMarkupCollectionReference) {
				final SourceDocument sd = 
						(SourceDocument)documentsTree.getParent(
								documentsTree.getParent(value));
				final CatmaApplication application = 
						(CatmaApplication)UI.getCurrent();
				UserMarkupCollectionReference 
					userMarkupCollectionReference = 
						(UserMarkupCollectionReference)value;
	
				application.openUserMarkupCollection(
						sd, 
						repository.getUserMarkupCollection(userMarkupCollectionReference), 
						repository);
			}
		}
		catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				"Error opening document! ", e);	
		}
	}

	private void handleSourceDocumentRemovalRequest() {
		Object value = documentsTree.getValue();
		
		if (value instanceof SourceDocument) {
			final SourceDocument sd = (SourceDocument)value;
			ConfirmDialog.show(
					UI.getCurrent(), 
					"Do you really want to delete the Source Document '"
							+ sd.toString() + "' and all its markup collections?",
							
							new ConfirmDialog.Listener() {
						
						public void onClose(ConfirmDialog dialog) {
							if (dialog.isConfirmed()) {
								try {
									repository.delete(sd);
								} catch (IOException e) {
									((CatmaApplication)UI.getCurrent()).showAndLogError(
											"Error deleting the Source Document!", e);
								}
							}
						}
					});
			
		}
		else {
			Notification.show(
					"Information", "Please select a Source Document!",
					Type.TRAY_NOTIFICATION);
		}
		
	}

	private void handleAnalyzeDocumentRequest() {
		if (repository instanceof IndexedRepository) {
			Object value = documentsTree.getValue();
			if (value != null) {
				if (value instanceof SourceDocument) {
					SourceDocument sd = (SourceDocument)value;
					if (sd.getUserMarkupCollectionRefs().isEmpty()) {
						Corpus c = new Corpus(sd.toString());
						c.addSourceDocument(sd);
						((AnalyzerProvider)UI.getCurrent()).analyze(
								c,
								(IndexedRepository)repository);
					}
					else {
						CorpusContentSelectionDialog dialog =
							new CorpusContentSelectionDialog(
								repository,
								sd,
								currentCorpus,
								new SaveCancelListener<Corpus>() {
									public void cancelPressed() {/* noop */}
									public void savePressed(Corpus result) {
										
										((AnalyzerProvider)UI.getCurrent()).analyze(
												result,
												(IndexedRepository)repository);
										
									}
								},
								"Analyze Document",
								"Select markup collections to include in analysis"
								);
						dialog.show();
					}
				}
				else {
					Notification.show(
						"Information", "Please select a Source Document first!",
						Type.TRAY_NOTIFICATION);
				}
			}			
		}
		else {
			Notification.show(
				"Information", 
				"This repository is not indexed, analysis is not supported!",
				Type.TRAY_NOTIFICATION);
		}
		
	}
	
	private MarkupCollectionItem getUserMarkupCollectionItemId(SourceDocument sd) {
		Collection<?> children = documentsTree.getChildren(sd);

		if (children != null) {
			for (Object child : children) {
				if ((child instanceof MarkupCollectionItem) &&
						((MarkupCollectionItem)child).isUserMarkupCollectionItem()) {
					return (MarkupCollectionItem)child;
				}
			}
		}
		return null;
	}

	private void initComponents() {
		setSplitPosition(70);
		addComponent(createOuterDocumentsPanel());
		addComponent(createContentInfoPanel());
	}
	
	private Component createOuterDocumentsPanel() {
		
		VerticalLayout outerDocumentsPanel = new VerticalLayout();
		outerDocumentsPanel.setSpacing(true);
		outerDocumentsPanel.setMargin(new MarginInfo(false, true, true, true));
		outerDocumentsPanel.setSizeFull();
		
		Component documentsPanel = createDocumentsPanel();
		outerDocumentsPanel.addComponent(documentsPanel);
		outerDocumentsPanel.setExpandRatio(documentsPanel, 1.0f);
		outerDocumentsPanel.addComponent(createDocumentButtonsPanel());
		
		return outerDocumentsPanel;
	}

	private Component createDocumentButtonsPanel() {
		HorizontalLayout documentButtonsPanelContent = new HorizontalLayout();
		Panel documentButtonsPanel = new Panel(documentButtonsPanelContent);
		documentButtonsPanel.setStyleName(Reindeer.PANEL_LIGHT);

		((HorizontalLayout)documentButtonsPanel.getContent()).setSpacing(true);
		
		btOpenDocument = new Button("Open Document");
		btOpenDocument.addStyleName("primary-button");
		btOpenDocument.setEnabled(false);
		documentButtonsPanelContent.addComponent(btOpenDocument);
		btAddDocument = new Button("Add Document");
		btAddDocument.addStyleName("secondary-button");
		documentButtonsPanelContent.addComponent(btAddDocument);

		MenuBar menuMoreDocumentActions = new MenuBar();
		miMoreDocumentActions = 
				menuMoreDocumentActions.addItem("More actions...", null);
		documentButtonsPanelContent.addComponent(menuMoreDocumentActions);
		
		return documentButtonsPanel;
	}
	
	private Component createDocumentsPanel() {
		VerticalLayout documentsPanelContent = new VerticalLayout();
		documentsPanelContent.setSizeFull();
		
		
		documentsContainer = new HierarchicalContainer();
		documentsContainer.setItemSorter(new ItemSorter() {
			
			private boolean asc;

			@Override
			public void setSortProperties(Sortable container, Object[] propertyId, boolean[] ascending) {
				this.asc = ascending[0];
			}
			
			@Override
			public int compare(Object itemId1, Object itemId2) {
				if (asc) {
					return itemId1.toString().toLowerCase().compareTo(itemId2.toString().toLowerCase());
				}
				return itemId2.toString().toLowerCase().compareTo(itemId1.toString().toLowerCase());
			}
		});
		documentsTree = new TreeTable("Documents");
		documentsTree.setContainerDataSource(documentsContainer);
		documentsTree.setSizeFull();
		
		documentsTree.addStyleName("bold-label-caption");
		documentsTree.setDragMode(TableDragMode.ROW);
		
		documentsPanelContent.addComponent(documentsTree);
		documentsPanelContent.setExpandRatio(documentsTree, 1.0f);
		
		documentsTree.addContainerProperty(TableProperty.title.name(), String.class, null);
		documentsTree.setVisibleColumns(new Object[] {TableProperty.title.name()});
		documentsTree.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		
		for (SourceDocument sd : repository.getSourceDocuments()) {
			addSourceDocumentToTree(sd);
		}		

		documentsContainer.sort(new Object[] {TableProperty.title.name()}, new boolean[] { true });
		
		return documentsPanelContent;
	}

	private void addSourceDocumentToTree(SourceDocument sd) {
		
		documentsContainer.removeAllContainerFilters();
		
		documentsTree.addItem(new Object[] {sd.toString()}, sd);
		documentsTree.setChildrenAllowed(sd, true);
		
		
		MarkupCollectionItem userMarkupItem =
				new MarkupCollectionItem(sd, userMarkupItemDisplayString, true);

		documentsTree.addItem(new Object[] {userMarkupItem.toString()}, userMarkupItem);
		documentsTree.setParent(userMarkupItem, sd);
	
		for (UserMarkupCollectionReference ucr : sd.getUserMarkupCollectionRefs()) {
			addUserMarkupCollectionReferenceToTree(ucr, userMarkupItem);
		}
		
	}
	
	private void addUserMarkupCollectionReferenceToTree(
			UserMarkupCollectionReference ucr, MarkupCollectionItem userMarkupItem) {
		documentsTree.addItem(new Object[]{ucr.toString()}, ucr);
		documentsTree.setParent(ucr, userMarkupItem);
		documentsTree.setChildrenAllowed(ucr, false);
	}


	private Component createContentInfoPanel() {
		VerticalLayout contentInfoPanel = new VerticalLayout();
		contentInfoPanel.setSpacing(true);
		contentInfoPanel.setSizeFull();
		contentInfoPanel.setMargin(new MarginInfo(false, false, true, true));
		Component contentInfoForm = createContentInfoForm();
		contentInfoPanel.addComponent(contentInfoForm);
		contentInfoPanel.setExpandRatio(contentInfoForm, 1.0f);
		
		contentInfoPanel.addComponent(createContentInfoButtonsPanel());
		
		return contentInfoPanel;
	}

	private Component createContentInfoButtonsPanel() {
		HorizontalLayout content = new HorizontalLayout();
		content.setSpacing(true);
		
		Panel contentInfoButtonsPanel = new Panel(content);
		
		contentInfoButtonsPanel.setStyleName(Reindeer.PANEL_LIGHT);
		
		btEditContentInfo = new Button("Edit");
		btEditContentInfo.setEnabled(false);
		content.addComponent(btEditContentInfo);
		btSaveContentInfoChanges = new Button("Save");
		btSaveContentInfoChanges.setVisible(false);
		content.addComponent(btSaveContentInfoChanges);
		btDiscardContentInfoChanges = new Button("Discard");
		btDiscardContentInfoChanges.setVisible(false);
		content.addComponent(btDiscardContentInfoChanges);
		
		return contentInfoButtonsPanel;
	}

	private Component createContentInfoForm() {
		VerticalLayout contentInfoPanelContent = new VerticalLayout();
		contentInfoPanelContent.setMargin(true);
		
		Panel contentInfoPanel = new Panel(contentInfoPanelContent);
		contentInfoPanel.getContent().setSizeUndefined();
		contentInfoPanel.getContent().setWidth("100%");
		contentInfoPanel.setSizeFull();
		
		contentInfoForm = new Form();
		contentInfoForm.setSizeFull();
		contentInfoForm.setCaption("Information");
		contentInfoForm.setBuffered(true);
		contentInfoForm.setReadOnly(true);
		contentInfoForm.setEnabled(false);
		
		BeanItem<ContentInfoSet> contentInfoItem = 
				new BeanItem<ContentInfoSet>(emptyContentInfoSet);
		contentInfoForm.setItemDataSource(contentInfoItem);
		contentInfoForm.setVisibleItemProperties(DEFAULT_VISIBIL_PROP);
		
		contentInfoForm.setReadOnly(true);
		contentInfoPanelContent.addComponent(contentInfoForm);
		
		return contentInfoPanel;
	}
	
	private void removUserMarkupCollectionReferenceFromTree(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		documentsTree.removeItem(userMarkupCollectionReference);
	}

	private void handleUserMarkupCollectionImportRequest() {
		Object value = documentsTree.getValue();
		if ((value == null) || !(value instanceof SourceDocument)) {
			 Notification.show(
                    "Information",
                    "Please select a Source Document first",
                    Type.TRAY_NOTIFICATION);
		}
		else{
			final SourceDocument sourceDocument = (SourceDocument)value;

			UploadDialog uploadDialog =
					new UploadDialog("Upload Markup Collection", 
							new SaveCancelListener<byte[]>() {
				
				public void cancelPressed() {}
				
				public void savePressed(byte[] result) {
					InputStream is = new ByteArrayInputStream(result);
					try {
						if (BOMFilterInputStream.hasBOM(result)) {
							is = new BOMFilterInputStream(
									is, Charset.forName("UTF-8"));
						}
						
						repository.importUserMarkupCollection(
								is, sourceDocument);
					} catch (IOException e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(
							"Error importing the Markup Collection!", e);
					}
					finally {
						CloseSafe.close(is);
					}
				}
				
			});
			uploadDialog.show();		
		}
	}

	private void handleUserMarkupRemoval() {
		Object selValue = documentsTree.getValue();
		if ((selValue != null) 
				&& (selValue instanceof UserMarkupCollectionReference)) {
			final UserMarkupCollectionReference userMarkupCollectionReference =
					(UserMarkupCollectionReference) selValue;
			
			ConfirmDialog.show(
				UI.getCurrent(), 
				"Do you really want to delete the Markup Collection '"
						+ userMarkupCollectionReference.toString() + "'?",
						
		        new ConfirmDialog.Listener() {

		            public void onClose(ConfirmDialog dialog) {
		                if (dialog.isConfirmed()) {
		                	try {
								repository.delete(userMarkupCollectionReference);
							} catch (IOException e) {
								((CatmaApplication)UI.getCurrent()).showAndLogError(
									"Error deleting the Markup Collection!", e);
							}
		                }
		            }
		        });
		}
		
	}
	
	private void handleUserMarkupCollectionCreation() {
		Object value = documentsTree.getValue();
		if (value == null) {
			 Notification.show(
                    "Information",
                    "Please select a Source Document first",
                    Type.TRAY_NOTIFICATION);
		}
		else {
			 while (!(value instanceof SourceDocument)){
				 value = documentsTree.getParent(value);
			 }
			
			
			final SourceDocument sourceDocument = (SourceDocument)value;
			final String userMarkupCollectionNameProperty = "name";
			
			SingleValueDialog singleValueDialog = new SingleValueDialog();
			
			singleValueDialog.getSingleValue(
					"Create a new Markup Collection",
					"You have to enter a name!",
					new SaveCancelListener<PropertysetItem>() {
				public void cancelPressed() {}
				public void savePressed(
						PropertysetItem propertysetItem) {
					Property<?> property = 
							propertysetItem.getItemProperty(
									userMarkupCollectionNameProperty);
					String name = (String)property.getValue();
					try {
						repository.createUserMarkupCollection(
								name, sourceDocument);
					} catch (IOException e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(
							"Error creating the Markup Collection!", e);
					}
				}
			}, userMarkupCollectionNameProperty);

		}
		
	}

	private void addUserMarkupCollectionReferenceToTree(
			UserMarkupCollectionReference userMarkupCollRef, 
			SourceDocument sourceDocument) {
		documentsContainer.removeAllContainerFilters();
		
		
		MarkupCollectionItem mi = getUserMarkupCollectionItemId(sourceDocument);
		if (mi != null) {
			addUserMarkupCollectionReferenceToTree(userMarkupCollRef, mi);
		}
		
		
		if (currentCorpus != null) {
			try {
				repository.update(currentCorpus, userMarkupCollRef);
				setSourceDocumentsFilter(currentCorpus);
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
						"Error adding Markup Collection to Corpus! " +
								"The Markup Collection has been added to 'All Documents's", e);
			}
		}
		documentsTree.setValue(userMarkupCollRef);
		documentsTree.setCollapsed(sourceDocument, false);
	}
	
	public void valueChange(ValueChangeEvent event) {
		Object value = event.getProperty().getValue();
		if (value != null) {
			if (value instanceof SourceDocument) {
				contentInfoForm.setEnabled(true);
				btEditContentInfo.setEnabled(true);
				contentInfoForm.setItemDataSource(
					new BeanItem<ContentInfoSet>(
						new ContentInfoSet(((SourceDocument)value).getSourceContentHandler()
							.getSourceDocumentInfo().getContentInfoSet())));
				contentInfoForm.setVisibleItemProperties(DEFAULT_VISIBIL_PROP);

				btOpenDocument.setCaption("Open Document");
				btOpenDocument.setEnabled(true);
			}
			else if (value instanceof MarkupCollectionReference) {
				btOpenDocument.setCaption("Open Markup Collection");
				btOpenDocument.setEnabled(true);
				btEditContentInfo.setEnabled(true);
				if (value instanceof UserMarkupCollectionReference) {
					contentInfoForm.setEnabled(true);
					contentInfoForm.setItemDataSource(
						new BeanItem<ContentInfoSet>(
							new ContentInfoSet(
								((UserMarkupCollectionReference)value)
									.getContentInfoSet())));
					contentInfoForm.setVisibleItemProperties(DEFAULT_VISIBIL_PROP);

				}
				else {
					contentInfoForm.setEnabled(false);
				}
			}
			else {
				btEditContentInfo.setEnabled(false);
				contentInfoForm.setEnabled(false);
				contentInfoForm.setItemDataSource(
						new BeanItem<ContentInfoSet>(emptyContentInfoSet));
				contentInfoForm.setVisibleItemProperties(DEFAULT_VISIBIL_PROP);

				btOpenDocument.setEnabled(false);
			}
		}
		else {
			btEditContentInfo.setEnabled(false);
			contentInfoForm.setEnabled(false);
			contentInfoForm.setItemDataSource(
					new BeanItem<ContentInfoSet>(emptyContentInfoSet));
			contentInfoForm.setVisibleItemProperties(DEFAULT_VISIBIL_PROP);
			btOpenDocument.setEnabled(false);
		}
		contentInfoForm.setReadOnly(true);
	}

	
	public void close() {
		this.repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.sourceDocumentChanged,
				sourceDocumentChangedListener);
		this.repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.userMarkupCollectionChanged, 
				userMarkupDocumentChangedListener);
	}

	public void setSourceDocumentsFilter(Corpus corpus) {
		this.currentCorpus = corpus;
		documentsContainer.removeAllContainerFilters();
		if (corpus != null) {
			SourceDocumentFilter sdf = new SourceDocumentFilter(corpus);
			documentsContainer.addContainerFilter(sdf);
			if(documentsContainer.size() > 0) {
				documentsTree.setValue(documentsContainer.getIdByIndex(0));
			}
			else {
				documentsTree.setValue(null);
			}
		}
	}
	
	private void handleIntrinsicMarkupCollection(
			SourceDocument sourceDocument) throws MalformedURLException, IOException {
		//only XML supported so far
		if (sourceDocument.getSourceContentHandler() instanceof XMLContentHandler) {
			XmlMarkupCollectionSerializationHandler xmlMarkupCollectionSerializationHandler = 
				new XmlMarkupCollectionSerializationHandler(
					repository.getTagManager(), 
					sourceDocument.getID(),
					(XMLContentHandler)sourceDocument.getSourceContentHandler());
			
			try (InputStream is = 
				sourceDocument.getSourceContentHandler()
				.getSourceDocumentInfo()
				.getTechInfoSet()
				.getURI()
				.toURL()
				.openStream()) {
				repository.importUserMarkupCollection(
					is, 
					sourceDocument, 
					xmlMarkupCollectionSerializationHandler);
			}
			
		}
		
	}
}
