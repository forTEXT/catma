package de.catma.ui.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.vaadin.ui.Component;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.IndexedRepository;
import de.catma.project.ProjectManager;
import de.catma.tag.TagLibrary;
import de.catma.ui.tabbedview.TabbedView;
import de.catma.ui.tagger.TaggerView;

public class ProjectManagerView extends TabbedView {

	private ProjectListView myProjects;
	private int tabId = 0;


	public ProjectManagerView(ProjectManager projectManager) {
		super(""); //there is at least one open tab: the projects overview
		
		initProjectManagerViewComponents(projectManager);
	
	}
	
	
//	
//	
//	tagManager = new TagManager();
//
//	repositoryManagerView = new RepositoryManagerView(
//			new RepositoryManager(this, tagManager, RepositoryProperties.INSTANCE.getProperties()));
//
//	tagManagerView = new TagManagerView(tagManager);
//
//	taggerManagerView = new TaggerManagerView();
//
//	analyzerManagerView = new AnalyzerManagerView();
//
//	visualizationManagerView = new VisualizationManagerView();
	

	private void initProjectManagerViewComponents(ProjectManager projectManager) {
		myProjects = new ProjectListView(projectManager, projectReference -> {
			addTab(new ProjectView(projectManager, projectReference), projectReference.getName()).setClosable(true);
		});
		
		addTab(myProjects, "My Projects");
		
	}


	public void openTagLibrary(Repository repository, TagLibrary tagLibrary, boolean switchToTagManagerView) {
		// TODO Auto-generated method stub

		//		if (switchToTagManagerView) {
//			menu.executeEntry(tagManagerView);
//		}
//		tagManagerView.openTagLibrary(this, repository, tagLibrary);
	}

	public TaggerView openSourceDocument(SourceDocument sourceDocument, Repository project) {
		TaggerView taggerView = 
			new TaggerView(tabId, sourceDocument, project, new PropertyChangeListener() {
				
				public void propertyChange(PropertyChangeEvent evt) {

					if (evt.getNewValue() == null) { //remove
						SourceDocument sd = (SourceDocument) evt.getOldValue();
						if (sd.getID().equals(sourceDocument.getID())) {
							TaggerView taggerView = 
									getTaggerView(sourceDocument);
							if (taggerView != null) {
								onTabClose(taggerView);
							}
						}
					}
					else if (evt.getOldValue() != null) { //update
						String sdID = (String) evt.getOldValue();
						if (sdID.equals(sourceDocument.getID())) {
							TaggerView taggerView = 
									getTaggerView(sourceDocument);
							if (taggerView != null) {
								taggerView.setSourceDocument(
										(SourceDocument) evt.getNewValue());
							}
						}								
					}
					
				}
			});
		
		addClosableTab(taggerView, sourceDocument.toString());
		setSelectedTab(taggerView);

		return taggerView;
	}
	
	
	private TaggerView getTaggerView(SourceDocument sourceDocument) {
		for (Component tabContent : this.getTabSheet()) {
			TaggerView taggerView = (TaggerView)tabContent;
			if (taggerView.getSourceDocument().getID().equals(
					sourceDocument.getID())) {
				return taggerView;
			}
		}
		
		return null;
	}

	public void analyze(Corpus corpus, IndexedRepository repository) {
		// TODO Auto-generated method stub
//		menu.executeEntry(analyzerManagerView);
//		analyzerManagerView.analyzeDocuments(corpus, repository);
		
	}

	public void analyzeCurrentlyActiveDocument(Repository repository) {
		// TODO Auto-generated method stub

//		menu.executeEntry(analyzerManagerView);
//		
//		Component selectedTab = taggerManagerView.getSelectedTab();
//		
//		if (selectedTab != null) {
//			TaggerView taggerView = (TaggerView) selectedTab;
//			taggerView.analyzeDocument();
//		} else {
//			Notification.show(Messages.getString("CatmaApplication.noOpenDocument"), Type.TRAY_NOTIFICATION); //$NON-NLS-1$
//		}
		
	}

	public void close() {
		// TODO Auto-generated method stub
		
//		repositoryManagerView.getRepositoryManager().close();		
	}


	public void openUserMarkupCollection(SourceDocument sourceDocument, UserMarkupCollection userMarkupCollection,
			Repository repository) {
		// TODO Auto-generated method stub
		
//		TaggerView taggerView = openSourceDocument(sourceDocument, repository);
//		taggerManagerView.openUserMarkupCollection(taggerView, userMarkupCollection);

	}

}
