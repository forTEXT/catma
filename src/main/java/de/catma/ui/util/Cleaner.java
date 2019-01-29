package de.catma.ui.util;

public class Cleaner {

	public static String clean(String html) {
		if (html == null) {
			return null;
		}
		return html.replaceAll("<", "&lt;").replaceAll(">", "&rt;");
	}
}
