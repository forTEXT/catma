package de.catma.ui.repository;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;

import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

import de.catma.CleaApplication;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.ISourceDocument;
import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.document.standoffmarkup.MarkupCollectionReference;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.IUserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.tag.ITagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.dialog.FormDialog;
import de.catma.ui.dialog.PropertyCollection;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.UploadDialog;
import de.catma.ui.repository.wizard.WizardFactory;
import de.catma.ui.repository.wizard.WizardResult;
import de.catma.ui.repository.wizard.WizardWindow;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.util.CloseSafe;
import de.catma.util.Pair;


public class RepositoryView extends VerticalLayout implements ClosableTab {
	
	private static final class MarkupItem {
		private String displayString;
		private boolean userMarkupCollectionItem = false;

		public MarkupItem(String displayString) {
			this(displayString, false);
		}
		
		public MarkupItem(String displayString, boolean userMarkupCollectionItem) {
			this.displayString = displayString;
			this.userMarkupCollectionItem = userMarkupCollectionItem;
		}

		@Override
		public String toString() {
			return displayString;
		}
		
		public boolean isUserMarkupCollectionItem() {
			return userMarkupCollectionItem;
		}
	}
	
	private String userMarkupItemDisplayString = "User Markup Collections";
	private String staticMarkupItemDisplayString = "Static Markup Collections";
	
	private Repository repository;
	private Tree documentsTree;
	private Tree corporaTree;
	private Tree tagLibrariesTree;
	private Form contentInfoForm;
	private String allDocuments = "All documents";
	private HierarchicalContainer documentsContainer;
	private HashMap<String, SourceDocumentFilter> filters = 
			new HashMap<String, SourceDocumentFilter>();
	
	private Button btOpenDocument;
	private Button btAddDocument;
	private MenuItem miMoreDocumentActions;
	
	private Button btCreateCorpus;
	private MenuItem miMoreCorpusActions;
	private MenuItem miRemoveCorpus;
	
	private Button btOpenTagLibrary;
	private Button btCreateTagLibrary;
	private MenuItem miMoreTagLibraryActions;
	
	private Button btEditContentInfo;
	private Button btSaveContentInfoChanges;
	private Button btDiscardContentInfoChanges;
	private PropertyChangeListener sourceDocumentAddedListener;
	private PropertyChangeListener userMarkupDocumentChangedListener;
	private PropertyChangeListener tagLibraryChangedListener;
	
