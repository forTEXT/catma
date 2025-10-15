package de.catma.ui;

import java.util.Map;

public interface ParameterProvider {
	Map<String, String[]> getParameters();
	String[] getParameters(Parameter parameter);
	String[] getParameters(String key);

	String getParameter(Parameter parameter);
	String getParameter(Parameter parameter, String defaultValue);
	String getParameter(String key);
}
