package de.catma.ui.client.ui.tagger;


public class StackTraceSerializer {
	//TODO: use it
	public static String serialize(Throwable t) {

		StringBuilder stackTrace = new StringBuilder(t.getMessage());
		StackTraceElement[] st = t.getStackTrace();
		for (int i=0; i<st.length; i++) {
			stackTrace.append(st[i].toString());
		}
		
		return stackTrace.toString();
	}
}
