package de.catma.ui.events;

import de.catma.ui.tagger.TaggerView;

public class TaggerViewSourceDocumentChangedEvent {
	
	private TaggerView taggerView;

	public TaggerViewSourceDocumentChangedEvent(TaggerView taggerView) {
		super();
		this.taggerView = taggerView;
	}
	
	public TaggerView getTaggerView() {
		return taggerView;
	}

}
