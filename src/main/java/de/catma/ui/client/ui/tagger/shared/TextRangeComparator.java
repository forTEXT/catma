package de.catma.ui.client.ui.tagger.shared;

import java.util.Comparator;

public class TextRangeComparator implements Comparator<TextRange> {
	@Override
	public int compare(TextRange o1, TextRange o2) {
		return o1.compareTo(o2);
	}
}