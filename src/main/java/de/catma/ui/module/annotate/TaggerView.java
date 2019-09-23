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
package de.catma.ui.module.annotate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vaadin.sliderpanel.SliderPanel;
import org.vaadin.sliderpanel.SliderPanelBuilder;
import org.vaadin.sliderpanel.client.SliderMode;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.document.Range;
import de.catma.document.annotation.Annotation;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionManager;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.corpus.Corpus;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.IndexedProject;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.project.Project.RepositoryChangeEvent;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.Slider;
import de.catma.ui.component.tabbedview.ClosableTab;
import de.catma.ui.component.tabbedview.TabCaptionChangeListener;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.events.TaggerViewSourceDocumentChangedEvent;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.module.analyze.visualization.ExpansionListener;
import de.catma.ui.module.analyze.visualization.kwic.KwicPanel;
import de.catma.ui.module.annotate.Tagger.TaggerListener;
import de.catma.ui.module.annotate.TaggerSplitPanel.SplitterPositionChangedEvent;
import de.catma.ui.module.annotate.TaggerSplitPanel.SplitterPositionChangedListener;
import de.catma.ui.module.annotate.annotationpanel.AnnotationPanel;
import de.catma.ui.module.annotate.annotationpanel.EditAnnotationPropertiesDialog;
import de.catma.ui.module.annotate.contextmenu.TaggerContextMenu;
import de.catma.ui.module.annotate.pager.Page;
import de.catma.ui.module.annotate.pager.Pager;
import de.catma.ui.module.annotate.pager.PagerComponent;
import de.catma.ui.module.annotate.pager.PagerComponent.PageChangeListener;
import de.catma.ui.module.annotate.resourcepanel.AnnotateResourcePanel;
import de.catma.ui.module.annotate.resourcepanel.ResourceSelectionListener;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.Pair;

