package de.catma.ui.analyzenew.resourcepanelanalyze;

import com.vaadin.data.TreeData;

import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public interface AnalyzeResourceSelectionListener {

	void updateQueryOptions(TreeData<DocumentTreeItem> data);
	

}
