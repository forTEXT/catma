package de.catma.ui.analyzenew.resourcepanelanalyze;

import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public interface AnalyzeResourceSelectionListener {

	void resourceSelected(SourceDocument sd, boolean selected);
	void resourceSelected(UserMarkupCollectionReference collectionRef, boolean selected);

}
