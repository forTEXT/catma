package de.catma.ui.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.SourceDocument;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.repository.Messages;
import de.catma.ui.repository.wizard.AddSourceDocWizardFactory;
import de.catma.ui.repository.wizard.AddSourceDocWizardResult;
import de.catma.ui.repository.wizard.SourceDocumentResult;
import de.catma.ui.tabbedview.ClosableTab;

public class ProjectView extends VerticalLayout implements ClosableTab {
	
	private Button btAddSourceDocuments;
	private Repository repository;

	public ProjectView(
			ProjectManager projectManager, ProjectReference projectReference) {
		projectManager.openProject(
			projectReference, 
			new OpenProjectListener() {
			
				@Override
				public void ready(Repository repository) {
					ProjectView.this.repository = repository;
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
		initComponents();
		initActions();
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

								repository.insert(sourceDocument);
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
				repository);
			
			Window sourceDocCreationWizardWindow = 
					factory.createWizardWindow(
							Messages.getString("SourceDocumentPanel.addNewSourceDoc"), "85%",  "98%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			UI.getCurrent().addWindow(
					sourceDocCreationWizardWindow);
			
			sourceDocCreationWizardWindow.center();
			
		});
	}

	private void initComponents() {
		btAddSourceDocuments = new Button("Add Document");
		addComponent(btAddSourceDocuments);
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
		// TODO Auto-generated method stub
		
	}
}
