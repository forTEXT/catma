package de.catma.ui.analyzer;

import java.util.List;

import de.catma.document.Corpus;

public interface RelevantUserMarkupCollectionProvider {
	List<String> getRelevantUserMarkupCollectionIDs();
	Corpus getCorpus();
}
