package de.catma.indexer.wildcard2regex;

import static de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter.ANY_CHAR_WILDCARD;
import static de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter.ANY_CHAR_WILDCARD_REPLACEMENT_PATTERN;
import static de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter.ESCAPE_CHAR;
import static de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter.SINGLE_CHAR_WILDCARD;
import static de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter.SINGLE_CHAR_WILDCARD_REPLACEMENT_PATTERN;

public class DefaultStateHandler implements StateHandler {

	@Override
	public State handleCharacter(char c, Context context) {
		
		switch (c) {
		case ANY_CHAR_WILDCARD: {
			context.add(new Wildcard(ANY_CHAR_WILDCARD_REPLACEMENT_PATTERN));
			context.setCurrentQuote(new Quote());
			break;
		}
		case SINGLE_CHAR_WILDCARD: {
			context.add(new Wildcard(SINGLE_CHAR_WILDCARD_REPLACEMENT_PATTERN));
			context.setCurrentQuote(new Quote());
			break;
		}
		case ESCAPE_CHAR: {
			return State.ESCAPE;
		}
		default:
			Quote currentQuote = context.getCurrentQuote();
			if (currentQuote == null) {
				currentQuote = new Quote();
				context.setCurrentQuote(currentQuote);
			}
			currentQuote.add(c);
		}

		return State.DEFAULT;
		
	}

}
