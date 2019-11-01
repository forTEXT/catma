package de.catma.ui.module.analyze.queryresultpanel;

import java.util.function.Supplier;

import com.vaadin.server.SerializablePredicate;

public class PunctuationFilter implements SerializablePredicate<QueryResultRowItem> {
	
	private Supplier<Boolean> isEnabled;

	public PunctuationFilter(Supplier<Boolean> isEnabled) {
		this.isEnabled = isEnabled;
	}

	@Override
	public boolean test(QueryResultRowItem item) {
		if (isEnabled.get()) {
			String key = item.getKey();
			
			if ((key != null) && key.length() == 1) {
				return Character.isLetter(Character.codePointAt(key, 0));
			}
		}
		
		return true;
	}

}
