package de.catma.queryengine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import de.catma.ui.tagger.annotationpanel.AnnotatedTextProvider;

public class QueryId {
	private final String query;
	private String name;
	private final LocalDateTime timestamp;
	
	private QueryId(String query, String timestamp) {
		this.query = query;
		this.timestamp = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
		this.name = query + " " + this.timestamp.format(DateTimeFormatter.ISO_LOCAL_TIME); 
	}
	
	public QueryId(String query) {
		this.query = query;
		this.timestamp = LocalDateTime.now();
		this.name = query + " " + this.timestamp.format(DateTimeFormatter.ISO_LOCAL_TIME);
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getQuery() {
		return query;
	}

	public String getName() {
		return name;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public static QueryId fromString(String queryIdStr) {
		String[] parts = queryIdStr.split(Pattern.quote("@"));
		return new QueryId(parts[0], parts[1]);
	}
	
	public String toSerializedString() {
		return query + "@" + this.timestamp.format(DateTimeFormatter.ISO_DATE_TIME);
	}
	
	@Override
	public String toString() {
		return getName();
	}

	public String getShortName() {
		return AnnotatedTextProvider.shorten(
				getName(), 
				AnnotatedTextProvider.SMALL_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH);
	}
}