	public RepositoryView(Repository repository) {

		this.repository = repository;
		initComponents();

		sourceDocumentAddedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				ISourceDocument sd = RepositoryView.this.repository.getSourceDocument(
						(String)evt.getNewValue());
				addSourceDocumentToTree(sd);
			}
		};
		this.repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.sourceDocumentChanged,
				sourceDocumentAddedListener);
		
		userMarkupDocumentChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) {
					@SuppressWarnings("unchecked")
					Pair<UserMarkupCollectionReference, ISourceDocument>
						result = (Pair<UserMarkupCollectionReference, ISourceDocument>)evt.getNewValue();
					addUserMarkupCollectionReferenceToTree(
							result.getFirst(), result.getSecond());
				}
				else if (evt.getNewValue() == null) {
					UserMarkupCollectionReference userMarkupCollectionReference =
							(UserMarkupCollectionReference) evt.getOldValue();
					removUserMarkupCollectionReferenceFromTree(
							userMarkupCollectionReference);
				}
				
			}
		};
		this.repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.userMarkupCollectionChanged,
				userMarkupDocumentChangedListener);
		
		tagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) {
					TagLibraryReference tagLibraryRef = 
							(TagLibraryReference)evt.getNewValue();
					addTagLibraryReferenceToTree(tagLibraryRef);
				}
				else if (evt.getNewValue() == null) {
					TagLibraryReference tagLibraryRef = 
							(TagLibraryReference)evt.getOldValue();
					tagLibrariesTree.removeItem(tagLibraryRef);
				}
			}
		};
		this.repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.tagLibraryChanged, 
				tagLibraryChangedListener);
		
		initActions();
	}

	private void initActions() {
		
		btCreateTagLibrary.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				final String nameProperty = "name";
				getNameFromUser(
						"Create a new Tag Library",
						new SaveCancelListener<PropertysetItem>() {
					public void cancelPressed() {}
					public void savePressed(
							PropertysetItem propertysetItem) {
						Property property = 
								propertysetItem.getItemProperty(
										nameProperty);
						String name = (String)property.getValue();
						try {
							repository.createTagLibrary(name);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}, nameProperty);
			}
		});
		
		btAddDocument.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				WizardFactory factory = new WizardFactory();
				final WizardResult wizardResult = new WizardResult();
				WizardWindow sourceDocCreationWizardWindow = 
						factory.createWizardWindow(new WizardProgressListener() {
					
					public void wizardCompleted(WizardCompletedEvent event) {
						event.getWizard().removeListener(this);
						try {
							repository.insert(wizardResult.getSourceDocument());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
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
				getApplication().getMainWindow().addWindow(sourceDocCreationWizardWindow);
				sourceDocCreationWizardWindow.center();
			}
		});
		
		btOpenDocument.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				final Object value = documentsTree.getValue();
				
				if (value instanceof ISourceDocument) {
					((CleaApplication)getApplication()).openSourceDocument(
							(ISourceDocument)value, repository);
				}
				else if (value instanceof StaticMarkupCollectionReference) {
						//TODO: implement
					
				}
				else if (value instanceof UserMarkupCollectionReference) {
					final ISourceDocument sd = 
							(ISourceDocument)documentsTree.getParent(
									documentsTree.getParent(value));
					final CleaApplication application = 
							(CleaApplication)getApplication();
					application.submit(
							new DefaultProgressCallable<IUserMarkupCollection>() {
								public IUserMarkupCollection call()
										throws Exception {
									getProgressListener().setProgress("Loading Markup Collection");
									UserMarkupCollectionReference 
										userMarkupCollectionReference = 
											(UserMarkupCollectionReference)value;
									
									return repository.getUserMarkupCollection(
											userMarkupCollectionReference);
								}
							},
							new ExecutionListener<IUserMarkupCollection>() {
								public void done(IUserMarkupCollection result) {
									application.openUserMarkupCollection(
											sd, result, repository);
								}
								
								public void error(Throwable t) {
									t.printStackTrace();
									// TODO Auto-generated method stub
									
								}
							});

				}
			}
		});
		
		corporaTree.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				Object value = event.getProperty().getValue();
				boolean corpusRemoveButtonEnabled = false;
				if (value != null) {
					documentsContainer.removeAllContainerFilters();
					
					if (!value.equals(allDocuments)) {
						Corpus corpus = (Corpus)value;
						
						if (!filters.containsKey(value)) {
							filters.put(
								corpus.toString(), new SourceDocumentFilter(corpus));
						}
						SourceDocumentFilter sdf = filters.get(corpus.toString());
						documentsContainer.addContainerFilter(sdf);
						if(documentsContainer.size() > 0) {
							documentsTree.setValue(documentsContainer.getIdByIndex(0));
						}
						corpusRemoveButtonEnabled = true;
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
		
		documentsTree.addListener(new ValueChangeListener() {
			//TODO: still messy:
			public void valueChange(ValueChangeEvent event) {
				Object value = event.getProperty().getValue();
				if (value != null) {
					if (value instanceof ISourceDocument) {
						contentInfoForm.setEnabled(true);
						contentInfoForm.setItemDataSource(
							new BeanItem<ContentInfo>(
								new StandardContentInfo((ISourceDocument)value)));
						contentInfoForm.setReadOnly(true);
						btOpenDocument.setCaption("Open Document");
						btOpenDocument.setEnabled(true);
						
					}
					else {
						if (value instanceof MarkupCollectionReference) {
							btOpenDocument.setCaption("Open Markup Collection");
							btOpenDocument.setEnabled(true);
							if (value instanceof UserMarkupCollectionReference) {
								contentInfoForm.setEnabled(true);
								contentInfoForm.setItemDataSource(
									new BeanItem<ContentInfo>(
										new StandardContentInfo((UserMarkupCollectionReference)value)));

							}
							else {
								contentInfoForm.setEnabled(false);
							}
						}
						else {
							contentInfoForm.setEnabled(false);
							btOpenDocument.setEnabled(false);
						}
					}
				
				}
			}
			
		});
		
		tagLibrariesTree.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				btOpenTagLibrary.setEnabled(event.getProperty().getValue()!=null);
			}
		});
		
		btOpenTagLibrary.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Object value = tagLibrariesTree.getValue();
				if (value != null) {
					TagLibraryReference tagLibraryReference = 
							(TagLibraryReference)value;
					ITagLibrary tagLibrary;
					try {
						tagLibrary = repository.getTagLibrary(tagLibraryReference);
						((CleaApplication)getApplication()).openTagLibrary(tagLibrary);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}				
			}
		});

		miMoreDocumentActions.addItem("Analyze Document", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				// TODO Auto-generated method stub
				
			}
		});
		
		miMoreDocumentActions.addItem("Remove Document", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				// TODO Auto-generated method stub
				
			}
		});
		
		miMoreDocumentActions.addItem("Export Document", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				// TODO Auto-generated method stub
				
			}
		});
		
		miMoreDocumentActions.addSeparator();
		
		miMoreDocumentActions.addItem("Create User Markup Collection", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleUserMarkupCollectionCreation();
			}

		});
		
		miMoreDocumentActions.addItem("Import User Markup Collection", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleUserMarkupCollectionImport();
			}
		});
		
		miMoreDocumentActions.addItem("Export User Markup Collection", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				// TODO Auto-generated method stub
				
			}
		});
		
		miMoreDocumentActions.addItem("Remove User Markup Collection", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				handleUserMarkupRemoval();
			}

		});
		
		miMoreDocumentActions.addSeparator();
		
		miMoreDocumentActions.addItem("Create Static Markup Collection", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				// TODO Auto-generated method stub
				
			}
		});
		
		miMoreDocumentActions.addItem("Import Static Markup Collection", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				// TODO Auto-generated method stub
				
			}
		});	
		
		miMoreDocumentActions.addItem("Export Static Markup Collection", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				// TODO Auto-generated method stub
				
			}
		});	
		
		
		miMoreDocumentActions.addItem("Remove Static Markup Collection", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				// TODO Auto-generated method stub
				
			}
		});	
		
		btEditContentInfo.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(false);
				btSaveContentInfoChanges.setVisible(true);
				btDiscardContentInfoChanges.setVisible(true);
				contentInfoForm.setReadOnly(false);
			}
		});
		
		btSaveContentInfoChanges.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(true);
				btSaveContentInfoChanges.setVisible(false);
				btDiscardContentInfoChanges.setVisible(false);
				contentInfoForm.commit();
				contentInfoForm.setReadOnly(true);				
			}
		});
		
		btDiscardContentInfoChanges.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(true);
				btSaveContentInfoChanges.setVisible(false);
				btDiscardContentInfoChanges.setVisible(false);
				contentInfoForm.discard();
				contentInfoForm.setReadOnly(true);				
			}
		});

		miMoreTagLibraryActions.addItem("Import Tag Library", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleTagLibraryImport();
			}
		});
		
		miMoreTagLibraryActions.addItem("Export Tag Library", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				// TODO Auto-generated method stub
				
			}
		});
		
		miMoreTagLibraryActions.addItem("Remove Tag Library", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleTagLibraryRemoval();
			}
		});
	}
	
	private void removUserMarkupCollectionReferenceFromTree(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		documentsTree.removeItem(userMarkupCollectionReference);
	}

	private void handleUserMarkupCollectionImport() {
		Object value = documentsTree.getValue();
		if ((value == null) || !(value instanceof ISourceDocument)) {
			 getWindow().showNotification(
                    "Information",
                    "Please select a Source Document first");
		}
		else{
			final ISourceDocument sourceDocument = (ISourceDocument)value;

			UploadDialog uploadDialog =
					new UploadDialog("Upload User Markup Collection", 
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					finally {
						CloseSafe.close(is);
					}
				}
				
			});
			uploadDialog.show(getApplication().getMainWindow());		
		}
	}

	private void handleUserMarkupRemoval() {
		Object selValue = documentsTree.getValue();
		if ((selValue != null) 
				&& (selValue instanceof UserMarkupCollectionReference)) {
			final UserMarkupCollectionReference userMarkupCollectionReference =
					(UserMarkupCollectionReference) selValue;
			
			ConfirmDialog.show(
				getApplication().getMainWindow(), 
				"Do you really want to delete the User Markup Collection '"
						+ userMarkupCollectionReference.toString() + "'?",
						
		        new ConfirmDialog.Listener() {

		            public void onClose(ConfirmDialog dialog) {
		                if (dialog.isConfirmed()) {
		                	try {
								repository.delete(userMarkupCollectionReference);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		                }
		            }
		        });
		}
		
	}
	
	private void handleTagLibraryRemoval() {
		final TagLibraryReference tagLibraryReference = 
				(TagLibraryReference) tagLibrariesTree.getValue();
		
		if (tagLibraryReference != null) {
			ConfirmDialog.show(
				getApplication().getMainWindow(), 
				"Do you really want to delete Tag Library '"
						+ tagLibraryReference.toString() + "'?",
		        new ConfirmDialog.Listener() {

		            public void onClose(ConfirmDialog dialog) {
		                if (dialog.isConfirmed()) {
		                	try {
								repository.delete(tagLibraryReference);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		                }
		            }
		        });
		}
		
	}

	private void handleTagLibraryImport() {
		UploadDialog uploadDialog =
				new UploadDialog("Upload Tag Library", 
						new SaveCancelListener<byte[]>() {
			
			public void cancelPressed() {}
			
			public void savePressed(byte[] result) {
				InputStream is = new ByteArrayInputStream(result);
				try {
					if (BOMFilterInputStream.hasBOM(result)) {
						is = new BOMFilterInputStream(
								is, Charset.forName("UTF-8"));
					}
					
					repository.importTagLibrary(is);
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally {
					CloseSafe.close(is);
				}
			}
			
		});
		uploadDialog.show(getApplication().getMainWindow());
	}

	private void initComponents() {
		setSizeFull();
		this.setMargin(false, true, true, true);
		this.setSpacing(true);

		Component documentsLabel = createDocumentsLabel();
		addComponent(documentsLabel);
		
		Component documentsManagerPanel = createDocumentsManagerPanel();
		addComponent(documentsManagerPanel);
		setExpandRatio(documentsManagerPanel, 0.6f);
		
		Component tagLibraryContainer = createTagLibraryContainer();
		addComponent(tagLibraryContainer);
		setExpandRatio(tagLibraryContainer, 0.4f);
	}

	private Component createTagLibraryContainer() {
		
		VerticalLayout tagLibraryContainer = new VerticalLayout();
		tagLibraryContainer.setSpacing(true);
		tagLibraryContainer.setSizeFull();
		Component tagLibraryPanel = createTagLibraryPanel();
		tagLibraryContainer.addComponent(tagLibraryPanel);
		tagLibraryContainer.setExpandRatio(tagLibraryPanel, 1.0f);
		tagLibraryContainer.addComponent(createTagLibraryButtonPanel());
		
		return tagLibraryContainer;
	}

	private Component createTagLibraryButtonPanel() {
		
		HorizontalLayout tagLibraryButtonPanel = new HorizontalLayout();
		tagLibraryButtonPanel.setSpacing(true);
		
		btOpenTagLibrary = new Button("Open Tag Library");
		btOpenTagLibrary.setEnabled(false);
		tagLibraryButtonPanel.addComponent(btOpenTagLibrary);

		btCreateTagLibrary = new Button("Create Tag Library");
		tagLibraryButtonPanel.addComponent(btCreateTagLibrary);
		
		MenuBar menuMoreTagLibraryActions = new MenuBar();
		miMoreTagLibraryActions = menuMoreTagLibraryActions.addItem("More actions...", null);
		tagLibraryButtonPanel.addComponent(menuMoreTagLibraryActions);
		
		return tagLibraryButtonPanel;
		
	}

	private Component createTagLibraryPanel() {

		Panel tagLibraryPanel = new Panel();
		tagLibraryPanel.getContent().setSizeUndefined();
		tagLibraryPanel.setSizeFull();
		
		tagLibrariesTree = new Tree();
		tagLibrariesTree.setCaption("Tag Libraries");
		tagLibrariesTree.addStyleName("repo-tree");
		tagLibrariesTree.setImmediate(true);
		tagLibrariesTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_ID);
		
		for (TagLibraryReference tlr : repository.getTagLibraryReferences()) {
			addTagLibraryReferenceToTree(tlr);
		}
		
		tagLibraryPanel.addComponent(tagLibrariesTree);
		
		return tagLibraryPanel;
	}
	
	private void addTagLibraryReferenceToTree(TagLibraryReference tlr) {
		tagLibrariesTree.addItem(tlr);
		tagLibrariesTree.setChildrenAllowed(tlr, false);
	}

	private Component createDocumentsManagerPanel() {
		
		HorizontalLayout documentsManagerPanel = new HorizontalLayout();
		documentsManagerPanel.setSpacing(true);
		documentsManagerPanel.setSizeFull();
		documentsManagerPanel.setMargin(false, false, true, false);
		
		Component outerCorporaPanel = createOuterCorporaPanel();
		documentsManagerPanel.addComponent(outerCorporaPanel);
		documentsManagerPanel.setExpandRatio(outerCorporaPanel, 1.3f);
		
		Component outerDocumentsPanel = createOuterDocumentsPanel();
		documentsManagerPanel.addComponent(outerDocumentsPanel);
		documentsManagerPanel.setExpandRatio(outerDocumentsPanel, 2.0f);
		
		Component contentInfoPanel = createContentInfoPanel();
		documentsManagerPanel.addComponent(contentInfoPanel);
		documentsManagerPanel.setExpandRatio(contentInfoPanel, 1.0f);
	
		return documentsManagerPanel;
	}

	private Component createContentInfoPanel() {
		VerticalLayout contentInfoPanel = new VerticalLayout();
		contentInfoPanel.setSpacing(true);
		contentInfoPanel.setSizeFull();
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
		contentInfoButtonsPanel.addComponent(btEditContentInfo);
		btSaveContentInfoChanges = new Button("Save");
		btSaveContentInfoChanges.setVisible(false);
		contentInfoButtonsPanel.addComponent(btSaveContentInfoChanges);
		btDiscardContentInfoChanges = new Button("Discard");
		btDiscardContentInfoChanges.setVisible(false);
		contentInfoButtonsPanel.addComponent(btDiscardContentInfoChanges);
		
		return contentInfoButtonsPanel;
	}

	private Component createContentInfoForm() {
		
		Panel contentInfoPanel = new Panel();
		contentInfoPanel.getContent().setSizeUndefined();
		contentInfoPanel.getContent().setWidth("100%");
		contentInfoPanel.setSizeFull();
		
		contentInfoForm = new Form();
		contentInfoForm.setSizeFull();
		contentInfoForm.setCaption("Information");
		contentInfoForm.setWriteThrough(false);
		
		BeanItem<ContentInfo> contentInfoItem = 
				new BeanItem<ContentInfo>(new StandardContentInfo());
		contentInfoForm.setItemDataSource(contentInfoItem);
		contentInfoForm.setVisibleItemProperties(new String[] {
				"title", "author", "description", "publisher"
		});
		
		contentInfoForm.setReadOnly(true);
		contentInfoPanel.addComponent(contentInfoForm);
		
		return contentInfoPanel;
	}

	private Component createOuterDocumentsPanel() {
		
		VerticalLayout outerDocumentsPanel = new VerticalLayout();
		outerDocumentsPanel.setSpacing(true);
		outerDocumentsPanel.setSizeFull();
		
		Component documentsPanel = createDocumentsPanel();
		outerDocumentsPanel.addComponent(documentsPanel);
		outerDocumentsPanel.setExpandRatio(documentsPanel, 1.0f);
		outerDocumentsPanel.addComponent(createDocumentButtonsPanel());
		
		return outerDocumentsPanel;
	}

	private Component createDocumentButtonsPanel() {
		
		Panel documentButtonsPanel = new Panel(new HorizontalLayout());
		documentButtonsPanel.setStyleName(Reindeer.PANEL_LIGHT);

		((HorizontalLayout)documentButtonsPanel.getContent()).setSpacing(true);
		
		btOpenDocument = new Button("Open Document");
		documentButtonsPanel.addComponent(btOpenDocument);
		btAddDocument = new Button("Add Document");
		documentButtonsPanel.addComponent(btAddDocument);

		MenuBar menuMoreDocumentActions = new MenuBar();
		miMoreDocumentActions = menuMoreDocumentActions.addItem("More actions...", null);
		documentButtonsPanel.addComponent(menuMoreDocumentActions);
		
		return documentButtonsPanel;
	}

	private Component createDocumentsPanel() {
		
		Panel documentsPanel = new Panel();
		
		documentsContainer = new HierarchicalContainer();
		documentsTree = new Tree();
		documentsTree.setContainerDataSource(documentsContainer);
		documentsTree.setCaption("Documents");
		documentsTree.addStyleName("repo-tree");
		documentsTree.setImmediate(true);
		documentsTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_ID);

		documentsPanel.addComponent(documentsTree);
		documentsPanel.getContent().setSizeUndefined();
		documentsPanel.setSizeFull();
		
		for (ISourceDocument sd : repository.getSourceDocuments()) {
			addSourceDocumentToTree(sd);
		}		
		
		return documentsPanel;
	}

	private Component createOuterCorporaPanel() {
		
		VerticalLayout outerCorporaPanel = new VerticalLayout();
		outerCorporaPanel.setSpacing(true);
		outerCorporaPanel.setSizeFull();
		Component corporaPanel = createCorporaPanel();
		outerCorporaPanel.addComponent(corporaPanel);
		outerCorporaPanel.setExpandRatio(corporaPanel, 1.0f);
		outerCorporaPanel.addComponent(createCorporaButtonPanel());
		
		return outerCorporaPanel;
	}

	private Component createCorporaButtonPanel() {
		
		Panel corporaButtonsPanel = new Panel(new HorizontalLayout());
		corporaButtonsPanel.setStyleName(Reindeer.PANEL_LIGHT);
		((HorizontalLayout)corporaButtonsPanel.getContent()).setSpacing(true);
		
		btCreateCorpus = new Button("Create Corpus");
		
		corporaButtonsPanel.addComponent(btCreateCorpus);
		MenuBar menuMoreCorpusActions = new MenuBar();
		miMoreCorpusActions = menuMoreCorpusActions.addItem("More actions...", null);
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

	private Component createDocumentsLabel() {
		Label documentsLabel = new Label("Document Manager");
		documentsLabel.addStyleName("repo-title-label");
		return documentsLabel;
	}

	public Repository getRepository() {
		return repository;
	}
	
	@Override
	public void detach() {
		this.repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.sourceDocumentChanged,
				sourceDocumentAddedListener);
		this.repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.userMarkupCollectionChanged, 
				userMarkupDocumentChangedListener);
		super.detach();
	}
	
	private void addSourceDocumentToTree(ISourceDocument sd) {
		documentsTree.addItem(sd);
		documentsTree.getItem(sd);
		documentsTree.setChildrenAllowed(sd, true);
		
		
		MarkupItem userMarkupItem =
				new MarkupItem(userMarkupItemDisplayString, true);

		documentsTree.addItem(userMarkupItem);
		documentsTree.setParent(userMarkupItem, sd);
	
		for (UserMarkupCollectionReference ucr : sd.getUserMarkupCollectionRefs()) {
			addUserMarkupCollectionReferenceToTree(ucr, userMarkupItem);
		}
		
		MarkupItem staticMarkupItem = 
				new MarkupItem(staticMarkupItemDisplayString);
		documentsTree.addItem(staticMarkupItem);
		documentsTree.setParent(staticMarkupItem, sd);
		
		for (StaticMarkupCollectionReference smcr : sd.getStaticMarkupCollectionRefs()) {
			documentsTree.addItem(smcr);
			documentsTree.setParent(smcr, staticMarkupItem);
			documentsTree.setChildrenAllowed(smcr, false);
		}
	}
	
	private void addUserMarkupCollectionReferenceToTree(
			UserMarkupCollectionReference ucr, MarkupItem userMarkupItem) {
		documentsTree.addItem(ucr);
		documentsTree.setParent(ucr, userMarkupItem);
		documentsTree.setChildrenAllowed(ucr, false);
	}
	
	
	private void addUserMarkupCollectionReferenceToTree(
			UserMarkupCollectionReference userMarkupCollRef, 
			ISourceDocument sourceDocument) {
		
		@SuppressWarnings("unchecked")
		Collection<MarkupItem> children = 
				(Collection<MarkupItem>) documentsTree.getChildren(sourceDocument);
		
		for (MarkupItem mi : children) {
			if (mi.isUserMarkupCollectionItem()) {
				addUserMarkupCollectionReferenceToTree(userMarkupCollRef, mi);
				break;
			}
		}
		
	}

	private void handleUserMarkupCollectionCreation() {
		Object value = documentsTree.getValue();
		if ((value == null) || !(value instanceof ISourceDocument)) {
			 getWindow().showNotification(
                    "Information",
                    "Please select a Source Document first");
		}
		else{
			final ISourceDocument sourceDocument = (ISourceDocument)value;
			final String userMarkupCollectionNameProperty = "name";
			getNameFromUser(
					"Create new User Markup Collection",
					new SaveCancelListener<PropertysetItem>() {
				public void cancelPressed() {}
				public void savePressed(
						PropertysetItem propertysetItem) {
					Property property = 
							propertysetItem.getItemProperty(
									userMarkupCollectionNameProperty);
					String name = (String)property.getValue();
					try {
						repository.createUserMarkupCollection(
								name, sourceDocument);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, userMarkupCollectionNameProperty);

		}
		
	}

	private void getNameFromUser(
			String dialogCaption,
			SaveCancelListener<PropertysetItem> listener, 
			String nameProperty) {
		PropertyCollection propertyCollection = 
				new PropertyCollection(nameProperty);

		FormDialog userMarkupCollFormDialog =
			new FormDialog(
				dialogCaption,
				propertyCollection,
				listener);
	
		userMarkupCollFormDialog.getField(
				nameProperty).setRequired(true);
		userMarkupCollFormDialog.getField(
				nameProperty).setRequiredError(
						"You have to enter a name!");
		userMarkupCollFormDialog.show(getApplication().getMainWindow());
	}
	
	public void close() {
		//TODO: fire event to notify analyzer and tagger components
		repository.close();
	}
}


