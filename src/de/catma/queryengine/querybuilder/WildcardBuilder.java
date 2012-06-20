package de.catma.queryengine.querybuilder;

public class WildcardBuilder {
	
	public String getWildcardFor(
			String startsWith, String contains, String endsWith, int position) {
		
		if (startsWith == null) {
			startsWith = "";
		}
		if (contains == null) {
			contains = "";
		}
		if (endsWith == null) {
			endsWith = "";
		}
		StringBuilder builder = new StringBuilder();
		
		for (int i=1; i<position; i++) {
			builder.append(" % ");
		}
		if (!startsWith.isEmpty()) {
			builder.append(escape(startsWith));
			builder.append("%");
		}
		if (!contains.isEmpty()) {
			if (startsWith.isEmpty()) {
				builder.append("%");
			}
			builder.append(escape(contains));
			builder.append("%");
		}
		if (!endsWith.isEmpty()) {
			if (contains.isEmpty() && startsWith.isEmpty()) {
				builder.append("%");
			}
			builder.append(escape(endsWith));
		}
		return builder.toString();
	}
	
	private String escape(String input) {
		return input.replace("_", "\\_").replace("%", "\\%");
	}
}
