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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.vaadin.shared.ui.ContentMode;
import org.apache.commons.lang3.StringUtils;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.sliderpanel.SliderPanel;
import org.vaadin.sliderpanel.SliderPanelBuilder;
import org.vaadin.sliderpanel.client.SliderMode;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.hazelcast.topic.ITopic;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Range;
import de.catma.document.annotation.Annotation;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionManager;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.document.corpus.Corpus;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.hazelcast.HazelCastService;
import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.indexer.IndexedProject;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.project.Project.ProjectEvent;
import de.catma.project.event.ChangeType;
import de.catma.project.event.CommentChangeEvent;
import de.catma.project.event.ReplyChangeEvent;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.CatmaApplication;
import de.catma.ui.UIMessageListener;
import de.catma.ui.client.ui.tagger.shared.ClientComment;
import de.catma.ui.client.ui.tagger.shared.ClientCommentReply;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.PagerComponent;
import de.catma.ui.component.PagerComponent.PageChangeListener;
import de.catma.ui.component.tabbedview.ClosableTab;
import de.catma.ui.component.tabbedview.TabCaptionChangeListener;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.events.CommentMessage;
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
import de.catma.ui.module.annotate.resourcepanel.AnnotateResourcePanel;
import de.catma.ui.module.annotate.resourcepanel.ResourceSelectionListener;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.User;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class TaggerView extends HorizontalLayout 
	implements TaggerListener, ClosableTab {
	
	public interface AfterDocumentLoadedOperation {
		public void afterDocumentLoaded(TaggerView taggerView);
	}
	
	private static interface FontStateHandler {
		public FontState increase();
		public FontState decrease();
	}
	
	private static enum FontState {
		XSMALL("tagger-font-xsmall", new FontStateHandler() {
			@Override
			public FontState decrease() {
				return FontState.XSMALL;
			}
			@Override
			public FontState increase() {
				return FontState.SMALL;
			}
		}),
		SMALL("tagger-font-small", new FontStateHandler() {
			@Override
			public FontState decrease() {
				return FontState.XSMALL;
			}
			@Override
			public FontState increase() {
				return FontState.NORMAL;
			}
		}),
		NORMAL("tagger-font-normal", new FontStateHandler() {
			@Override
			public FontState decrease() {
				return FontState.SMALL;
			}
			@Override
			public FontState increase() {
				return FontState.LARGE;
			}
		}),
		LARGE("tagger-font-large", new FontStateHandler() {
			@Override
			public FontState decrease() {
				return FontState.NORMAL;
			}
			@Override
			public FontState increase() {
				return FontState.XLARGE;
			}
		}),
		XLARGE("tagger-font-xlarge", new FontStateHandler() {
			@Override
			public FontState decrease() {
				return FontState.LARGE;
			}
			@Override
			public FontState increase() {
				return FontState.XLARGE;
			}
		}),
		;
		
		private String style;
		private FontStateHandler fontStateHandler;
		private FontState(String style, FontStateHandler fontStateHandler) {
			this.style = style;
			this.fontStateHandler = fontStateHandler;
		}
		
		public String getStyle() {
			return style;
		}
		
		public FontStateHandler getFontStateHandler() {
			return fontStateHandler;
		}
	}
	
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private SourceDocument sourceDocument;
	private Tagger tagger;
	private Pager pager;
	private TagManager tagManager;
	private int taggerID;
	private Button btAnalyze;
	private Project project;
	private PagerComponent pagerComponent;
	private Slider linesPerPageSlider;
	private double totalLineCount;
	private PropertyChangeListener tagReferencesChangedListener;
	private int approxMaxLineLength;
	private int maxPageLengthInLines = 30;
	private int initialSplitterPositionInPixels = 530; // this can't be higher! Due to a strange rendering bug in some browsers (e. g. Chrome)

	private IconButton cbTraceSelection;
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
	private TabCaptionChangeListener tabNameChangeListener;
	private final List<Comment> comments = new ArrayList<Comment>();
	private TaggerSplitPanel splitPanel;
	private ITopic<CommentMessage> commentTopic;
	private UIMessageListener<CommentMessage> commentMessageListener;
	private UUID commentMessageListenerRegId;
	private IconButton cbAutoShowComments;
	
	private IconButton btIncreaseFont;
	private IconButton btDecreaseFont;
	private FontState fontState = FontState.NORMAL;
	
	public TaggerView(
			int taggerID, 
			SourceDocumentReference sourceDocumentReference, final Project project, 
			EventBus eventBus,
			AfterDocumentLoadedOperation afterDocumentLoadedOperation) {
		this.tagManager = project.getTagManager();
		this.project = project;
		try {
			if (sourceDocumentReference != null) {
				this.sourceDocument = project.getSourceDocument(sourceDocumentReference.getUuid());
			}
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Error showing the document", e);
		}
		this.eventBus = eventBus;
		this.approxMaxLineLength = getApproximateMaxLineLengthForSplitterPanel(initialSplitterPositionInPixels);
		this.userMarkupCollectionManager = new AnnotationCollectionManager(project);
		this.errorHandler = (ErrorHandler)UI.getCurrent();
		initComponents();
		initActions();
		initListeners();
		pager.setMaxPageLengthInLines(maxPageLengthInLines);
		initData(sourceDocumentReference, afterDocumentLoadedOperation);
		this.eventBus.register(this);
		final UI ui = UI.getCurrent();
		
		commentMessageListener = new CommentMessageListener(
				ui, 
				project, 
				cbAutoShowComments, 
				comments, 
				tagger, 
				() -> getSourceDocumentReference());
		
		addCommentMessageListener();
	}

	private void addCommentMessageListener() {
		try {
			removeCommentMessageListener();
			if (this.sourceDocument != null) {
				HazelCastService hazelcastService = ((CatmaApplication)UI.getCurrent()).getHazelCastService();
				this.commentTopic = 
					hazelcastService.getHazelcastClient().getTopic(
							HazelcastConfiguration.TopicName.COMMENT + "_" + this.sourceDocument.getUuid());
				
				this.commentMessageListenerRegId = 
					this.commentTopic.addMessageListener(this.commentMessageListener);
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Error registering listener for comment messages", e);
		}	
	}
	
	private void removeCommentMessageListener() {
		try {
			if ((this.commentTopic != null) && (commentMessageListenerRegId != null)) {
				boolean result = this.commentTopic.removeMessageListener(commentMessageListenerRegId);
				logger.info("Removal of comment message listener from topic: " + result);
				this.commentMessageListenerRegId = null;
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Error removing listener for comment messages", e);
		}
	}

	@Override
	public void contextMenuSelected(int x, int y) {
		if (annotationPanel.getSelectedEditableCollection() == null) {
			annotationPanel.highlightCurrentEditableCollectionBox();
		}
		else {
			if (taggerContextMenu != null) {
				taggerContextMenu.show(x, y);
			}
		}
	}
	
	private void initData(final SourceDocumentReference sdRef, final AfterDocumentLoadedOperation afterDocumentLoadedOperation) {
		if (sourceDocument != null) {
			
			// loading of the document is done in an extra step,
			// because of a client side rendering racing condition which prevents the first page to be displayed
			final UI ui = UI.getCurrent();
			((CatmaApplication)ui).submit("load-document",
			new DefaultProgressCallable<Void>() {
				@Override
				public Void call() throws Exception {
					ui.accessSynchronously(() -> {
						try {
							linesPerPageSlider.setEnabled(true);
							btAnalyze.setEnabled(project instanceof IndexedProject);
							pagerComponent.setEnabled(true);
							
							TaggerView.this.comments.clear();
							TaggerView.this.comments.addAll(TaggerView.this.project.getComments(sourceDocument.getUuid()));

							tagger.setText(sourceDocument.getContent(), TaggerView.this.comments);
							
							totalLineCount = pager.getTotalLineCount();
							try {
								linesPerPageSlider.setValue((100.0/totalLineCount)*maxPageLengthInLines);
							} catch (ValueOutOfBoundsException toBeIgnored) {}
							
							List<AnnotationCollectionReference> collectionReferences =
									resourcePanel.getSelectedAnnotationCollectionReferences();
							
							userMarkupCollectionManager.clear();
							
							for (AnnotationCollectionReference collectionRef : collectionReferences) {
								AnnotationCollection collection = project.getAnnotationCollection(collectionRef);
								userMarkupCollectionManager.add(collection);
							}
							
							Collection<TagsetDefinition> tagsets = 
									new HashSet<>(resourcePanel.getSelectedTagsets());
							
							annotationPanel.setData(
									sdRef,
									tagsets, 
									new ArrayList<>(userMarkupCollectionManager.getAnnotationCollections()));
							if (taggerContextMenu != null) {
								taggerContextMenu.setTagsets(tagsets);
							}
							
							if (afterDocumentLoadedOperation != null) {
								afterDocumentLoadedOperation.afterDocumentLoaded(TaggerView.this);
							}
							
							ui.push();
						} catch (Exception e) {
							errorHandler.showAndLogError("Error showing the document", e);
						}
					});
					return null;
				}
			}, new ExecutionListener<Void>() {
				@Override
				public void done(Void result) {/*noop*/}
				
				@Override
				public void error(Throwable t) {
					errorHandler.showAndLogError("Error showing the document", t);
				}
			});
		}			
		else {
			linesPerPageSlider.setEnabled(false);
			btAnalyze.setEnabled(false);
			pagerComponent.setEnabled(false);
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
								userMarkupCollectionManager.getAnnotationCollections())) {
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
						userMarkupCollectionManager.removeTagInstances(annotationIds, false);
					}
					
					tagger.removeTagInstances(annotationIds);
					annotationPanel.removeAnnotations(annotationIds);
				}
			}
		};
		
		project.addEventListener(
			ProjectEvent.tagReferencesChanged,
			tagReferencesChangedListener);
		
		annotationPropertiesChangedListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				TagInstance tagInstance = (TagInstance) evt.getOldValue();
				tagger.updateAnnotation(tagInstance.getUuid());
			}
		};
		project.addEventListener(
				ProjectEvent.propertyValueChanged,
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
						userMarkupCollectionManager.getCollectionReferencesForTagDefinition(deleted.getSecond())) {
					
						setAnnotationCollectionSelected(ref, false);
						setAnnotationCollectionSelected(ref, true);
					}
					
				}
				else { //update
					TagDefinition tag = (TagDefinition) newValue;
					
					for (AnnotationCollection collection : 
						userMarkupCollectionManager.getAnnotationCollections()) {
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
		try {
			SourceDocumentReference docRef = getSourceDocumentReference();
			corpus.addSourceDocument(docRef);
			
			for (AnnotationCollection umc : userMarkupCollectionManager.getAnnotationCollections()) {
				AnnotationCollectionReference userMarkupCollRef =
					docRef.getUserMarkupCollectionReference(umc.getId());
				if (userMarkupCollRef != null) {
					corpus.addUserMarkupCollectionReference(
							userMarkupCollRef);
				}
			}	
			if (project instanceof IndexedProject) {
				eventBus.post(new RouteToAnalyzeEvent((IndexedProject)project, corpus));
			}
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Error analyzing the document", e);
		}
	}

	private void initActions() {
		btClearSearchHighlights.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				tagger.removeHighlights();
			}
		});
		cbTraceSelection.addClickListener(clickEvent -> {
				boolean traceSelection = (boolean) cbTraceSelection.getData();
				traceSelection = !traceSelection;
				cbTraceSelection.setData(traceSelection);
				tagger.setTraceSelection(traceSelection);
				if (traceSelection) {
					cbTraceSelection.addStyleName("tagger-trace-checkbox-selected");
				}
				else {
					cbTraceSelection.removeStyleName("tagger-trace-checkbox-selected");
				}
		});
		
		cbAutoShowComments.addClickListener(clickEvent -> {
			CommentToggleState commentToggleState = (CommentToggleState) cbAutoShowComments.getData();
			
			switch (commentToggleState) {
			case OFF: {
				commentToggleState = CommentToggleState.ON;
				updateComments();
				break;
			}
			case ON: {
				commentToggleState = CommentToggleState.LIVE;
				updateComments();
				break;
			}
			case LIVE: {
				commentToggleState = CommentToggleState.OFF;
				break;
			}
			}

			tagger.setCommentsVisible(commentToggleState.isVisible());
			cbAutoShowComments.setIcon(commentToggleState.getIcon());
			cbAutoShowComments.setDescription(commentToggleState.getDescription(), ContentMode.HTML);
			cbAutoShowComments.setData(commentToggleState);
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
				
				Page currentPage = null;
				if (pager.hasPages()) {
					currentPage = pager.getCurrentPage();
				}
				
				pager.setMaxPageLengthInLines(lines);
				
				if (pager.hasPages()) {
					//recalculate pages
					try {
						pager.setText(sourceDocument.getContent(), comments);
						int previousPageNumber = pager.getPageNumberFor(currentPage.getPageStart());
						tagger.setPage(previousPageNumber);					
						tagger.setTagInstancesVisible(absoluteTagInstances, true);
	
						pagerComponent.setPage(previousPageNumber);
					} catch (IOException e) {
						errorHandler.showAndLogError("Error showing the document", e);
					}
				}
			}
		});
		
		resourcePanel.setSelectionListener(new ResourceSelectionListener() {
			
			@Override
			public void resourcesChanged() {
				AnnotationCollection selectedEditableCollection = 
						annotationPanel.getSelectedEditableCollection();
				
				List<AnnotationCollectionReference> selectedAnnotationCollectionRefs = 
						resourcePanel.getSelectedAnnotationCollectionReferences();
				
				for (AnnotationCollection collection : 
						userMarkupCollectionManager.getAnnotationCollections()) {
					userMarkupCollectionManager.remove(collection.getId());
					annotationPanel.removeCollection(collection.getId());
					tagger.setVisible(collection.getTagReferences(), false);						
				}
				
				userMarkupCollectionManager.clear();
				
				for (AnnotationCollectionReference collectionReference : selectedAnnotationCollectionRefs) {
					try {
						AnnotationCollection collection = project.getAnnotationCollection(collectionReference);
						setAnnotationCollectionSelected(
							new AnnotationCollectionReference(collection), 
							true);
					}
					catch (IOException e) {
						errorHandler.showAndLogError("Error refreshing annotation collection", e);
					}
				}
				
				if ((selectedEditableCollection != null) 
						&& (userMarkupCollectionManager.contains(selectedEditableCollection.getId()))) {
					annotationPanel.setSelectedEditableCollection(
						userMarkupCollectionManager.getAnnotationCollection(
								selectedEditableCollection.getId()));
				}
				
				annotationPanel.clearTagsets();
				tagsetsSelected(resourcePanel.getSelectedTagsets());
			}

			@Override
			public void documentSelected(SourceDocumentReference sourceDocumentReference) {
				setSourceDocument(sourceDocumentReference, null);
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
					for (AnnotationCollection collection : userMarkupCollectionManager.getAnnotationCollections()) {
						tagger.setVisible(collection.getTagReferences(), false);
						List<TagReference> visibleRefs = 
								annotationPanel.getVisibleTagReferences(collection.getTagReferences());
						if (!visibleRefs.isEmpty()) {
							tagger.setVisible(visibleRefs, true);
						}						
					}
				}
				catch (Exception e) {
					errorHandler.showAndLogError("Error handling tagset", e);
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
		
		btIncreaseFont.addClickListener(event -> {
			tagger.removeStyleName(fontState.getStyle());
			fontState = fontState.getFontStateHandler().increase();
			tagger.addStyleName(fontState.getStyle());
			btIncreaseFont.setEnabled(!fontState.equals(fontState.getFontStateHandler().increase()));
			btDecreaseFont.setEnabled(true);
		});
		
		btDecreaseFont.addClickListener(event -> {
			tagger.removeStyleName(fontState.getStyle());
			fontState = fontState.getFontStateHandler().decrease();
			tagger.addStyleName(fontState.getStyle());	
			btDecreaseFont.setEnabled(!fontState.equals(fontState.getFontStateHandler().decrease()));
			btIncreaseFont.setEnabled(true);
		});
	}

	private void updateComments() {
		try {
			TaggerView.this.comments.clear();
			TaggerView.this.comments.addAll(TaggerView.this.project.getComments(sourceDocument.getUuid()));
			tagger.updateComments(comments);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to reload comments", e);
		}
	}

	private void setAnnotationCollectionSelected(AnnotationCollectionReference collectionReference,
			boolean selected) {
		try {
			AnnotationCollection collection = project.getAnnotationCollection(collectionReference);
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
			errorHandler.showAndLogError("Error handling annotation collection", e);
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

		boolean isRtl = sourceDocument == null?false: 
			sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getIndexInfoSet().isRightToLeftWriting(); 

		pager = new Pager(taggerID, approxMaxLineLength, maxPageLengthInLines, 
				isRtl);
		
		tagger = new Tagger(taggerID, pager, this, project);
		tagger.addStyleName("tagger"); //$NON-NLS-1$
		tagger.addStyleName(this.fontState.getStyle());
		
		tagger.setWidth("100%"); //$NON-NLS-1$
		
		taggerPanel.addComponent(tagger);
		taggerPanel.setExpandRatio(tagger, 1.0f);
		
		HorizontalLayout actionPanel = new HorizontalLayout();
		actionPanel.setSpacing(false);
		
		taggerPanel.addComponent(actionPanel);
		
		pagerComponent = new PagerComponent(
			pager, 
			() -> pager.getLastPageNumber(), 
			new PageChangeListener() {						
				public void pageChanged(int number) {
					tagger.setPage(number);
				}
			}
		);
		
		
		actionPanel.addComponent(pagerComponent);
		
		linesPerPageSlider =  new Slider(1, 100, 0);
		linesPerPageSlider.setWidth("100px"); //$NON-NLS-1$
		actionPanel.addComponent(linesPerPageSlider);
		
		cbTraceSelection = new IconButton(VaadinIcons.TWIN_COL_SELECT);
		cbTraceSelection.setData(false); //state
		
		cbTraceSelection.setDescription("Allow multiple discontinuous selections");
		actionPanel.addComponent(cbTraceSelection);

		btClearSearchHighlights = new IconButton(VaadinIcons.ERASER);
		btClearSearchHighlights.setDescription("Clear all search highlights");
		actionPanel.addComponent(btClearSearchHighlights);
		
		CommentToggleState initialState = CommentToggleState.LIVE;
		cbAutoShowComments = new IconButton(initialState.getIcon());
		cbAutoShowComments.setDescription(initialState.getDescription(), ContentMode.HTML);
		cbAutoShowComments.setData(initialState); // state management
		actionPanel.addComponent(cbAutoShowComments);

		btDecreaseFont = new IconButton(VaadinIcons.FONT);
		btDecreaseFont.addStyleName("tagger-bt-decrease");
		btDecreaseFont.setDescription("Decrease font size");
		actionPanel.addComponent(btDecreaseFont);

		btIncreaseFont = new IconButton(VaadinIcons.FONT);
		btIncreaseFont.addStyleName("tagger-bt-increase");
		btIncreaseFont.setDescription("Increase font size");
		actionPanel.addComponent(btIncreaseFont);

		btAnalyze = new Button("Analyze");
		btAnalyze.addStyleName("primary-button");
		btAnalyze.setEnabled(project instanceof IndexedProject);
		actionPanel.addComponent(btAnalyze);

		
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
			collectionChangeEvent -> handleCollectionValueChange(collectionChangeEvent),
			tag -> tagger.addTagInstanceWith(tag),
			() -> getSourceDocumentReference(),
			eventBus);
		rightSplitPanel.addComponent(annotationPanel);
		
		splitPanel = new TaggerSplitPanel();
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
				
				Page currentPage = null;
				
				if (pager.hasPages()) {
					currentPage = pager.getCurrentPage();
				}
				
				pager.setApproxMaxLineLength(approxMaxLineLength);
				
				if (pager.hasPages()) {
					//recalculate pages
					try {
						pager.setText(sourceDocument.getContent(), comments);
						int previousPageNumber = pager.getPageNumberFor(currentPage.getPageStart());
						tagger.setPage(previousPageNumber);					
						tagger.setTagInstancesVisible(absoluteTagInstances, true);
	
						pagerComponent.setPage(previousPageNumber);
					} catch (IOException e) {
						errorHandler.showAndLogError("Error showing the document", e); //$NON-NLS-1$
					}
				}
			}
			
		};
		
		splitPanel.addListener(SplitterPositionChangedEvent.class,
                listener, SplitterPositionChangedListener.positionChangedMethod);
		SourceDocumentReference preselection = null;
		
		try {
			if (this.sourceDocument != null) {
				preselection = project.getSourceDocumentReference(this.sourceDocument.getUuid());
			}
		} catch (Exception e) {
			errorHandler.showAndLogError("Error loading document", e);
		} 
		resourcePanel = new AnnotateResourcePanel(
				project, 
				preselection, eventBus);

		drawer = new SliderPanelBuilder(resourcePanel)
				.mode(SliderMode.LEFT).expanded(sourceDocument == null).build();
		
		addComponent(drawer);
		
		addComponent(splitPanel);
		setExpandRatio(splitPanel, 1.0f);
	}
	
	private void handleCollectionValueChange(ValueChangeEvent<AnnotationCollection> collectionChangeEvent) {
		AnnotationCollection collection = collectionChangeEvent.getValue();
		if (collection == null) {
			if (taggerContextMenu != null) {
				taggerContextMenu.close();
				taggerContextMenu = null;
			}
		}
		else if (taggerContextMenu == null && !project.isReadOnly()) {
			taggerContextMenu = new TaggerContextMenu(
					tagger, 
					this.tagManager);
				
			taggerContextMenu.setTagSelectionListener(
					tag -> tagger.addTagInstanceWith(tag));
			Collection<TagsetDefinition> tagsets = 
					new HashSet<>(resourcePanel.getSelectedTagsets());
			taggerContextMenu.setTagsets(tagsets);
		}
	
		if (collectionChangeEvent.getOldValue() != null && !project.isReadOnly()) {
			try {
				project.commitAndPushChanges("Auto-committing annotations");
			} catch (Exception e) {
				logger.log(
					Level.WARNING, 
					"Error auto-committing annotations due to a switch to a "
					+ "different editable target collection", e);
			}
		}
	}

	public int getApproximateMaxLineLengthForSplitterPanel(float width){
		// based on ratio of 80:550
		int approxMaxLineLength = (int) (width * 0.125);
		
		return approxMaxLineLength;
	}

	public SourceDocumentReference getSourceDocumentReference() {
		if (this.sourceDocument == null) {
			return null;
		}
		
		try {
			return project.getSourceDocumentReference(this.sourceDocument.getUuid());
		} catch (Exception e) {
			errorHandler.showAndLogError("Error loading document", e);
			return null;
		}
	}
	
	public AnnotationCollection openUserMarkupCollection(
			AnnotationCollectionReference userMarkupCollectionRef) throws IOException {
		AnnotationCollection umc = project.getAnnotationCollection(userMarkupCollectionRef);
		openUserMarkupCollection(umc);
		return umc;
	}
	
	public void openUserMarkupCollection(
			AnnotationCollection userMarkupCollection) {
		resourcePanel.selectCollectionVisible(userMarkupCollection.getUuid());
	}

	public void close() {
		removeCommentMessageListener();

		this.eventBus.unregister(this);
		annotationPanel.close();
		resourcePanel.close();
		if (taggerContextMenu != null) {
			taggerContextMenu.close();
		}
		project.removeEventListener(
				ProjectEvent.tagReferencesChanged,
				tagReferencesChangedListener);
		project.removeEventListener(
				ProjectEvent.propertyValueChanged,
				annotationPropertiesChangedListener);
		project.getTagManager().removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);
		
		try {
			project.commitAndPushChanges("Auto-committing annotations");
		} catch (Exception e) {
			logger.log(
				Level.WARNING, 
				"Error auto-committing annotations due to closing the TaggerView", e);
		}	
		
		project = null;
	}

	public void tagInstanceAdded(ClientTagInstance clientTagInstance) {
		AnnotationCollection annotationCollection = annotationPanel.getSelectedEditableCollection();

		if (annotationCollection == null) { // shouldn't happen, but just in case
			Notification.show(
					"Info",
					"Please make sure you have an editable collection available and select this collection as 'currently being edited'! " +
							"Your annotation hasn't been saved!",
					Type.ERROR_MESSAGE
			);
			return;
		}

		TagLibrary tagLibrary = annotationCollection.getTagLibrary();
		TagDefinition tagDefinition = tagLibrary.getTagDefinition(clientTagInstance.getTagDefinitionID());

		TagInstance tagInstance = new TagInstance(
				clientTagInstance.getInstanceID(),
				tagDefinition.getUuid(),
				project.getCurrentUser().getIdentifier(),
				ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
				tagDefinition.getUserDefinedPropertyDefinitions(),
				tagDefinition.getTagsetDefinitionUuid()
		);

		List<TagReference> tagReferences = clientTagInstance.getRanges().stream().map(textRange -> new TagReference(
				annotationCollection.getId(),
				tagInstance,
				sourceDocument.getUuid(),
				new Range(textRange.getStartPos(), textRange.getEndPos())
		)).collect(Collectors.toList());

		final Annotation annotation = new Annotation(tagInstance, tagReferences, annotationCollection, tagLibrary.getTagPath(tagDefinition));

		// if the tag has properties, automatically open EditAnnotationPropertiesDialog
		if (!tagDefinition.getUserDefinedPropertyDefinitions().isEmpty()) {
			EditAnnotationPropertiesDialog editAnnotationPropertiesDialog = new EditAnnotationPropertiesDialog(
					project,
					annotation,
					new SaveCancelListener<List<Property>>() {
						@Override
						public void savePressed(List<Property> notOfInterest) {
							userMarkupCollectionManager.addTagReferences(tagReferences, annotationCollection);
						}
					}
			);
			editAnnotationPropertiesDialog.show();
		}
		else {
			userMarkupCollectionManager.addTagReferences(tagReferences, annotationCollection);
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
						pager.setText(sourceDocument.getContent(), comments);
					} catch (IOException e) {
						logger.log(Level.SEVERE, "Error adjusting page zoom", e); //$NON-NLS-1$
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
			logger.log(Level.SEVERE, "Error during highlighting", e); //$NON-NLS-1$
		}
	}
	
	public void tagInstanceSelected(String instancePartID, String lineID) {
		if (pager.hasPages()) {
			try {
				annotationPanel.showAnnotationDetails(
						userMarkupCollectionManager.getAnnotationsForTagInstances(
								pager.getCurrentPage().getTagInstanceIDs(instancePartID, lineID)));
			} catch (IOException e) {
				errorHandler.showAndLogError("Error showing annotation details", e);
			}
		}
	}
	
	@Override
	public void tagInstanceSelected(Set<String> tagInstanceIDs) {
		try {
			annotationPanel.showAnnotationDetails(
				userMarkupCollectionManager.getAnnotationsForTagInstances(tagInstanceIDs));
		} catch (IOException e) {
			errorHandler.showAndLogError("Error showing annotation details", e);
		}
	}
	
	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }

	public void setSourceDocument(SourceDocumentReference sdRef, final AfterDocumentLoadedOperation afterDocumentLoadedOperation) {
		boolean tryAutoCommit = this.sourceDocument != null;
		try {
			this.sourceDocument = project.getSourceDocument(sdRef.getUuid());
			
			this.resourcePanel.setSelectedDocument(sdRef);
			
			pager.setRightToLeftWriting(
				this.sourceDocument
				.getSourceContentHandler()
				.getSourceDocumentInfo()
				.getIndexInfoSet()
				.isRightToLeftWriting());
			
			initData(sdRef, afterDocumentLoadedOperation);
			if (tabNameChangeListener != null) {
				tabNameChangeListener.tabCaptionChange(this);
			}
			this.drawer.collapse();
			
			addCommentMessageListener();
		} catch (Exception e) {
			errorHandler.showAndLogError("Error opening document", e);
		}
		
		if (tryAutoCommit) {
			try {
				project.commitAndPushChanges("Auto-committing annotations");
			} catch (Exception e) {
				logger.log(
					Level.WARNING, 
					"Error auto-committing annotations due to a switch to a "
					+ "different document", e);
			}			
		}
	}

	@Override
	public Annotation getTagInstanceInfo(String tagInstanceId) {
		return userMarkupCollectionManager.getAnnotationForTagInstance(tagInstanceId);
	}
	
	@Override
	public void setTabNameChangeListener(TabCaptionChangeListener tabNameChangeListener) {
		this.tabNameChangeListener =  tabNameChangeListener;
	}
	
	@Override
	public String getCaption() {
		return this.sourceDocument==null?"no selection yet":sourceDocument.toString();
	}

	@Override
	public void addComment(List<Range> absoluteRanges, int x, int y) {
		User user = project.getCurrentUser();
		IDGenerator idGenerator = new IDGenerator();
		CommentDialog commentDialog = new CommentDialog(
				commentBody -> {
					if (!StringUtils.isBlank(commentBody)) {
						try {
							project.addComment(
									new Comment(
											idGenerator.generate(),
											user.getName(),
											user.getUserId(),
											commentBody,
											absoluteRanges,
											sourceDocument.getUuid()
									)
							);
						}
						catch (IOException e) {
							errorHandler.showAndLogError("Failed to create comment", e);
						}
					}
				}
		);
		commentDialog.show(x, y);
	}

	@Override
	public void editComment(Optional<Comment> optionalComment, int x, int y) {
		if (!optionalComment.isPresent()) {
			Notification.show("Info", "Couldn't find the comment to edit", Type.HUMANIZED_MESSAGE);
			return;
		}

		CommentDialog commentDialog = new CommentDialog(
				optionalComment.get().getBody(),
				false,
				commentBody -> {
					if (!StringUtils.isBlank(commentBody)) {
						final String oldBody = optionalComment.get().getBody();
						optionalComment.get().setBody(commentBody);
						try {
							project.updateComment(optionalComment.get());
						}
						catch (IOException e) {
							errorHandler.showAndLogError("Failed to update comment", e);
							optionalComment.get().setBody(oldBody);
						}
					}
				}
		);
		commentDialog.show(x, y);
	}

	@Override
	public void removeComment(Optional<Comment> optionalComment) {
		if (!optionalComment.isPresent()) {
			Notification.show("Info", "Couldn't find the comment to delete", Type.HUMANIZED_MESSAGE);
			return;
		}

		ConfirmDialog.show(
				UI.getCurrent(),
				"Delete Comment",
				"Are you sure you want to delete this comment?",
				"Yes",
				"Cancel",
				dlg -> {
					if (dlg.isConfirmed()) {
						try {
							project.removeComment(optionalComment.get());
						}
						catch (IOException e) {
							errorHandler.showAndLogError("Failed to delete comment", e);
						}
					}
				}
		);
	}

	@Override
	public void replyToComment(Optional<Comment> optionalComment, int x, int y) {
		if (!optionalComment.isPresent()) {
			Notification.show("Info", "Couldn't find the comment to reply to", Type.HUMANIZED_MESSAGE);
			return;
		}

		User user = project.getCurrentUser();
		IDGenerator idGenerator = new IDGenerator();
		CommentDialog commentDialog = new CommentDialog(
				true,
				replyBody -> {
					if (!StringUtils.isBlank(replyBody)) {
						try {
							project.addCommentReply(
									optionalComment.get(),
									new Reply(
											idGenerator.generate(),
											replyBody,
											user.getName(),
											user.getUserId(),
											optionalComment.get().getUuid()
									)
							);
						}
						catch (IOException e) {
							errorHandler.showAndLogError("Failed to create reply", e);
						}
					}
				}
		);
		commentDialog.show(x, y);
	}

	public void updateReplyToComment(Optional<Comment> optionalComment, String replyUuid, int x, int y) {
		if (!optionalComment.isPresent()) {
			Notification.show("Info", "Couldn't find the comment that the reply relates to", Type.HUMANIZED_MESSAGE);
			return;
		}

		Comment comment = optionalComment.get();
		Reply reply = comment.getReply(replyUuid);

		if (reply == null) {
			Notification.show("Info", "Couldn't find the reply to edit", Type.HUMANIZED_MESSAGE);
			return;
		}

		CommentDialog commentDialog = new CommentDialog(
				reply.getBody(),
				true,
				replyBody -> {
					if (!StringUtils.isBlank(replyBody)) {
						final String oldBody = reply.getBody();
						reply.setBody(replyBody);
						try {
							project.updateCommentReply(optionalComment.get(), reply);
						}
						catch (IOException e) {
							errorHandler.showAndLogError("Failed to update reply", e);
							reply.setBody(oldBody);
						}
					}
				}
		);
		commentDialog.show(x, y);
	}

	public void removeReplyToComment(Optional<Comment> optionalComment, String replyUuid) {
		if (!optionalComment.isPresent()) {
			Notification.show("Info", "Couldn't find the comment that the reply relates to", Type.HUMANIZED_MESSAGE);
			return;
		}

		Comment comment = optionalComment.get();
		Reply reply = comment.getReply(replyUuid);

		if (reply == null) {
			Notification.show("Info", "Couldn't find the reply to delete", Type.HUMANIZED_MESSAGE);
			return;
		}

		ConfirmDialog.show(
				UI.getCurrent(),
				"Delete Reply",
				"Are you sure you want to delete this reply?",
				"Yes",
				"Cancel",
				dlg -> {
					if (dlg.isConfirmed()) {
						try {
							project.deleteCommentReply(optionalComment.get(), reply);
						}
						catch (IOException e) {
							errorHandler.showAndLogError("Failed to delete reply", e);
						}
					}
				}
		);
	}

	@Subscribe
	public void handleCommentChange(CommentChangeEvent commentChangeEvent) {
		switch(commentChangeEvent.getChangeType()) {
		case CREATED: {
			try {
				comments.add(commentChangeEvent.getComment());
				tagger.addComment(commentChangeEvent.getComment());
			} catch (IOException e) {
				errorHandler.showAndLogError("Error adding comment", e);
			}
			break;
		}
		case UPDATED: {
			tagger.updateComment(commentChangeEvent.getComment());
			break;
		}
		case DELETED: {
			this.comments.remove(commentChangeEvent.getComment());
			tagger.removeComment(commentChangeEvent.getComment());
			break;
		}
		}
		try {
			if (commentTopic != null) {
				Comment comment = commentChangeEvent.getComment();
				ClientComment clientComment = new ClientComment(
						comment.getUuid(),
						comment.getUsername(),
						comment.getUserId(),
						comment.getBody(),
						comment.getReplyCount(),
						comment.getRanges()
							.stream()
							.map(r -> new TextRange(r.getStartPoint(), r.getEndPoint()))
							.collect(Collectors.toList()));

				
				commentTopic.publish(
					new CommentMessage(
						comment.getId(),
						comment.getIid(),
						project.getCurrentUser().getUserId(),
						clientComment,
						comment.getDocumentId(),
						commentChangeEvent.getChangeType() == ChangeType.DELETED
				));
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Error publishing a comment message", e);
		}
	}
	
	@Subscribe
	public void handleReplyChange(ReplyChangeEvent replyChangeEvent) {
		switch(replyChangeEvent.getChangeType()) {
		case CREATED: {
			try {
				tagger.addReply(replyChangeEvent.getComment(), replyChangeEvent.getReply());
			} catch (IOException e) {
				errorHandler.showAndLogError("Error adding reply", e);
			}
			break;
		}
		case UPDATED: {
			try {
				tagger.updateReply(replyChangeEvent.getComment(), replyChangeEvent.getReply());
			} catch (IOException e) {
				errorHandler.showAndLogError("Error updating reply", e);
			}
			break;
		}
		case DELETED: {
			try {
				tagger.removeReply(replyChangeEvent.getComment(), replyChangeEvent.getReply());
			} catch (IOException e) {
				errorHandler.showAndLogError("Error removing reply", e);
			}
			break;
		}
		}
		try {
			if (commentTopic != null) {
				Comment comment = replyChangeEvent.getComment();
				ClientComment clientComment = new ClientComment(
						comment.getUuid(),
						comment.getUsername(),
						comment.getUserId(),
						comment.getBody(),
						comment.getReplyCount(),
						comment.getRanges()
							.stream()
							.map(r -> new TextRange(r.getStartPoint(), r.getEndPoint()))
							.collect(Collectors.toList()));
				
				Reply reply = replyChangeEvent.getReply();
				
				ClientCommentReply clientCommentReply = new ClientCommentReply(
						reply.getUuid(), 
						reply.getBody(), 
						reply.getUsername(), 
						reply.getUserId(), 
						reply.getCommentUuid());
				

				
				commentTopic.publish(
					new CommentMessage(
						comment.getId(),
						comment.getIid(),
						project.getCurrentUser().getUserId(),
						clientComment,
						comment.getDocumentId(),
						replyChangeEvent.getChangeType() == ChangeType.DELETED,
						reply.getId(),
						clientCommentReply));
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Error publishing a comment message", e);
		}
	}
	
	@Override
	public void loadReplies(Optional<Comment> optionalComment) {
		if(optionalComment.isPresent()) {
			final Comment comment = optionalComment.get();
			final UI ui = UI.getCurrent();
			((BackgroundServiceProvider)ui).submit(
				"load-comment-replies", new DefaultProgressCallable<Void>() {
					@Override
					public Void call() throws Exception {
						ui.accessSynchronously(() -> {
							try {
								List<Reply> replies = project.getCommentReplies(comment);
								tagger.setReplies(replies, comment);
								ui.push();
							}
							catch(IOException e) {
								logger.log(Level.WARNING, "Error loading replies", e);
							}
						});
						
						
						return null;
					}
				}, new ExecutionListener<Void>() {
					@Override
					public void done(Void result) {
						ui.push();
					}
					@Override
					public void error(Throwable t) {
						errorHandler.showAndLogError("Error loading replies", t);
					}
				});
		}
		
		
	}
}