public class TaggerView extends HorizontalLayout 
	implements TaggerListener, ClosableTab {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private SourceDocument sourceDocument;
	private Tagger tagger;
	private Pager pager;
	private TagManager tagManager;
	private int taggerID;
	private Button btAnalyze;
	private Button btHelp;
	private Project project;
	private PagerComponent pagerComponent;
	private Slider linesPerPageSlider;
	private double totalLineCount;
	private PropertyChangeListener tagReferencesChangedListener;
	private int approxMaxLineLength;
	private int maxPageLengthInLines = 30;
	private int initialSplitterPositionInPixels = 785;
	
	private TaggerHelpWindow taggerHelpWindow = new TaggerHelpWindow();
	private CheckBox cbTraceSelection;
	private Button btClearSearchHighlights;
	private AnnotateResourcePanel resourcePanel;
	private AnnotationPanel annotationPanel;
	private AnnotationCollectionManager userMarkupCollectionManager;
	private final EventBus eventBus;
	private TaggerContextMenu taggerContextMenu;
	private ErrorHandler errorHandler;
	private PropertyChangeListener annotationPropertiesChangedListener;
	private PropertyChangeListener tagChangedListener;
	private SliderPanel drawer;
	private KwicPanel kwicPanel;
	private VerticalSplitPanel rightSplitPanel;
	
	public TaggerView(
			int taggerID, 
			SourceDocument sourceDocument, Project project, 
			EventBus eventBus){
		this.tagManager = project.getTagManager();
		this.project = project;
		this.sourceDocument = sourceDocument;
		this.eventBus = eventBus;
		
		this.approxMaxLineLength = getApproximateMaxLineLengthForSplitterPanel(initialSplitterPositionInPixels);
		this.userMarkupCollectionManager = new AnnotationCollectionManager(project);
		this.errorHandler = (ErrorHandler)UI.getCurrent();
		initComponents();
		initActions();
		initListeners();
		pager.setMaxPageLengthInLines(maxPageLengthInLines);
		initData();
	}

	private void initData() {
		try {
			if (sourceDocument != null) {
				linesPerPageSlider.setEnabled(true);
				btAnalyze.setEnabled(true);
				
				tagger.setText(sourceDocument.getContent());
				totalLineCount = pager.getTotalLineCount();
				try {
					linesPerPageSlider.setValue((100.0/totalLineCount)*maxPageLengthInLines);
				} catch (ValueOutOfBoundsException toBeIgnored) {}
				
				List<AnnotationCollectionReference> collectionReferences =
					resourcePanel.getSelectedUserMarkupCollectionReferences();
				
				userMarkupCollectionManager.clear();
				
				for (AnnotationCollectionReference collectionRef : collectionReferences) {
					AnnotationCollection collection = project.getUserMarkupCollection(collectionRef);
					userMarkupCollectionManager.add(collection);
				}
				
				Collection<TagsetDefinition> tagsets = 
						new HashSet<>(resourcePanel.getSelectedTagsets());
				
				annotationPanel.setData(
						sourceDocument, 
						tagsets, 
						new ArrayList<>(userMarkupCollectionManager.getUserMarkupCollections()));
				if (taggerContextMenu != null) {
					taggerContextMenu.setTagsets(tagsets);
				}
			}			
			else {
				linesPerPageSlider.setEnabled(false);
				btAnalyze.setEnabled(false);
			}
		} catch (IOException e) {
			errorHandler.showAndLogError("Error showing the Document!", e);
		}
	}

	private void initListeners() {
		this.tagReferencesChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() != null) {
					
					@SuppressWarnings("unchecked")
					Pair<AnnotationCollection, List<TagReference>> changeValue = 
							(Pair<AnnotationCollection, List<TagReference>>) evt.getNewValue();
					
					List<TagReference> tagReferences = changeValue.getSecond(); 
					
					List<TagReference> relevantTagReferences = 
							new ArrayList<TagReference>();

					for (TagReference tr : tagReferences) {
						if (isRelevantTagReference(
								tr, 
								userMarkupCollectionManager.getUserMarkupCollections())) {
							relevantTagReferences.add(tr);
						}
					}
					tagger.setVisible(relevantTagReferences, true);

					Set<String> tagInstanceUuids = new HashSet<String>();

					for (TagReference tr : relevantTagReferences){
						tagInstanceUuids.add(tr.getTagInstance().getUuid());
					}
					
					
					tagInstanceUuids.forEach(annotationId -> tagger.updateAnnotation(annotationId));

				}
				else if (evt.getOldValue() != null) {
					@SuppressWarnings("unchecked")
					Pair<String, Collection<String>> changeValue = 
							(Pair<String, Collection<String>>) evt.getOldValue();
					
					String collectionId = changeValue.getFirst();
					Collection<String> annotationIds = changeValue.getSecond(); 

					if (userMarkupCollectionManager.contains(collectionId)) {
						userMarkupCollectionManager.removeTagInstance(annotationIds, false);
					}
					
					tagger.removeTagInstances(annotationIds);
					annotationPanel.removeAnnotations(annotationIds);
				}
			}
		};
		
		project.addPropertyChangeListener(
			RepositoryChangeEvent.tagReferencesChanged, 
			tagReferencesChangedListener);
		
		annotationPropertiesChangedListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				TagInstance tagInstance = (TagInstance) evt.getOldValue();
				tagger.updateAnnotation(tagInstance.getUuid());
			}
		};
		project.addPropertyChangeListener(
				RepositoryChangeEvent.propertyValueChanged,
				annotationPropertiesChangedListener);
		
		tagChangedListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Object newValue = evt.getNewValue();
				Object oldValue = evt.getOldValue();
				if (oldValue == null) { //created
					// noop
				}
				else if (newValue == null) { //removed
					@SuppressWarnings("unchecked")
					Pair<TagsetDefinition,TagDefinition> deleted = 
						(Pair<TagsetDefinition, TagDefinition>) oldValue;
					
					for (AnnotationCollectionReference ref : 
						userMarkupCollectionManager.getCollections(deleted.getSecond())) {
					
						setAnnotationCollectionSelected(ref, false);
						setAnnotationCollectionSelected(ref, true);
					}
					
				}
				else { //update
					TagDefinition tag = (TagDefinition) newValue;
					
					for (AnnotationCollection collection : 
						userMarkupCollectionManager.getUserMarkupCollections()) {
						List<TagReference> relevantTagReferences = 
								collection.getTagReferences(tag);
						tagger.setVisible(relevantTagReferences, false);
						tagger.setVisible(relevantTagReferences, true);
					}
					
				}
				
			}
		};
		
		project.getTagManager().addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);
		
	}

	private boolean isRelevantTagReference(TagReference tr,
			List<AnnotationCollection> userMarkupCollections) {
		
		for (AnnotationCollection umc : userMarkupCollections) {
			if (umc.hasTagInstance(tr.getTagInstanceId())) {
				return true;
			}
		}
		
		return false;
	}

	public void  analyzeDocument(){
		Corpus corpus = new Corpus();
		corpus.addSourceDocument(sourceDocument);
		for (AnnotationCollection umc : userMarkupCollectionManager.getUserMarkupCollections()) {
			AnnotationCollectionReference userMarkupCollRef =
				sourceDocument.getUserMarkupCollectionReference(
						umc.getId());
			if (userMarkupCollRef != null) {
				corpus.addUserMarkupCollectionReference(
						userMarkupCollRef);
			}
		}	
		if (project instanceof IndexedProject) {
			eventBus.post(new RouteToAnalyzeEvent((IndexedProject)project, corpus));
		}	
	}

	private void initActions() {
		btClearSearchHighlights.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				tagger.removeHighlights();
			}
		});
		cbTraceSelection.addValueChangeListener(new ValueChangeListener<Boolean>() {
			
			@Override
			public void valueChange(ValueChangeEvent<Boolean> event) {
				Boolean traceSelection = event.getValue();
				tagger.setTraceSelection(traceSelection);
			}
		});

		btAnalyze.addClickListener(new ClickListener() {	
		
			public void buttonClick(ClickEvent event) {	
				analyzeDocument();
			}
		});
		
		linesPerPageSlider.addValueChangeListener(new ValueChangeListener<Double>() {
			
			public void valueChange(ValueChangeEvent<Double> event) {
				Double perCentValue = (Double)linesPerPageSlider.getValue();
				int lines = (int)((totalLineCount/100.0)*perCentValue);
				
				List<ClientTagInstance> absoluteTagInstances = 
						pager.getAbsoluteTagInstances();
				
				Page currentPage = pager.getCurrentPage();
				pager.setMaxPageLengthInLines(lines);
				//recalculate pages
				try {
					pager.setText(sourceDocument.getContent());
					int previousPageNumber = pager.getPageNumberFor(currentPage.getPageStart());
					tagger.setPage(previousPageNumber);					
					tagger.setTagInstancesVisible(absoluteTagInstances, true);

					pagerComponent.setPage(previousPageNumber);
				} catch (IOException e) {
					errorHandler.showAndLogError("Error showing the Document!", e);
				}

			}
		});
		
		btHelp.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
				if(taggerHelpWindow.getParent() == null){
					UI.getCurrent().addWindow(taggerHelpWindow);
				} else {
					UI.getCurrent().removeWindow(taggerHelpWindow);
				}
				
			}
		});
		
		resourcePanel.setSelectionListener(new ResourceSelectionListener() {

			@Override
			public void documentSelected(SourceDocument sourceDocument) {
				setSourceDocument(sourceDocument);
			}

			@Override
			public void annotationCollectionSelected(AnnotationCollectionReference collectionReference,
					boolean selected) {
				setAnnotationCollectionSelected(collectionReference, selected);
			}

			@Override
			public void tagsetsSelected(Collection<TagsetDefinition> tagsets) {
				try {
					annotationPanel.setTagsets(tagsets);
					if (taggerContextMenu != null) {
						taggerContextMenu.setTagsets(tagsets);
					}
					for (AnnotationCollection collection : userMarkupCollectionManager.getUserMarkupCollections()) {
						tagger.setVisible(collection.getTagReferences(), false);
						List<TagReference> visibleRefs = 
								annotationPanel.getVisibleTagReferences(collection.getTagReferences());
						if (!visibleRefs.isEmpty()) {
							tagger.setVisible(visibleRefs, true);
						}						
					}
				}
				catch (Exception e) {
					errorHandler.showAndLogError("Error handling Tagset!", e);
				}
			}

			
		});
		
		annotationPanel.setTagReferenceSelectionChangeListener(
			(tagReferences, selected) -> {
				if (!tagReferences.isEmpty()) {
					tagger.setVisible(tagReferences, selected);
				}
			});
		
		
		kwicPanel.setExpansionListener(new ExpansionListener() {

			@Override
			public void expand() {
				hideKwicPanel(); // we hide on expand and on compress since both is considered a "close"
			}

			@Override
			public void compress() {
				hideKwicPanel(); // we hide on expand and on compress since both is considered a "close"
			}
			
		});
		
		kwicPanel.addItemClickListener(itemClick -> {
			if (itemClick.getMouseEventDetails().isDoubleClick()) {
				show(itemClick.getItem().getRange());
			}
		});
		
	}

	private void setAnnotationCollectionSelected(AnnotationCollectionReference collectionReference,
			boolean selected) {
		try {
			AnnotationCollection collection = project.getUserMarkupCollection(collectionReference);
			if (selected) {
				userMarkupCollectionManager.add(collection);
				annotationPanel.addCollection(collection);
				List<TagReference> visibleRefs = 
					annotationPanel.getVisibleTagReferences(collection.getTagReferences());
				if (!visibleRefs.isEmpty()) {
					tagger.setVisible(visibleRefs, true);
				}
			}
			else {
				userMarkupCollectionManager.remove(collectionReference.getId());
				annotationPanel.removeCollection(collectionReference.getId());
				tagger.setVisible(collection.getTagReferences(), false);
			}
			
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Error handling Annotation Collection!", e);
		}

	}

	private void hideKwicPanel() {
		kwicPanel.setVisible(false);
		rightSplitPanel.setSplitPosition(0);
		rightSplitPanel.setLocked(true);
	}

	private void initComponents() {
		setSizeFull();
		
		VerticalLayout taggerPanel = new VerticalLayout();
		taggerPanel.setSizeFull();
		taggerPanel.setSpacing(true);
		taggerPanel.setMargin(new MarginInfo(true, true, true, false));

		btHelp = new IconButton(VaadinIcons.QUESTION_CIRCLE);
		btHelp.addStyleName("help-button"); //$NON-NLS-1$
		
		boolean isRtl = sourceDocument == null?false: 
			sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getIndexInfoSet().isRightToLeftWriting(); 

		pager = new Pager(taggerID, approxMaxLineLength, maxPageLengthInLines, 
				isRtl);
		
		tagger = new Tagger(taggerID, pager, this, project);
		tagger.addStyleName("tagger"); //$NON-NLS-1$
		tagger.setWidth("100%"); //$NON-NLS-1$
		
		taggerPanel.addComponent(tagger);
		taggerPanel.setExpandRatio(tagger, 1.0f);
		
		HorizontalLayout actionPanel = new HorizontalLayout();
		actionPanel.setSpacing(true);
		
		taggerPanel.addComponent(actionPanel);
		
		pagerComponent = new PagerComponent(
				pager, new PageChangeListener() {
					
			public void pageChanged(int number) {
				tagger.setPage(number);
			}
		});
		
		actionPanel.addComponent(btHelp);
		
		actionPanel.addComponent(pagerComponent);

		btAnalyze = new Button("Analyze");
		btAnalyze.addStyleName("primary-button"); //$NON-NLS-1$
		btAnalyze.setEnabled(project instanceof IndexedProject);
		actionPanel.addComponent(btAnalyze);
		
		linesPerPageSlider =  new Slider(null, 1, 100, "% page size");
		linesPerPageSlider.setWidth("150px"); //$NON-NLS-1$
		actionPanel.addComponent(linesPerPageSlider);
		
		cbTraceSelection = new CheckBox();
		cbTraceSelection.setIcon(VaadinIcons.AREA_SELECT);
		cbTraceSelection.setDescription("Allow multiple discontinous selections");
		actionPanel.addComponent(cbTraceSelection);
		cbTraceSelection.addStyleName("tagger-trace-checkbox"); //$NON-NLS-1$

		btClearSearchHighlights = new IconButton(VaadinIcons.ERASER);
		btClearSearchHighlights.setDescription("Clear all search highlights");
		actionPanel.addComponent(btClearSearchHighlights);
		
		rightSplitPanel = new VerticalSplitPanel();
		rightSplitPanel.setSizeFull();
		
		kwicPanel = new KwicPanel(eventBus, project, KwicProvider.buildKwicProviderByDocumentIdCache(project));
		kwicPanel.setExpandResource(VaadinIcons.CLOSE);
		kwicPanel.setCompressResource(VaadinIcons.CLOSE);
		
		rightSplitPanel.addComponent(kwicPanel);
		hideKwicPanel();
		
		annotationPanel = new AnnotationPanel(
			project, 
			userMarkupCollectionManager,
			selectedAnnotationId -> tagger.setTagInstanceSelected(selectedAnnotationId),
			collection -> handleCollectionValueChange(collection),
			() -> sourceDocument,
			eventBus);
		rightSplitPanel.addComponent(annotationPanel);
		
		final TaggerSplitPanel splitPanel = new TaggerSplitPanel();
		splitPanel.addComponent(taggerPanel);
		splitPanel.addComponent(rightSplitPanel);
		
		splitPanel.setSplitPosition(initialSplitterPositionInPixels, Unit.PIXELS);
		splitPanel.addStyleName("catma-tab-spacing"); //$NON-NLS-1$
		
		SplitterPositionChangedListener listener = new SplitterPositionChangedListener(){

			@Override
			public void positionChanged(SplitterPositionChangedEvent event) {
				float width = event.getPosition();
				
				// unit != Unit.PERCENTAGE && unit != Unit.PIXELS
				// TODO: if it is PERCENTAGE, work out the splitter position in pixels
				if (event.getPositionUnit() != Unit.PIXELS){
					String message = "Must use PIXELS Unit for split position"; //$NON-NLS-1$
					errorHandler.showAndLogError(
							message, new IllegalArgumentException(message));
				}							
				
				int approxMaxLineLength = getApproximateMaxLineLengthForSplitterPanel(width);
				
				List<ClientTagInstance> absoluteTagInstances = pager.getAbsoluteTagInstances();
				
				Page currentPage = pager.getCurrentPage();
				pager.setApproxMaxLineLength(approxMaxLineLength);
				//recalculate pages
				try {
					pager.setText(sourceDocument.getContent());
					int previousPageNumber = pager.getPageNumberFor(currentPage.getPageStart());
					tagger.setPage(previousPageNumber);					
					tagger.setTagInstancesVisible(absoluteTagInstances, true);

					pagerComponent.setPage(previousPageNumber);
				} catch (IOException e) {
					errorHandler.showAndLogError("Error showing the Document!", e); //$NON-NLS-1$
				}							
			}
			
		};
		
		splitPanel.addListener(SplitterPositionChangedEvent.class,
                listener, SplitterPositionChangedListener.positionChangedMethod);
		
		resourcePanel = new AnnotateResourcePanel(project, sourceDocument, eventBus); 
		drawer = new SliderPanelBuilder(resourcePanel)
				.mode(SliderMode.LEFT).expanded(sourceDocument == null).build();
		
		addComponent(drawer);
		
		addComponent(splitPanel);
		setExpandRatio(splitPanel, 1.0f);
	}
	
	private void handleCollectionValueChange(AnnotationCollection collection) {
		if (collection == null) {
			if (taggerContextMenu != null) {
				taggerContextMenu.close();
				taggerContextMenu = null;
			}
		}
		else if (taggerContextMenu == null) {
			taggerContextMenu = new TaggerContextMenu(
					tagger, 
					this.tagManager);
				
			taggerContextMenu.setTagSelectionListener(
					tag -> tagger.addTagInstanceWith(tag));
			Collection<TagsetDefinition> tagsets = 
					new HashSet<>(resourcePanel.getSelectedTagsets());
			taggerContextMenu.setTagsets(tagsets);
		}
		
	}

	public int getApproximateMaxLineLengthForSplitterPanel(float width){
		// based on ratio of 80:550
		int approxMaxLineLength = (int) (width * 0.135);
		
		return approxMaxLineLength;
	}

	public SourceDocument getSourceDocument() {
		return sourceDocument;
	}
	
	public AnnotationCollection openUserMarkupCollection(
			AnnotationCollectionReference userMarkupCollectionRef) throws IOException {
		AnnotationCollection umc = project.getUserMarkupCollection(userMarkupCollectionRef);
		openUserMarkupCollection(umc);
		return umc;
	}
	
	public void openUserMarkupCollection(
			AnnotationCollection userMarkupCollection) {
		resourcePanel.selectCollectionVisible(userMarkupCollection.getUuid());
	}

	public void close() {
		this.eventBus.unregister(this);
		annotationPanel.close();
		resourcePanel.close();
		if (taggerContextMenu != null) {
			taggerContextMenu.close();
		}
		project.removePropertyChangeListener(
				RepositoryChangeEvent.tagReferencesChanged, 
				tagReferencesChangedListener);
		project.removePropertyChangeListener(
				RepositoryChangeEvent.propertyValueChanged,
				annotationPropertiesChangedListener);
		project.getTagManager().removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);
				
		project = null;
	}
	
	public void tagInstanceAdded(
			ClientTagInstance clientTagInstance) {
		
		AnnotationCollection collection = annotationPanel.getSelectedEditableCollection();
		if (collection == null) { //shouldn't happen, but just in case
			Notification.show("Info", 
					"Please make sure you have a editable Collection available "
					+ "and select this Collection as 'currently being edited'! "
					+ "Your Annotation hasn't been saved!",
					Type.ERROR_MESSAGE);
		}
		else {
			TagLibrary tagLibrary = collection.getTagLibrary();
			
			TagDefinition tagDef = 
					tagLibrary.getTagDefinition(
							clientTagInstance.getTagDefinitionID());
			
			TagInstance ti = 
				new TagInstance(
					clientTagInstance.getInstanceID(), 
					tagDef.getUuid(),
					project.getUser().getIdentifier(),
		        	ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
		        	tagDef.getUserDefinedPropertyDefinitions(),
		        	tagDef.getTagsetDefinitionUuid());
			
			List<TagReference> tagReferences = new ArrayList<TagReference>();
			
			try {
				String userMarkupCollectionUuid = collection.getId();
	
				for (TextRange tr : clientTagInstance.getRanges()) {
					Range r = new Range(tr.getStartPos(), tr.getEndPos());
					TagReference ref = 
							new TagReference(ti, sourceDocument.getUuid(), r, userMarkupCollectionUuid);
					tagReferences.add(ref);
				}
				
				final Annotation annotation = 
					new Annotation(ti, tagReferences, collection, tagLibrary.getTagPath(tagDef));
				if (!tagDef.getUserDefinedPropertyDefinitions().isEmpty()) {
					EditAnnotationPropertiesDialog editAnnotationPropertiesDialog = 
						new EditAnnotationPropertiesDialog(
							project, annotation, 
							new SaveCancelListener<List<Property>>() {
								
								@Override
								public void savePressed(List<Property> result) {
									userMarkupCollectionManager.addTagReferences(
											tagReferences, collection);
								}
						});
					editAnnotationPropertiesDialog.show();
				}
				else {
					userMarkupCollectionManager.addTagReferences(tagReferences, collection);
				}
				
			} catch (URISyntaxException e) {
				errorHandler.showAndLogError("Error adding Annotations!", e);
			}
		}
	}

	public void showQueryResultRows(QueryResultRow selectedRow, List<QueryResultRow> rows) {
		kwicPanel.setVisible(true);
		kwicPanel.clear();
		kwicPanel.addQueryResultRows(rows);
		kwicPanel.sortByStartPosAsc();
		kwicPanel.setSelectedItem(selectedRow);
		
		rightSplitPanel.setSplitPosition(50);
		rightSplitPanel.setLocked(false);
		
		show(selectedRow.getRange());
	}
	
	public void show(Range range) {
		try {
			int startPage = pager.getPageNumberFor(range.getStartPoint());
			int endPage = pager.getPageNumberFor(range.getEndPoint());
			
			if (startPage != endPage) { // range spans several pages
				Double perCentValue = 100.0;

				// increase page zoom so that the highlighter fits into one page
				while(startPage != endPage) {
					pager.setMaxPageLengthInLines(pager.getMaxPageLengthInLines()+5);
					try {
						pager.setText(sourceDocument.getContent());
					} catch (IOException e) {
						logger.log(Level.SEVERE, "error adjusting  page zoom", e); //$NON-NLS-1$
					}
	
					startPage = pager.getPageNumberFor(range.getStartPoint());
					endPage = pager.getPageNumberFor(range.getEndPoint());
					
					perCentValue = 
						((double)pager.getApproxMaxLineLength())/(((double)totalLineCount)/100.0);
				}
				// set computed zoom value
				linesPerPageSlider.setValue(perCentValue);
			}
			// set page that contains the range to be highlighted
			int pageNumber = pager.getStartPageNumberFor(range);
			pagerComponent.setPage(pageNumber);
			
			tagger.highlight(range);
		} catch (ValueOutOfBoundsException e) {
			logger.log(Level.SEVERE, "error during highlighting", e); //$NON-NLS-1$
		}
	}
	
	public void tagInstanceSelected(String instancePartID, String lineID) {
		try {
			annotationPanel.showAnnotationDetails(
					userMarkupCollectionManager.getAnnotations(
							pager.getCurrentPage().getTagInstanceIDs(instancePartID, lineID)));
		} catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("error showing Annotation details", e);
		}
	}
	
	@Override
	public void tagInstanceSelected(Set<String> tagInstanceIDs) {
		try {
			annotationPanel.showAnnotationDetails(
				userMarkupCollectionManager.getAnnotations(tagInstanceIDs));
		} catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("error showing Annotation details", e);
		}
	}
	
	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }

	public void setSourceDocument(SourceDocument sd) {
		this.sourceDocument = sd;
		this.resourcePanel.setSelectedDocument(sd);
		
		pager.setRightToLeftWriting(
			this.sourceDocument
			.getSourceContentHandler()
			.getSourceDocumentInfo()
			.getIndexInfoSet()
			.isRightToLeftWriting());
		
		initData();
		eventBus.post(new TaggerViewSourceDocumentChangedEvent(TaggerView.this));
		this.drawer.collapse();
	}

	@Override
	public Annotation getTagInstanceInfo(String tagInstanceId) {
		return userMarkupCollectionManager.getAnnotation(tagInstanceId);
	}
	
	@Override
	public void setTabNameChangeListener(TabCaptionChangeListener tabNameChangeListener) {
		//noop tabname is not changeable
	}
}
