package de.catma.serialization.tei.pointer;

import de.catma.core.document.Range;


public class CharFragmentIdentifier extends TextFragmentIdentifier {
	
	public static final String SCHEME_NAME = "char";

	public CharFragmentIdentifier(Range range) {
		super();
		setRange(range);
	}

	@Override
	public String getTextSchemeName() {
		return SCHEME_NAME + "=";
	}

}
