package de.catma.ui.tagger.annotationpanel;

import java.io.IOException;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.UI;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.Annotation;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.KwicProvider;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.util.Cleaner;
import de.catma.util.ColorConverter;

public class AnnotationDataItem implements AnnotationTreeItem {
	private static final int SMALL_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH = 30;
	private static final int LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH = 300;

	private Annotation annotation;
	private String annotatedText;
	private String keywordInContext;
	private KwicProvider kwicProvider;
	private TagsetDefinition tagset;
	
	public AnnotationDataItem(Annotation annotation,  TagsetDefinition tagset, KwicProvider kwicProvider) {
		super();
		this.annotation = annotation;
		this.kwicProvider = kwicProvider;
		this.tagset = tagset;
		this.annotatedText = buildAnnotatedText();
		this.keywordInContext = buildKeywordInContext();
	}
	
	private String buildAnnotatedText() {
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
			.map(keyword -> shorten(keyword, SMALL_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH))
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

	@Override
	public String getDetail() {
		return annotatedText;
	}

	@Override
	public String getTag() {
		return annotation.getTagInstance().getTagDefinition().getName();
	}

	@Override
	public String getAuthor() {
		return annotation.getTagInstance().getAuthor();
	}

	@Override
	public String getCollection() {
		return annotation.getUserMarkupCollection().getName();
	}

	@Override
	public String getTagset() {
		return tagset.getName();
	}

	@Override
	public String getAnnotationId() {
		return annotation.getTagInstance().getUuid();
	}
	
	@Override
	public String getDescription() {
		return keywordInContext;
	}

	private String buildKeywordInContext() {
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
						shorten(
								kwic.getKeyword(), 
								LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH)));
				builder.append("</span>");	
			
				builder.append(Cleaner.clean(kwic.getForwardContext()));
				builder.append(conc);
				conc = " [" + HORIZONTAL_ELLIPSIS + "] ";
			}
			
			builder.append("<br /><hr />");
			builder.append("Tag Path: <b>");
			builder.append(tagset.getTagPath(annotation.getTagInstance().getTagDefinition()));
			builder.append("</b>");
		}
		catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError(
					"Error loading keyword in context!", e);
		}		
		
		return builder.toString();
	}
	
	@Override
	public String getEditIcon() {
		return VaadinIcons.EDIT.getHtml();
	}
	
	@Override
	public String getDeleteIcon() {
		return VaadinIcons.TRASH.getHtml();
	}
	
	public Annotation getAnnotation() {
		return annotation;
	}
	
}
