package de.catma.queryengine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import de.catma.ui.module.annotate.annotationpanel.AnnotatedTextProvider;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QueryId))
			return false;
		QueryId other = (QueryId) obj;
		if (query == null) {
			if (other.query != null)
				return false;
		} else if (!query.equals(other.query))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		return true;
	}
	
	
}
