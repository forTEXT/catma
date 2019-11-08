package de.catma.util;

import java.util.Objects;

public class ExceptionUtil {
	public static boolean stackTraceContains(String exceptionClassName, Throwable t) {
		if (t==null) {
			return false;
		}
		return getThrowableFor(exceptionClassName, t) != null;
	}

	public static Throwable getThrowableFor(String exceptionClassName, Throwable t) {
		if(Objects.isNull(t)){
			return null;
		}
		
		Throwable cause = t.getCause();
		
		do {
			if (t.getClass().getName().equals(exceptionClassName)) {
				return t;
			}
			
			t = cause;
			if (t != null) {
				cause = cause.getCause();
			}

		} while ((t != null) && (cause != t));
		
		return null;
	}
	
	public static String getMessageFor(String exceptionClassName, Throwable t) {
		Throwable cause = getThrowableFor(exceptionClassName, t);
		if (cause != null) {
			return cause.getMessage();
		}
		return null;
	}
}
