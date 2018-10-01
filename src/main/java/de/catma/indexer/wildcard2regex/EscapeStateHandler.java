package de.catma.indexer.wildcard2regex;

public class EscapeStateHandler implements StateHandler {

	@Override
	public State handleCharacter(char c, Context context) {
		Quote currentQuote = context.getCurrentQuote();
		if (currentQuote == null) {
			currentQuote = new Quote();
			context.setCurrentQuote(currentQuote);
		}
		currentQuote.add(c);

		return State.DEFAULT;
	}
}
