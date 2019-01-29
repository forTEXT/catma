package de.catma.ui.tagger.annotationpanel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.DescriptionGenerator;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.Annotation;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.KwicProvider;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.util.Cleaner;
import de.catma.util.ColorConverter;

public class AnnotationDetailsPanel extends VerticalLayout {
	private static final int SMALL_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH = 30;
	private static final int LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH = 300;
	private static final String HORIZONTAL_ELLIPSIS = "\u2026";
	private Grid<Annotation> annotationDetailsGrid;
	private Button btMinimize;
	private Repository project;
	private KwicProvider kwicProvider;
	private ListDataProvider<Annotation> annotationDataProvider;

	public AnnotationDetailsPanel(Repository project) {
		this.project = project;
		initComponents();
		
	}
	
	public void setDocument(SourceDocument document) throws IOException {
		this.kwicProvider = new KwicProvider(document);
	}
	
	private void initComponents() {
		setSpacing(true);
		setSizeFull();
		addStyleName("annotation-details-panel");
		
		HorizontalLayout headerPanel = new HorizontalLayout();
		headerPanel.setWidth("100%");
		addComponent(headerPanel);
		
		btMinimize = new IconButton(VaadinIcons.ANGLE_DOUBLE_DOWN);
		headerPanel.addComponent(btMinimize);
		headerPanel.setComponentAlignment(btMinimize, Alignment.TOP_RIGHT);
		
		annotationDataProvider = new ListDataProvider<Annotation>(new ArrayList<>());
		annotationDetailsGrid = new Grid<>(annotationDataProvider);
		annotationDetailsGrid.setSizeFull();
		annotationDetailsGrid.addStyleName("annotation-details-panel-annotation-details-grid");
		ButtonRenderer<Annotation> togglePropertyDetailsRenderer = 
				new ButtonRenderer<>(event -> handleToggleDetailsEvent(event));
		togglePropertyDetailsRenderer.setHtmlContentAllowed(true);
		
		annotationDetailsGrid.addColumn(
			annotation -> annotationDetailsGrid.isDetailsVisible(annotation)?
					VaadinIcons.CARET_DOWN.getHtml()
					:VaadinIcons.CARET_RIGHT.getHtml(),
			togglePropertyDetailsRenderer);
		
		annotationDetailsGrid.addColumn(annotation -> 
			getAnnotatedText(annotation),
			new HtmlRenderer())
		.setCaption("Annotation");
		
		annotationDetailsGrid.addColumn(
			annotation -> annotation.getTagInstance().getTagDefinition().getName())
		.setCaption("Tag");
		
		annotationDetailsGrid.addColumn(
			annotation -> annotation.getTagInstance().getAuthor())
		.setCaption("Author");
		
		annotationDetailsGrid.addColumn(
			annotation -> annotation.getUserMarkupCollection().getName())
		.setCaption("Collection");
		
		annotationDetailsGrid.addColumn(
			annotation -> project.getTagManager().getTagLibrary().getTagsetDefinition(
					annotation.getTagInstance().getTagDefinition().getTagsetDefinitionUuid()).getName())
		.setCaption("Tagset");
		
		annotationDetailsGrid.addColumn(
			annotation -> annotation.getTagInstance().getUuid())
		.setCaption("Annotation ID")
		.setHidable(true)
		.setHidden(true);
		
		annotationDetailsGrid.setDescriptionGenerator(new DescriptionGenerator<Annotation>() {
			@Override
			public String apply(Annotation annotation) {
				return getKeywordInContext(annotation);
			}
		}, ContentMode.HTML);
		
		
		annotationDetailsGrid.setDetailsGenerator(annotation -> new AnnotationDetailsCard(annotation));
		
		//Edit/ Delete
		//
		//
		
		ActionGridComponent<Grid<Annotation>> annotationDetailsGridComponent = 
				new ActionGridComponent<>(new Label("Selected Annotations"), annotationDetailsGrid);
		
		addComponent(annotationDetailsGridComponent);
		setExpandRatio(annotationDetailsGridComponent, 1.0f);
	}
	
	
	
