package de.catma.ui.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.components.grid.MultiSelectionModel;
import com.vaadin.ui.components.grid.MultiSelectionModel.SelectAllCheckBoxVisibility;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.dialog.SingleValueDialog;
import de.catma.ui.repository.Messages;
import de.catma.ui.repository.wizard.AddSourceDocWizardFactory;
import de.catma.ui.repository.wizard.AddSourceDocWizardResult;
import de.catma.ui.repository.wizard.SourceDocumentResult;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tagmanager.TagsetTree;

public class ProjectView extends HorizontalSplitPanel implements ClosableTab {
	
	private Button btAddSourceDocuments;
	private Repository project;
	private Grid<SourceDocument> docGrid;
	private Grid<UserMarkupCollectionReference> collectionGrid;
	private Button btOpenSourceDocuments;
	private Button btOpenCollections;
	private Button btCreateCollections;
	private TagsetTree tagsetTree;

	public ProjectView(
			ProjectManager projectManager, ProjectReference projectReference) {
		projectManager.openProject(
			projectReference, 
			new OpenProjectListener() {
				
				@Override
				public void ready(Repository project) {
					try {
						ProjectView.this.project = project;
						addProjectListener();
						initComponents();
						initActions();
						initData();
					} catch (IOException e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError("Error loading Project", e);
					}
				}
				
				@Override
				public void progress(String msg, Object... params) {
					Logger.getLogger(ProjectView.class.getName()).info(msg);
					
				}
				
				@Override
				public void failure(Throwable t) {
					t.printStackTrace();
				}
			},
			(BackgroundServiceProvider)UI.getCurrent()); 
	}
	
	private void addProjectListener() {
		project.addPropertyChangeListener(RepositoryChangeEvent.exceptionOccurred, changeEvent -> {
			Exception e = (Exception) changeEvent.getNewValue();
			((CatmaApplication)UI.getCurrent()).showAndLogError("Unexpected Project Error", e);
		});
	}

	private void initData() {
		DataProvider<SourceDocument, String> sourceDocumentProvider = 
			DataProvider.fromFilteringCallbacks(
				query -> {
					try {
						return project.getSourceDocuments().stream();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return Stream.empty();
					}
				},
				query -> {
					try {
						return project.getSourceDocuments().size();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return 0;
					}
				});
		docGrid.setDataProvider(sourceDocumentProvider);
		
		DataProvider<UserMarkupCollectionReference, String> collectionProvider =
			DataProvider.fromFilteringCallbacks(
				query -> {
					try {
						return project.getUserMarkupCollectionReferences(query.getOffset(), query.getLimit()).stream();
					}
					catch (Exception e) {
						e.printStackTrace();//TODO:
						return Stream.empty();
					}
				},
				query -> {
					try {
						return project.getUserMarkupCollectionReferenceCount();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return 0;
					}
				}
			);
		
		collectionGrid.setDataProvider(collectionProvider);
		
		try {
			this.tagsetTree.setTagLibrary(project.getTagLibrary(null));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void initActions() {
		btAddSourceDocuments.addClickListener(event -> {
			
			final AddSourceDocWizardResult wizardResult = 
					new AddSourceDocWizardResult();
			
			AddSourceDocWizardFactory factory = 
				new AddSourceDocWizardFactory(
						new WizardProgressListener() {
					
					public void wizardCompleted(WizardCompletedEvent event) {
						event.getWizard().removeListener(this);
//						final boolean generateStarterKit = repository.getSourceDocuments().isEmpty();
						try {
							for(SourceDocumentResult sdr : wizardResult.getSourceDocumentResults()){
								final SourceDocument sourceDocument = sdr.getSourceDocument();
								
								project.addPropertyChangeListener(
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
											
												
											project.removePropertyChangeListener(
												RepositoryChangeEvent.sourceDocumentChanged, 
												this);
											
											//TODO:
//											if (currentCorpus != null) {
//												try {
//													repository.update(currentCorpus, sourceDocument);
//													setSourceDocumentsFilter(currentCorpus);
//													
//												} catch (IOException e) {
//													((CatmaApplication)UI.getCurrent()).showAndLogError(
//														Messages.getString("SourceDocumentPanel.errorAddingSourceDocToCorpus"), e); //$NON-NLS-1$
//												}
//												
//											}
											
											//TODO:
//											if (sourceDocument
//													.getSourceContentHandler()
//													.hasIntrinsicMarkupCollection()) {
//												try {
//													handleIntrinsicMarkupCollection(sourceDocument);
//												} catch (IOException e) {
//													((CatmaApplication)UI.getCurrent()).showAndLogError(
//														Messages.getString("SourceDocumentPanel.errorExtratingIntrinsicAnnotations"), e); //$NON-NLS-1$
//												}
//											}
											
//											if (generateStarterKit) {
//												generateStarterKit(sourceDocument);
//											}
										}
									});

								project.insert(sourceDocument);
								docGrid.getDataProvider().refreshAll(); //TODO: all seems overkill here
							}
							
						} catch (Exception e) {
							((CatmaApplication)UI.getCurrent()).showAndLogError(
								Messages.getString("SourceDocumentPanel.errorAddingSourceDoc"), e); //$NON-NLS-1$
						}
					}

					public void wizardCancelled(WizardCancelledEvent event) {
						event.getWizard().removeListener(this);
					}
					
					public void stepSetChanged(WizardStepSetChangedEvent event) {/*not needed*/}
					
					public void activeStepChanged(WizardStepActivationEvent event) {/*not needed*/}
				}, 
				wizardResult,
				project);
			
			Window sourceDocCreationWizardWindow = 
					factory.createWizardWindow(
							Messages.getString("SourceDocumentPanel.addNewSourceDoc"), "85%",  "98%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			UI.getCurrent().addWindow(
					sourceDocCreationWizardWindow);
			
			sourceDocCreationWizardWindow.center();
			
		});
		
		btOpenSourceDocuments.addClickListener(event -> {
			Set<SourceDocument> selectedSourceDocs = docGrid.getSelectedItems();
			if (selectedSourceDocs.isEmpty()) {
				Notification.show("Info", "Please select one or more Documents first!", Type.TRAY_NOTIFICATION);
			}
			else {
				selectedSourceDocs.forEach(
					sourceDoc -> ((CatmaApplication)UI.getCurrent()).openSourceDocument(sourceDoc, project));
			}
		});
		
		btCreateCollections.addClickListener(event -> {
			Set<SourceDocument> selectedSourceDocs = docGrid.getSelectedItems();
			if (selectedSourceDocs.isEmpty()) {
				Notification.show("Info", "Please select one or more Documents first!", Type.TRAY_NOTIFICATION);
			}
			else {
				SingleValueDialog collectionNameDlg = 
						new SingleValueDialog();
				collectionNameDlg.getSingleValue(
						"Create Annotation Collections", 
						"Please enter a name pattern for the Colleciton!",
						namePattern -> createCollections(
								selectedSourceDocs, 
								namePattern.getItemProperty("name").getValue().toString()), 
						"name");
			}
		});
	}

