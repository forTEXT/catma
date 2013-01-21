package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.vaadin.Application;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.terminal.ClassResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.Notification;

import de.catma.CatmaApplication;
import de.catma.document.Corpus;
import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.Slider;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tagger.Tagger.TaggerListener;
import de.catma.ui.tagger.pager.Pager;
import de.catma.ui.tagger.pager.PagerComponent;
import de.catma.ui.tagger.pager.PagerComponent.PageChangeListener;
import de.catma.ui.tagmanager.ColorButtonColumnGenerator.ColorButtonListener;

public class TaggerView extends VerticalLayout 
	implements TaggerListener, ClosableTab {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private SourceDocument sourceDocument;
	private Tagger tagger;
	private Pager pager;
	private MarkupPanel markupPanel;
	private TagManager tagManager;
	private int taggerID;
	private Button btAnalyze;
	private Repository repository;
	private PropertyChangeListener sourceDocChangedListener;
	private PagerComponent pagerComponent;
	private Slider linesPerPageSlider;
	private double totalLineCount;
	private PropertyChangeListener tagReferencesChangedListener;
	
	public TaggerView(
			int taggerID, 
			SourceDocument sourceDocument, Repository repository, 
			PropertyChangeListener sourceDocChangedListener,
			Application application) {
		this.taggerID = taggerID;
		this.tagManager = repository.getTagManager();
		this.repository = repository;
		this.sourceDocument = sourceDocument;
		this.sourceDocChangedListener = sourceDocChangedListener;

		initComponents(application);
		initActions();
		initListeners();
		pager.setMaxPageLengthInLines(30);
		try {
			tagger.setText(sourceDocument.getContent());
			totalLineCount = pager.getTotalLineCount();
			try {
				linesPerPageSlider.setValue((100/totalLineCount)*30);
			} catch (ValueOutOfBoundsException toBeIgnored) {}
		} catch (IOException e) {
			((CatmaApplication)getApplication()).showAndLogError(
				"Error showing Source Document!", e);
		}
	}

	private void initListeners() {
		repository.addPropertyChangeListener(
			RepositoryChangeEvent.sourceDocumentChanged,
			sourceDocChangedListener);
		
		this.tagReferencesChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() != null) {

					@SuppressWarnings("unchecked")
					List<TagReference> tagReferences = (List<TagReference>)evt.getNewValue(); 
					
					List<TagReference> relevantTagReferences = new ArrayList<TagReference>();

					for (TagReference tr : tagReferences) {
						if (isRelevantTagReference(tr, markupPanel.getUserMarkupCollections())) {
							relevantTagReferences.add(tr);
						}
					}
					tagger.setVisible(relevantTagReferences, true);
					
				}
				else if (evt.getOldValue() != null) {
					@SuppressWarnings("unchecked")
					List<TagReference> tagReferences = (List<TagReference>)evt.getOldValue(); 
					tagger.setVisible(tagReferences, false);
					markupPanel.showTagInstanceInfo(
							tagReferences.toArray(new TagReference[]{}));
				}
			}
		};
		
		repository.addPropertyChangeListener(
			RepositoryChangeEvent.tagReferencesChanged, 
			tagReferencesChangedListener);
	}

	private boolean isRelevantTagReference(TagReference tr,
			List<UserMarkupCollection> userMarkupCollections) {
		
		for (UserMarkupCollection umc : userMarkupCollections) {
			if (umc.hasTagInstance(tr.getTagInstanceID())) {
				return true;
			}
		}
		
		return false;
	}

	private void initActions() {
		btAnalyze.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Corpus corpus = new Corpus(sourceDocument.toString());
				corpus.addSourceDocument(sourceDocument);
				for (UserMarkupCollection umc : 
					markupPanel.getUserMarkupCollections()) {
					UserMarkupCollectionReference userMarkupCollRef =
							sourceDocument.getUserMarkupCollectionReference(
									umc.getId());
					if (userMarkupCollRef != null) {
						corpus.addUserMarkupCollectionReference(
								userMarkupCollRef);
					}
				}
				//TODO: add static markup colls
				
				((AnalyzerProvider)getApplication()).analyze(
						corpus, (IndexedRepository)markupPanel.getRepository());
			}
		});
		
		linesPerPageSlider.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				Double perCentValue = (Double)linesPerPageSlider.getValue();
				int lines = (int)((totalLineCount/100.0)*perCentValue);
				
				List<ClientTagInstance> absoluteTagInstances = 
						pager.getAbsoluteTagInstances();
				
				pager.setMaxPageLengthInLines(lines);
				//recalculate pages
				try {
					tagger.setText(sourceDocument.getContent());
				} catch (IOException e) {
					((CatmaApplication)getApplication()).showAndLogError(
						"Error showing Source Document!", e);
				}
				tagger.setTagInstancesVisible(absoluteTagInstances, true);

				pagerComponent.setPage(1);
			}
		});
	}

	private void initComponents(Application application) {
		setSizeFull();
		
		VerticalLayout taggerPanel = new VerticalLayout();
		
		taggerPanel.setSpacing(true);
		taggerPanel.setSizeFull();

		Label helpLabel = new Label();
		
		helpLabel.setIcon(new ClassResource(
				"ui/resources/icon-help.gif", 
				application));
		helpLabel.setWidth("20px");
		helpLabel.setDescription(
				"<h3>Hints</h3>" +
				"<h4>Tag this Source Document</h4>" +
				"<ol><li>First you have to tell CATMA which Tagset you want to use. " +
				"Open a Tag Library from the Repository Manager and drag a Tagset to the \"Active Tagsets\" section.</li>" +
				"<li>Now you can mark the text sequence you want to tag.</li><li>Click the colored button of the desired Tag to apply it to the marked sequence.</li></ol> " +
				"When you click on a tagged text, i. e. a text that is underlined with colored bars you should see " +
				"the available Tag Instances in the section on the lower right of this view.");		
		pager = new Pager(taggerID, 80, 30);
		
		tagger = new Tagger(taggerID, pager, this);
		tagger.setSizeFull();
		
		CustomComponent cc = new CustomComponent(tagger);
		cc.setWidth("400px");
		cc.addStyleName("tagger");
		
		Panel scrollPanel = new Panel(cc);
		scrollPanel.setSizeFull();
		
		scrollPanel.getContent().setSizeUndefined();
		
		addComponent(scrollPanel);
		
		taggerPanel.addComponent(scrollPanel);
		taggerPanel.setExpandRatio(scrollPanel, 0.8f);
		
		Panel actionPanel = new Panel(new HorizontalLayout());
		
		((HorizontalLayout)actionPanel.getContent()).setSpacing(true);
		actionPanel.setSizeFull();

		taggerPanel.addComponent(actionPanel);
		taggerPanel.setExpandRatio(actionPanel, 0.2f);
		
		pagerComponent = new PagerComponent(
				pager, new PageChangeListener() {
					
			public void pageChanged(int number) {
				tagger.setPage(number);
			}
		});
		
		actionPanel.addComponent(helpLabel);
		
		actionPanel.addComponent(pagerComponent);
		((HorizontalLayout)actionPanel.getContent()).setExpandRatio(pagerComponent, 0.1f);
		
		btAnalyze = new Button("Analyze Document");
		btAnalyze.setEnabled(repository instanceof IndexedRepository);
		actionPanel.addComponent(btAnalyze);
		((HorizontalLayout)actionPanel.getContent()).setExpandRatio(btAnalyze, 0.1f);
		
		linesPerPageSlider =  new Slider("page size zoom", 1, 100, "%");
		linesPerPageSlider.setImmediate(true);
		linesPerPageSlider.setWidth("150px");
		
		actionPanel.addComponent(linesPerPageSlider);
		((HorizontalLayout)actionPanel.getContent()).setExpandRatio(linesPerPageSlider, 0.8f);
		
		markupPanel = new MarkupPanel(
				repository,
				new ColorButtonListener() {
					
					private boolean enabled = false;
			
					public void colorButtonClicked(TagDefinition tagDefinition) {
						if (enabled) {
							tagger.addTagInstanceWith(tagDefinition);
						}
						else {
							getWindow().showNotification(
	                                "Information",
	                                "Please select a User Markup Collection "
	                                + " to store your markup first!<br>"
	                                + "See 'Active Markup Colletions'.",
	                                Notification.TYPE_TRAY_NOTIFICATION);
						}
					}
					
					public void setEnabled(boolean enabled) {
						this.enabled = enabled;
					}
				},
				new PropertyChangeListener() {
					
					public void propertyChange(PropertyChangeEvent evt) {
						boolean selected = evt.getNewValue() != null;
						@SuppressWarnings("unchecked")
						List<TagReference> tagReferences = 
							(List<TagReference>)(
									selected?evt.getNewValue():evt.getOldValue());
						
						tagger.setVisible(tagReferences, selected);
					}
				},
				new PropertyChangeListener() {
					
					public void propertyChange(PropertyChangeEvent evt) {
						@SuppressWarnings("unchecked")
						Set<TagDefinition> removedTagDefinitions = 
								(Set<TagDefinition>) evt.getOldValue();
						pager.removeTagInstances(removedTagDefinitions);
						tagger.setPage(pager.getCurrentPageNumber());
					}
				});
		
		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		splitPanel.addComponent(taggerPanel);
		splitPanel.addComponent(markupPanel);
		addComponent(splitPanel);
	}

	public SourceDocument getSourceDocument() {
		return sourceDocument;
	}
	
	public void openUserMarkupCollection(
			UserMarkupCollection userMarkupCollection) {
		markupPanel.openUserMarkupCollection(userMarkupCollection);
	}

	public void close() {
		markupPanel.close();
		repository.removePropertyChangeListener(
				RepositoryChangeEvent.sourceDocumentChanged,
				sourceDocChangedListener);
		repository.removePropertyChangeListener(
				RepositoryChangeEvent.tagReferencesChanged, 
				tagReferencesChangedListener);

		sourceDocChangedListener = null;
	}
	
	public void tagInstanceAdded(
			ClientTagInstance clientTagInstance) {
		TagLibrary tagLibrary =
				markupPanel.getCurrentWritableUserMarkupCollection().getTagLibrary();
		
		if (tagLibrary.getTagDefinition(clientTagInstance.getTagDefinitionID())
				== null) {
			TagsetDefinition tagsetDef =
					markupPanel.getTagsetDefinition(
							clientTagInstance.getTagDefinitionID());
			if (tagLibrary.getTagsetDefinition(tagsetDef.getUuid()) == null) {
				tagManager.addTagsetDefinition(
						tagLibrary, new TagsetDefinition(tagsetDef));
			}
			else {
				//this should not happen, because we update TagsetDefinitions immedately
				logger.severe(
					"TagDefinition not found, but TagsetDefinition is present, " +
					"expected was either a complete TagsetDefiniton or no TagsetDefinition," +
					"adding TagDefinition instead of TagsetDefinition now: orig TagsetDef: " + 
					tagsetDef + " orig TagDef: " +tagsetDef.getTagDefinition(
							clientTagInstance.getTagDefinitionID()));
				
				tagManager.addTagDefinition(
					tagLibrary.getTagsetDefinition(tagsetDef.getUuid()),
					new TagDefinition(
						tagsetDef.getTagDefinition(
							clientTagInstance.getTagDefinitionID())));
			}
		}
		
		TagDefinition tagDef = 
				tagLibrary.getTagDefinition(
						clientTagInstance.getTagDefinitionID());
		
		TagInstance ti = 
			new TagInstance(clientTagInstance.getInstanceID(), tagDef);
		
		List<TagReference> tagReferences = new ArrayList<TagReference>();
		
		try {
			for (TextRange tr : clientTagInstance.getRanges()) {
				Range r = new Range(tr.getStartPos(), tr.getEndPos());
				TagReference ref = 
						new TagReference(ti, sourceDocument.getID(), r);
				tagReferences.add(ref);
			}
			markupPanel.addTagReferences(tagReferences);
		} catch (URISyntaxException e) {
			((CatmaApplication)getApplication()).showAndLogError(
				"Error adding Tags!", e);
		}
	}

	public void show(Range range) {
		try {
			int startPage = pager.getPageNumberFor(range.getStartPoint());
			int endPage = pager.getPageNumberFor(range.getEndPoint());
			
			if (startPage != endPage) {
				Double perCentValue = 100.0;

				while(startPage != endPage) {
					pager.setMaxPageLengthInLines(pager.getMaxPageLengthInLines()+5);
					try {
						pager.setText(sourceDocument.getContent());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	
					startPage = pager.getPageNumberFor(range.getStartPoint());
					endPage = pager.getPageNumberFor(range.getEndPoint());
					
					perCentValue = ((double)pager.getApproxMaxLineLength())/(((double)totalLineCount)/100.0);
				}
				
				linesPerPageSlider.setValue(perCentValue);
			}
			
			int pageNumber = pager.getStartPageNumberFor(range);
			pagerComponent.setPage(pageNumber);
			TextRange tr = pager.getCurrentPage().getRelativeRangeFor(range);
			tagger.highlight(tr);
		} catch (ValueOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void tagInstancesSelected(List<String> instanceIDs) {
		markupPanel.showTagInstanceInfo(instanceIDs);
	}
	
	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }

}
