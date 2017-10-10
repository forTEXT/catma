package de.catma.ui.tagger;

public class TagsetSelectionHandler {
	public static interface TagsetSelectedListener {
		public void tagsetSelected(Object selectedParent);
	}
	
	public void handleSelection(TagsetSelectedListener tagsetSelectedListener) {
		
		
		
		
		// fertig:
		tagsetSelectedListener.tagsetSelected(selectedTagset);
	}
}