	private void createCollections(Set<SourceDocument> selectedSourceDocs, String namePattern) {
		for (SourceDocument sourceDocument : selectedSourceDocs) {
			project.createUserMarkupCollection(namePattern, sourceDocument);
		}
	}

	private void initComponents() throws IOException {
		setSizeFull();
		setSplitPosition(65f, Unit.PERCENTAGE);
		
		HorizontalSplitPanel leftCenterPanel = new HorizontalSplitPanel();
		leftCenterPanel.setSizeFull();
		addComponent(leftCenterPanel);
		
		VerticalLayout leftPanel = new VerticalLayout();
		leftPanel.setSizeFull();
		leftPanel.setSpacing(true);
		leftPanel.setMargin(new MarginInfo(false, true, false, false));
		leftCenterPanel.addComponent(leftPanel);
		
		docGrid = new Grid<SourceDocument>("Documents");
		((MultiSelectionModel<SourceDocument>)docGrid
			.setSelectionMode(SelectionMode.MULTI))
			.setSelectAllCheckBoxVisibility(SelectAllCheckBoxVisibility.VISIBLE); //TODO: check how this behaves with large datasets
		docGrid.setSizeFull();
		
		leftPanel.addComponent(docGrid);
		leftPanel.setExpandRatio(docGrid, 1.0f);
		
		docGrid
			.addColumn(
				sourceDoc -> 
					sourceDoc
						.getSourceContentHandler().getSourceDocumentInfo()
						.getContentInfoSet().getTitle())
			.setCaption("Title");
		
		HorizontalLayout docButtonPanel = new HorizontalLayout();
		docButtonPanel.setSpacing(true);
		leftPanel.addComponent(docButtonPanel);

		btOpenSourceDocuments = new Button("Open Documents");
		docButtonPanel.addComponent(btOpenSourceDocuments);
		
		btAddSourceDocuments = new Button("Add Documents");
		docButtonPanel.addComponent(btAddSourceDocuments);
		
		
		
		VerticalLayout centerPanel = new VerticalLayout();
		centerPanel.setSpacing(true);
		centerPanel.setSizeFull();
		centerPanel.setMargin(new MarginInfo(false, true, false, true));

		leftCenterPanel.addComponent(centerPanel);
		
		collectionGrid = new Grid<UserMarkupCollectionReference>("Annotations");
		collectionGrid.addColumn(collection -> collection.getName()).setCaption("Title");
		
		collectionGrid.setSizeFull();
		
		centerPanel.addComponent(collectionGrid);
		centerPanel.setExpandRatio(collectionGrid, 1.0f);
		
		HorizontalLayout collectionButtonPanel = new HorizontalLayout();
		collectionButtonPanel.setSpacing(true);
		centerPanel.addComponent(collectionButtonPanel);
		
		btOpenCollections = new Button("Open Annotations");
		collectionButtonPanel.addComponent(btOpenCollections);
		
		btCreateCollections = new Button("Create Annotation Collections");
		collectionButtonPanel.addComponent(btCreateCollections);
		
		tagsetTree = new TagsetTree(project.getTagManager(), null);
		tagsetTree.setSizeFull();
		
		addComponent(tagsetTree);
	}

	@Override
	public void addClickshortCuts() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeClickshortCuts() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		if (project != null) {
			project.close();
		}
	}
}
