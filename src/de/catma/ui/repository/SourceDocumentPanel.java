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
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.TreeDragMode;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;

import de.catma.CatmaApplication;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.document.standoffmarkup.MarkupCollectionReference;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleValueDialog;
import de.catma.ui.dialog.UploadDialog;
import de.catma.ui.repository.wizard.AddSourceDocWizardFactory;
import de.catma.ui.repository.wizard.AddSourceDocWizardResult;
import de.catma.util.CloseSafe;
import de.catma.util.ContentInfoSet;
import de.catma.util.Pair;

public class SourceDocumentPanel extends HorizontalSplitPanel
	implements ValueChangeListener {
	
	private final ContentInfoSet emptyContentInfoSet = new ContentInfoSet();
	
	private HierarchicalContainer documentsContainer;
	private HashMap<String, SourceDocumentFilter> filters = 
			new HashMap<String, SourceDocumentFilter>();
	private Tree documentsTree;
	private Repository repository;
	private String userMarkupItemDisplayString = "User Markup Collections";
	private String staticMarkupItemDisplayString = "Static Markup Collections";
	private Button btOpenDocument;
	private Button btAddDocument;
	private MenuItem miMoreDocumentActions;
	private Form contentInfoForm;
	private Button btEditContentInfo;
	private Button btSaveContentInfoChanges;
	private Button btDiscardContentInfoChanges;
	
	private PropertyChangeListener sourceDocumentChangedListener;
	private PropertyChangeListener userMarkupDocumentChangedListener;
	
	public SourceDocumentPanel(Repository repository) {
		this.repository = repository;
		initComponents();
		initActions();
		initListeners();
	}

	private void initListeners() {
		sourceDocumentChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) { //insert
					SourceDocument sd = repository.getSourceDocument(
							(String)evt.getNewValue());
					addSourceDocumentToTree(sd);
				}
				else if (evt.getNewValue() == null) { //remove
					removeSourceDocumentFromTree(
						(SourceDocument)evt.getOldValue());
				}
				else { //update
					documentsTree.requestRepaint();
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
					documentsTree.requestRepaint();
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
		btAddDocument.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
				final AddSourceDocWizardResult wizardResult = 
						new AddSourceDocWizardResult();
				
				AddSourceDocWizardFactory factory = 
						new AddSourceDocWizardFactory(
								new WizardProgressListener() {
							
							public void wizardCompleted(WizardCompletedEvent event) {
								event.getWizard().removeListener(this);
								try {
									repository.insert(wizardResult.getSourceDocument());
								} catch (IOException e) {
									((CatmaApplication)getApplication()).showAndLogError(
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
				
				getApplication().getMainWindow().addWindow(
						sourceDocCreationWizardWindow);
				
				sourceDocCreationWizardWindow.center();
			}
		});
		
		btOpenDocument.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				final Object value = documentsTree.getValue();
				
				if (value instanceof SourceDocument) {
					((CatmaApplication)getApplication()).openSourceDocument(
							(SourceDocument)value, repository);
				}
				else if (value instanceof StaticMarkupCollectionReference) {
						//TODO: implement
					
				}
				else if (value instanceof UserMarkupCollectionReference) {
					final SourceDocument sd = 
							(SourceDocument)documentsTree.getParent(
									documentsTree.getParent(value));
					final CatmaApplication application = 
							(CatmaApplication)getApplication();
					application.submit(
							"Loading Markup collection...",
							new DefaultProgressCallable<UserMarkupCollection>() {
								public UserMarkupCollection call()
										throws Exception {
									UserMarkupCollectionReference 
										userMarkupCollectionReference = 
											(UserMarkupCollectionReference)value;
									
									return repository.getUserMarkupCollection(
											userMarkupCollectionReference);
								}
							},
							new ExecutionListener<UserMarkupCollection>() {
								public void done(UserMarkupCollection result) {
									application.openUserMarkupCollection(
											sd, result, repository);
								}
								
								public void error(Throwable t) {
									((CatmaApplication)getApplication()).showAndLogError(
											"Error loading markup collection!", t);
								}
							});

				}
			}
		});
		
		documentsTree.addListener(this);
		
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
		
		btDiscardContentInfoChanges.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(true);
				btSaveContentInfoChanges.setVisible(false);
				btDiscardContentInfoChanges.setVisible(false);
				contentInfoForm.discard();
				contentInfoForm.setReadOnly(true);				
			}
		});


	}

	private void handleSourceDocumentRemovalRequest() {
		Object value = documentsTree.getValue();
		
		if (value instanceof SourceDocument) {
			final SourceDocument sd = (SourceDocument)value;
			ConfirmDialog.show(
					getApplication().getMainWindow(), 
					"Do you really want to delete the Source Document '"
							+ sd.toString() + "' and all its markup collections?",
							
							new ConfirmDialog.Listener() {
						
						public void onClose(ConfirmDialog dialog) {
							if (dialog.isConfirmed()) {
								try {
									repository.delete(sd);
								} catch (IOException e) {
									((CatmaApplication)getApplication()).showAndLogError(
											"Error deleting the Source Document!", e);
								}
							}
						}
					});
			
		}
		else {
			getWindow().showNotification(
					"Information", "Please select a Source Document!");
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
						((AnalyzerProvider)getApplication()).analyze(
								c,
								(IndexedRepository)repository);
					}
					else {
						CorpusContentSelectionDialog dialog =
							new CorpusContentSelectionDialog(
								sd,
								new SaveCancelListener<Corpus>() {
									public void cancelPressed() {/* noop */}
									public void savePressed(Corpus result) {
										
										((AnalyzerProvider)getApplication()).analyze(
												result,
												(IndexedRepository)repository);
										
									}
								});
						dialog.show(getApplication().getMainWindow());
					}
				}
				else {
					getWindow().showNotification(
						"Information", "Please select a Source Document first!");
				}
			}			
		}
		else {
			getWindow().showNotification(
				"Information", 
				"This repository is not indexed, analysis is not supported!");
		}
		
	}

	private void initComponents() {
		setSplitPosition(70);
		addComponent(createOuterDocumentsPanel());
		addComponent(createContentInfoPanel());
	}
	
	private Component createOuterDocumentsPanel() {
		
		VerticalLayout outerDocumentsPanel = new VerticalLayout();
		outerDocumentsPanel.setSpacing(true);
		outerDocumentsPanel.setMargin(false, true, true, true);
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
		miMoreDocumentActions = 
				menuMoreDocumentActions.addItem("More actions...", null);
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
		documentsTree.setDragMode(TreeDragMode.NODE);
		
		documentsPanel.addComponent(documentsTree);
		documentsPanel.getContent().setSizeUndefined();
		documentsPanel.setSizeFull();
		
		for (SourceDocument sd : repository.getSourceDocuments()) {
			addSourceDocumentToTree(sd);
		}		
		
		return documentsPanel;
	}


	private void addSourceDocumentToTree(SourceDocument sd) {
		documentsTree.addItem(sd);

		documentsTree.setChildrenAllowed(sd, true);
		
		
		MarkupCollectionItem userMarkupItem =
				new MarkupCollectionItem(userMarkupItemDisplayString, true);

		documentsTree.addItem(userMarkupItem);
		documentsTree.setParent(userMarkupItem, sd);
	
		for (UserMarkupCollectionReference ucr : sd.getUserMarkupCollectionRefs()) {
			addUserMarkupCollectionReferenceToTree(ucr, userMarkupItem);
		}
		
		MarkupCollectionItem staticMarkupItem = 
				new MarkupCollectionItem(staticMarkupItemDisplayString);
		documentsTree.addItem(staticMarkupItem);
		documentsTree.setParent(staticMarkupItem, sd);
		
		for (StaticMarkupCollectionReference smcr : sd.getStaticMarkupCollectionRefs()) {
			documentsTree.addItem(smcr);
			documentsTree.setParent(smcr, staticMarkupItem);
			documentsTree.setChildrenAllowed(smcr, false);
		}
	}
	
	private void addUserMarkupCollectionReferenceToTree(
			UserMarkupCollectionReference ucr, MarkupCollectionItem userMarkupItem) {
		documentsTree.addItem(ucr);
		documentsTree.setParent(ucr, userMarkupItem);
		documentsTree.setChildrenAllowed(ucr, false);
	}


	private Component createContentInfoPanel() {
		VerticalLayout contentInfoPanel = new VerticalLayout();
		contentInfoPanel.setSpacing(true);
		contentInfoPanel.setSizeFull();
		contentInfoPanel.setMargin(false, false, true, true);
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
		contentInfoForm.setReadOnly(true);
		contentInfoForm.setEnabled(false);
		
		BeanItem<ContentInfoSet> contentInfoItem = 
				new BeanItem<ContentInfoSet>(emptyContentInfoSet);
		contentInfoForm.setItemDataSource(contentInfoItem);
		contentInfoForm.setVisibleItemProperties(new String[] {
				"title", "author", "description", "publisher"
		});
		
		contentInfoForm.setReadOnly(true);
		contentInfoPanel.addComponent(contentInfoForm);
		
		return contentInfoPanel;
	}
	
	private void removUserMarkupCollectionReferenceFromTree(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		documentsTree.removeItem(userMarkupCollectionReference);
	}

	private void handleUserMarkupCollectionImport() {
		Object value = documentsTree.getValue();
		if ((value == null) || !(value instanceof SourceDocument)) {
			 getWindow().showNotification(
                    "Information",
                    "Please select a Source Document first");
		}
		else{
			final SourceDocument sourceDocument = (SourceDocument)value;

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
						((CatmaApplication)getApplication()).showAndLogError(
							"Error importing the User Markup Collection!", e);
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
								((CatmaApplication)getApplication()).showAndLogError(
									"Error deleting the User Markup Collection!", e);
							}
		                }
		            }
		        });
		}
		
	}
	
	private void handleUserMarkupCollectionCreation() {
		Object value = documentsTree.getValue();
		if ((value == null) || !(value instanceof SourceDocument)) {
			 getWindow().showNotification(
                    "Information",
                    "Please select a Source Document first");
		}
		else{
			final SourceDocument sourceDocument = (SourceDocument)value;
			final String userMarkupCollectionNameProperty = "name";
			
			SingleValueDialog singleValueDialog = new SingleValueDialog();
			
			singleValueDialog.getSingleValue(
					getApplication().getMainWindow(),
					"Create a new User Markup Collection",
					"You have to enter a name!",
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
						((CatmaApplication)getApplication()).showAndLogError(
							"Error creating the User Markup Collection!", e);
					}
				}
			}, userMarkupCollectionNameProperty);

		}
		
	}

	private void addUserMarkupCollectionReferenceToTree(
			UserMarkupCollectionReference userMarkupCollRef, 
			SourceDocument sourceDocument) {
		
		@SuppressWarnings("unchecked")
		Collection<MarkupCollectionItem> children = 
				(Collection<MarkupCollectionItem>) documentsTree.getChildren(
						sourceDocument);
		
		for (MarkupCollectionItem mi : children) {
			if (mi.isUserMarkupCollectionItem()) {
				addUserMarkupCollectionReferenceToTree(userMarkupCollRef, mi);
				break;
			}
		}
		
	}
	
	public void valueChange(ValueChangeEvent event) {
		Object value = event.getProperty().getValue();
		if (value != null) {
			if (value instanceof SourceDocument) {
				contentInfoForm.setEnabled(true);
				contentInfoForm.setItemDataSource(
					new BeanItem<ContentInfoSet>(
						new ContentInfoSet(((SourceDocument)value).getSourceContentHandler()
							.getSourceDocumentInfo().getContentInfoSet())));
				
				btOpenDocument.setCaption("Open Document");
				btOpenDocument.setEnabled(true);
			}
			else if (value instanceof MarkupCollectionReference) {
				btOpenDocument.setCaption("Open Markup Collection");
				btOpenDocument.setEnabled(true);
				
				if (value instanceof UserMarkupCollectionReference) {
					contentInfoForm.setEnabled(true);
					contentInfoForm.setItemDataSource(
						new BeanItem<ContentInfoSet>(
							new ContentInfoSet(
								((UserMarkupCollectionReference)value)
									.getContentInfoSet())));

				}
				else {
					contentInfoForm.setEnabled(false);
				}
			}
			else {
				contentInfoForm.setEnabled(false);
				contentInfoForm.setItemDataSource(
						new BeanItem<ContentInfoSet>(emptyContentInfoSet));
				btOpenDocument.setEnabled(false);
			}
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
		documentsContainer.removeAllContainerFilters();
		if (corpus != null) {
		
			if (!filters.containsKey(corpus)) {
				filters.put(
					corpus.toString(), new SourceDocumentFilter(corpus));
			}
			SourceDocumentFilter sdf = filters.get(corpus.toString());
			documentsContainer.addContainerFilter(sdf);
			if(documentsContainer.size() > 0) {
				documentsTree.setValue(documentsContainer.getIdByIndex(0));
			}
		}
	}
}
