package de.catma.ui.util;

public class HtmlEscaper {
	public static String escape(String html) {
		if (html == null) {
			return null;
		}
		return html
				.replaceAll("&", "&amp;")
				.replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;")
				.replaceAll("\"", "&quot;")
				.replaceAll("'", "&#39;");
	}
}
