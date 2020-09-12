package de.catma.ui.module.annotate.annotationpanel;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.vaadin.ui.UI;

import de.catma.document.Range;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.KwicProvider;
import de.catma.tag.TagDefinition;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.util.Cleaner;
import de.catma.util.ColorConverter;

public class AnnotatedTextProvider {
	public static final int SMALL_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH = 30;
	public static final int LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH = 300;

	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 100;

	public static String shorten(String keyword, int maxLength) {
		if (keyword == null) {
			return "";
		}
		
		if (keyword.length() <= maxLength) {
			return keyword;
		}
		
		return keyword.substring(0, maxLength/2) 
				+"["+HORIZONTAL_ELLIPSIS+"]"
				+ keyword.substring(keyword.length()-((maxLength/2)-2), keyword.length());
	}

	private static String buildKeywordInContext(
			String keyword, Range range, KwicProvider kwicProvider, int keywordLength) {

		StringBuilder builder = new StringBuilder();

		try {
			KeywordInSpanContext kwic = kwicProvider.getKwic(range, 5);
			builder.append(Cleaner.clean(kwic.getBackwardContext()));

			builder.append("<span");
			builder.append(" class=\"annotation-details-tag-color\"");
			builder.append(" style=\"");
			builder.append(" background-color:");
			builder.append("#cacfd2");
			builder.append(";");
			builder.append("\">");
			builder.append(
				Cleaner.clean(
					shorten(
							kwic.getKeyword(), 
							keywordLength)));
			builder.append("</span>");	
		
			builder.append(Cleaner.clean(kwic.getForwardContext()));
		}
		catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError(
					"Error loading keyword in context!", e);
		}		
		
		return builder.toString();
	}
	
	public static String buildKeywordInContext(
		String keyword, Range range, KwicProvider kwicProvider) {
		
		return buildKeywordInContext(
			keyword, range, kwicProvider, SMALL_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH);
	}
	
	public static String buildKeywordInContextLarge(
			String keyword, Range range, KwicProvider kwicProvider) {
		
		return buildKeywordInContext(
			keyword, range, kwicProvider, LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH);
	}

	public static String buildAnnotatedKeywordInContext(
			Collection<TagReference> tagReferences, KwicProvider kwicProvider, 
			TagDefinition tagDefinition, String tagPath) {
		return buildAnnotatedKeywordInContext(
					tagReferences
					.stream()
					.map(tagRef -> tagRef.getRange())
					.collect(Collectors.toList()), 
				kwicProvider, 
				tagDefinition, 
				tagPath);
	}
	
	public static String buildAnnotatedKeywordInContext(
		List<Range> ranges, KwicProvider kwicProvider, 
		TagDefinition tagDefinition, String tagPath) {

		StringBuilder builder = new StringBuilder();
		
		try {
			List<KeywordInSpanContext> kwics = kwicProvider.getKwic(Range.mergeRanges(new TreeSet<>(ranges)), 5);
			String conc = "";
			for (KeywordInSpanContext kwic : kwics) {
				builder.append(Cleaner.clean(kwic.getBackwardContext()));

				builder.append("<span");
				builder.append(" class=\"annotation-details-tag-color\"");
				builder.append(" style=\"");
				builder.append(" background-color:");
				builder.append("#"+ColorConverter.toHex(
						tagDefinition.getColor()));
				builder.append(";");
				builder.append(" color:");
				builder.append(ColorConverter.isLightColor(
					tagDefinition.getColor())?"black":"white");
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
			builder.append(tagPath);
			builder.append("</b>");
		}
		catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError(
					"Error loading keyword in context!", e);
		}		
		
		return builder.toString();
	}


	public static String buildAnnotatedText(
			Collection<TagReference> tagReferences, KwicProvider kwicProvider, TagDefinition tagDefinition) {
		return buildAnnotatedText(Range.mergeRanges(
				new TreeSet<>(
					tagReferences
					.stream()
					.map(tagRef -> tagRef.getRange())
					.collect(Collectors.toList()))), kwicProvider, tagDefinition);
	}
	public static String buildAnnotatedText(
			List<Range> ranges, KwicProvider kwicProvider, TagDefinition tagDefinition) {
		StringBuilder builder = new StringBuilder();
		builder.append("<div");
		builder.append(" class=\"annotation-details-tag-color\"");
		if (tagDefinition != null) {
			builder.append(" style=\"");
			builder.append(" background-color:");
			builder.append("#"+ColorConverter.toHex(
					tagDefinition.getColor()));
			builder.append(";");
			builder.append(" color:");
			builder.append(ColorConverter.isLightColor(
				tagDefinition.getColor())?"black":"white");
			builder.append(";");
		}
		builder.append("\">");
		
		try {
			List<KeywordInSpanContext> kwics = kwicProvider.getKwic(Range.mergeRanges(new TreeSet<>(ranges)), 5);

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

	public static String buildCommentedKeyword(String phrase, Comment comment) {
		StringBuilder builder = new StringBuilder();
		builder.append("<div class=\"commented-keyword\">");
		builder.append(Cleaner.clean(shorten(phrase, SMALL_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH)));
		builder.append("</div>");
		builder.append("<div class=\"commented-keyword-comment\">");
		builder.append("<div class=\"commented-keyword-username\">");
		builder.append(Cleaner.clean(comment.getUsername()));
		if (comment.getReplyCount() > 0) {
			builder.append("(+");
			builder.append(comment.getReplyCount());
			builder.append(comment.getReplyCount()==1?" reply)":" replies)");
		}
		builder.append(":");
		builder.append("</div>");
		builder.append(Cleaner.clean(
				shorten(
						comment.getBody(), 
						LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH)));
		builder.append("</div>");
		return builder.toString();
	}
	
}