	private void handleToggleDetailsEvent(RendererClickEvent<Annotation> event) {
		annotationDetailsGrid.setDetailsVisible(
			event.getItem(), 
			!annotationDetailsGrid.isDetailsVisible(event.getItem()));
	}

	public Registration addMinimizeButtonClickListener(ClickListener listener) {
		return btMinimize.addClickListener(listener);
	}

	public void addAnnotation(Annotation annotation) throws IOException {
		annotationDataProvider.getItems()
		.forEach(
			anno -> annotationDetailsGrid.setDetailsVisible(anno, false));
		annotationDataProvider.getItems().add(annotation);
		annotationDetailsGrid.setDetailsVisible(annotation, true);
		annotationDataProvider.refreshAll();
	}
	
	private String getKeywordInContext(Annotation annotation) {
		StringBuilder builder = new StringBuilder();
		
		List<Range> ranges = Range.mergeRanges(
				new TreeSet<>(annotation.getTagReferences()
				.stream()
				.map(tagRef -> tagRef.getRange())
				.collect(Collectors.toList())));
		try {
			List<KeywordInSpanContext> kwics = kwicProvider.getKwic(ranges, 5);
			String conc = "";
			for (KeywordInSpanContext kwic : kwics) {
				
				
				builder.append(Cleaner.clean(kwic.getBackwardContext()));

				builder.append("<span");
				builder.append(" class=\"annotation-details-tag-color\"");
				builder.append(" style=\"");
				builder.append(" background-color:");
				builder.append("#"+ColorConverter.toHex(
						annotation.getTagInstance().getTagDefinition().getColor()));
				builder.append(";");
				builder.append(" color:");
				builder.append(ColorConverter.isLightColor(
					annotation.getTagInstance().getTagDefinition().getColor())?"black":"white");
				builder.append(";");
				builder.append("\">");
				builder.append(
					Cleaner.clean(
						shortenKeyword(
								kwic.getKeyword(), 
								LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH)));
				builder.append("</span>");	
			
				builder.append(Cleaner.clean(kwic.getForwardContext()));
				builder.append(conc);
				conc = " [" + HORIZONTAL_ELLIPSIS + "] ";
			}
		}
		catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError(
					"Error loading keyword in context!", e);
		}		
		
		return builder.toString();
	}

	private String getAnnotatedText(Annotation annotation) {
		StringBuilder builder = new StringBuilder();
		builder.append("<div");
		builder.append(" class=\"annotation-details-tag-color\"");
		builder.append(" style=\"");
		builder.append(" background-color:");
		builder.append("#"+ColorConverter.toHex(
				annotation.getTagInstance().getTagDefinition().getColor()));
		builder.append(";");
		builder.append(" color:");
		builder.append(ColorConverter.isLightColor(
			annotation.getTagInstance().getTagDefinition().getColor())?"black":"white");
		builder.append(";");
		builder.append("\">");
		
		List<Range> ranges = Range.mergeRanges(
				new TreeSet<>(annotation.getTagReferences()
				.stream()
				.map(tagRef -> tagRef.getRange())
				.collect(Collectors.toList())));
		try {
			List<KeywordInSpanContext> kwics = kwicProvider.getKwic(ranges, 5);

			String joinedAnnotatedText = kwics.stream()
			.map(kwic -> kwic.getKeyword())
			.map(keyword -> shortenKeyword(keyword, SMALL_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH))
			.collect(Collectors.joining(" [" + HORIZONTAL_ELLIPSIS + "] "));
			
			builder.append(joinedAnnotatedText);
		}
		catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError(
					"Error loading annotated text!", e);
			builder.append("&nbsp;");
		}		
		builder.append("</div>");		
		
		return builder.toString();
	}
	
	private String shortenKeyword(String keyword, int maxLength) {
		if (keyword.length() <= maxLength) {
			return keyword;
		}
		
		return keyword.substring(0, maxLength/2) 
				+"["+HORIZONTAL_ELLIPSIS+"]"
				+ keyword.substring(keyword.length()-((maxLength/2)-2), keyword.length());
	}
}
