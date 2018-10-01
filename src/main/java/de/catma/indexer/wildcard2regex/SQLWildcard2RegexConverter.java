package de.catma.indexer.wildcard2regex;

public class SQLWildcard2RegexConverter {
	static final char ANY_CHAR_WILDCARD = '%';
	static final char SINGLE_CHAR_WILDCARD = '_';
	static final char ESCAPE_CHAR = '\\';

	static final String SINGLE_CHAR_WILDCARD_REPLACEMENT_PATTERN = ".{1}?";
	static final String ANY_CHAR_WILDCARD_REPLACEMENT_PATTERN = ".*?";
	
	private State currentState = State.DEFAULT;
	
	private SQLWildcard2RegexConverter() {
	}
	
	public static String convert(String str) {
		SQLWildcard2RegexConverter sqlWildcard2RegexConverter = new SQLWildcard2RegexConverter();
		return sqlWildcard2RegexConverter.getAsRegex(str);
	}

	public static boolean containsWildcard(String str) {
		SQLWildcard2RegexConverter sqlWildcard2RegexConverter = new SQLWildcard2RegexConverter();
		return sqlWildcard2RegexConverter.existsWildcard(str);
	}
	
	private boolean existsWildcard(String str) {
		Context context = new Context();
		
		for (char c : str.toCharArray()) {
			currentState = currentState.getHandler().handleCharacter(c, context);
			if (context.hasWildcard()) {
				return true;
			}
		}
		
		return false;
	}
	
	private String getAsRegex(String str) {

		Context context = new Context();
		
		for (char c : str.toCharArray()) {
			currentState = currentState.getHandler().handleCharacter(c, context);
		}
		
		System.out.println(context);
		return context.toString();
	}
	
}
